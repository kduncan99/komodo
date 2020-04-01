/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.hardwarelib.exceptions.*;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import java.io.BufferedWriter;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class which models an IO Processor node.
 * An IO is presented to an IO processor in one of two ways:
 *      1) A UPI is sent to the IOP with the absolute address of an IO packet.
 *      2) startIO() is invoked, with the absolute address of an IO packet as a parameter.
 * In either case, the IO is routed directly to the channel module object in question.
 * If the channel module index is wrong, the IO is rejected immediately.
 * The channel module may also reject the IO immediately, for any number of reasons including a bad device number.
 * If the IO can be scheduled, it is scheduled on the channel module's IO queue, and the status of the given
 * channel program (see below) is set to in-progress.
 * At some point, the channel module picks up the IO and does various things with it (hopefully sending it on
 * to the targeted device).
 * When the IO is complete, the device notifies the channel module which then notifies the IOP.
 * The IOP will then either SEND to the processor complex resulting in one IP getting a UPI NORMAL interrupt,
 * or it will set up the interrupt directly on an IP.
 * When the IP interrupt is scheduled, the SEND is ACK'd, and the IOP forgets the IO.
 */
@SuppressWarnings("Duplicates")
public class InputOutputProcessor extends Processor {

    private static final Logger LOGGER = LogManager.getLogger(InputOutputProcessor.class);

    /**
     * Constructor
     */
    public InputOutputProcessor(
        final String name,
        final int upi
    ) {
        super(Type.InputOutputProcessor, name, upi);
    }

    /**
     * We are top-level - there are no ancestors for us
     */
    @Override
    public boolean canConnect(Node ancestor) { return false; }

    /**
     * Nothing to do here - all clearing happens in our subordinate channel modules
     */
    @Override
    public void clear() {
        super.clear();
    }

    /**
     * For debugging purposes
     */
    @Override
    public void dump(BufferedWriter writer) { super.dump(writer); }

    /**
     * ChanneProgram calls here to notify us that it is finished with a channel program.
     * If it is a successful read, and if we created an interim word buffer, we need to populate
     * the ACW buffers from the interim buffer.
     * In any case, notify an IP via broadcast UPI interrupt that we are done.
     * @param channelProgram the channel program of interest
     * @param source the Processor which initiated the channel program (an IP or an SP)
     */
    public void finalizeIo(
        final ChannelModule.ChannelProgram channelProgram,
        final Processor source
    ) {
        if (channelProgram.getFunction().isReadFunction()) {
            int acwCount = channelProgram.getAccessControlWordCount();
            //  If acwCount is zero, there are no buffers and no copy is needed.
            //  If one, and if we are strictly incrementing, the buffer points directly to storage, and again no copy is needed.
            //  If more than one, or if we are not strictly incrementing, we need to copy data from the temporary buffer to storage.
            if (acwCount > 0) {
                AccessControlWord.AddressModifier mod = channelProgram.getAccessControlWord(0).getAddressModifier();
                if ((acwCount > 1) || (mod != AccessControlWord.AddressModifier.Increment)) {
                    //  The composite buffer is interim - distribute the results from the read accordingly
                    int wordsToCopy = channelProgram.getWordsTransferred();
                    for (int acwx = 0; acwx < channelProgram.getAccessControlWordCount(); ++acwx) {
                        AccessControlWord acw = channelProgram.getAccessControlWord(acwx);
                        //TODO
                    }
                }
            }
        }

        switch (source._Type) {
            case InstructionProcessor:
                //  The IO was initiated by an IP - send a broadcast.
                //  An available IP will pick it up, will know it is a signal of a completed IO,
                //  and will do something pretty smart.  This *might* entail looping through all known
                //  outstanding channel programs to find the one (or possibly more) which are no longer
                //  in progress.
                try {
                    upiSendBroadcast();
                } catch (InvalidSystemConfigurationException ex) {
                    //  I don't think this can happen - if the system config is invalid,
                    //  the OS can't be running and thus the IP cannot send us an interrupt.
                    LOGGER.catching(ex);
                }
                break;

            case SystemProcessor:
                //  The IO was initiated by an SP - send a directed interrupt to that SP.
                //  The SP knows that one of the IOs it initiated is complete - it is up to the SP
                //  to work out what to do from there.
                try {
                    upiSendDirected(source._upiIndex);
                } catch (UPINotAssignedException ex) {
                    //  This also cannot happen - if we got an interrupt from an SP, that SP obviously exists.
                    LOGGER.catching(ex);
                }
                break;
        }
    }

