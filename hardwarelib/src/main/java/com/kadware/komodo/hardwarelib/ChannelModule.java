/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import java.io.BufferedWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.kadware.komodo.baselib.Worker;

/**
 * Base class which models a channel module node
 * <p>
 * Channel Module (CM) nodes are managed lightly by InputOutputProcessor (IOP) nodes.
 * The IOP passes (via a method call) the CM an absolute address which references a location within a StaticMainStorageProcessor (MSP).
 * This location contains a channel address word (CAW) (actually 2 words, of potentially varying formats).
 * Upon return from this method call, the CM releases the CAW for further use (it may preserve the content of the CAW for the
 * duration of the requested operation).
 * <p>
 * The CAW specifies a particular operation from among the following:
 *      StartIO - an IO operation of some sort, to be send to one of the Controller nodes which is attached to the CM
 *      Reset - indicates that the CM should reset itself, abort any in-flight IOs, etc
 * <p>
 * For StartIO, the CAW refers to a channel program which consists of a chain of one or more channel command words (CCWs).
 * The chain is comprised of linked partial chains, with each partial chain consisting of one or more contiguous CCWs.
 * The partial chains are forward-linked by a transfer-in-channel CCW (located as the last CCW in a partial chain) which
 * contains the absolute address of the first CCW in the next partial chain.
 * This allows channel programs to be comprised of multiple (possibly reusable) partial chains.
 * <p>
 * The format of a CCW differs based on the type of CM; however, each CCW consists of two actual words,
 * and requests one of the following actions:
 *      A read operation
 *      A write operation
 *      Some external/utility function (a non-read-write operation)
 *      A request for sense data
 *      A transfer-in-channel link to a non-contiguous CCW
 */

public abstract class ChannelModule extends Node implements Worker {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Indicates the type of the channel module
     */
    public static enum ChannelModuleType {
        None(0),
        Byte(1),
        Word(2);

        /**
         * Unique code for this particular ChannelModuleType
         */
        private final int _code;

        /**
         * Constructor
         * <p>
         * @param code
         */
        ChannelModuleType(
            final int code
        ) {
            _code = code;
        }

        /**
         * Retrieves the unique code assigned to this ChannelModuleType
         * <p>
         * @return
         */
        public int getCode(
        ) {
            return _code;
        }

        /**
         * Converts a code to a ChannelModuleType
         * <p>
         * @param code
         * <p>
         * @return
         */
        public static ChannelModuleType getValue(
            final int code
        ) {
            switch (code) {
                case 1:     return Byte;
                case 2:     return Word;
                default:    return None;
            }
        }
    };

    /**
     * Indicates a particular channel status
     */
    public static enum Status {
        None(0),
        Successful(1),
        Cancelled(2),
        DeviceError(3),
        InvalidChannelModuleAddress(4),
        InvalidControllerAddress(5),
        InvalidUPINumber(6),
        InsufficientBuffers(7),
        InProgress(8);

        /**
         * Unique code for this particular Status
         */
        private final int _code;

        /**
         * Constructor
         * <p>
         * @param code
         */
        Status(
            final int code
        ) {
            _code = code;
        }

        /**
         * Retrieves the unique code assigned to this Status
         * <p>
         * @return
         */
        public int getCode(
        ) {
            return _code;
        }

        /**
         * Converts a code to a Status
         * <p>
         * @param code
         * <p>
         * @return
         */
        public static Status getValue(
            final int code
        ) {
            switch (code) {
                case 1:     return Successful;
                case 2:     return Cancelled;
                case 3:     return DeviceError;
                case 4:     return InvalidChannelModuleAddress;
                case 5:     return InvalidControllerAddress;
                case 6:     return InvalidUPINumber;
                case 7:     return InsufficientBuffers;
                case 8:     return InProgress;
                default:    return None;
            }
        }
    };


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(ChannelModule.class);

    private final ChannelModuleType _channelModuleType;

    /**
     * From Configurator, via DeviceManager (at startup)
     */
    protected boolean _skipDataFlag;

    /**
     * Thread for the active (or now-terminated) worker for this object
     */
    protected final Thread _workerThread;

    /**
     * Set this flag so the worker thread can self-terminate
     */
    protected boolean _workerTerminate;

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    public ChannelModule(
        final ChannelModuleType channelModuleType,
        final String name
    ) {
        super(Node.Category.ChannelModule, name);
        _channelModuleType = channelModuleType;
        _workerThread = new Thread(this);
        _workerTerminate = false;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Getter
     * <p>
     * @return
     */
    public ChannelModuleType getChannelModuleType(
    ) {
        return _channelModuleType;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getSkipDataFlag(
    ) {
        return _skipDataFlag;
    }

    /**
     * Setter
     * <p.
     * @param value
     */
    public void setSkipDataFlag(
        final boolean value
    ) {
        _skipDataFlag = value;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * To be implemented by the subclass - this is the worker thread entry point.
     */
    @Override
    public abstract void run();

    /**
     * Invoked when this object is the source of an IO which has been cancelled or completed
     * <p>
     * @param source the Node which is signalling us
     */
    @Override
    public abstract void signal(
        final Node source
    );


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks to see whether this node is a valid descendant of the candidate node
     * @param ancestor
     * @return
     */
    @Override
    public boolean canConnect(
        final Node ancestor
    ) {
        //  We can connect only to IOProcessors
        return ancestor instanceof InputOutputProcessor;
    }

    /**
     * For debugging purposes
     * <p>
     * @param writer
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        //???? anything else here?
    }

    /**
     * Worker interface implementation
     * <p>
     * @return our node name
     */
    @Override
    public String getWorkerName(
    ) {
        return getName();
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


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

}
