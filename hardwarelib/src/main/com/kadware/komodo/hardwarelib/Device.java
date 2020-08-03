/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.Word36;
import java.io.BufferedWriter;
import java.io.IOException;
import org.apache.logging.log4j.message.EntryMessage;

/**
 * Abstract base class for a device such as a disk or tape unit
 * Devices are child Nodes of ChannelModules, and are the lowest units in the IO chain.
 * All IO to Devices is done asynchronously (as far as we are concerned at this level).
 * The ChannelModule sends a IOInfo object to the Device.
 * If the IO cannot be scheduled, the IOStatus field of the IOInfo is updated immediately, and the IO is rejected.
 * For IOs which are asynchronous at the system level, the IO is performed, IOStatus is updated,
 *   a signal is sent to the sender, and the IO is considered complete.
 * For IOs which are synchronous at the system level, the IO is scheduled, IOStatus is updated, and the IO is accepted.
 * When the IO completes at the system level, a signal is sent to the sender.
 * Senders must preserve IOInfo objects in memory until the Device marks the IOStatus field with some
 *   value indicating that the IO is complete, rejected, or has failed.
 * Actual system IO may involve invoking the host system's asynchronous IO facility.
 */
@SuppressWarnings("Duplicates")
public abstract class Device extends Node {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested things
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Indicates what type of device this particular object is modelling
     */
    public enum Model
    {
        None(0),
        FileSystemDisk(1),      //  Disk drive which uses native host filesystem for storage
        FileSystemPrinter(2),   //  Virtual printer, storing output to the native host filesystem
        FileSystemPunch(3),     //  Virtual card punch, storing output to the native host filesystem
        FileSystemReader(4),    //  Virtual card reader, taking input from the native host filesystem
        FileSystemTape(5),      //  Tape drive which uses native host filesystem for volume persistence
        RAMDisk(6),             //  Disk drive implemented entirely in memory, with persisted backing storage
        ScratchDisk(076),       //  Disk drive implemented in volatile storage, mostly for testing
        ScratchTape(077);       //  Tape drive implemented in volatile storage, mostly for testing

        private final int _code;

        Model(int code) { _code = code; }
        public int getCode() { return _code; }

        public static Model getValue(
            final int code
        ) {
            return switch (code) {
                case 1 -> FileSystemDisk;
                case 2 -> FileSystemPrinter;
                case 3 -> FileSystemPunch;
                case 4 -> FileSystemReader;
                case 5 -> FileSystemTape;
                case 6 -> RAMDisk;
                case 076 -> ScratchDisk;
                case 077 -> ScratchTape;
                default -> None;
            };
        }
    }

    /**
     * Indicates the type of the device
     */
    public enum Type
    {
        None(0),
        Disk(1),        //  Disk Device
        Symbiont(2),    //  Symbiont Device (printer, reader, punch, etc)
        Tape(3);        //  Tape Device

        private final int _code;

        Type(int code) { _code = code; }
        public int getCode() { return _code; }

        public static Type getValue(
            final int code
        ) {
            return switch (code) {
                case 1 -> Disk;
                case 2 -> Symbiont;
                case 3 -> Tape;
                default -> None;
            };
        }
    }

    /**
     * Identifies a particular IO operation
     */
    public enum IOFunction
    {
        //                                      Card     Card
        //                             Printer .Punch.. .Reader. ..Disk.. ..Tape..
        None(0),              //    X       X        X        X        X
        Close(1),             //            X
        GetInfo(2),           //    X       X        X        X        X
        MoveBlock(3),         //                                       X
        MoveBlockBackward(4), //                                       X
        MoveFile(5),          //                                       X
        MoveFileBackward(6),  //                                       X
        Read(7),              //                     X        X        X
        ReadBackward(8),      //                                       X
        Reset(9),             //    X       X        X        X        X
        Rewind(10),           //                                       X
        RewindInterlock(11),  //                                       X
        SetMode(12),          //                                       X
        Unload(13),           //                                       X
        Write(14),            //    X       X                 X        X
        WriteEndOfFile(15);   //                                       X

