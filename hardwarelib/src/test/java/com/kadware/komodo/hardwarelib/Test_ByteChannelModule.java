/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.BlockId;
import com.kadware.komodo.hardwarelib.exceptions.*;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * Unit tests for ByteChannelModule class
 */
@SuppressWarnings("Duplicates")
public class Test_ByteChannelModule {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Stub classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static class TestInputOutputProcessor extends InputOutputProcessor {

        private final List<ChannelProgram> _inFlight = new LinkedList<>();

        TestInputOutputProcessor(
        ) {
            super("IOP0", InventoryManager.FIRST_INPUT_OUTPUT_PROCESSOR_UPI_INDEX);
            try {
                InventoryManager.getInstance().addInputOutputProcessor(this);
            } catch (NodeNameConflictException | UPIConflictException ex) {
            }
        }

        @Override
        public boolean startIO(
            final Processor source,
            final ChannelProgram channelProgram
        ) {
            _inFlight.add(channelProgram);
            return true;
        }

        @Override
        public void finalizeIo(
            final ChannelProgram channelProgram,
            final Processor source
        ) {
            _inFlight.remove(channelProgram);
        }
    }

    private static class TestInstructionProcessor extends InstructionProcessor {

        TestInstructionProcessor() {
            super("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI_INDEX);
            try {
                InventoryManager.getInstance().addInstructionProcessor(this);
            } catch (NodeNameConflictException | UPIConflictException ex) {
            }
        }
    }

    private static class TestTapeDevice extends Device {

        interface Block {};
        class FileMarkBlock implements Block {}
        class DataBlock implements Block {
            final byte[] _data;
            DataBlock(byte[] data) { _data = data; }
        }

        final List<Block> _stream = new LinkedList<>();
        int _position = 0;      //  -1 is lost position

        TestTapeDevice() {
            super(DeviceType.Tape, DeviceModel.None, "TAPE0");
            setReady(true);
        }

        @Override
        public boolean canConnect(Node ancestor) { return true; }

