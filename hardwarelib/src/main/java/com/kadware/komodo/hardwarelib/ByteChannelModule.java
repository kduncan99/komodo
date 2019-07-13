/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.hardwarelib.exceptions.*;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import java.nio.ByteBuffer;
import java.util.Iterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements a word channel module.
 * Designed for connected devices which do IO on long integers, of which the lower 36 bits are significant.
 */

/*
Forward operation:

Format A
*AAAAAAAA*BBBBBBBB*CCCCCCCC*DDDDDDDD
Format B
AAAAAABBBBBBCCCCCCDDDDDDEEEEEEFFFFFF
Format C
AAAAAAAABBBBBBBBCCCCCCCCDDDDDDDDE(7-4)
E(3-0)FFFFFFFFGGGGGGGGHHHHHHHHIIIIIIII

Backward operation:

Format A
*DDDDDDDD*CCCCCCCC*BBBBBBBB*AAAAAAAA
* If the ninth bit in any Format A is set, the data transfer is terminated (write only).
Format B
FFFFFFEEEEEEDDDDDDCCCCCCBBBBBBAAAAAA
Format C
E(3-0)DDDDDDDDCCCCCCCCBBBBBBBBAAAAAAAA
IIIIIIIIHHHHHHHHGGGGGGGGFFFFFFFFE(7-4)

* If the ninth bit in any Format A is set, the data transfer is terminated (write only).
The letters indicate the order in which bits are transferred - each letter being a frame.

 */

@SuppressWarnings("Duplicates")
public class ByteChannelModule extends ChannelModule {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    protected class ByteTracker extends Tracker {

        private ByteBuffer _byteBuffer = null;

        ByteTracker(
            final Processor source,
            final InputOutputProcessor ioProcessor,
            final ChannelProgram channelProgram
        ) {
            super(source, ioProcessor, channelProgram);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(ByteChannelModule.class);


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    protected ByteChannelModule(
        final String name
    ) {
        super(ChannelModuleType.Byte, name);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Clear list of IO ChannelPrograms.
     * Called during session startup - subclasses may override this if they have any specific work to do.
     * If they do override this, they *must* callback here before returning.
     */
    @Override
    public void clear() {
        super.clear();
    }

    /**
     * To be implemented by the subclass - we call this object and queue it up
     */
    @Override
    public Tracker createTracker(
        final Processor source,
        final InputOutputProcessor ioProcessor,
        final ChannelProgram channelProgram
    ) {
        return new ByteTracker(source, ioProcessor, channelProgram);
    }

    /**
     * Worker interface implementation
     * @return our node name
     */
    @Override
    public String getWorkerName(
    ) {
        return _name;
    }

    /**
     * Thread
     */
    public void run() {
        LOGGER.info("worker starting for " + _name);

        while (!_workerTerminate) {
            boolean sleepFlag = true;
            synchronized (_workerThread) {
                Iterator<Tracker> iter = _trackers.iterator();
                while (iter.hasNext()) {
                    ByteTracker tracker = (ByteTracker) iter.next();
                    if (!tracker._started) {
                        //  IO has not been started
                        startIO(tracker);
                        sleepFlag = false;
                    } else if (tracker._completed) {
                        //  CM IO is done - notify source, and delist it
                        tracker._source.upiHandleInterrupt(tracker._ioProcessor, false);
                        iter.remove();
                        sleepFlag = false;
                    } else if (tracker._ioInfo._status != DeviceStatus.InProgress) {
                        //  Dev IO is done - need to possibly move some data around and set some status
                        if (tracker._ioInfo._status == DeviceStatus.Successful) {
                            //  device io was successful.  Do we need to move input data into storage?
                            if (tracker._channelProgram.getFunction().isReadFunction()) {
                                unpackByteBuffer(tracker);
                            }

                            tracker._channelProgram.setDeviceStatus(DeviceStatus.Successful);
                            tracker._channelProgram.setChannelStatus(ChannelStatus.Successful);
                        } else {
                            //  device io was unsuccessful.
                            tracker._channelProgram.setDeviceStatus(tracker._ioInfo._status);
                            tracker._channelProgram.setChannelStatus(ChannelStatus.DeviceError);
                        }
                        sleepFlag = false;
                    }
                }
            }

            if (sleepFlag) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    LOGGER.catching(ex);
                }
            }
        }

        LOGGER.info("worker stopping for " + _name);
    }

