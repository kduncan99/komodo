/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.kadware.em2200.baselib.types.*;
import com.kadware.em2200.baselib.Word36Array;

/**
 * Abstract base class for a device such as a disk or tape unit
 * A Device is the lowest unit in an IO chain.
 * Generally, Devices are child Nodes for Controllers.
 * All IO to Devices is done asynchronously (as far as we are concerned at this level)
 * The Controller sends a DeviceIOInfo to the Device node via a pointer.
 * If the IO cannot be scheduled, the IOStatus field of the DeviceIOInfo is updated immediately, and the IO is rejected.
 * For IOs which are asynchronous at the system level, the IO is performed, IOStatus is updated,
 * a signal is sent to the sender, and the IO is considered complete.
 *      For IOs which are synchronous at the system level, the IO is scheduled, IOStatus is updated, and the IO is accepted.
 *          When the IO completes at the system level, a signal is sent to the sender.
 *      Senders must preserve DeviceIOInfo objects in memory until the Device marks the IOStatus field with some
 *          value indicating that the IO is complete, rejected, or has failed.
 *      Actual system IO may involve invoking the host system's asynchronous IO facility.
 *          For Windows, this is accomplished via ReadFileEx() and WriteFileEx().
 */
public abstract class Device extends Node {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Indicates what type of device this particular object is modelling
     */
    public static enum DeviceModel
    {
        None(0),
        FileSystemDisk(1),      //  Disk drive which uses native host filesystem for storage
        FileSystemPrinter(2),   //  Virtual printer, storing output to the native host filesystem
        FileSystemPunch(3),     //  Virtual card punch, storing output to the native host filesystem
        FileSystemReader(4),    //  Virtual card reader, taking input from the native host filesystem
        FileSystemTape(5),      //  Tape drive which uses native host filesystem for volume persistence
        RAMDisk(6);             //  Disk drive implemented entirely in memory, with persisted backing storage

        /**
         * Unique code for this particular DeviceModel
         */
        private final int _code;

        /**
         * Constructor
         * <p>
         * @param code
         */
        DeviceModel(
            final int code
        ) {
            _code = code;
        }

        /**
         * Retrieves the unique code assigned to this DeviceModel
         * <p>
         * @return
         */
        public int getCode(
        ) {
            return _code;
        }

        /**
         * Converts a code to a DeviceModel
         * <p>
         * @param code
         * <p>
         * @return
         */
        public static DeviceModel getValue(
            final int code
        ) {
            switch (code) {
                case 1:     return FileSystemDisk;
                case 2:     return FileSystemPrinter;
                case 3:     return FileSystemPunch;
                case 4:     return FileSystemReader;
                case 5:     return FileSystemTape;
                case 6:     return RAMDisk;
                default:    return None;
            }
        }
    };

    /**
     * Indicates the type of the device
     */
    public static enum DeviceType
    {
        None(0),
        Disk(1),        //  Disk Device
        Symbiont(2),    //  Symbiont Device (printer, reader, punch, etc)
        Tape(3);        //  Tape Device

        /**
         * Unique code for this particular DeviceType
         */
        private final int _code;

        /**
         * Constructor
         * <p>
         * @param code
         */
        DeviceType(
            final int code
        ) {
            _code = code;
        }

        /**
         * Retrieves the unique code assigned to this DeviceType
         * <p>
         * @return
         */
        public int getCode(
        ) {
            return _code;
        }

        /**
         * Converts a code to a DeviceType
         * <p>
         * @param code
         * <p>
         * @return
         */
        public static DeviceType getValue(
            final int code
        ) {
            switch (code) {
                case 1:     return Disk;
                case 2:     return Symbiont;
                case 3:     return Tape;
                default:    return None;
            }
        }
    };

    /**
     * Identifies a particular IO operation
     */
    public static enum IOFunction
    {                           // Printer .Punch.. .Reader. ..Disk.. ..Tape.. Terminal
        None(0),                //
        Close(1),               //            X
        GetInfo(2),             //    X       X        X        X        X        X
        MoveBlock(3),           //                                       X
        MoveBlockBackward(4),   //                                       X
        MoveFile(5),            //                                       X
        MoveFileBackward(6),    //                                       X
        Read(7),                //                     X        X        X        X
        ReadBackward(8),        //                                       X
        Reset(9),               //    X       X        X        X        X
        Rewind(10),             //                                       X
        RewindInterlock(11),    //                                       X
        Unload(12),             //                                       X
        Write(13),              //    X       X                 X        X        X
        WriteEndOfFile(14);     //                                       X