    /**
     * IOP thread - we pick up UPI traffic and handle it appropriately
     */
    @Override
    public void run() {
        LOGGER.info(_name + " worker thread starting");

        while (!_workerTerminate) {
            boolean waitFlag = true;

            //  We don't care about ACKs, they're not relevant in the architecture for IOPs.
            synchronized (_upiPendingAcknowledgements) {
                _upiPendingAcknowledgements.clear();
            }

            //  We DO care about SENDs - they indicate that an IO should be scheduled.
            //  Use the communication area to retrieve a two-word absolute address.
            //  This is the address of a channel program in storage.
            //  Construct a ChannelProgram object around that bit of storage, and start an IO.
            List<Processor> ackList = new LinkedList<>();
            int broadcastCount = 0;
            List<Processor> sendList = new LinkedList<>();

            synchronized (_upiPendingInterrupts) {
                waitFlag = _upiPendingInterrupts.isEmpty();
                for (Processor source : _upiPendingInterrupts) {
                    try {
                        AbsoluteAddress addr = _upiCommunicationLookup.get(new UPIIndexPair(source._upiIndex, this._upiIndex));
                        MainStorageProcessor msp = InventoryManager.getInstance().getMainStorageProcessor(addr._upiIndex);
                        ArraySlice mspStorage = msp.getStorage(addr._segment);
                        ChannelModule.ChannelProgram channelProgram = ChannelModule.ChannelProgram.create(mspStorage, addr._offset);
                        boolean started = startIO(source, channelProgram);
                        if (!started) {
                            if (source._Type == Type.SystemProcessor) {
                                sendList.add(source);
                            } else {
                                ++broadcastCount;
                            }
                        }
                        waitFlag = false;
                        ackList.add(source);
                    } catch (AddressingExceptionInterrupt
                        |  UPINotAssignedException
                        | UPIProcessorTypeException ex) {
                        //  Shouldn't be possible
                        LOGGER.catching(ex);
                    }
                }
            }

            //  ACK all the SENDs we processed from the various IPs and the SP as appropriate.
            //  We don't worry about whether the ACK went through - the source should never have more than
            //  one SEND to us outstanding, and thus cannot have an unhandled ACK from us at this point.
            for (Processor ackDest : ackList) {
                try {
                    if (!upiAcknowledge(ackDest._upiIndex)) {
                        LOGGER.error(String.format("ACK from %s to %s was rejected", _name, ackDest._name));
                    }
                } catch (UPINotAssignedException ex) {
                    //  terribly wrong...  can't fix it, just move on.
                    LOGGER.catching(ex);
                }
            }

            //  Now SEND a broadcast for each channel program which ended up *not* scheduled, to notify the OS
            //  that the IO is already done or dead.  It is conceivable (though not likely) that there is already
            //  a SEND pending from us to the particular chosen IP - if so, we just wait briefly, then try again.
            for (int bx = 0; bx < broadcastCount; ++bx) {
                try {
                    while (!upiSendBroadcast()) {
                        try {
                            synchronized (this) { wait(10); }
                        } catch (InterruptedException ex) {
                            LOGGER.catching(ex);
                        }
                    }
                } catch (InvalidSystemConfigurationException ex) {
                    //  this should never happen
                    LOGGER.catching(ex);
                }
            }

            //  SEND a directed interrupt for each channel program not scheduled, invoked by an SP.
            //  Again, the SP might have an unhandled SEND from us for a previous IO, so we retry if necessary.
            for (Processor sp : sendList) {
                try {
                    while (!upiSendDirected(sp._upiIndex)) {
                        try {
                            synchronized (this) { wait(10); }
                        } catch (InterruptedException ex) {
                            LOGGER.catching(ex);
                        }
                    }
                } catch (UPINotAssignedException ex) {
                    //  this should never happen
                    LOGGER.catching(ex);
                }
            }

            //  If we didn't do anything, sleep for a bit
            if (waitFlag) {
                try {
                    synchronized (this) { wait(100); }
                } catch (InterruptedException ex) {
                    LOGGER.catching(ex);
                }
            }
        }

        LOGGER.info(_name + " worker thread terminating");
    }

