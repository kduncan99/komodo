/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import java.io.BufferedWriter;
import java.util.Iterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements a word channel module.
 * Designed for connected devices which do IO on long integers, of which the lower 36 bits are significant.
 */
@SuppressWarnings("Duplicates")
public class WordChannelModule extends ChannelModule {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    protected class WordTracker extends Tracker {

        private ArraySlice _workingBuffer = null;

        WordTracker(
            final Processor source,
            final InputOutputProcessor ioProcessor,
            final ChannelProgram channelProgram,
            final ArraySlice compositeBuffer
        ) {
            super(source, ioProcessor, channelProgram, compositeBuffer);
            int bufferSize = channelProgram.getCumulativeTransferWords();
            if (bufferSize > 0) {
                _workingBuffer = new ArraySlice(new long[bufferSize]);
            }
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(WordChannelModule.class);


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    protected WordChannelModule(
        final String name
    ) {
        super(ChannelModuleType.Word, name);
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
        final ChannelProgram channelProgram,
        final ArraySlice compositeBuffer
    ) {
        return new WordTracker(source, ioProcessor, channelProgram, compositeBuffer);
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
                    WordTracker tracker = (WordTracker) iter.next();
                    if (!tracker._started) {
                        startIO(tracker);
                        sleepFlag = false;
                    } else if (tracker._completed) {
                        //  IO is done - notify source, and delist it
                        tracker._source.upiHandleInterrupt(tracker._ioProcessor, false);
                        iter.remove();
                        sleepFlag = false;
                    } else {
                        //  tracker is started and not completed.  Is the device IO complete?
                        if (tracker._ioInfo._status != DeviceStatus.InProgress) {
                            //TODO
                            sleepFlag = false;
                        }
                    }
                }
            }

            if (sleepFlag) {
                synchronized (_workerThread) {
                    try {
                        _workerThread.wait(100);
                    } catch (InterruptedException ex) {
                        LOGGER.catching(ex);
                    }
                }
            }
        }

        LOGGER.info("worker stopping for " + _name);
    }

    /**
     * Transitions a tracker to the started state
     */
    private void startIO(
        final WordTracker tracker
    ) {
        ChannelProgram cp = tracker._channelProgram;
        if (tracker._workingBuffer == null) {
            tracker._ioInfo = new DeviceIOInfo.NonTransferBuilder().setSource(this)
                                                                   .setIOFunction(cp.getFunction())
                                                                   .build();
            Device device = (Device) _descendants.get(cp.getDeviceAddress());
            device.ioStart(tracker._ioInfo);
        } else {
//            long blockId = cp.getBlockAddress() == null ? 0 : cp.getBlockAddress().getValue();
//            tracker._ioInfo = new DeviceIOInfo.WordTransferBuilder().setSource(this)
//                                                                    .setIOFunction(cp.getFunction())
//                                                                    .setTransferCount(cp.getWordsRequested())
//                                                                    .setBuffer()
//                                                                    .setBlockId(blockId)
//                                                                    .build();
        }

        tracker._started = true;
    }
}
