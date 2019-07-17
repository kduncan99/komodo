/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
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

        ByteTracker(
            final Processor source,
            final InputOutputProcessor ioProcessor,
            final ChannelProgram channelProgram,
            final ArraySlice compositeBuffer
        ) {
            super(source, ioProcessor, channelProgram, compositeBuffer);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(ByteChannelModule.class);


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    ByteChannelModule(
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
     * Calculates the number of bytes that can be put into the composite buffer
     */
    private static int calculateByteCount(
        final ByteTracker tracker
    ) {
        int words = tracker._compositeBuffer.getSize();
        switch (tracker._channelProgram.getByteTranslationFormat()) {
            case QuarterWordPerByte:
            case QuarterWordPerByteNoTermination:
                return words * 4;

            case SixthWordByte:
                return words * 6;

            case QuarterWordPacked:
                int units = words / 2;
                int halfUnits = words % 2;
                return units * 9 + halfUnits * 5;
        }

        return 0;
    }

    /**
     * To be implemented by the subclass - we call this object and queue it up
     */
    @Override
    public Tracker createTracker(
        final Processor source,
        final InputOutputProcessor ioProcessor,
        final ChannelProgram channelProgram,
        final ArraySlice compositeBuffer
    ) {
        return new ByteTracker(source, ioProcessor, channelProgram, compositeBuffer);
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
     * For unit tests
     */
    boolean isWorkerActive() { return _workerThread.isAlive(); }

    /**
     * Creates an output ByteBuffer for writing to the device,
     * given the content of the cumulative word buffer and the controlling format specification.
     * Call here only for write functions which have buffers (e.g., not for writing end-of-file marks)
     */
    private static byte[] packByteBuffer(
        final ByteTracker tracker
    ) {
        ChannelProgram cp = tracker._channelProgram;
        byte[] result = null;
        switch (cp.getByteTranslationFormat()) {
            case QuarterWordPerByte:                //  Format A quarter word -> frame
            case QuarterWordPerByteNoTermination:   //  Format D quarter word -> frame
            {
                //  Format A might stop short of the end of the word buffer based on bit 9 of any quarter word being set.
                //  We create a byte[] of the longest length we'll need, but we might truncate it due to stopping short.
                result = new byte[tracker._compositeBuffer._length * 4];
                tracker._compositeBuffer.packQuarterWords(result);
                cp.setWordsTransferred(tracker._compositeBuffer._length);
                break;
            }

            case SixthWordByte:                     //  Format B sixth word -> frame
            {
                result = new byte[tracker._compositeBuffer._length * 6];
                tracker._compositeBuffer.packSixthWords(result);
                cp.setWordsTransferred(tracker._compositeBuffer._length);
                break;
            }

            case QuarterWordPacked:              //  Format C two words -> 9 frames
            {
                int bytes = tracker._compositeBuffer._length * 9 / 2;
                if (tracker._compositeBuffer._length % 2 > 0) {
                    ++bytes;
                }
                result = new byte[bytes];
                tracker._compositeBuffer.pack(result);
                cp.setWordsTransferred(tracker._compositeBuffer._length);
                break;
            }
        }

        return result;
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
                        tracker._ioProcessor.finalizeIo(tracker._channelProgram, tracker._source);
                        iter.remove();
                        sleepFlag = false;
                    } else if (tracker._ioInfo._status != DeviceStatus.InProgress) {
                        //  Dev IO is done - need to possibly move some data around and set some status
                        if (tracker._ioInfo._status == DeviceStatus.Successful) {
                            //  device io was successful.  Do we need to move input data into storage?
                            IOFunction func = tracker._channelProgram.getFunction();
                            boolean truncation = false;
                            if (func.isReadFunction() && func.requiresBuffer()) {
                                truncation = unpackByteBuffer(tracker);
                            }

                            tracker._channelProgram.setChannelStatus(ChannelStatus.Successful);
                            if (truncation) {
                                tracker._channelProgram.setChannelStatus(ChannelStatus.InsufficientBuffers);
                            } else {
                                tracker._channelProgram.setDeviceStatus(DeviceStatus.Successful);
                            }
                        } else {
                            //  device io was unsuccessful.
                            tracker._channelProgram.setDeviceStatus(tracker._ioInfo._status);
                            tracker._channelProgram.setChannelStatus(ChannelStatus.DeviceError);
                        }
                        tracker._completed = true;
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
     * Transitions a tracker to the started state
     */
    private void startIO(
        final ByteTracker tracker
    ) {
        ChannelProgram cp = tracker._channelProgram;

        IOFunction func = cp.getFunction();
        if (func.isWriteFunction() && func.requiresBuffer()) {
            byte[] buffer = (tracker._compositeBuffer != null) ? packByteBuffer(tracker) : null;
            int bytes = (tracker._compositeBuffer != null) ? calculateByteCount(tracker) : 0;
            tracker._ioInfo = new DeviceIOInfo.ByteTransferBuilder().setSource(this)
                                                                    .setIOFunction(cp.getFunction())
                                                                    .setBlockId(cp.getBlockId().getValue())
                                                                    .setTransferCount(bytes)
                                                                    .setBuffer(buffer)
                                                                    .build();
        } else if (func.isReadFunction() && func.requiresBuffer()) {
            int bytes = (tracker._compositeBuffer != null) ? calculateByteCount(tracker) : 0;
            tracker._ioInfo = new DeviceIOInfo.ByteTransferBuilder().setSource(this)
                                                                    .setIOFunction(cp.getFunction())
                                                                    .setBlockId(cp.getBlockId().getValue())
                                                                    .setTransferCount(bytes)
                                                                    .build();
        } else {
            tracker._ioInfo = new DeviceIOInfo.NonTransferBuilder().setSource(this)
                                                                   .setIOFunction(cp.getFunction())
                                                                   .build();
        }

        Device device = (Device) _descendants.get(cp.getDeviceAddress());
        device.handleIo(tracker._ioInfo);
        tracker._started = true;
    }

    /**
     * Moves data in the appropriate format, into the composite word buffer.
     * Call here only for functions which read, and which have a buffer (that would be all of them, maybe?)
     * @return true if there are unprocessed bytes in the source buffer - i.e., indicating truncation
     */
    private boolean unpackByteBuffer(
        final ByteTracker tracker
    ) {
        ChannelProgram cp = tracker._channelProgram;
        switch (cp.getByteTranslationFormat()) {
            case QuarterWordPerByte:                //  Format A
            case QuarterWordPerByteNoTermination:   //  Format D
            {
                int frames = tracker._compositeBuffer.unpackQuarterWords(tracker._ioInfo._byteBuffer,
                                                                         0,
                                                                         tracker._ioInfo._transferredCount);
                int fullWords = frames / 4;
                int residualBytes = frames % 4;
                cp.setResidualBytes(residualBytes);
                cp.setWordsTransferred(fullWords + ((residualBytes > 0) ? 1 : 0));
                return frames < tracker._ioInfo._transferredCount;
            }

            case SixthWordByte:                     //  Format B
            {
                int frames = tracker._compositeBuffer.unpackSixthWords(tracker._ioInfo._byteBuffer,
                                                                       0,
                                                                       tracker._ioInfo._transferredCount);
                int fullWords = frames / 6;
                int residualBytes = frames % 6;
                cp.setResidualBytes(residualBytes);
                cp.setWordsTransferred(fullWords + ((residualBytes > 0) ? 1 : 0));
                return frames < tracker._ioInfo._transferredCount;
            }

            case QuarterWordPacked:              //  Format C
            {
                int bytesRead = tracker._ioInfo._transferCount;
                int wordsRequired = bytesRead * 2 / 9;
                wordsRequired += (bytesRead * 2) % 9 == 0 ? 0 : 1;
                int wordsRead = tracker._compositeBuffer.unpack(tracker._ioInfo._byteBuffer);
                if ((wordsRead & 01) != 0) {
                    //  Odd number of words read - we might need to do abnormal frame count stuff.
                    //  We only provide that if the number of words in the ACW buffers exceeds the number transferred.
                    if (cp.getAccessControlWordCount() > wordsRead) {
                        cp.setResidualBytes(5);
                    }
                }
                cp.setWordsTransferred(wordsRead);
                return wordsRequired > wordsRead;
            }
        }

        return false;   //  stupid compiler
    }
}
