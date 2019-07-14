/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.hardwarelib.exceptions.*;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import java.io.BufferedWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class which models an IO Processor node.
 * An IO is presented to an IO processor in one of two ways:
 *      1) A UPI is sent to the IOP with the absolute address of an IO packet.  TODO
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
        super(ProcessorType.InputOutputProcessor, name, upi);
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
     * Nop override
     */
    @Override
    public void initialize() {}

    /**
     * ChanneProgram calls here to notify us that it is finished with a channel program.
     * If it is a successful read, and if we created an interim word buffer, we need to populate
     * the ACW buffers from the interim buffer.
     * In any case, notify an IP via broadcast UPI interrupt that we are done.
     * @param channelProgram the channel program of interest
     * @param source the Processor which initiated the channel program (an IP or an SP)
     */
    public void finalizeIo(
        final ChannelProgram channelProgram,
        final Processor source
    ) {
        if (channelProgram.getFunction().isReadFunction()) {
            int acwCount = channelProgram.getAccessControlWordCount();
            if ((acwCount == 1)
                && (channelProgram.getAccessControlWord(0).getAddressModifier() == AccessControlWord.AddressModifier.Increment)) {
                //  composite buffer points to storage - nothing to do here
            } else if (acwCount == 0) {
                //  No buffers - again, nothing to do here
            } else {
                //  The composite buffer is interim - distribute the results from the read accordingly
                int wordToCopy = channelProgram.getWordsTransferred();
                for (int acwx = 0; acwx < channelProgram.getAccessControlWordCount(); ++acwx) {
                    AccessControlWord acw = channelProgram.getAccessControlWord(acwx);

                }
            }
        }

        switch (source._processorType) {
            case InstructionProcessor:
                //TODO
                break;

            case SystemProcessor:
                //TODO
                break;
        }
    }

    /**
     * Starts an IO as defined by the channel program at the given absolute address
     * @return true if the IO was scheduled, false if something immediately wrong was discovered
     */
    public boolean startIO(
        final Processor source,
        final ChannelProgram channelProgram
    ) {
        ChannelModule channelModule = (ChannelModule) _descendants.get(channelProgram.getChannelModuleIndex());
        if (channelModule == null) {
            channelProgram.setChannelStatus(ChannelStatus.UnconfiguredChannelModule);
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
                                                mspStorage._offset + bufferAddress._offset + acw._offset,
                                                bufferSize);
                }
            } else if (acwCount > 0) {
                int bufferWords = channelProgram.getCumulativeTransferWords();
                wordBuffer = new ArraySlice(new long[bufferWords]);

                if (channelProgram.getFunction().isWriteFunction()) {
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

                        for (int sc = 0, sx = mspStorage._offset + bufferAddress._offset + acw._offset, dx = 0;
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
            channelProgram.setChannelStatus(ChannelStatus.InvalidAddress);
            return false;
        }
    }

    /**
     * Invoked just before tearing down the configuration.
     */
    @Override
    public void terminate() {}

    /**
     * Handle UPI interrupt.  We should get these only from clients which are requesting IOs.
     * @param source processor initiating the interrupt
     * @param broadcast true if this is a broadcast
     */
    @Override
    public void upiHandleInterrupt(
        final Processor source,
        final boolean broadcast
    ) {
        if ((source._processorType == ProcessorType.InstructionProcessor)
            || (source._processorType == ProcessorType.SystemProcessor)) {
            //  Someone wants to do an IO.
            //TODO
        } else {
            //  This shouldn't happen.
            LOGGER.error(String.format("%s received a %s interrupt from %s",
                                       _name,
                                       broadcast ? "broadcast" : "directed",
                                       source._name));
        }
    }
}