        private final int _code;

        IOFunction(int code) {
            _code = code;
        }

        public int getCode() {
            return _code;
        }

        public static IOFunction getValue(
            final int code
        ) {
            return switch (code) {
                case 1 -> Close;
                case 2 -> GetInfo;
                case 3 -> MoveBlock;
                case 4 -> MoveBlockBackward;
                case 5 -> MoveFile;
                case 6 -> MoveFileBackward;
                case 7 -> Read;
                case 8 -> ReadBackward;
                case 9 -> Reset;
                case 10 -> Rewind;
                case 11 -> RewindInterlock;
                case 12 -> SetMode;
                case 13 -> Unload;
                case 14 -> Write;
                case 15 -> WriteEndOfFile;
                default -> None;
            };
        }

        public boolean requiresBuffer() {
            return (this == IOFunction.Read)
                   || (this == IOFunction.ReadBackward)
                   || (this == IOFunction.Write);
        }

        public boolean isReadFunction() {
            return (this == IOFunction.GetInfo)
                   || (this == IOFunction.Read)
                   || (this == IOFunction.ReadBackward);
        }

        public boolean isWriteFunction() {
            return (this == IOFunction.Write)
                   || (this == IOFunction.WriteEndOfFile);
        }
    }

    /**
     * Identifies a particular Device IO status
     */
    public enum IOStatus
    {
        //                                           Card      Card
        //                                 Printer. .Punch.. .Reader. ..Disk.. ..Tape..
        Successful(0),            //    X        X        X        X        X
        BufferTooSmall(1),        //                               X        X
        DeviceBusy(2),            //                               X        ?
        EndOfTape(3),             //                                        X
        FileMark(4),              //                                        X
        InvalidBlockCount(5),     //                               X
        InvalidBlockId(6),        //                               X
        InvalidBlockSize(7),      //                               X
        InvalidFunction(010),     //    X        X        X        X        X
        InvalidMode(011),         //                                        X
        InvalidTransferSize(012), //                                        X
        InProgress(040),          //                               X
        LostPosition(013),        //                                        X
        MediaError(014),          //                      X
        NoInput(015),             //                      X
        NotPrepped(016),          //                               X
        NotReady(017),            //    X        X        X        X        X
        QueueFull(020),           //                               X
        SystemException(021),     //    X        X        X        X        X
        UnitAttention(022),       //                               X        ?
        WriteProtected(023),      //                               X        X
        InvalidStatus(077);

        private final int _code;

        IOStatus(int code) { _code = code; }

        public int getCode() { return _code; }

        public static IOStatus getValue(
            final int code
        ) {
            return switch (code) {
                case 0 -> Successful;
                case 1 -> BufferTooSmall;
                case 2 -> DeviceBusy;
                case 3 -> EndOfTape;
                case 4 -> FileMark;
                case 5 -> InvalidBlockCount;
                case 6 -> InvalidBlockId;
                case 7 -> InvalidBlockSize;
                case 010 -> InvalidFunction;
                case 011 -> InvalidMode;
                case 012 -> InvalidTransferSize;
                case 013 -> LostPosition;
                case 014 -> MediaError;
                case 015 -> NoInput;
                case 016 -> NotPrepped;
                case 017 -> NotReady;
                case 020 -> QueueFull;
                case 021 -> SystemException;
                case 022 -> UnitAttention;
                case 023 -> WriteProtected;
                case 040 -> InProgress;
                default -> InvalidStatus;
            };
        }
    }

    /**
     * Represents an IO passed from a ChannelModule object to one of its descendant Device objects
     */
    public static class IOInfo {

        /**
         * Source of the IO request
         */
        final ChannelModule _source;

        /**
         * Function to be performed
         */
        final IOFunction _ioFunction;

        /**
         * Reference to a byte buffer for byte data transfers for byte devices
         * Required for writes, should be null for reads - the device should create this.
         * Must be at least as large as indicated by _transferCount.
         */
        byte[] _byteBuffer;

