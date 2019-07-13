/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import static org.junit.Assert.*;

import com.kadware.komodo.hardwarelib.exceptions.CannotConnectException;
import com.kadware.komodo.hardwarelib.exceptions.MaxNodesException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import org.junit.*;

/**
 * Unit tests for ByteChannelModule class
 */
public class Test_ByteChannelModule {

    public static class TestModule extends ByteChannelModule {

        TestModule(String name) { super(name); }
        boolean isWorkerActive() { return _workerThread.isAlive(); }
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
        TestModule cm = new TestModule("CM1-01");
        assertFalse(cm.isWorkerActive());
    }

    @Test
    public void threadAlive_false_2() {
        TestModule cm = new TestModule("CM1-01");
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
        TestModule cm = new TestModule("CM1-01");
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
    public void badDevice(
    ) throws CannotConnectException,
             MaxNodesException {
        InstructionProcessor ip = InventoryManager.getInstance().createInstructionProcessor();
        InputOutputProcessor iop = InventoryManager.getInstance().createInputOutputProcessor();
        ByteChannelModule cm = new ByteChannelModule("CM0-0");
        FileSystemDiskDevice d = new FileSystemDiskDevice("D01");
        Node.connect(iop, 0, cm);
        Node.connect(cm, 1, d);

        ChannelProgram cp = new ChannelProgram.Builder().setIopUpiIndex(iop._upiIndex)
                                                        .setChannelModuleIndex(0)
                                                        .setDeviceAddress(0)
                                                        .setIOFunction(IOFunction.Reset)
                                                        .build();
        boolean scheduled = cm.scheduleChannelProgram(ip, iop, cp);
        assertFalse(scheduled);
    }

    @Test
    public void io_formatA() {
        ByteChannelModule cm = new ByteChannelModule("CM0-0");
        FileSystemDiskDevice d = new FileSystemDiskDevice("D01");
    }

    @Test
    public void io_formatA_residual() {
        //TODO
    }

    @Test
    public void io_formatA_stopBit() {
        //TODO
    }

    public void io_formatB() {
        //TODO
    }

    @Test
    public void io_formatB_residual() {
        //TODO
    }

    public void io_formatC() {
        //TODO
    }

    @Test
    public void io_formatC_residual() {
        //TODO
    }

    public void io_formatD() {
        //TODO
    }

    public void io_formatD_stopBit() {
        //TODO
    }
}