        /**
         * Unique code for this particular IOFunction
         */
        private final int _code;

        /**
         * Constructor
         * <p>
         * @param code
         */
        IOFunction(
            final int code
        ) {
            _code = code;
        }

        /**
         * Retrieves the unique code assigned to this IOFunction
         * <p>
         * @return
         */
        public int getCode(
        ) {
            return _code;
        }

        /**
         * Converts a code to an IOFunction
         * <p>
         * @param code
         * <p>
         * @return
         */
        public static IOFunction getValue(
            final int code
        ) {
            switch (code) {
                case 1:     return Close;
                case 2:     return GetInfo;
                case 3:     return MoveBlock;
                case 4:     return MoveBlockBackward;
                case 5:     return MoveFile;
                case 6:     return MoveFileBackward;
                case 7:     return Read;
                case 8:     return ReadBackward;
                case 9:     return Reset;
                case 10:    return Rewind;
                case 11:    return RewindInterlock;
                case 12:    return Unload;
                case 13:    return Write;
                case 14:    return WriteEndOfFile;
                default:    return None;
            }
        }

        /**
         * Indicates if this function is a READ function
         * <p>
         * @return
         */
        public boolean isReadFunction(
        ) {
            return (this == IOFunction.Read) || (this == IOFunction.ReadBackward);
        };

        /**
         * Indicates if this function is a WRITE function
         * <p>
         * @return
         */
        public boolean isWriteFunction(
        ) {
            return (this == IOFunction.Write) || (this == IOFunction.WriteEndOfFile);
        }
    };

    /**
     * Identifies a particular IO status
     */
    public static enum IOStatus
    {                               // Printer. .Punch.. .Reader. ..Disk.. ..Tape.. Terminal
        None(0),                    //
        Successful(1),              //    X        X        X        X        X        X
        BufferTooSmall(2),          //                               X        X
        DeviceBusy(3),              //                               X        ?
        EndOfTape(4),               //                                        X
        FileMark(5),                //                                        X
        InvalidBlockCount(6),       //                               X
        InvalidBlockId(7),          //                               X
        InvalidBlockSize(8),        //                               X
        InvalidDeviceAddress(9),    //    X        X        X        X        X        X    // returned by controller
        InvalidFunction(10),        //    X        X        X        X        X
        InvalidMode(11),            //                                        X
        InvalidTransferSize(12),    //                                        X
        InProgress(13),             //                               X
        LostPosition(14),           //                                        X
        MediaError(15),             //                      X
        NoDevice(16),               //    X        X        X        X        X        X    // returned by controller
        NoInput(17),                //                      X                          X
        NotPrepped(18),             //                               X
        NotReady(19),               //    X        X        X        X        X
        QueueFull(20),              //                               X
        SystemException(21),        //    X        X        X        X        X
        UnitAttention(22),          //                               X        ?
        WriteProtected(23);         //                               X        X

        /**
         * Unique code for this particular IOStatus
         */
        private final int _code;

        /**
         * Constructor
         * <p>
         * @param code
         */
        IOStatus(
            final int code
        ) {
            _code = code;
        }

        /**
         * Retrieves the unique code assigned to this IOFunction
         * <p>
         * @return
         */
        public int getCode(
        ) {
            return _code;
        }

        /**
         * Converts a code to an IOStatus
         * <p>
         * @param code
         * <p>
         * @return
         */
        public static IOStatus getValue(
            final int code
        ) {
            switch (code) {
                case 1:     return Successful;
                case 2:     return BufferTooSmall;
                case 3:     return DeviceBusy;
                case 4:     return EndOfTape;
                case 5:     return FileMark;
                case 6:     return InvalidBlockCount;
                case 7:     return InvalidBlockId;
                case 8:     return InvalidBlockSize;
                case 9:     return InvalidDeviceAddress;
                case 10:    return InvalidFunction;
                case 11:    return InvalidMode;
                case 12:    return InvalidTransferSize;
                case 13:    return InProgress;
                case 14:    return LostPosition;
                case 15:    return MediaError;
                case 16:    return NoDevice;
                case 17:    return NoInput;
                case 18:    return NotPrepped;
                case 19:    return NotReady;
                case 20:    return QueueFull;
                case 21:    return SystemException;
                case 22:    return UnitAttention;
                case 23:    return WriteProtected;
                default:    return None;
            }
        }
    };


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * This class (or a subset thereof) is returned by IOFunction::GetInfo in a byte buffer for byte devices
     */
    public static class DeviceInfo {