    /**
     * Creates an output byte buffer for writing to the device,
     * given the content of the cumulative word buffer and the controlling format specification.
     */
    private void packByteBuffer(
        final ByteTracker tracker
    ) {
        ChannelProgram cp = tracker._channelProgram;
        switch (cp.getByteTranslationFormat()) {
            case QuarterWordPerByte:                //  Format A quarter word -> frame
            case QuarterWordPerByteNoTermination:   //  Format D quarter word -> frame
            {
                //  Format A might stop short of the end of the word buffer based on bit 9 of any quarter word being set.
                //  We create a byte[] of the longest length we'll need, but we might truncate it due to stopping short.
                byte[] byteData = new byte[tracker._contiguousBuffer._length * 4];
                int byteCount = tracker._contiguousBuffer.packQuarterWords(byteData);
                tracker._byteBuffer = ByteBuffer.wrap(byteData, 0, byteCount);
                break;
            }

            case SixthWordByte:                     //  Format B sixth word -> frame
            {
                byte[] byteData = new byte[tracker._contiguousBuffer._length * 6];
                int byteCount = tracker._contiguousBuffer.packSixthWords(byteData);
                tracker._byteBuffer = ByteBuffer.wrap(byteData, 0, byteCount);
                break;
            }

            case QuarterWordPerPacked:              //  Format C two words -> 9 frames
                if (tracker._contiguousBuffer._length % 2 == 0) {
                    //  even number of words to be translated - nice and neat.
                    byte[] byteData = new byte[tracker._contiguousBuffer._length * 9 / 2];
                    tracker._contiguousBuffer.pack(byteData);
                } else {
                    //  odd number of words.  messy, but doable.
                    byte[] byteData = new byte[(tracker._contiguousBuffer._length * 9 / 2) + 1];
                    tracker._contiguousBuffer.pack(byteData);
                    tracker._byteBuffer = ByteBuffer.wrap(byteData);
                }
                break;
        }
    }

    /**
     * Transitions a tracker to the started state
     */
    private void startIO(
        final ByteTracker tracker
    ) {
        ChannelProgram cp = tracker._channelProgram;

        //  If this is a write operation, allocate and populate a contiguous word buffer,
        //  then translate it according to the translation mode.
        if (cp.getFunction().isWriteFunction()) {
            try {
                tracker._contiguousBuffer = cp.createContiguousBuffer();
                packByteBuffer(tracker);
            } catch (AddressingExceptionInterrupt
                | UPINotAssignedException
                | UPIProcessorTypeException ex) {
                cp.setChannelStatus(ChannelStatus.InvalidAddress);
                tracker._started = true;
                tracker._completed = true;
                return;
            }
        }

        if (cp.getFunction().isWriteFunction()) {
            tracker._ioInfo = new DeviceIOInfo.ByteTransferBuilder().setSource(this)
                                                                    .setIOFunction(cp.getFunction())
                                                                    .setBlockId(cp.getBlockId().getValue())
                                                                    .setTransferCount(tracker._byteBuffer.array().length)
                                                                    .setBuffer(tracker._byteBuffer)
                                                                    .build();
        } else if (cp.getFunction().isReadFunction()) {
            tracker._ioInfo = new DeviceIOInfo.ByteTransferBuilder().setSource(this)
                                                                    .setIOFunction(cp.getFunction())
                                                                    .setBlockId(cp.getDeviceAddress())
                                                                    .setTransferCount(tracker._byteBuffer.array().length)
                                                                    .build();
        } else {
            tracker._ioInfo = new DeviceIOInfo.NonTransferBuilder().setSource(this)
                                                                   .setIOFunction(cp.getFunction())
                                                                   .build();
        }

        Device device = (Device) _descendants.get(cp.getDeviceAddress());
        device.ioStart(tracker._ioInfo);
        tracker._started = true;
    }

    /**
     * Moves data in the appropriate format, into a temporary ArraySlice (word buffer) from where it will
     * subsequently be distributed among the various ACW buffers (not by us, but...)
     */
    private void unpackByteBuffer(
        final ByteTracker tracker
    ) {
        ChannelProgram cp = tracker._channelProgram;
        int bufferWords = cp.getAccessControlWordCount();
        tracker._contiguousBuffer = new ArraySlice(new long[bufferWords]);

        switch (cp.getByteTranslationFormat()) {
            case QuarterWordPerByte:                //  Format A
            case QuarterWordPerByteNoTermination:   //  Format D
            {
                int frames = tracker._contiguousBuffer.unpackQuarterWords(tracker._byteBuffer.array(),
                                                                          0,
                                                                          tracker._byteBuffer.position());
                int fullWords = frames / 4;
                int residualBytes = frames % 4;
                cp.setResidualBytes(residualBytes);
                cp.setWordsTransferred(fullWords + ((residualBytes > 0) ? 1 : 0));
                break;
            }

            case SixthWordByte:                     //  Format B
            {
                int frames = tracker._contiguousBuffer.unpackSixthWords(tracker._byteBuffer.array(),
                                                                        0,
                                                                        tracker._byteBuffer.position());
                int fullWords = frames / 6;
                int residualBytes = frames % 6;
                cp.setResidualBytes(residualBytes);
                cp.setWordsTransferred(fullWords + ((residualBytes > 0) ? 1 : 0));
                break;
            }

            case QuarterWordPerPacked:              //  Format C
            {
                int bytesRead = tracker._byteBuffer.position();
                int wordsRead = tracker._contiguousBuffer.unpack(tracker._byteBuffer.array(),
                                                                 0,
                                                                 bytesRead);
                if ((wordsRead & 01) != 0) {
                    //  Odd number of words read - we might need to do abnormal frame count stuff.
                    //  We only provide that if the number of words in the ACW buffers exceeds the number transferred.
                    if (cp.getAccessControlWordCount() > wordsRead) {
                        cp.setResidualBytes(5);
                    }
                }
                cp.setWordsTransferred(wordsRead);
                break;
            }
        }
    }
}