        /**
         * Reference to a word buffer for word data transfers for word devices
         * Required for writes, should be null for reads - the device should create this.
         * Must be at least as large as indicated by _transferCount.
         */
        ArraySlice _wordBuffer;

        /**
         * Number of bytes to be transferred for byte devices
         * Number of words to be transferred for word devices
         */
        final int _transferCount;

        /**
         * Identifies a particular hardware block address or offset for device-addressed functions
         */
        final long _blockId;

        /**
         * Result of the operation
         */
        IOStatus _status = IOStatus.InvalidStatus;

        /**
         * Number of bytes or words successfully transferred - cannot be larger than _transferCount
         */
        int _transferredCount = 0;

        private IOInfo(
            ChannelModule source,
            IOFunction function,
            byte[] byteBuffer,
            ArraySlice wordBuffer,
            int transferCount,
            Long blockId
        ) {
            _source = source;
            _ioFunction = function;
            _byteBuffer = byteBuffer;
            _wordBuffer = wordBuffer;
            _transferCount = transferCount;
            _blockId = (blockId == null) ? 0 : blockId;
        }

        public static class NonTransferBuilder {

            private ChannelModule _source = null;
            private IOFunction _ioFunction = null;

            NonTransferBuilder setSource(ChannelModule value) { _source = value; return this; }
            NonTransferBuilder setIOFunction(IOFunction value) { _ioFunction = value; return this; }

            public IOInfo build() {
                if (_source == null) {
                    throw new RuntimeException("No source parameter");
                } else if (_ioFunction == null) {
                    throw new RuntimeException("No ioFunction parameter");
                }

                return new IOInfo(_source, _ioFunction, null, null, 0, null);
            }
        }

        public static class ByteTransferBuilder {

            private ChannelModule _source = null;
            private IOFunction _ioFunction = null;
            private byte[] _buffer = null;
            Integer _transferCount = null;
            Long _blockId = null;

            ByteTransferBuilder setSource(ChannelModule value) { _source = value; return this; }
            ByteTransferBuilder setIOFunction(IOFunction value) { _ioFunction = value; return this; }
            ByteTransferBuilder setBuffer(byte[] value) { _buffer = value; return this; }
            ByteTransferBuilder setTransferCount(int value) { _transferCount = value; return this; }
            ByteTransferBuilder setBlockId(long value) { _blockId = value; return this; }

            public IOInfo build() {
                if (_source == null) {
                    throw new RuntimeException("No source parameter");
                } else if (_ioFunction == null) {
                    throw new RuntimeException("No ioFunction parameter");
                } else if (_ioFunction.isWriteFunction() && _ioFunction.requiresBuffer() && (_buffer == null)) {
                    throw new RuntimeException("No buffer parameter");
                } else if ((_ioFunction.isReadFunction() || (!_ioFunction.requiresBuffer()))
                           && (_buffer != null)) {
                    throw new RuntimeException("Buffer wrongly provided");
                } else if (_transferCount == null) {
                    throw new RuntimeException("No transferCount parameter");
                }

                return new IOInfo(_source, _ioFunction, _buffer, null, _transferCount, _blockId);
            }
        }

        public static class WordTransferBuilder {

            private ChannelModule _source = null;
            private IOFunction _ioFunction = null;
            private ArraySlice _buffer = null;
            Integer _transferCount = null;
            Long _blockId = null;

            WordTransferBuilder setSource(ChannelModule value) { _source = value; return this; }
            WordTransferBuilder setIOFunction(IOFunction value) { _ioFunction = value; return this; }
            WordTransferBuilder setBuffer(ArraySlice value) { _buffer = value; return this; }
            WordTransferBuilder setTransferCount(int value) { _transferCount = value; return this; }
            WordTransferBuilder setBlockId(long value) { _blockId = value; return this; }

