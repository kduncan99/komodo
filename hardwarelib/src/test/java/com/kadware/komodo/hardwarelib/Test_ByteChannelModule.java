/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.BlockId;
import com.kadware.komodo.hardwarelib.exceptions.*;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
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
public class Test_ByteChannelModule {

    public static class TestInputOutputProcessor extends InputOutputProcessor {

        public final List<ChannelProgram> _inFlight = new LinkedList<>();

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

    public static class TestInstructionProcessor extends InstructionProcessor {

        TestInstructionProcessor() {
            super("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI_INDEX);
            try {
                InventoryManager.getInstance().addInstructionProcessor(this);
            } catch (NodeNameConflictException | UPIConflictException ex) {
            }
        }
    }

    public static class TestDiskDevice extends Device {

        private final static int BLOCK_COUNT = 256;
        private final static int BLOCK_SIZE = 128;
        private final static int PREP_FACTOR = 28;
        private final Map<Long, byte[]> _dataStore = new HashMap<>();
        TestDiskDevice() { super(DeviceType.None, DeviceModel.None, "DISK0"); }

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
                    int bytesLeft = ioInfo._transferCount;
                    int byteBufferLength = ioInfo._byteBuffer.array().length;

                    Long blockId = ioInfo._blockId.getValue();
                    if (blockId >= BLOCK_COUNT) {
                        ioInfo._status = DeviceStatus.InvalidBlockId;
                    }