        @Override
        public boolean handleIo(
            final DeviceIOInfo ioInfo
        ) {
            ioInfo._transferredCount = 0;
            switch (ioInfo._ioFunction) {
                case MoveBlock: {
                    if (!_readyFlag) {
                        ioInfo._status = DeviceStatus.NotReady;
                        break;
                    }
                    if (_position == _stream.size()) {
                        _position = -1;
                    }
                    if (_position < 0) {
                        ioInfo._status = DeviceStatus.LostPosition;
                        break;
                    }

                    if (_stream.get(_position++) instanceof FileMarkBlock) {
                        ioInfo._status = DeviceStatus.FileMark;
                    } else {
                        ioInfo._status = DeviceStatus.Successful;
                    }
                    break;
                }

                case MoveFile: {
                    if (!_readyFlag) {
                        ioInfo._status = DeviceStatus.NotReady;
                        break;
                    }
                    boolean done = false;
                    while (!done) {
                        if (_position < 0) {
                            ioInfo._status = DeviceStatus.LostPosition;
                            done = true;
                        } else if (_position == _stream.size()) {
                            _position = -1;
                            ioInfo._status = DeviceStatus.LostPosition;
                            done = true;
                        } else {
                            if (_stream.get(_position++) instanceof FileMarkBlock) {
                                ioInfo._status = DeviceStatus.FileMark;
                                done = true;
                            }
                        }
                    }
                    break;
                }

                case MoveBlockBackward: {
                    if (!_readyFlag) {
                        ioInfo._status = DeviceStatus.NotReady;
                        break;
                    }
                    if (_position < 0) {
                        ioInfo._status = DeviceStatus.LostPosition;
                        break;
                    }
                    if (_position == 0) {
                        ioInfo._status = DeviceStatus.EndOfTape;
                        break;
                    }

                    if (_stream.get(--_position) instanceof FileMarkBlock) {
                        ioInfo._status = DeviceStatus.FileMark;
                    } else {
                        ioInfo._status = DeviceStatus.Successful;
                    }
                    break;
                }

                case MoveFileBackward: {
                    if (!_readyFlag) {
                        ioInfo._status = DeviceStatus.NotReady;
                        break;
                    }
                    boolean done = false;
                    while (!done) {
                        if (_position < 0) {
                            ioInfo._status = DeviceStatus.LostPosition;
                            done = true;
                        } else if (_position == 0) {
                            ioInfo._status = DeviceStatus.EndOfTape;
                            done = true;
                        } else {
                            if (_stream.get(--_position) instanceof FileMarkBlock) {
                                ioInfo._status = DeviceStatus.FileMark;
                                done = true;
                            }
                        }
                    }
                    break;
                }

                case Read: {
                    if (!_readyFlag) {
                        ioInfo._status = DeviceStatus.NotReady;
                        break;
                    }
                    if (_position == _stream.size()) {
                        _position = -1;
                    }
                    if (_position < 0) {
                        ioInfo._status = DeviceStatus.LostPosition;
                        break;
                    }

                    Block block = _stream.get(_position++);
                    if (block instanceof FileMarkBlock) {
                        ioInfo._status = DeviceStatus.FileMark;
                        break;
                    }

                    ioInfo._byteBuffer = ((DataBlock) block)._data;
                    ioInfo._transferredCount = ioInfo._byteBuffer.length;
                    ioInfo._status = DeviceStatus.Successful;
                    break;
                }

                case ReadBackward: {
                    if (!_readyFlag) {
                        ioInfo._status = DeviceStatus.NotReady;
                        break;
                    }
                    if (_position < 0) {
                        ioInfo._status = DeviceStatus.LostPosition;
                        break;
                    }
                    if (_position == 0) {
                        ioInfo._status = DeviceStatus.EndOfTape;
                        break;
                    }

                    Block block = _stream.get(--_position);
                    if (block instanceof FileMarkBlock) {
                        ioInfo._status = DeviceStatus.FileMark;
                        break;
                    }

                    DataBlock dataBlock = (DataBlock) block;
                    ioInfo._byteBuffer = new byte[dataBlock._data.length];
                    for (int dx = 0, sx = dataBlock._data.length;
                         (dx < dataBlock._data.length) && (sx > 0); ++dx) {
                        ioInfo._byteBuffer[dx] = dataBlock._data[--sx];
                        ++ioInfo._transferredCount;
                    }
                    ioInfo._status = DeviceStatus.Successful;
                    break;
                }

                case Reset: {
                    _stream.clear();
                    _position = 0;
                    ioInfo._status = DeviceStatus.Successful;
                    break;
                }

                case Rewind: {
                    if (!_readyFlag) {
                        ioInfo._status = DeviceStatus.NotReady;
                        break;
                    }
                    _position = 0;
                    ioInfo._status = DeviceStatus.Successful;
                    break;
                }

                case RewindInterlock: {
                    if (!_readyFlag) {
                        ioInfo._status = DeviceStatus.NotReady;
                        break;
                    }
                    _position = 0;
                    _readyFlag = false;
                    ioInfo._status = DeviceStatus.Successful;
                    break;
                }

                case Write: {
                    if (!_readyFlag) {
                        ioInfo._status = DeviceStatus.NotReady;
                        break;
                    }
                    if (_position < 0) {
                        ioInfo._status = DeviceStatus.LostPosition;
                        break;
                    }

                    byte[] sourceArray = ioInfo._byteBuffer;
                    _stream.add(_position++, new DataBlock(Arrays.copyOf(sourceArray, sourceArray.length)));
                    ioInfo._status = DeviceStatus.Successful;
                    ioInfo._transferredCount = sourceArray.length;
                    break;
                }

                case WriteEndOfFile: {
                    if (!_readyFlag) {
                        ioInfo._status = DeviceStatus.NotReady;
                        break;
                    }
                    if (_position < 0) {
                        ioInfo._status = DeviceStatus.LostPosition;
                        break;
                    }

                    _stream.add(_position++, new FileMarkBlock());
                    ioInfo._status = DeviceStatus.Successful;
                    break;
                }

                default:
                    ioInfo._status = DeviceStatus.InvalidFunction;
            }

            //  We're always async, so we never 'schedule' an IO
            return false;
        }

        @Override
        public boolean hasByteInterface() { return true; }

        @Override
        public boolean hasWordInterface() { return false; }

        @Override
        public void initialize() {
            _stream.clear();
        }

        @Override
        public void terminate() {}

        @Override
        public void writeBuffersToLog(DeviceIOInfo ioInfo) {}
    }

    public static class TestDiskDevice extends Device {

        private final static int BLOCK_COUNT = 256;
        private final static int BLOCK_SIZE = 128;
        private final static int PREP_FACTOR = 28;
        private final Map<Long, byte[]> _dataStore = new HashMap<>();
        TestDiskDevice() { super(DeviceType.Disk, DeviceModel.None, "DISK0"); }

        @Override
        public boolean canConnect(Node ancestor) { return true; }