        private DeviceModel _deviceModel;
        private DeviceType _deviceType;
        private boolean _isReady;
        private short _subsystemIdentifier;
        private boolean _unitAttention;

        /**
         * Standard constructor - to be used prior to deserialization
         */
        public DeviceInfo(
        ) {
            _deviceModel = DeviceModel.None;
            _deviceType = DeviceType.None;
            _isReady = false;
            _subsystemIdentifier = 0;
            _unitAttention = false;
        }

        /**
         * Alternate onstructor
         * <p>
         * @param type
         * @param model
         * @param isReady
         * @param subsystemIdentifier
         * @param unitAttention
         */
        public DeviceInfo(
            final DeviceType type,
            final DeviceModel model,
            final boolean isReady,
            final short subsystemIdentifier,
            final boolean unitAttention
        ) {
            _deviceModel = model;
            _deviceType = type;
            _isReady = isReady;
            _subsystemIdentifier = subsystemIdentifier;
            _unitAttention = unitAttention;
        }

        /**
         * For unit testing
         * <p>
         * @param obj
         * <p>
         * @return
         */
        @Override
        public boolean equals(
            final Object obj
        ) {
            if ((obj != null) && (obj instanceof DeviceInfo)) {
                DeviceInfo i = (DeviceInfo)obj;
                return (_deviceType == i._deviceType)
                        && (_deviceModel == i._deviceModel)
                        && (_isReady == i._isReady)
                        && (_subsystemIdentifier == i._subsystemIdentifier)
                        && (_unitAttention == i._unitAttention);
            }

            return false;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public DeviceModel getDeviceModel(
        ) {
            return _deviceModel;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public DeviceType getDeviceType(
        ) {
            return _deviceType;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public short getSubsystemIdentifier(
        ) {
            return _subsystemIdentifier;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public boolean getUnitAttention(
        ) {
            return _unitAttention;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public boolean isReady(
        ) {
            return _isReady;
        }

        /**
         * Deserializes the pertinent information for this device, to a byte stream.
         * We might be overridden by a subclass, which calls here first, then deserializes its own information.
         * <p>
         * @param buffer source for deserialization
         */
        public void deserialize(
            final ByteBuffer buffer
        ) {
            _deviceType = DeviceType.getValue(buffer.getInt());
            _deviceModel = DeviceModel.getValue(buffer.getInt());
            _isReady = deserializeBoolean(buffer);
            _subsystemIdentifier = buffer.getShort();
            _unitAttention = deserializeBoolean(buffer);
        }

        /**
         * Serializes information for this device to a byte stream.
         * We might be overridden by a subclass, which calls here first, then serializes its own information.
         * <p>
         * @param buffer target of serialization
         */
        public void serialize(
            final ByteBuffer buffer
        ) {
            buffer.putInt(_deviceType.getCode());
            buffer.putInt(_deviceModel.getCode());
            serializeBoolean(buffer, _isReady);
            buffer.putShort(_subsystemIdentifier);
            serializeBoolean(buffer, _unitAttention);
        }
    };

    /**
     * Describes an IO at the device level.  This might be extended for more specificity, by a subclass of Device.
     */
    public static class IOInfo {
        /**
         * Source of the IO request
         */
        private final Node _source;

        /**
         * Function to be performed
         */
        private final IOFunction _function;

        /**
         * Reference to a byte buffer for byte data transfers
         */
        private final byte[] _byteBuffer;

        /**
         * Reference to a Word36Array buffer for word data transfers
         */
        private final Word36Array _wordBuffer;

        /**
         * Number of bytes or words to be transferred
         */
        private final int _transferCount;

        /**
         * Identifies a particular hardware block address or offset for device-addressed functions
         */
        private final BlockId _blockId;

        /**
         * Sense bytes returned from a physical IO (if we ever implement such)
         */
        private byte[] _senseBytes;

        /**
         * Result of the operation
         */
        private IOStatus _status;

        /**
         * Number of bytes or words successfully transferred
         */
        private int _transferredCount;

        /**
         * Constructor for a non-transfer IO
         * <p>
         * @param source
         * @param function
         */
        public IOInfo(
            final Node source,
            final IOFunction function
        ) {
            _source = source;
            _function = function;

            _byteBuffer = null;
            _blockId = null;
            _transferCount = 0;
            _wordBuffer = null;
            _senseBytes = null;
            _status = IOStatus.None;
            _transferredCount = 0;
        }

        /**
         * Constructor for a byte transfer IO which does not need a block ID
         * <p>
         * @param source
         * @param function
         * @param buffer
         * @param transferCount
         */
        public IOInfo(
            final Node source,
            final IOFunction function,
            final byte[] buffer,
            final int transferCount
        ) {
            _source = source;
            _function = function;
            _byteBuffer = buffer;
            _transferCount = transferCount;

            _blockId = null;
            _wordBuffer = null;
            _senseBytes = null;
            _status = IOStatus.None;
            _transferredCount = 0;
        }

        /**
         * Constructor for a byte transfer IO
         * <p>
         * @param source
         * @param function
         * @param buffer
         * @param blockId
         * @param transferCount
         */
        public IOInfo(
            final Node source,
            final IOFunction function,
            final byte[] buffer,
            final BlockId blockId,
            final int transferCount
        ) {
            _source = source;
            _function = function;
            _byteBuffer = buffer;
            _blockId = blockId;
            _transferCount = transferCount;

            _wordBuffer = null;
            _senseBytes = null;
            _status = IOStatus.None;
            _transferredCount = 0;
        }

        public IOInfo(
            final Node source,
            final IOFunction function,
            final Word36Array buffer,
            final BlockId blockId,
            final int transferCount
        ) {
            _source = source;
            _function = function;
            _wordBuffer = buffer;
            _blockId = blockId;
            _transferCount = transferCount;

            _byteBuffer = null;
            _senseBytes = null;
            _status = IOStatus.Successful;
            _transferredCount = 0;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public BlockId getBlockId(
        ) {
            return _blockId;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public byte[] getByteBuffer(
        ) {
            return _byteBuffer;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public IOFunction getFunction(
        ) {
            return _function;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public byte[] getSenseBytes(
        ) {
            return _senseBytes;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public Node getSource(
        ) {
            return _source;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public IOStatus getStatus(
        ) {
            return _status;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public int getTransferCount(
        ) {
            return _transferCount;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public int getTransferredCount(
        ) {
            return _transferredCount;
        }

        /**
         * Getter
         * <p>
         * @return
         */
        public Word36Array getWordBuffer(
        ) {
            return _wordBuffer;
        }

        /**
         * Setter
         * <p>
         * @param count
         */
        public void setTransferredCount(
            final int count
        ) {
            _transferredCount = count;
        }

        /**
         * Setter
         * <p>
         * @param senseBytes
         */
        public void setSenseBytes(
            final byte[] senseBytes
        ) {
            _senseBytes = senseBytes;
        }

        /**
         * Setter
         * <p>
         * @param status
         */
        public void setStatus(
            final IOStatus status
        ) {
            _status = status;
        }

        /**
         * Produces a displayable string describing this object
         * <p>
         * @return
         */
        @Override
        public String toString(
        ) {
            return String.format("Src=%s Fcn=%s BlkId=%s Count=%s Xferd=%s Stat=%s",
                                 _source == null ? "<null>" : _source.getName(),
                                 String.valueOf(_function),
                                 String.valueOf(_blockId),
                                 String.valueOf(_transferCount),
                                 String.valueOf(_transferredCount),
                                 String.valueOf(_status));
        }
    };


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(Device.class);

    private final DeviceModel _deviceModel;
    private final DeviceType _deviceType;

    /**
     * Indicates the device can do reads and writes.
     * If not ready, the device *may* respond to non-read/write IOs.
     */
    private boolean _readyFlag = false;

    /**
     * Ties all devices and controllers in a logical subsystem together.
     * Used *very sparingly* by the Exec; to be established and verified by the wrapping executable (e.g., emssp).
     */
    private short _subsystemIdentifier;

    /**
     * Indicates that something regarding the physical characteristics of this device has changed.
     * Setting a device ready should ALWAYS set this flag.
     * All reads and writes will be rejected with IOStatus.UnitAttention until an IOFunction.GetInfo is issued.
     */
    private boolean _unitAttentionFlag = false;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    public Device(
        final DeviceType deviceType,
        final DeviceModel deviceModel,
        final String name,
        final short subsystemIdentifier
    ) {
        super(Node.Category.Device, name);
        _deviceModel = deviceModel;
        _deviceType = deviceType;
        _readyFlag = false;
        _subsystemIdentifier = subsystemIdentifier;
        _unitAttentionFlag = false;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Getter
     * <p>
     * @return
     */
    public DeviceModel getDeviceModel(
    ) {
        return _deviceModel;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public DeviceType getDeviceType(
    ) {
        return _deviceType;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public short getSubsystemIdentifier(
    ) {
        return _subsystemIdentifier;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getUnitAttentionFlag(
    ) {
        return _unitAttentionFlag;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean isReady(
    )  {
        return _readyFlag;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Indicates whether this Node can connect as a descendant to the candidate ancestor Node.
     * This is an override of Node.canConnect(), and only appears here as an abstract for documentary purposes.
     * <p>
     * @param candidate
     * <p>
     * @return
     */
    @Override
    public abstract boolean canConnect(
        final Node candidate
    );

    /**
     * IO Router - what happens here is depending upon the subclass
     * <p>
     * @param ioInfo
     */
    public abstract void handleIo(
        final IOInfo ioInfo
    );

    /**
     * Does this device have a byte interface?
     * <p>
     * @return
     */
    public abstract boolean hasByteInterface(
    );

    /**
     * Does this device have a word interface?
     * <p>
     * @return
     */
    public abstract boolean hasWordInterface(
    );

    /**
     * Initializes the subclass if/as necessary
     */
    @Override
    public abstract void initialize();

    /**
     * Invoked when this object is the source of an IO which has been cancelled or completed
     * <p>
     * @param source the Node which is signalling us
     */
    @Override
    public abstract void signal(
        final Node source
    );

    /**
     * Invoked just before tearing down the configuration.
     */
    @Override
    public abstract void terminate();

    /**
     * Writes IO buffers to the log.
     * What actually happens is dependant upon the particulars of the various subclasses, so this is abstract
     * <p>
     * @param ioInfo describes the IO buffer(s)
     */
    protected abstract void writeBuffersToLog(
        final IOInfo ioInfo
    );


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

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
        try {
            writer.write(String.format("  Type:%s  Model:%s  SubSysId:%s\n",
                                       String.valueOf(_deviceType.toString()),
                                       String.valueOf(_deviceModel.toString()),
                                       String.valueOf(_subsystemIdentifier)));

            writer.write(String.format("  Ready:%s  UnitAttn:%s\n",
                                       String.valueOf(isReady()),
                                       String.valueOf(_unitAttentionFlag)));
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Subclasses must call here at the end of handling an IO
     * <p>
     * @param ioInfo
     */
    protected void ioEnd(
        final IOInfo ioInfo
    ) {
        if (LOG_IO_ERRORS) {
            if ((ioInfo.getStatus() != IOStatus.Successful) && (ioInfo.getStatus() != IOStatus.NoInput)) {
                LOGGER.error(String.format("IoError:%s", ioInfo.toString()));
            }
        }

        if (LOG_DEVICE_IO_BUFFERS) {
            if (ioInfo.getFunction().isReadFunction() && (ioInfo.getStatus() == IOStatus.Successful)) {
                writeBuffersToLog(ioInfo);
            }
        }
    }

    /**
     * Subclasses must call this at the beginning of handling an IO.
     * <p>
     * @param ioInfo
     */
    public void ioStart(
        final IOInfo ioInfo
    ) {
        if (LOG_DEVICE_IOS) {
            LOGGER.debug(String.format("IoStart:%s", ioInfo.toString()));
        }

        if (LOG_DEVICE_IO_BUFFERS) {
            if (ioInfo.getFunction().isWriteFunction()) {
                writeBuffersToLog(ioInfo);
            }
        }
    }

    /**
     * Sets the device ready or not-ready, depending upon the given state parameter.
     * Subclasses may override this to do more specific stuff (such as verifying that the state can be set as request),
     * but at some point they must call back here to actually effect the state change.
     * <p>
     * @param state true to set ready, false to set not-ready
     * <p>
     * @return true if successful, otherwise false
     */
    public boolean setReady(
        final boolean state
    ) {
        _readyFlag = state;
        LOGGER.info(String.format("Device %s Ready %s", getName(), state ? "Set" : "Cleared"));
        setUnitAttention(state);
        return true;
    }

    /**
     * Sets the unit attention flag (or clears it)
     * <p>
     * @param state
     * <p>
     * @return
     */
    public boolean setUnitAttention(
        final boolean state
    ) {
        _unitAttentionFlag = state;
        LOGGER.info(String.format("Device %s Unit Attention %s", getName(), state ? "Set" : "Cleared"));
        return true;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

}
