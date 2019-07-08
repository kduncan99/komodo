/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import java.io.BufferedWriter;
import java.util.Iterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements a word channel module.
 * Designed for connected devices which do IO on long integers, of which the lower 36 bits are significant.
 */
@SuppressWarnings("Duplicates")
public abstract class ByteChannelModule extends ChannelModule {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    protected class ByteTracker extends Tracker {

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
        final ChannelProgram channelProgram
    ) {
        return new ByteTracker(source, ioProcessor, channelProgram);
    }

    /**
     * For debugging purposes
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        //TODO anything else here?
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
                        startIO(tracker);
                        sleepFlag = false;
                    } else if (tracker._completed) {
                        //  IO is done - notify source, and delist it
                        tracker._source.upiHandleInterrupt(tracker._ioProcessor, false);
                        iter.remove();
                        sleepFlag = false;
                    } else {
                        //TODO
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
        //TODO
        tracker._started = true;
    }
}
