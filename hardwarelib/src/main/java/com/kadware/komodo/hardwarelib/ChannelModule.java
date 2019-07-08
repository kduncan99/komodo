/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.Worker;
import java.io.BufferedWriter;
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
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(ChannelModule.class);

    public final ChannelModuleType _channelModuleType;

    /**
     * From Configurator, via DeviceManager (at startup)
     */
    //TODO the heck is this for?
    protected boolean _skipDataFlag;

    /**
     * Thread for the active (or now-terminated) worker for this object
     */
    final Thread _workerThread;

    /**
     * Set this flag so the worker thread can self-terminate
     */
    boolean _workerTerminate;

    /**
     * Channel program lists
     */
    protected final List<ChannelProgram> _unstarted = new LinkedList<>();
    protected final List<ChannelProgram> _inProgress = new LinkedList<>();


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
        _inProgress.clear();
        _unstarted.clear();
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
            }
        }
    }

    /**
     * Put a channel program on our list of unstarted IOs
     */
    public final void scheduleChannelProgram(
        final ChannelProgram channelProgram
    ) {
        synchronized (_unstarted) {
            _unstarted.add(channelProgram);
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
            }
        }
    }
}