        @Override
        public boolean handleIo(
            final DeviceIOInfo ioInfo
        ) {
            ioInfo._transferredCount = 0;
            switch (ioInfo._ioFunction) {
                case Read:
                {
                    Long blockId = ioInfo._blockId.getValue();
                    if (blockId >= BLOCK_COUNT) {
                        ioInfo._status = DeviceStatus.InvalidBlockId;
                    }

                    ioInfo._byteBuffer = new byte[ioInfo._transferCount];
                    int dx = 0;
                    int bytesLeft = ioInfo._transferCount;
                    while (bytesLeft > 0) {
                        if (blockId >= BLOCK_COUNT) {
                            ioInfo._status = DeviceStatus.InvalidTransferSize;
                            return false;
                        }

                        byte[] dataBlock = _dataStore.get(blockId);
                        if (dataBlock == null) {
                            dataBlock = new byte[BLOCK_SIZE];
                            _dataStore.put(blockId, dataBlock);
                        }

                        int sx = 0;
                        while ((bytesLeft > 0) && (sx < BLOCK_SIZE)) {
                            ioInfo._byteBuffer[dx++] = dataBlock[sx++];
                            ++ioInfo._transferredCount;
                            --bytesLeft;
                        }

                        ++blockId;
                    }

                    ioInfo._status = DeviceStatus.Successful;
                    break;
                }

                case Write:
                {
                    int bytesLeft = ioInfo._transferCount;
                    Long blockId = ioInfo._blockId.getValue();
                    if (blockId >= BLOCK_COUNT) {
                        ioInfo._status = DeviceStatus.InvalidBlockId;
                    }

                    int sx = 0;
                    while (bytesLeft > 0) {
                        if (blockId >= BLOCK_COUNT) {
                            ioInfo._status = DeviceStatus.InvalidTransferSize;
                            return false;
                        }

                        byte[] dataBlock = _dataStore.get(blockId);
                        if (dataBlock == null) {
                            dataBlock = new byte[BLOCK_SIZE];
                            _dataStore.put(blockId, dataBlock);
                        }

                        for (int dx = 0;
                             (bytesLeft > 0) && (dx < BLOCK_SIZE);
                             ++ioInfo._transferredCount, --bytesLeft) {
                            dataBlock[dx++] = ioInfo._byteBuffer[sx++];
                        }

                        ++blockId;
                    }

                    ioInfo._status = DeviceStatus.Successful;
                    break;
                }

                default:
                    ioInfo._status = DeviceStatus.InvalidFunction;
            }

            //  We're always async, so we never 'schedule' an IO
            return false;
        }

        @Override
        public boolean hasByteInterface() { return true; }

        @Override
        public boolean hasWordInterface() { return false; }

        @Override
        public void initialize() {
            _dataStore.clear();
        }

        @Override
        public void terminate() {}

        @Override
        public void writeBuffersToLog(DeviceIOInfo ioInfo) {}
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  members
    //  ----------------------------------------------------------------------------------------------------------------------------

    private ByteChannelModule _cm = null;
    private int _cmIndex = 0;
    private Device _device = null;
    private int _deviceIndex = 0;
    private InstructionProcessor _ip = null;
    private InputOutputProcessor _iop = null;
    private MainStorageProcessor _msp = null;
    private final Random _random = new Random(System.currentTimeMillis());

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  useful methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    private void setup(
        final DeviceType deviceType
    ) throws CannotConnectException,
             MaxNodesException {
        //  Create stub nodes and one real channel module
        _ip = InventoryManager.getInstance().createInstructionProcessor();
        _iop = InventoryManager.getInstance().createInputOutputProcessor();
        _msp = InventoryManager.getInstance().createMainStorageProcessor();
        _cm = new ByteChannelModule("CM0-0");
        if (deviceType == DeviceType.Disk) {
            _device = new TestDiskDevice();
        } else if (deviceType == DeviceType.Tape) {
            _device = new TestTapeDevice();

        }

        _cmIndex = Math.abs(_random.nextInt()) % 6;
        _deviceIndex = Math.abs(_random.nextInt() % 16);
        Node.connect(_iop, _cmIndex, _cm);
        Node.connect(_cm, _deviceIndex, _device);
        _cm.initialize();
        _device.initialize();
    }