            public IOInfo build() {
                if (_source == null) {
                    throw new RuntimeException("No source parameter");
                } else if (_ioFunction == null) {
                    throw new RuntimeException("No ioFunction parameter");
                } else if (_ioFunction.isWriteFunction() && _ioFunction.requiresBuffer() && (_buffer == null)) {
                    throw new RuntimeException("No buffer parameter");
                } else if ((_ioFunction.isReadFunction() || (!_ioFunction.requiresBuffer()))
                           && (_buffer != null)) {
                    throw new RuntimeException("Buffer wrongly provided");
                } else if (_transferCount == null) {
                    throw new RuntimeException("No transferCount parameter");
                }

                return new IOInfo(_source, _ioFunction, null, _buffer, _transferCount, _blockId);
            }
        }

        /**
         * Produces a displayable string describing this object
         */
        @Override
        public String toString(
        ) {
            return String.format("Source=%s Fcn=%s BlkId=%s Count=%s Xferd=%s Stat=%s",
                                 _source == null ? "<null>" : _source._name,
                                 _ioFunction,
                                 _blockId,
                                 _transferCount,
                                 _transferredCount,
                                 _status);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    final Model _deviceModel;
    final Type _deviceType;

    /**
     * Indicates the device can do reads and writes.
     * If not ready, the device *may* respond to non-read/write IOs.
     */
    boolean _readyFlag;

    /**
     * Indicates that something regarding the physical characteristics of this device has changed.
     * Setting a device ready should ALWAYS set this flag.
     * All reads and writes will be rejected with IOStatus.UnitAttention until an IOFunction.GetInfo is issued.
     */
    boolean _unitAttentionFlag;

    /**
     * IO counters for this device - they are to be kept updated by the various device terminal subclasses.
     * TODO:Should a RESET function clear these?
     */
    long _miscCount = 0;        //  anything non-read/write
    long _readBytes = 0;        //  total bytes transferred from storage
    long _readCount = 0;
    long _writeBytes = 0;       //  total bytes transferred to storage
    long _writeCount = 0;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    protected Device(
        final Type deviceType,
        final Model deviceModel,
        final String name
    ) {
        super(NodeCategory.Device, name);
        _deviceModel = deviceModel;
        _deviceType = deviceType;
        _readyFlag = false;
        _unitAttentionFlag = false;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Invoked when a new session is started by the system processor.
     * Any classes which have work to do to enter the cleared state should override this.
     */
    @Override
    public void clear() {}

    /**
     * IO Router - what happens here is depending upon the subclass
     * If we return false, it means we've completed the IO - there will be no scheduling or notification of completion
     */
    public abstract boolean handleIo(IOInfo ioInfo);

    /**
     * Does this device have a byte interface?
     */
    public abstract boolean hasByteInterface();

    /**
     * Does this device have a word interface?
     */
    public abstract boolean hasWordInterface();

    /**
     * Initializes the subclass if/as necessary
     */
    @Override
    public abstract void initialize();

    /**
     * Invoked just before tearing down the configuration.
     */
    @Override
    public abstract void terminate();

    /**
     * Writes IO buffers to the log.
     * What actually happens is dependent upon the particulars of the various subclasses, so this is abstract
     * @param ioInfo describes the IO buffer(s)
     */
    protected abstract void writeBuffersToLog(IOInfo ioInfo);


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * For debugging purposes
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        try {
            writer.write(String.format("  ProcessorType:            %s\n", _deviceType.toString()));
            writer.write(String.format("  Model:           %s\n", _deviceModel.toString()));
            writer.write(String.format("  Ready:           %s\n", _readyFlag));
            writer.write(String.format("  Unit Attention:  %s\n", _unitAttentionFlag));
            writer.write(String.format("  Misc IO Count:   %d\n", _miscCount));
            writer.write(String.format("  Read IO Count:   %d\n", _readCount));
            writer.write(String.format("  Read Bytes:      %d\n", _readBytes));
            writer.write(String.format("  Write IO Count:  %d\n", _writeCount));
            writer.write(String.format("  Write Bytes:     %d\n", _writeBytes));
        } catch (IOException ex) {
            _logger.catching(ex);
        }
    }

    /**
     * Produces an array slice which represents, in 36-bit mode, the device info data which is presented to the requestor.
     * Subclasses should call here first to get an ArraySlice with the basic info, add more info, and return the whole thing.
     * The format of the basic info block is:
     *      +----------+----------+----------+----------+----------+----------+
     *  +0  |  FLAGS   |  MODEL   |   TYPE   |             zeroes             |
     *      +----------+----------+----------+----------+----------+----------+
     *  +1  |                          MISC_IO_COUNT                          |
     *      +----------+----------+----------+----------+----------+----------+
     *  +2  |                          READ_IO_COUNT                          |
     *      +----------+----------+----------+----------+----------+----------+
     *  +3  |                          WRITE_IO_COUNT                         |
     *      +----------+----------+----------+----------+----------+----------+
     *  +4  |                          READ_IO_BYTES                          |
     *      +----------+----------+----------+----------+----------+----------+
     *  +5  |                          WRITE_IO_BYTES                         |
     *      +----------+----------+----------+----------+----------+----------+
     *  +6  |                                                                 |
     *  ..  |                      device-specific data                       |
     * +27  |                                                                 |
     *      +----------+----------+----------+----------+----------+----------+
     *
     * FLAGS:
     *      Bit 0:  device_ready
     * MODEL:       integer code for the Model
     * TYPE:        integer code for the ProcessorType
     */
    protected ArraySlice getInfo() {
        long[] buffer = new long[28];
        buffer[0] = Word36.setS1(buffer[0], _readyFlag ? 040 : 0);
        buffer[0] = Word36.setS2(buffer[0], _deviceModel.getCode());
        buffer[0] = Word36.setS3(buffer[0], _deviceType.getCode());
        buffer[1] = _miscCount;
        buffer[2] = _readCount;
        buffer[3] = _writeCount;
        buffer[4] = _readBytes;
        buffer[5] = _writeBytes;
        return new ArraySlice(buffer);
    }

    /**
     * Subclasses must call here at the end of handling an IO
     */
    void ioEnd(
        final IOInfo ioInfo
    ) {
        if (LOG_IO_ERRORS) {
            if ((ioInfo._status != IOStatus.Successful) && (ioInfo._status != IOStatus.NoInput)) {
                _logger.error(String.format("IoError:%s", ioInfo.toString()));
            }
        }

        if (LOG_DEVICE_IO_BUFFERS) {
            if (ioInfo._ioFunction.isReadFunction() && (ioInfo._status == IOStatus.Successful)) {
                writeBuffersToLog(ioInfo);
            }
        }
    }

    /**
     * Subclasses must call this at the beginning of handling an IO.
     */
    void ioStart(
        final IOInfo ioInfo
    ) {
        if (LOG_DEVICE_IOS) {
            _logger.trace("ioStart():" + ioInfo.toString());
        }

        if (LOG_DEVICE_IO_BUFFERS) {
            if (ioInfo._ioFunction.isWriteFunction()) {
                writeBuffersToLog(ioInfo);
            }
        }
    }

    /**
     * Sets the device ready or not-ready, depending upon the given state parameter.
     * Subclasses may override this to do more specific stuff (such as verifying that the state can be set as request),
     * but at some point they must call back here to actually effect the state change.
     * @param state true to set ready, false to set not-ready
     * @return true if successful
     */
    boolean setReady(
        final boolean state
    ) {
        EntryMessage em = _logger.traceEntry("setReady(state={})", state);
        _readyFlag = state;
        setUnitAttention(state);
        _logger.traceExit(em, true);
        return true;
    }

    /**
     * Sets the unit attention flag (or clears it)
     */
    void setUnitAttention(
        final boolean state
    ) {
        EntryMessage em = _logger.traceEntry("setUnitAttention(state={})", state);
        _unitAttentionFlag = state;
        _logger.traceExit(em);
    }
}
