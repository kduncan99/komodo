/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.BlockId;
import com.kadware.komodo.baselib.PrepFactor;
import com.kadware.komodo.hardwarelib.exceptions.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import static org.junit.Assert.*;

import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import org.junit.*;

/**
 * Unit tests for ByteChannelModule class
 */
public class Test_ByteChannelModule {

    public static class TestInputOutputProcessor extends InputOutputProcessor {

        TestInputOutputProcessor(
        ) {
            super("IOP0", InventoryManager.FIRST_INPUT_OUTPUT_PROCESSOR_UPI_INDEX);
            try {
                InventoryManager.getInstance().addInputOutputProcessor(this);
            } catch (NodeNameConflictException | UPIConflictException ex) {
            }
        }

        @Override
        public void finalizeIo(
            final ChannelProgram channelProgram,
            final Processor source
        ) {
            //TODO
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
        private final Map<Long, Byte[]> _dataStore = new HashMap<>();
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
                    System.out.println("READ-----------------------------------------");//TODO
                    int bytesLeft = ioInfo._transferCount;

                    Long blockId = ioInfo._blockId.getValue();
                    if (blockId >= BLOCK_COUNT) {
                        ioInfo._status = DeviceStatus.InvalidBlockId;
                    }

                    while (bytesLeft > 0) {
                        if (blockId >= BLOCK_COUNT) {
                            ioInfo._status = DeviceStatus.InvalidTransferSize;
                            return false;
                        }

                        Byte[] dataBlock = _dataStore.get(blockId);
                        if (dataBlock == null) {
                            dataBlock = new Byte[BLOCK_SIZE];
                            _dataStore.put(blockId, dataBlock);
                        }

                        int dx = 0;
                        while ((bytesLeft > 0) && (dx < BLOCK_SIZE)) {
                            ioInfo._byteBuffer.array()[dx] = (dataBlock[dx]);
                            ++ioInfo._transferredCount;
                            --bytesLeft;
                            ++dx;
                        }
                    }

                    ioInfo._status = DeviceStatus.Successful;
                    break;
                }

                case Write:
                {
                    System.out.println("WRITE-----------------------------------------");//TODO
                    int bytesLeft = ioInfo._transferCount;

                    Long blockId = ioInfo._blockId.getValue();
                    if (blockId >= BLOCK_COUNT) {
                        ioInfo._status = DeviceStatus.InvalidBlockId;
                    }

                    ioInfo._byteBuffer.position(0);
                    while (bytesLeft > 0) {
                        if (blockId >= BLOCK_COUNT) {
                            ioInfo._status = DeviceStatus.InvalidTransferSize;
                            return false;
                        }

                        Byte[] dataBlock = _dataStore.get(blockId);
                        if (dataBlock == null) {
                            dataBlock = new Byte[BLOCK_SIZE];
                            _dataStore.put(blockId, dataBlock);
                        }

                        for (int dx = 0;
                             (bytesLeft > 0) && (dx < BLOCK_SIZE);
                             ++ioInfo._transferredCount, --bytesLeft, ++dx) {
                            dataBlock[dx] = ioInfo._byteBuffer.array()[dx];
                        }
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
             MaxNodesException,
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
    public void io_formatC_oneACW(
        ) throws AddressingExceptionInterrupt,
                 CannotConnectException,
                 MaxNodesException,
                 UPINotAssignedException {
        //  Create stub nodes and one real channel module
        InstructionProcessor ip = InventoryManager.getInstance().createInstructionProcessor();
        InputOutputProcessor iop = InventoryManager.getInstance().createInputOutputProcessor();
        MainStorageProcessor msp = InventoryManager.getInstance().createMainStorageProcessor();
        ByteChannelModule cm = new ByteChannelModule("CM0-0");
        TestDiskDevice d = new TestDiskDevice();

        int cmIndex = 0;
        int devIndex = 0;
        Node.connect(iop, cmIndex, cm);
        Node.connect(cm, devIndex, d);
        cm.initialize();
        d.initialize();

        //  Populate MSP storage
        Random r = new Random(System.currentTimeMillis());
        long[] dataSample = new long[TestDiskDevice.PREP_FACTOR];
        for (int dx = 0; dx < dataSample.length; ++dx) {
            dataSample[dx] = r.nextLong() & 0_777777_777777L;
        }

        int dataSegmentIndex = msp.createSegment(1024);
        ArraySlice dataStorage = msp.getStorage(dataSegmentIndex);
        for (int dx = 0; dx < TestDiskDevice.PREP_FACTOR; ++dx) {
            dataStorage._array[dx] = dataSample[dx];
        }

        //  blank acws so builder won't complain
        AccessControlWord[] acws = {};
        for (int blockId = 0; blockId < TestDiskDevice.BLOCK_COUNT; ++blockId) {
            //  Write data sample
            ChannelProgram cp = new ChannelProgram.Builder().setIopUpiIndex(iop._upiIndex)
                                                            .setChannelModuleIndex(cmIndex)
                                                            .setDeviceAddress(devIndex)
                                                            .setIOFunction(IOFunction.Write)
                                                            .setBlockId(new BlockId(blockId))
                                                            .setAccessControlWords(acws)
                                                            .setByteTranslationFormat(ByteTranslationFormat.QuarterWordPacked)
                                                            .build();
            boolean started = cm.scheduleChannelProgram(ip, iop, cp, new ArraySlice(dataSample));
            assertTrue(started);
            if (started) {
                while (cp.getChannelStatus() == ChannelStatus.InProgress) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                    }
                }
            }

            if (cp.getChannelStatus() != ChannelStatus.Successful) {
                System.out.println(String.format("ChStat:%s DevStat:%s", cp.getChannelStatus(), cp.getDeviceStatus()));
            }
            assertEquals(ChannelStatus.Successful, cp.getChannelStatus());

            //  Read data sample
            long[] dataResult = new long[TestDiskDevice.PREP_FACTOR];
            cp = new ChannelProgram.Builder().setIopUpiIndex(iop._upiIndex)
                                             .setChannelModuleIndex(cmIndex)
                                             .setDeviceAddress(devIndex)
                                             .setIOFunction(IOFunction.Read)
                                             .setBlockId(new BlockId(blockId))
                                             .setAccessControlWords(acws)
                                             .setByteTranslationFormat(ByteTranslationFormat.QuarterWordPacked)
                                             .build();
            started = cm.scheduleChannelProgram(ip, iop, cp, new ArraySlice(dataResult));
            assertTrue(started);
            if (started) {
                while (cp.getChannelStatus() == ChannelStatus.InProgress) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                    }
                }
            }

            if (cp.getChannelStatus() != ChannelStatus.Successful) {
                System.out.println(String.format("ChStat:%s DevStat:%s", cp.getChannelStatus(), cp.getDeviceStatus()));
            }
            assertEquals(ChannelStatus.Successful, cp.getChannelStatus());

            assertArrayEquals(dataSample, dataResult);
        }

        cm.terminate();
        InventoryManager.getInstance().deleteProcessor(ip._upiIndex);
        InventoryManager.getInstance().deleteProcessor(iop._upiIndex);
        InventoryManager.getInstance().deleteProcessor(msp._upiIndex);

        //  Unmount and delete disk pack
        //TODO

        //  Compare buffers
        //TODO
    }

    @Test
    public void io_formatC_residual() {
        //TODO
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