    private void teardown(
    ) throws UPINotAssignedException {
        _cm.terminate();
        _device.terminate();
        _cm = null;
        _device = null;
        InventoryManager.getInstance().deleteProcessor(_ip._upiIndex);
        InventoryManager.getInstance().deleteProcessor(_iop._upiIndex);
        InventoryManager.getInstance().deleteProcessor(_msp._upiIndex);
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  unit tests
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void create() {
        ByteChannelModule cm = new ByteChannelModule("CM1-01");
        assertEquals(NodeCategory.ChannelModule, cm._category);
        assertEquals(ChannelModuleType.Byte, cm._channelModuleType);
        assertEquals("CM1-01", cm._name);
    }

    @Test
    public void canConnect_success(
    ) throws CannotConnectException,
             MaxNodesException,
             UPINotAssignedException{
        setup(DeviceType.Disk);
        assertTrue(_cm.canConnect(_iop));
        teardown();
    }

    @Test
    public void canConnect_failure(
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             UPINotAssignedException{
        setup(DeviceType.Disk);
        ByteChannelModule cm = new ByteChannelModule("CM1-0");
        assertFalse(cm.canConnect(new FileSystemDiskDevice("DISK0")));
        assertFalse(cm.canConnect(new FileSystemTapeDevice("TAPE0")));
        assertFalse(cm.canConnect(new ByteChannelModule("CM1-0")));
        assertFalse(cm.canConnect(new WordChannelModule("CM1-1")));
        assertFalse(cm.canConnect(_msp));
        assertFalse(cm.canConnect(new InstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI_INDEX)));
        teardown();
    }

    @Test
    public void threadAlive_false_1() {
        ByteChannelModule cm = new ByteChannelModule("CM1-01");
        assertFalse(cm.isWorkerActive());
    }

    @Test
    public void threadAlive_false_2() {
        ByteChannelModule cm = new ByteChannelModule("CM1-01");
        cm.initialize();

        try {
            Thread.sleep(1000);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        cm.terminate();
        assertFalse(cm.isWorkerActive());
    }

    @Test
    public void threadAlive_true() {
        ByteChannelModule cm = new ByteChannelModule("CM1-01");
        cm.initialize();

        try {
            Thread.sleep(1000);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        assertTrue(cm.isWorkerActive());
        cm.terminate();
    }

    @Test
    public void unconfiguredDevice(
    ) throws CannotConnectException,
             MaxNodesException,
             UPINotAssignedException {
        setup(DeviceType.Disk);

        ChannelProgram cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                        .setChannelModuleIndex(0)
                                                        .setDeviceAddress(5)
                                                        .setIOFunction(IOFunction.Reset)
                                                        .build();
        boolean scheduled = _cm.scheduleChannelProgram(_ip, _iop, cp, null);

        assertFalse(scheduled);
        assertEquals(ChannelStatus.UnconfiguredDevice, cp.getChannelStatus());
        teardown();
    }

    @Test
    public void io_formatA(
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             UPINotAssignedException{
        setup(DeviceType.Tape);

        int[] blockSizes = { 28, 56, 112, 224, 448, 896, 1792, 1800 };
        long[][] sourceBuffers = new long[blockSizes.length][];
        ArraySlice[] mspStorages = new ArraySlice[blockSizes.length];
        for (int bsx = 0; bsx < blockSizes.length; ++bsx) {
            sourceBuffers[bsx] = new long[blockSizes[bsx]];
            for (int dx = 0; dx < sourceBuffers[bsx].length; ++dx) {
                sourceBuffers[bsx][dx] = _random.nextLong() & 0_377377_377377L;
            }

            int dataSegmentIndex = _msp.createSegment(blockSizes[bsx]);
            mspStorages[bsx] = _msp.getStorage(dataSegmentIndex);
            for (int dx = 0; dx < TestDiskDevice.PREP_FACTOR; ++dx) {
                mspStorages[bsx]._array[dx] = sourceBuffers[bsx][dx];
            }
        }

        //  Write data samples
        for (int bsx = 0; bsx < blockSizes.length; ++bsx) {
            ChannelProgram cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                            .setChannelModuleIndex(_cmIndex)
                                                            .setDeviceAddress(_deviceIndex)
                                                            .setIOFunction(IOFunction.Write)
                                                            .setAccessControlWords(new AccessControlWord[0])
                                                            .setByteTranslationFormat(ByteTranslationFormat.QuarterWordPerByte)
                                                            .build();
            boolean started = _cm.scheduleChannelProgram(_ip, _iop, cp, new ArraySlice(sourceBuffers[bsx]));
            if (started) {
                while (cp.getChannelStatus() == ChannelStatus.InProgress) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }

            if (cp.getChannelStatus() != ChannelStatus.Successful) {
                System.out.println(String.format("ChStat:%s DevStat:%s", cp.getChannelStatus(), cp.getDeviceStatus()));
            }
            assertEquals(ChannelStatus.Successful, cp.getChannelStatus());
            assertEquals(blockSizes[bsx], cp.getWordsTransferred());
            assertEquals(0, cp.getResidualBytes());
        }

        //  Write two file marks and rewind
        ChannelProgram eofcp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                           .setChannelModuleIndex(_cmIndex)
                                                           .setDeviceAddress(_deviceIndex)
                                                           .setIOFunction(IOFunction.WriteEndOfFile)
                                                           .build();
        boolean started = _cm.scheduleChannelProgram(_ip, _iop, eofcp, null);
        if (started) {
            while (eofcp.getChannelStatus() == ChannelStatus.InProgress) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
        if (eofcp.getChannelStatus() != ChannelStatus.Successful) {
            System.out.println(String.format("ChStat:%s DevStat:%s", eofcp.getChannelStatus(), eofcp.getDeviceStatus()));
        }
        assertEquals(ChannelStatus.Successful, eofcp.getChannelStatus());

        started = _cm.scheduleChannelProgram(_ip, _iop, eofcp, null);
        if (started) {
            while (eofcp.getChannelStatus() == ChannelStatus.InProgress) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
        if (eofcp.getChannelStatus() != ChannelStatus.Successful) {
            System.out.println(String.format("ChStat:%s DevStat:%s", eofcp.getChannelStatus(), eofcp.getDeviceStatus()));
        }
        assertEquals(ChannelStatus.Successful, eofcp.getChannelStatus());

        ChannelProgram rewcp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                           .setChannelModuleIndex(_cmIndex)
                                                           .setDeviceAddress(_deviceIndex)
                                                           .setIOFunction(IOFunction.Rewind)
                                                           .build();
        started = _cm.scheduleChannelProgram(_ip, _iop, rewcp, null);
        if (started) {
            while (rewcp.getChannelStatus() == ChannelStatus.InProgress) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
        if (rewcp.getChannelStatus() != ChannelStatus.Successful) {
            System.out.println(String.format("ChStat:%s DevStat:%s", rewcp.getChannelStatus(), rewcp.getDeviceStatus()));
        }
        assertEquals(ChannelStatus.Successful, rewcp.getChannelStatus());

        //  Read data samples
        for (int bsx = 0; bsx < blockSizes.length; ++bsx) {
            long[] dataResult = new long[8000];
            ChannelProgram cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                            .setChannelModuleIndex(_cmIndex)
                                                            .setDeviceAddress(_deviceIndex)
                                                            .setIOFunction(IOFunction.Read)
                                                            .setAccessControlWords(new AccessControlWord[0])
                                                            .setByteTranslationFormat(ByteTranslationFormat.QuarterWordPerByte)
                                                            .build();
            started = _cm.scheduleChannelProgram(_ip, _iop, cp, new ArraySlice(dataResult));
            if (started) {
                while (cp.getChannelStatus() == ChannelStatus.InProgress) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }

            if (cp.getChannelStatus() != ChannelStatus.Successful) {
                System.out.println(String.format("ChStat:%s DevStat:%s", cp.getChannelStatus(), cp.getDeviceStatus()));
            }

            assertEquals(ChannelStatus.Successful, cp.getChannelStatus());
            assertEquals(blockSizes[bsx], cp.getWordsTransferred());
            assertEquals(0, cp.getResidualBytes());
            assertArrayEquals(sourceBuffers[bsx], Arrays.copyOf(dataResult, sourceBuffers[bsx].length));
        }

        teardown();
    }

    @Test
    public void io_formatA_smallBuffer() {
        //TODO - for tape
    }

    @Test
    public void io_formatA_residual() {
        //TODO - for tape
    }

    @Test
    public void io_formatA_stopBit() {
        //TODO - for tape
    }

    @Test
    public void io_formatA_backward(
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             UPINotAssignedException {
        setup(DeviceType.Tape);

        int blockSize = 1800;
        long[] sourceBuffer = new long[blockSize];
        for (int dx = 0; dx < blockSize; ++dx) {
            sourceBuffer[dx] = _random.nextLong() & 0_377377_377377L;
        }

        int dataSegmentIndex = _msp.createSegment(blockSize);
        ArraySlice mspStorage = _msp.getStorage(dataSegmentIndex);

        //  Write data block
        ChannelProgram cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                        .setChannelModuleIndex(_cmIndex)
                                                        .setDeviceAddress(_deviceIndex)
                                                        .setIOFunction(IOFunction.Write)
                                                        .setAccessControlWords(new AccessControlWord[0])
                                                        .setByteTranslationFormat(ByteTranslationFormat.QuarterWordPerByte)
                                                        .build();
        boolean started = _cm.scheduleChannelProgram(_ip, _iop, cp, new ArraySlice(sourceBuffer));
        if (started) {
            while (cp.getChannelStatus() == ChannelStatus.InProgress) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }

        if (cp.getChannelStatus() != ChannelStatus.Successful) {
            System.out.println(String.format("ChStat:%s DevStat:%s", cp.getChannelStatus(), cp.getDeviceStatus()));
        }
        assertEquals(ChannelStatus.Successful, cp.getChannelStatus());
        assertEquals(blockSize, cp.getWordsTransferred());
        assertEquals(0, cp.getResidualBytes());

        long[] dataResult = new long[8000];
        cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                         .setChannelModuleIndex(_cmIndex)
                                         .setDeviceAddress(_deviceIndex)
                                         .setIOFunction(IOFunction.ReadBackward)
                                         .setAccessControlWords(new AccessControlWord[0])
                                         .setByteTranslationFormat(ByteTranslationFormat.QuarterWordPerByte)
                                         .build();
        started = _cm.scheduleChannelProgram(_ip, _iop, cp, new ArraySlice(dataResult));
        if (started) {
            while (cp.getChannelStatus() == ChannelStatus.InProgress) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }

        if (cp.getChannelStatus() != ChannelStatus.Successful) {
            System.out.println(String.format("ChStat:%s DevStat:%s", cp.getChannelStatus(), cp.getDeviceStatus()));
        }

        assertEquals(ChannelStatus.Successful, cp.getChannelStatus());
        assertEquals(blockSize, cp.getWordsTransferred());
        assertEquals(0, cp.getResidualBytes());
        assertArrayEquals(sourceBuffer, Arrays.copyOf(dataResult, sourceBuffer.length));

        teardown();
    }

    @Test
    public void io_formatA_backwardPartial() {
        //TODO
    }

    @Test
    public void io_formatB(
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             UPINotAssignedException{
        setup(DeviceType.Tape);

        int[] blockSizes = { 28, 56, 112, 224, 448, 896, 1792, 1800 };
        long[][] sourceBuffers = new long[blockSizes.length][];
        ArraySlice[] mspStorages = new ArraySlice[blockSizes.length];
        for (int bsx = 0; bsx < blockSizes.length; ++bsx) {
            sourceBuffers[bsx] = new long[blockSizes[bsx]];
            for (int dx = 0; dx < sourceBuffers[bsx].length; ++dx) {
                sourceBuffers[bsx][dx] = _random.nextLong() & 0_777777_777777L;
            }

            int dataSegmentIndex = _msp.createSegment(blockSizes[bsx]);
            mspStorages[bsx] = _msp.getStorage(dataSegmentIndex);
            for (int dx = 0; dx < TestDiskDevice.PREP_FACTOR; ++dx) {
                mspStorages[bsx]._array[dx] = sourceBuffers[bsx][dx];
            }
        }

        //  Write data samples
        for (int bsx = 0; bsx < blockSizes.length; ++bsx) {
            ChannelProgram cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                .setChannelModuleIndex(_cmIndex)
                .setDeviceAddress(_deviceIndex)
                .setIOFunction(IOFunction.Write)
                .setAccessControlWords(new AccessControlWord[0])
                .setByteTranslationFormat(ByteTranslationFormat.SixthWordByte)
                .build();
            boolean started = _cm.scheduleChannelProgram(_ip, _iop, cp, new ArraySlice(sourceBuffers[bsx]));
            if (started) {
                while (cp.getChannelStatus() == ChannelStatus.InProgress) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }

            if (cp.getChannelStatus() != ChannelStatus.Successful) {
                System.out.println(String.format("ChStat:%s DevStat:%s", cp.getChannelStatus(), cp.getDeviceStatus()));
            }
            assertEquals(ChannelStatus.Successful, cp.getChannelStatus());
            assertEquals(blockSizes[bsx], cp.getWordsTransferred());
            assertEquals(0, cp.getResidualBytes());
        }

        //  Write two file marks and rewind
        ChannelProgram eofcp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
            .setChannelModuleIndex(_cmIndex)
            .setDeviceAddress(_deviceIndex)
            .setIOFunction(IOFunction.WriteEndOfFile)
            .build();
        boolean started = _cm.scheduleChannelProgram(_ip, _iop, eofcp, null);
        if (started) {
            while (eofcp.getChannelStatus() == ChannelStatus.InProgress) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
        if (eofcp.getChannelStatus() != ChannelStatus.Successful) {
            System.out.println(String.format("ChStat:%s DevStat:%s", eofcp.getChannelStatus(), eofcp.getDeviceStatus()));
        }
        assertEquals(ChannelStatus.Successful, eofcp.getChannelStatus());

        started = _cm.scheduleChannelProgram(_ip, _iop, eofcp, null);
        if (started) {
            while (eofcp.getChannelStatus() == ChannelStatus.InProgress) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
        if (eofcp.getChannelStatus() != ChannelStatus.Successful) {
            System.out.println(String.format("ChStat:%s DevStat:%s", eofcp.getChannelStatus(), eofcp.getDeviceStatus()));
        }
        assertEquals(ChannelStatus.Successful, eofcp.getChannelStatus());

        ChannelProgram rewcp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
            .setChannelModuleIndex(_cmIndex)
            .setDeviceAddress(_deviceIndex)
            .setIOFunction(IOFunction.Rewind)
            .build();
        started = _cm.scheduleChannelProgram(_ip, _iop, rewcp, null);
        if (started) {
            while (rewcp.getChannelStatus() == ChannelStatus.InProgress) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
        if (rewcp.getChannelStatus() != ChannelStatus.Successful) {
            System.out.println(String.format("ChStat:%s DevStat:%s", rewcp.getChannelStatus(), rewcp.getDeviceStatus()));
        }
        assertEquals(ChannelStatus.Successful, rewcp.getChannelStatus());

        //  Read data samples
        for (int bsx = 0; bsx < blockSizes.length; ++bsx) {
            long[] dataResult = new long[8000];
            ChannelProgram cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                .setChannelModuleIndex(_cmIndex)
                .setDeviceAddress(_deviceIndex)
                .setIOFunction(IOFunction.Read)
                .setAccessControlWords(new AccessControlWord[0])
                .setByteTranslationFormat(ByteTranslationFormat.SixthWordByte)
                .build();
            started = _cm.scheduleChannelProgram(_ip, _iop, cp, new ArraySlice(dataResult));
            if (started) {
                while (cp.getChannelStatus() == ChannelStatus.InProgress) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }

            if (cp.getChannelStatus() != ChannelStatus.Successful) {
                System.out.println(String.format("ChStat:%s DevStat:%s", cp.getChannelStatus(), cp.getDeviceStatus()));
            }

            assertEquals(ChannelStatus.Successful, cp.getChannelStatus());
            assertEquals(blockSizes[bsx], cp.getWordsTransferred());
            assertEquals(0, cp.getResidualBytes());
            assertArrayEquals(sourceBuffers[bsx], Arrays.copyOf(dataResult, sourceBuffers[bsx].length));
        }

        teardown();
    }

    @Test
    public void io_formatB_smallBuffer() {
        //TODO - for tape
    }

    @Test
    public void io_formatB_residual() {
        //TODO - for tape
    }

    @Test
    public void io_formatC_oneBlock(
    ) throws AddressingExceptionInterrupt,
                 CannotConnectException,
                 MaxNodesException,
                 UPINotAssignedException {
        setup(DeviceType.Disk);

        //  Populate MSP storage
        Random r = new Random(System.currentTimeMillis());
        long[] dataSample = new long[TestDiskDevice.PREP_FACTOR];
        for (int dx = 0; dx < dataSample.length; ++dx) {
            dataSample[dx] = r.nextLong() & 0_777777_777777L;
        }

        int dataSegmentIndex = _msp.createSegment(1024);
        ArraySlice dataStorage = _msp.getStorage(dataSegmentIndex);
        for (int dx = 0; dx < TestDiskDevice.PREP_FACTOR; ++dx) {
            dataStorage._array[dx] = dataSample[dx];
        }

        //  blank acws so builder won't complain
        AccessControlWord[] acws = {};
        for (int blockId = 0; blockId < TestDiskDevice.BLOCK_COUNT; ++blockId) {
            //  Write data sample
            ChannelProgram cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                            .setChannelModuleIndex(_cmIndex)
                                                            .setDeviceAddress(_deviceIndex)
                                                            .setIOFunction(IOFunction.Write)
                                                            .setBlockId(new BlockId(blockId))
                                                            .setAccessControlWords(acws)
                                                            .setByteTranslationFormat(ByteTranslationFormat.QuarterWordPacked)
                                                            .build();
            boolean started = _cm.scheduleChannelProgram(_ip, _iop, cp, new ArraySlice(dataSample));
            if (started) {
                while (cp.getChannelStatus() == ChannelStatus.InProgress) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }

            if (cp.getChannelStatus() != ChannelStatus.Successful) {
                System.out.println(String.format("ChStat:%s DevStat:%s", cp.getChannelStatus(), cp.getDeviceStatus()));
            }
            assertEquals(ChannelStatus.Successful, cp.getChannelStatus());

            //  Read data sample
            long[] dataResult = new long[TestDiskDevice.PREP_FACTOR];
            cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                             .setChannelModuleIndex(_cmIndex)
                                             .setDeviceAddress(_deviceIndex)
                                             .setIOFunction(IOFunction.Read)
                                             .setBlockId(new BlockId(blockId))
                                             .setAccessControlWords(acws)
                                             .setByteTranslationFormat(ByteTranslationFormat.QuarterWordPacked)
                                             .build();
            started = _cm.scheduleChannelProgram(_ip, _iop, cp, new ArraySlice(dataResult));
            if (started) {
                while (cp.getChannelStatus() == ChannelStatus.InProgress) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }

            if (cp.getChannelStatus() != ChannelStatus.Successful) {
                System.out.println(String.format("ChStat:%s DevStat:%s", cp.getChannelStatus(), cp.getDeviceStatus()));
            }
            assertEquals(ChannelStatus.Successful, cp.getChannelStatus());
            assertArrayEquals(dataSample, dataResult);
        }

        teardown();
    }

    @Test
    public void io_formatC_smallBuffer() {
        //TODO - for disk
    }

    @Test
    public void io_formatC_multipleBlocks(
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             UPINotAssignedException {
        setup(DeviceType.Disk);

        int maxBlocks = 16;

        //  Populate MSP storage
        long[] dataSample = new long[maxBlocks * TestDiskDevice.PREP_FACTOR];
        for (int dx = 0; dx < dataSample.length; ++dx) {
            dataSample[dx] = _random.nextLong() & 0_777777_777777L;
        }

        int dataSegmentIndex = _msp.createSegment(1024);
        ArraySlice dataStorage = _msp.getStorage(dataSegmentIndex);
        for (int dx = 0; dx < TestDiskDevice.PREP_FACTOR; ++dx) {
            dataStorage._array[dx] = dataSample[dx];
        }

        //  blank acws so builder won't complain
        AccessControlWord[] acws = {};
        int blockId = 0;
        while (blockId < TestDiskDevice.BLOCK_COUNT) {
            int blockCount = _random.nextInt() % maxBlocks;
            if (blockCount < 0) { blockCount = -blockCount; }
            if (blockCount == 0) { blockCount = 1; }
            if (blockId + blockCount >= TestDiskDevice.BLOCK_COUNT) { blockCount = 1; }

            //  Write data sample
            ChannelProgram cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                            .setChannelModuleIndex(_cmIndex)
                                                            .setDeviceAddress(_deviceIndex)
                                                            .setIOFunction(IOFunction.Write)
                                                            .setBlockId(new BlockId(blockId))
                                                            .setAccessControlWords(acws)
                                                            .setByteTranslationFormat(ByteTranslationFormat.QuarterWordPacked)
                                                            .build();
            ArraySlice asBuffer = new ArraySlice(dataSample, 0, blockCount * TestDiskDevice.PREP_FACTOR);
            boolean started = _cm.scheduleChannelProgram(_ip, _iop, cp, asBuffer);
            if (started) {
                while (cp.getChannelStatus() == ChannelStatus.InProgress) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }

            if (cp.getChannelStatus() != ChannelStatus.Successful) {
                System.out.println(String.format("ChStat:%s DevStat:%s", cp.getChannelStatus(), cp.getDeviceStatus()));
            }
            assertEquals(ChannelStatus.Successful, cp.getChannelStatus());

            //  Read data sample
            long[] dataResult = new long[blockCount * TestDiskDevice.PREP_FACTOR];
            cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                             .setChannelModuleIndex(_cmIndex)
                                             .setDeviceAddress(_deviceIndex)
                                             .setIOFunction(IOFunction.Read)
                                             .setBlockId(new BlockId(blockId))
                                             .setAccessControlWords(acws)
                                             .setByteTranslationFormat(ByteTranslationFormat.QuarterWordPacked)
                                             .build();
            started = _cm.scheduleChannelProgram(_ip, _iop, cp, new ArraySlice(dataResult));
            if (started) {
                while (cp.getChannelStatus() == ChannelStatus.InProgress) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }

            if (cp.getChannelStatus() != ChannelStatus.Successful) {
                System.out.println(String.format("ChStat:%s DevStat:%s", cp.getChannelStatus(), cp.getDeviceStatus()));
            }
            assertEquals(ChannelStatus.Successful, cp.getChannelStatus());
            assertArrayEquals(asBuffer.getAll(), dataResult);
            blockId += blockCount;
        }

        teardown();
    }

    @Test
    public void io_formatC_residual(
    ) throws AddressingExceptionInterrupt,
            CannotConnectException,
            MaxNodesException,
            UPINotAssignedException {
        setup(DeviceType.Disk);

        int maxBlockSize = 8 * TestDiskDevice.PREP_FACTOR;

        //  Populate MSP storage
        long[] dataSample = new long[maxBlockSize];
        for (int dx = 0; dx < dataSample.length; ++dx) {
            dataSample[dx] = _random.nextLong() & 0_777777_777777L;
        }

        int dataSegmentIndex = _msp.createSegment(1024);
        ArraySlice dataStorage = _msp.getStorage(dataSegmentIndex);
        for (int dx = 0; dx < TestDiskDevice.PREP_FACTOR; ++dx) {
            dataStorage._array[dx] = dataSample[dx];
        }

        //  blank acws so builder won't complain
        AccessControlWord[] acws = {};

        int maxWords = TestDiskDevice.PREP_FACTOR * TestDiskDevice.BLOCK_COUNT;
        for (int count = 0; count < 32; ++count) {
            int blockId;
            int blockSize;
            do {
                blockId = Math.abs(_random.nextInt()) % TestDiskDevice.BLOCK_COUNT;
                blockSize = Math.abs(_random.nextInt()) % maxBlockSize;
            } while (((blockSize % TestDiskDevice.PREP_FACTOR) == 0)
                || (((blockId * TestDiskDevice.PREP_FACTOR) + blockSize) >= maxWords));

            //  Write data sample
            ChannelProgram cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                            .setChannelModuleIndex(_cmIndex)
                                                            .setDeviceAddress(_deviceIndex)
                                                            .setIOFunction(IOFunction.Write)
                                                            .setBlockId(new BlockId(blockId))
                                                            .setAccessControlWords(acws)
                                                            .setByteTranslationFormat(ByteTranslationFormat.QuarterWordPacked)
                                                            .build();

            int offset = Math.abs(_random.nextInt()) % maxWords;
            while ((long)offset + (long)blockSize > maxBlockSize) {
                offset = Math.abs(_random.nextInt()) % maxWords;
            }
            ArraySlice asBuffer = new ArraySlice(dataSample, offset, blockSize);
            boolean started = _cm.scheduleChannelProgram(_ip, _iop, cp, asBuffer);
            if (started) {
                while (cp.getChannelStatus() == ChannelStatus.InProgress) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }

            if (cp.getChannelStatus() != ChannelStatus.Successful) {
                System.out.println(String.format("ChStat:%s DevStat:%s", cp.getChannelStatus(), cp.getDeviceStatus()));
            }
            assertEquals(ChannelStatus.Successful, cp.getChannelStatus());

            //  Read data sample
            long[] dataResult = new long[blockSize];
            cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                             .setChannelModuleIndex(_cmIndex)
                                             .setDeviceAddress(_deviceIndex)
                                             .setIOFunction(IOFunction.Read)
                                             .setBlockId(new BlockId(blockId))
                                             .setAccessControlWords(acws)
                                             .setByteTranslationFormat(ByteTranslationFormat.QuarterWordPacked)
                                             .build();
            started = _cm.scheduleChannelProgram(_ip, _iop, cp, new ArraySlice(dataResult));
            if (started) {
                while (cp.getChannelStatus() == ChannelStatus.InProgress) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }

            if (cp.getChannelStatus() != ChannelStatus.Successful) {
                System.out.println(String.format("ChStat:%s DevStat:%s", cp.getChannelStatus(), cp.getDeviceStatus()));
            }
            assertEquals(ChannelStatus.Successful, cp.getChannelStatus());
            assertArrayEquals(asBuffer.getAll(), dataResult);
        }

        teardown();
    }

    @Test
    public void io_formatD() {
        //TODO - for tape
    }

    @Test
    public void io_formatD_stopBit() {
        //TODO - for tape
    }
}
