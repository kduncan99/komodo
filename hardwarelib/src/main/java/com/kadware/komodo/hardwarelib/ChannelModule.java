/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.Word36;
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
    //  Nested things
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Represents the various methods of translating byte streams to word buffers
     */
    public enum ByteTranslationFormat {
        QuarterWordPerByte(0),                  //  Format A
        SixthWordByte(1),                       //  Format B
        QuarterWordPacked(2),                //  Format C
        QuarterWordPerByteNoTermination(3);     //  Format D

        private final int _code;
        ByteTranslationFormat(int code) { _code = code; }
        public int getCode() { return _code; }

        public static ByteTranslationFormat getValue(
            final int code
        ) {
            switch (code) {
                case 0:     return QuarterWordPerByte;
                case 1:     return SixthWordByte;
                case 2:     return QuarterWordPacked;
                case 3:     return QuarterWordPerByteNoTermination;
                default:    return null;
            }
        }
    }

    /**
     * Indicates the type of the channel module
     */
    public enum ChannelModuleType {

        Byte(1),
        Word(2);

        private final int _code;
        ChannelModuleType(int code) { _code = code; }
        public int getCode() { return _code; }

        public static ChannelModuleType getValue(
            final int code
        ) {
            switch (code) {
                case 1:     return Byte;
                case 2:     return Word;
                default:    return null;
            }
        }
    }

    /**
     * Sort of a channel program.
     * Essentially it is a packet in storage which describes a requested IO.
     *
     * The channel program is formatted as follows:
     *      +----------+----------+----------+----------+----------+----------+
     * +00: | IOP UPI  |  CHMOD   |  DEVNUM  | FUNCTION |  FORMAT  |  #ACWS   |
     *      +----------+----------+----------+----------+----------+----------+
     * +01: |                      DEVICE_BLOCK_ADDRESS                       |
     *      +----------+----------+----------+----------+----------+----------+
     * +02: | CHAN_STS | DEV_STS  |RES_BYTES |            reserved            |
     *      +----------+----------+----------+----------+----------+----------+
     * +03: |            reserved            |       WORDS_TRANSFERRED        |
     *      +----------+----------+----------+----------+----------+----------+
     * +04: |                {n} 3-WORD ACCESS_CONTROL_WORDS                  |
     *      |                           (see +0 S6)                           |
     *      +----------+----------+----------+----------+----------+----------+
     *
     *      IOP UPI:                UPI number of IOP which should process this request
     *                                  (mostly ignored, as this is already being handled by the IOP in question)
     *      CHMOD:                  Index of channel module relative to the IOP
     *      DEVNUM:                 Device number of the device relative to the  channel module
     *      FUNCTION:               See IOFunction class
     *      FORMAT:                 Only for byte transfers
     *                                  00: Format A, quarter word -> byte, high bit not transferred
     *                                      if high bit is set on input, transfer ends
     *                                  01: Format B, sixth word -> byte
     *                                  02: Format C, 2 words unpacked into 9 bytes
     *                                  03: Format D, same as A except high bit is always ignored
     *      #ACS:                   Number of access control words present in the packet
     *      DEVICE_BLOCK_ADDRESS:   For disk IO - starting block number of the IO
     *      CHAN_STS:               Channel status word - see ChannelStatus class
     *      DEV_STS:                Device status word - see IOStatus class
     *                                  only valid if CHAN_STS indicates a device status is present
     *      RES_BYTES:              Number of bytes/frames in excess of a full word, written to or read
     *                                  i.e., at 4 bytes per word, an 11 byte transfer has a residual bytes value of 3
     *                                  only applies to byte transfers
     *      WORDS_TRANSFERRED:      Number of complete or partial words actually transferred
     *      ACCESS_CONTROL_WORDS:   See AccessControlWord class
     */
    @SuppressWarnings("Duplicates")
    public static class ChannelProgram extends ArraySlice {

        /**
         * Constructor for builder
         */
        private ChannelProgram(
            final int iopUpiIndex,
            final int channelModuleIndex,
            final int deviceAddress,
            final Device.IOFunction ioFunction,
            final ByteTranslationFormat byteFormat,
            final Long blockId,
            final AccessControlWord[] accessControlWords
        ) {
            super(new long[4 + (accessControlWords != null ? 3 * accessControlWords.length : 0)]);
            setIopUpiIndex(iopUpiIndex);
            setChannelModuleIndex(channelModuleIndex);
            setDeviceAddress(deviceAddress);
            setIOFunction(ioFunction);

            if (byteFormat != null) {
                setByteTranslationFormat(byteFormat);
            }

            if (blockId != null) {
                setBlockId(blockId);
            }

            if (accessControlWords != null) {
                set(0, Word36.setS6(get(0), accessControlWords.length));
                int dx = _offset + 4;
                for (AccessControlWord acw : accessControlWords) {
                    _array[dx++] = acw._array[acw._offset];
                    _array[dx++] = acw._array[acw._offset + 1];
                    _array[dx++] = acw._array[acw._offset + 2];
                }
            }
        }

        /**
         * Constructor from storage
         */
        public ChannelProgram (
            final ArraySlice baseSlice,
            final int offset,
            final int length
        ) {
            super(baseSlice, offset, length);
        }

        /**
         * Constructor from storage, which uses the number of ACWs in the channel program located
         * at the storage offset to determine how long the channel program should be.
         * @param baseSlice describes the general storage where the channel program exists
         * @param offset offset from the start of the storage where the channel program exists
         * @return new ChannelProgram array slice
         */
        public static ChannelProgram create(
            final ArraySlice baseSlice,
            final int offset
        ) {
            int acws = (int) baseSlice.get(offset) & 077;
            int len = 4 + (3 * acws);
            return new ChannelProgram(baseSlice, offset, len);
        }

        //  Getters
        public int getIopUpiIndex() { return (int) Word36.getS1(get(0)); }
        public int getChannelModuleIndex() { return (int) Word36.getS2(get(0)); }
        public int getDeviceAddress() { return (int) Word36.getS3(get(0)); }
        public Device.IOFunction getFunction() { return Device.IOFunction.getValue((int) Word36.getS4(get(0))); }
        public ByteTranslationFormat getByteTranslationFormat() { return ByteTranslationFormat.getValue((int) Word36.getS5(get(0))); }
        public int getAccessControlWordCount() { return (int) Word36.getS6(get(0)); }
        public long getBlockId() { return get(1); }
        public ChannelStatus getChannelStatus() { return ChannelStatus.getValue((int) Word36.getS1(get(2))); }
        public Device.IOStatus getDeviceStatus() { return Device.IOStatus.getValue((int) Word36.getS2(get(2))); }
        public int getResidualBytes() { return (int) Word36.getS3(get(2)); }
        public int getWordsTransferred() { return (int) Word36.getH2(get(3)); }

        public AccessControlWord getAccessControlWord(
            final int index
        ) {
            return new AccessControlWord(this, 4 + 3 * index);
        }

        //  Setters
        public void setIopUpiIndex(int value) { set(0, Word36.setS1(get(0), value)); }
        public void setChannelModuleIndex(int value) { set(0, Word36.setS2(get(0), value)); }
        public void setDeviceAddress(int value) { set(0, Word36.setS3(get(0), value)); }
        public void setIOFunction(Device.IOFunction func) { set(0, Word36.setS4(get(0), func.getCode())); }
        public void setByteTranslationFormat(ByteTranslationFormat fmt) { set(0, Word36.setS5(get(0), fmt.getCode())); }
        public void setBlockId(long addr) { set(1, addr & 0_777777_777777L); }
        public void setChannelStatus(ChannelStatus stat) { set(2, Word36.setS1(get(2), stat.getCode())); }
        public void setDeviceStatus(Device.IOStatus stat) { set(2, Word36.setS2(get(2), stat.getCode())); }
        public void setResidualBytes(int value) { set(2, Word36.setS3(get(2), value)); }
        public void setWordsTransferred(int value) { set(3, Word36.setH2(get(3), value)); }

        public int getCumulativeTransferWords() {
            int result = 0;
            for (int acwx = 0; acwx < getAccessControlWordCount(); ++acwx) {
                result += getAccessControlWord(acwx).getBufferSize();
            }

            return result;
        }

        public static class Builder {

            private Integer _iopUpiIndex = null;
            private Integer _channelModuleIndex = null;
            private Integer _deviceAddress = null;
            private Device.IOFunction _ioFunction = null;
            private ByteTranslationFormat _byteFormat = null;
            private Long _blockId = null;
            private AccessControlWord[] _acws = null;

            public Builder setIopUpiIndex(int value) { _iopUpiIndex = value; return this; }
            public Builder setChannelModuleIndex(int value) { _channelModuleIndex = value; return this; }
            public Builder setDeviceAddress(int value) { _deviceAddress = value; return this; }
            public Builder setIOFunction(Device.IOFunction func) { _ioFunction = func; return this; }
            public Builder setByteTranslationFormat(ByteTranslationFormat fmt) { _byteFormat = fmt; return this; }
            public Builder setBlockId(long id) { _blockId = id & 0_777777_777777L; return this; }
            public Builder setAccessControlWords(AccessControlWord[] acws) { _acws = acws; return this; }

            public ChannelProgram build() {
                assert(_iopUpiIndex != null);
                assert(_channelModuleIndex != null);
                assert(_deviceAddress != null);
                assert(_ioFunction != null);

                if (_ioFunction.isReadFunction()
                    || (_ioFunction.isWriteFunction() && (_ioFunction != Device.IOFunction.WriteEndOfFile))) {
                    assert(_acws != null);
                }

                return new ChannelProgram(_iopUpiIndex,
                                          _channelModuleIndex,
                                          _deviceAddress,
                                          _ioFunction,
                                          _byteFormat,
                                          _blockId,
                                          _acws);

            }
        }
    }

    /**
     * Describes the status of a particular channel I/O
     */
    public enum ChannelStatus {
        Successful(0),
        Cancelled(1),
        DeviceError(2),                     //  device status is available
        UnconfiguredChannelModule(3),       //  returned by IOP
        UnconfiguredDevice(4),
        InvalidAddress(5),
        InsufficientBuffers(6),             //  successful read, but some data was truncated
        InProgress(040),
        InvalidStatus(077);

        private final int _code;

        ChannelStatus(int code) { _code = code; }

        public int getCode() { return _code; }

        public static ChannelStatus getValue(
            final int code
        ) {
            switch (code) {
                case 0:     return Successful;
                case 1:     return Cancelled;
                case 2:     return DeviceError;
                case 3:     return UnconfiguredChannelModule;
                case 4:     return UnconfiguredDevice;
                case 5:     return InvalidAddress;
                case 6:     return InsufficientBuffers;
                case 040:   return InProgress;
                default:    return InvalidStatus;
            }
        }
    }

    /**
     * For tracking in-flight IOs - this base class should be extended by the ChannleModule subclass.
     */
    protected abstract class Tracker {
        public final Processor _source;
        public final InputOutputProcessor _ioProcessor;
        public final ChannelProgram _channelProgram;
        public Device.IOInfo _ioInfo = null;
        public boolean _started = false;
        public boolean _completed = false;

        //  For writes, this is the cumulative buffer containing all the data from all the ACWs
        //  after data direction has been applied.
        //  For reads, this is the buffer resulting from the IO, which must then be distributed among
        //  the various ACWs while observing data direction.
        public final ArraySlice _compositeBuffer;

        protected Tracker(
            final Processor source,
            final InputOutputProcessor ioProcessor,
            final ChannelProgram channelProgram,
            final ArraySlice compositeBuffer
        ) {
            _source = source;
            _ioProcessor = ioProcessor;
            _channelProgram = channelProgram;
            _compositeBuffer = compositeBuffer;
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
        final ChannelProgram channelProgram,
        final ArraySlice compositeBuffer
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
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                LOGGER.catching(ex);
            }
        }
    }

    /**
     * Put a channel program on our list of unstarted IOs and wake up the channel module thread.
     * If we find an obvious problem, we kill the IO here instead of scheduling it.
     * @param source Entity which initiated the IO - should be an IP or an SP
     * @param ioProcessor The IOP involved in the IO
     * @param channelProgram describes the IO
     * @param compositeBuffer If this is a read/write IO, it is read into/written from this buffer.
     *                          This is null for non-read/write IOs.
     */
    final boolean scheduleChannelProgram(
        final Processor source,
        final InputOutputProcessor ioProcessor,
        final ChannelProgram channelProgram,
        final ArraySlice compositeBuffer
    ) {
        if (!_descendants.containsKey(channelProgram.getDeviceAddress())) {
            channelProgram.setChannelStatus(ChannelStatus.UnconfiguredDevice);
            return false;
        }

        channelProgram.setChannelStatus(ChannelStatus.InProgress);
        synchronized (this) {
            _trackers.add(createTracker(source, ioProcessor, channelProgram, compositeBuffer));
            notify();
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
