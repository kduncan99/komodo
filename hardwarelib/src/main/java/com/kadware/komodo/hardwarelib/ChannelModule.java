/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.Worker;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class which models a channel module node
 *
 * ChannelModule (CM) nodes are managed lightly by InputOutputProcessor (IOP) nodes.
 * Each ChannelModule is active - that is, it is managed by a running thread.
 * When an IO is presented to our parent IOP, it is briefly validated, then it is presented to us
 * so that we can place it on the unstarted IO list.
 * Later, we pull it from that list and begin working the IO.
 * When the IO is complete, we notify our parent IOP, which then takes appropriate action.
 */
@SuppressWarnings("Duplicates")
public abstract class ChannelModule extends Node implements Worker {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * For tracking in-flight IOs - this base class should be extended by the ChannleModule subclass.
     */
    protected abstract class Tracker {
        public final Processor _source;
        public final InputOutputProcessor _ioProcessor;
        public final ChannelProgram _channelProgram;
        public DeviceIOInfo _ioInfo = null;
        public boolean _started = false;
        public boolean _completed = false;

        protected Tracker(
            final Processor source,
            final InputOutputProcessor ioProcessor,
            final ChannelProgram channelProgram
        ) {
            _source = source;
            _ioProcessor = ioProcessor;
            _channelProgram = channelProgram;
        }

        public void dump(
            final BufferedWriter writer
        ) {
            try {
                writer.write(String.format("    Src:%s IOP:%s Started:%s Completed:%s Dev:%d Func:%s\n",
                                           _source._name,
                                           _ioProcessor._name,
                                           String.valueOf(_started),
                                           String.valueOf(_completed),
                                           _channelProgram.getDeviceAddress(),
                                           _channelProgram.getFunction().toString()));
            } catch (IOException ex) {
                LOGGER.catching(ex);
            }
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(ChannelModule.class);

    public final ChannelModuleType _channelModuleType;

    /**
     * Thread for the active (or now-terminated) worker for this object
     */
    protected final Thread _workerThread;

    /**
     * Set this flag so the worker thread can self-terminate
     */
    protected boolean _workerTerminate;

    /**
     * List of in-flight IOs
     */
    protected final List<Tracker> _trackers = new LinkedList<>();


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    protected ChannelModule(
        final ChannelModuleType channelModuleType,
        final String name
    ) {
        super(NodeCategory.ChannelModule, name);
        _channelModuleType = channelModuleType;
        _workerThread = new Thread(this);
        _workerTerminate = false;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * To be implemented by the subclass - we call this object and queue it up
     */
    protected abstract Tracker createTracker(
        final Processor source,
        final InputOutputProcessor ioProcessor,
        final ChannelProgram channelProgram
    );

    /**
     * To be implemented by the subclass - this is the worker thread entry point.
     */
    @Override
    public abstract void run();


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks to see whether this node is a valid descendant of the candidate node
     */
    @Override
    public boolean canConnect(
        final Node ancestor
    ) {
        //  We can connect only to IOProcessors
        return ancestor instanceof InputOutputProcessor;
    }

    /**
     * Clear list of IO ChannelPrograms.
     * Called during session startup - subclasses may override this if they have any specific work to do.
     * If they do override this, they *must* callback here before returning.
     */
    @Override
    public void clear() {
        _trackers.clear();
    }

    /**
     * For debugging purposes
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        try {
            super.dump(writer);
            writer.write("  Trackers:\n");
            for (Tracker tracker : _trackers) {
                tracker.dump(writer);
            }
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
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
     * Starts the instantiated thread
     */
    @Override
    public final void initialize(
    ) {
        _workerThread.start();
        while (!_workerThread.isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                LOGGER.catching(ex);
            }
        }
    }

    /**
     * Put a channel program on our list of unstarted IOs and wake up the channel module thread.
     * If we find an obvious problem, we kill the IO here instead of scheduling it.
     */
    final boolean scheduleChannelProgram(
        final Processor source,
        final InputOutputProcessor ioProcessor,
        final ChannelProgram channelProgram
    ) {
        if (!_descendants.containsKey(channelProgram.getDeviceAddress())) {
            channelProgram.setChannelStatus(ChannelStatus.UnconfiguredDevice);
            return false;
        }

        channelProgram.setChannelStatus(ChannelStatus.InProgress);
        synchronized (_workerThread) {
            _trackers.add(createTracker(source, ioProcessor, channelProgram));
            _workerThread.notify();
        }

        return true;
    }

    /**
     * For the descendent device to signal the channel module when an IO has completed
     */
    final void signal() {
        synchronized(_workerThread) {
            _workerThread.notify();
        }
    }

    /**
     * Invoked just before tearing down the configuration.  Stop the thread.
     */
    @Override
    public final void terminate(
    ) {
        _workerTerminate = true;
        synchronized(_workerThread) {
            _workerThread.notify();
        }

        while (_workerThread.isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                LOGGER.catching(ex);
            }
        }
    }
}