                    int dx = 0;
                    while ((bytesLeft > 0) && (dx < byteBufferLength)) {
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
                        while ((bytesLeft > 0) && (sx < BLOCK_SIZE) && (dx < byteBufferLength)) {
                            ioInfo._byteBuffer.array()[dx++] = dataBlock[sx++];
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
                    int byteBufferLength = ioInfo._byteBuffer.array().length;

                    Long blockId = ioInfo._blockId.getValue();
                    if (blockId >= BLOCK_COUNT) {
                        ioInfo._status = DeviceStatus.InvalidBlockId;
                    }

                    int sx = 0;
                    byte[] sourceData = ioInfo._byteBuffer.array();
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
                            dataBlock[dx++] = ioInfo._byteBuffer.array()[sx++];
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

    private ByteChannelModule _cm = null;
    private int _cmIndex = 0;
    private Device _device = null;
    private int _deviceIndex = 0;
    private InstructionProcessor _ip = null;
    private InputOutputProcessor _iop = null;
    private MainStorageProcessor _msp = null;

    private void setup(
    ) throws CannotConnectException,
             MaxNodesException {
        //  Create stub nodes and one real channel module
        _ip = InventoryManager.getInstance().createInstructionProcessor();
        _iop = InventoryManager.getInstance().createInputOutputProcessor();
        _msp = InventoryManager.getInstance().createMainStorageProcessor();
        _cm = new ByteChannelModule("CM0-0");
        _device = new TestDiskDevice();

        int cmIndex = 0;
        int devIndex = 0;
        Node.connect(_iop, cmIndex, _cm);
        Node.connect(_cm, devIndex, _device);
        _cm.initialize();
        _device.initialize();
    }

    private void teardown(
    ) throws UPINotAssignedException {
        _cm.terminate();
        _device.terminate();
        InventoryManager.getInstance().deleteProcessor(_ip._upiIndex);
        InventoryManager.getInstance().deleteProcessor(_iop._upiIndex);
        InventoryManager.getInstance().deleteProcessor(_msp._upiIndex);
    }

    @Test
    public void create() {
        ByteChannelModule cm = new ByteChannelModule("CM1-01");
        assertEquals(NodeCategory.ChannelModule, cm._category);
        assertEquals(ChannelModuleType.Byte, cm._channelModuleType);
        assertEquals("CM1-01", cm._name);
    }

    @Test
    public void canConnect_success() {
        ByteChannelModule cm = new ByteChannelModule("CM1-0");
        InputOutputProcessor iop = new InputOutputProcessor("IOP0", InventoryManager.FIRST_INPUT_OUTPUT_PROCESSOR_UPI_INDEX);
        assertTrue(cm.canConnect(iop));
    }

    @Test
    public void canConnect_failure() {
        ByteChannelModule cm = new ByteChannelModule("CM1-0");
        assertFalse(cm.canConnect(new FileSystemDiskDevice("DISK0")));
        assertFalse(cm.canConnect(new FileSystemTapeDevice("TAPE0")));
        assertFalse(cm.canConnect(new ByteChannelModule("CM1-0")));
        assertFalse(cm.canConnect(new WordChannelModule("CM1-1")));
        assertFalse(cm.canConnect(new MainStorageProcessor("MSP0",
                                                           InventoryManager.FIRST_MAIN_STORAGE_PROCESSOR_UPI_INDEX,
                                                           InventoryManager.MAIN_STORAGE_PROCESSOR_SIZE)));
        assertFalse(cm.canConnect(new InstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI_INDEX)));
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
             UPINotAssignedException {
        InstructionProcessor ip = new TestInstructionProcessor();
        InputOutputProcessor iop = new TestInputOutputProcessor();
        ByteChannelModule cm = new ByteChannelModule("CM0-0");
        cm.initialize();
        int cmIndex = 0;
        TestDiskDevice d = new TestDiskDevice();
        int devIndex = 0;
        Node.connect(iop, cmIndex, cm);
        Node.connect(cm, devIndex, d);

        ChannelProgram cp = new ChannelProgram.Builder().setIopUpiIndex(iop._upiIndex)
                                                        .setChannelModuleIndex(0)
                                                        .setDeviceAddress(5)
                                                        .setIOFunction(IOFunction.Reset)
                                                        .build();
        boolean scheduled = cm.scheduleChannelProgram(ip, iop, cp, null);

        cm.terminate();
        InventoryManager.getInstance().deleteProcessor(ip._upiIndex);
        InventoryManager.getInstance().deleteProcessor(iop._upiIndex);
        assertFalse(scheduled);
        assertEquals(ChannelStatus.UnconfiguredDevice, cp.getChannelStatus());
    }

    @Test
    public void io_formatA() {
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

    public void io_formatB() {
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
        setup();

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
    public void io_formatC_multipleBlocks(
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             UPINotAssignedException {
        setup();

        int maxBlocks = 16;

        //  Populate MSP storage
        Random r = new Random(System.currentTimeMillis());
        long[] dataSample = new long[maxBlocks * TestDiskDevice.PREP_FACTOR];
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
        int blockId = 0;
        while (blockId < TestDiskDevice.BLOCK_COUNT) {
            int blockCount = r.nextInt() % maxBlocks;
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
        setup();

        int maxBlockSize = 8 * TestDiskDevice.PREP_FACTOR;

        //  Populate MSP storage
        Random r = new Random(System.currentTimeMillis());
        long[] dataSample = new long[maxBlockSize];
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

        int maxWords = TestDiskDevice.PREP_FACTOR * TestDiskDevice.BLOCK_COUNT;
        for (int count = 0; count < 32; ++count) {
            int blockId = -1;
            int blockSize = -1;
            while ((blockId < 0)
                   || (blockSize < 0)
                   || ((blockSize % TestDiskDevice.PREP_FACTOR) == 0)
                   || (((blockId * TestDiskDevice.PREP_FACTOR) + blockSize) >= maxWords)) {
                blockId = r.nextInt() % TestDiskDevice.BLOCK_COUNT;
                blockSize = r.nextInt() % maxBlockSize;
            }

            //  Write data sample
            ChannelProgram cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                            .setChannelModuleIndex(_cmIndex)
                                                            .setDeviceAddress(_deviceIndex)
                                                            .setIOFunction(IOFunction.Write)
                                                            .setBlockId(new BlockId(blockId))
                                                            .setAccessControlWords(acws)
                                                            .setByteTranslationFormat(ByteTranslationFormat.QuarterWordPacked)
                                                            .build();

            int offset = r.nextInt() % maxWords;
            while ((offset < 0) || ((long)offset + (long)blockSize > maxBlockSize)) {
                offset = r.nextInt() % maxWords;
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

    //  TODO read backward test needed for all formats
}
