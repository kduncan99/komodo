/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.kadware.komodo.baselib.IOAccessControlWord;
import com.kadware.komodo.baselib.Worker;
import com.kadware.komodo.baselib.types.*;

/**
 * Base class which models a channel module node
 *
 * A channel module is a class and an activity which accepts channel programs describing a simple IO,
 * queueing them, servicing them until they are complete, and updating the status fields appropriately,
 * and waking up the source activity.  At this point, we disavow all further knowledge of the channel
 * program.
 *
 * The caller is expected (at some point after posting the IO) to enter a wait, from which it will be
 * awakened (per the above).  At that point, it is free to dispose of the channel program as it deems
 * appropriate.
 *
 * This is where all the conversion is done from a low-level Exec IO request to device IO requests.
 * All IO is specified with a simple buffer, modifier, and word count.  Requests for byte modules will
 * also have a byte translation value indicating how words should be wrapped into bytes.  All requests
 * will additionally contain the IO path desired for the IO.
 *
 * We do not do scatter/gather here; It will be handled in the exec itself via Executive Request,
 * being converted to a single monolithic IO before being sent here.
 */

public abstract class SoftwareChannelModule extends Node implements Worker {

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
     * Indicates a particular channel command
     */
    public static enum Command {
        None(0),
        Nop(1),
        Inquiry(2),
        Read(3),
        Write(4);
        //TODO:TAPE more commands for tape handling... maybe...

        /**
         * Unique code for this particular Command
         */
        private final int _code;

        /**
         * Constructor
         * <p>
         * @param code
         */
        Command(
            final int code
        ) {
            _code = code;
        }

        /**
         * Retrieves the unique code assigned to this Command
         * <p>
         * @return
         */
        public int getCode(
        ) {
            return _code;
        }

        /**
         * Converts a code to a Command
         * <p>
         * @param code
         * <p>
         * @return
         */
        public static Command getValue(
            final int code
        ) {
            switch (code) {
                case 1:     return Nop;
                case 2:     return Inquiry;
                case 3:     return Read;
                case 4:     return Write;
                default:    return None;
            }
        }

        /**
         * Characteristic getter
         * <p>
         * @return
         */
        public boolean isTransferCommand(
        ) {
            return isTransferInCommand() || isTransferOutCommand();
        }

        /**
         * Characteristic getter
         * <p>
         * @return
         */
        public boolean isTransferInCommand(
        ) {
            return (this == Command.Inquiry) || (this == Command.Read);
        }

        /**
         * Characteristic getter
         * <p>
         * @return
         */
        public boolean isTransferOutCommand(
        ) {
            return this == Command.Write;
        }
    };

    /**
     * Indicates how 36-bit data is translated (by the channel module) into byte streams by ByteChannelModules.
     * Ignored by WordChannelModules.
     */
    public static enum IOTranslateFormat {
        None(0),
        A(1),       //  For 9-track tapes, mainly.
                    //      On output, the 8 LSBits of each quarter-word are encoded per output byte.
                    //          Transfer is terminated whenever MSBit of any quarter-word is set.
                    //      On input, subsequent bytes are read into 8 LSBits of succeding words,
                    //          with MSBit set to 0.
        B(2),       //  For 7-track tapes.
                    //      On output, each sixth-word is encoded into a byte, with bits 7 and 8 cleared.
                    //      On input, bits 7 and 8 from the channel are ignored.
        C(3),       //  Most IO is done via this.
                    //      Each 36-bit word is packed into 4.5 bytes of output.  Generally, transfers are
                    //          expected to be done in increments of 2 words.
                    //      On input, subsequent bytes are read into 36-bit words, 9 bytes->2 words.
        D(4);       //  Same as A Format, except that on output the MSB of each quarter-word is ignored.

        /**
         * Unique code for this particular IOTranslateFormat
         */
        private final int _code;

        /**
         * Constructor
         * <p>
         * @param code
         */
        IOTranslateFormat(
            final int code
        ) {
            _code = code;
        }