    /**
     * Starts an IO as defined by the channel program at the given absolute address
     * @return true if the IO was scheduled, false if something immediately wrong was discovered
     */
    public boolean startIO(
        final Processor source,
        final ChannelModule.ChannelProgram channelProgram
    ) {
        int cmIndex = channelProgram.getChannelModuleIndex();
        ChannelModule channelModule = (ChannelModule) _descendants.get(cmIndex);
        if (channelModule == null) {
            channelProgram.setChannelStatus(ChannelModule.ChannelStatus.UnconfiguredChannelModule);
            return false;
        }

        //  If there is exactly one ACW and it is incrementing, construct an absolute address to
        //  describe the buffer and stuff it in the channel program.
        //  If more than one, build a consilidated buffer the size of the various individual ACW buffers,
        //  and if this is a write function, populate the buffer from the component ACW buffers.
        try {
            ArraySlice wordBuffer = null;
            int acwCount = channelProgram.getAccessControlWordCount();
            if ((acwCount == 1)
                && (channelProgram.getAccessControlWord(0).getAddressModifier() == AccessControlWord.AddressModifier.Increment)) {
                AccessControlWord acw = channelProgram.getAccessControlWord(0);
                if (acw.getAddressModifier() == AccessControlWord.AddressModifier.Increment) {
                    AbsoluteAddress bufferAddress = acw.getBufferAddress();
                    int bufferSize = acw.getBufferSize();
                    MainStorageProcessor msp = InventoryManager.getInstance().getMainStorageProcessor(bufferAddress._upiIndex);
                    ArraySlice mspStorage = msp.getStorage(bufferAddress._segment);
                    wordBuffer = new ArraySlice(mspStorage._array,
                                                mspStorage._offset + bufferAddress._offset,
                                                bufferSize);
                }
            } else if (acwCount > 0) {
                int bufferWords = channelProgram.getCumulativeTransferWords();
                wordBuffer = new ArraySlice(new long[bufferWords]);

                if (channelProgram.getFunction().isWriteFunction()) {
                    int dx = 0;
                    for (int acwx = 0; acwx < acwCount; ++acwx) {
                        AccessControlWord acw = channelProgram.getAccessControlWord(acwx);
                        int inc = 0;
                        if (acw.getAddressModifier() == AccessControlWord.AddressModifier.Increment) {
                            inc = 1;
                        } else if (acw.getAddressModifier() == AccessControlWord.AddressModifier.Decrement) {
                            inc = 2;
                        }

                        AbsoluteAddress bufferAddress = acw.getBufferAddress();
                        int bufferSize = acw.getBufferSize();
                        MainStorageProcessor msp = InventoryManager.getInstance().getMainStorageProcessor(bufferAddress._upiIndex);
                        ArraySlice mspStorage = msp.getStorage(bufferAddress._segment);

                        for (int sc = 0, sx = mspStorage._offset + bufferAddress._offset;
                             sc < bufferSize;
                             ++sc, ++dx, sx += inc) {
                            wordBuffer._array[dx] = mspStorage._array[sx];
                        }
                    }
                }
            }

            return channelModule.scheduleChannelProgram(source, this, channelProgram, wordBuffer);
        } catch (AddressingExceptionInterrupt
            | UPINotAssignedException
            | UPIProcessorTypeException ex) {
            channelProgram.setChannelStatus(ChannelModule.ChannelStatus.InvalidAddress);
            return false;
        }
    }
}