        /**
         * Retrieves the unique code assigned to this IOTranslateFormat
         * <p>
         * @return
         */
        public int getCode(
        ) {
            return _code;
        }

        /**
         * Converts a code to a IOTranslateFormat
         * <p>
         * @param code
         * <p>
         * @return
         */
        public static IOTranslateFormat getValue(
            final int code
        ) {
            switch (code) {
                case 1:     return A;
                case 2:     return B;
                case 3:     return C;
                case 4:     return D;
                default:    return None;
            }
        }
    };

    /**
     * Indicates a particular channel command
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

    /**
     * Callers send IOs via one of these objects
     */
    public static class ChannelProgram {

        public final Worker             _source;

        public short                    _processorUPI;
        public int                      _channelModuleAddress;
        public int                      _controllerAddress;
        public int                      _deviceAddress;

        public Command                  _command;
        public long                     _address;           //  This could be anything depending on the target.  For disk, this is a block-id
        public int                      _wordCount;         //  Transfer size in words for transfer commands
        public IOAccessControlWord      _accessControlWord;
        public PrepFactor               _prepFactor;        //  for byte disk devices
        public IOTranslateFormat        _translateFormat;   //  only for byte channel modules

        //  Values returned by io handler
        public int                      _bytesTransferred;  //  only applies to byte channel modules
        public int                      _wordsTransferred;
        public Status                   _channelStatus;
        public Device.IOStatus          _deviceStatus;
        public byte[]                   _senseBytes;

        /**
         * Standard constructor
         * <p>
         * @param source
         */
        public ChannelProgram(
            final Worker source
        ) {
            _source = source;

            _processorUPI = (short)0;
            _channelModuleAddress = 0;
            _controllerAddress = 0;
            _deviceAddress =0;
            _command = Command.None;
            _address = 0;
            _wordCount = 0;
            _prepFactor = null;
            _translateFormat = IOTranslateFormat.None;
            _bytesTransferred = 0;
            _wordsTransferred = 0;
            _channelStatus = Status.None;
            _deviceStatus = Device.IOStatus.None;
            _senseBytes = null;
        }
    };

    //  Base class for tracking an IO.  Derived ChannelModule classes may extend this...
    public static class Tracker {

        public boolean _cancelled;              //  caller cancelled the IO; _channelProgram will be null
        public ChannelProgram _channelProgram;  //  The associated channel program
        public Device.IOInfo _childIO;          //  Reference to a sub-IO for this requested IO

        public Tracker(
            final ChannelProgram channelProgram
        ) {
            _cancelled = false;
            _channelProgram = channelProgram;
            _childIO = null;
        }

        public void dump(
            final BufferedWriter writer
        ) {
            try {
                writer.write(String.format("    ChannelProgram:%s\n", _cancelled ? " CANCELLED" : ""));
                if (!_cancelled) {
                    writer.write(String.format("      Source:           %s",
                                               _channelProgram._source == null ?
                                                    "<none>" :
                                                    _channelProgram._source.getWorkerName()));
                    writer.write(String.format("      Path:             %d/%d/%d/%d\n",
                                               _channelProgram._processorUPI,
                                               _channelProgram._channelModuleAddress,
                                               _channelProgram._controllerAddress,
                                               _channelProgram._deviceAddress));
                    writer.write(String.format("      Command:          %s\n",
                                               _channelProgram._command.toString()));
                    writer.write(String.format("      Address:          0%o\n",
                                               _channelProgram._address));
                    writer.write(String.format("      WordCount:        0%o\n",
                                               _channelProgram._wordCount));
                    writer.write(String.format("      ACW: %s\n",
                                               _channelProgram._accessControlWord.toString()));
                    writer.write(String.format("      Prep Factor:      %s\n",
                                               String.valueOf(_channelProgram._prepFactor)));
                    writer.write(String.format("      Format:           %s\n",
                                               _channelProgram._translateFormat));
                    writer.write(String.format("      BytesTransferred: %d\n",
                                               _channelProgram._bytesTransferred));
                    writer.write(String.format("      WordsTransferred: %d\n",
                                               _channelProgram._wordsTransferred));
                    writer.write(String.format("      Channel Status:   %s\n",
                                               _channelProgram._channelStatus.toString()));
                    writer.write(String.format("      Device Status:    %s\n",
                                               _channelProgram._deviceStatus.toString()));
                }

                if (_childIO != null) {
                    writer.write(String.format("    ChildIo: %s\n", _childIO.toString()));
                }
            } catch (IOException ex) {
                LOGGER.catching(ex);
            }
        }
    };


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(SoftwareChannelModule.class);

    private final ChannelModuleType _channelModuleType;

    /**
     * List of all active Tracker objects describing in-flight IOs
     */
    protected final List<Tracker> _trackers = new LinkedList<>();

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

    public SoftwareChannelModule(
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
     * For the subclass - it creates a tracker specific to its needs, upon being invoked by this class
     * <p>
     * @param channelProgram
     * <p>
     * @return
     */
    protected abstract Tracker createTracker(
        final ChannelProgram channelProgram
    );

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
     * Cancels an IO in our list of trackers.
     * Note that we CANNOT remove the tracker while a device IO is in progress.
     * Thus, we simply set the cancelled flag and let other code do more interesting things.
     * The subclasses which actually deal with device IO will be expected to do the Right Thing - whatever that is.
     * @param channelProgram
     * @return
     */
    public boolean cancelIO(
        final ChannelProgram channelProgram
    ) {
        boolean result = false;
        synchronized(this) {
            for (Tracker tracker : _trackers) {
                if (tracker._channelProgram == channelProgram) {
                    tracker._cancelled = true;
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Checks to see whether this node is a valid descendant of the candidate node
     * @param candidateAncestor
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
        synchronized(this) {
            for (Tracker tracker : _trackers) {
                tracker.dump(writer);
            }
        }
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
     * Does all setup stuff common to all types of channel modules.
     * We do some early grooming on the thing, then queue it up for the worker to deal with.
     * <p>
     * Exactly how the ACWs are handled is up to the byte or word channel derived class;
     * What is presented, must be a set of ACWs (one or more) describing a total region of exactly one block.
     * <p>
     * The buffer space may be divided in any fashion (including non-aligned), so some rearrangement
     * of the data may be necessary on input or output.
     * <p>
     * @param channelProgram
     */
    public void handleIO(
        final ChannelProgram channelProgram
    ) {
        //  Check to ensure the controller address is correct
        if (_descendants.get(channelProgram._controllerAddress) == null) {
            channelProgram._channelStatus = Status.InvalidControllerAddress;
            if (channelProgram._source != null) {
                synchronized(channelProgram._source) {
                    channelProgram._source.notify();
                }
            }

            return;
        }

        //  Logging anything?
        if (LOG_CHANNEL_IOS) {
            LOGGER.info(String.format("channel IO:Path=%d/%d/%d/%d  Cmd=%s  Addr=%d",
                                      channelProgram._processorUPI,
                                      channelProgram._channelModuleAddress,
                                      channelProgram._controllerAddress,
                                      channelProgram._deviceAddress,
                                      channelProgram._command.toString(),
                                      channelProgram._address));
            LOGGER.info(String.format("          :ACW=%s\n",
                                      channelProgram._accessControlWord.toString()));
            if (LOG_CHANNEL_IO_BUFFERS) {
                if (channelProgram._command.isTransferOutCommand()) {
                    channelProgram._accessControlWord.logBuffer(LOGGER,
                                                                Level.INFO,
                                                                String.format("%s Data to be Written", getName()));
                }
            }
        }

        //  Let the derived class create a tracker, queue it, and wake up our subclasses' worker
        Tracker tracker = createTracker(channelProgram);
        if (tracker != null) {
            synchronized(this) {
                _trackers.add(tracker);
                notify();
            }
        }
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
