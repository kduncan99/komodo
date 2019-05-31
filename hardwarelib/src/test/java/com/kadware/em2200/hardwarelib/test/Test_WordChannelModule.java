/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import static org.junit.Assert.*;
import org.junit.*;

import com.kadware.em2200.hardwarelib.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Unit tests for WordChannelModule class
 */
public class Test_WordChannelModule {

//    public static class TestModule extends SoftwareWordChannelModule {
//
//        public static class TestTracker extends WordTracker {
//
//            public TestTracker(
//                final ChannelProgram program
//            ) {
//                super(program);
//            }
//        }
//
//        public TestModule(
//            final String name
//        ) {
//            super(name);
//        }
//
//        public boolean isWorkerActive(
//        ) {
//            return _workerThread.isAlive();
//        }
//    }
//
//    private final Logger LOGGER = LogManager.getLogger(SoftwareByteChannelModule.class);
//
//    @Test
//    public void create(
//    ) {
//        SoftwareByteChannelModule cm = new SoftwareByteChannelModule("CM1-01");
//        assertEquals(Node.Category.ChannelModule, cm.getCategory());
//        assertEquals(ChannelModule.ChannelModuleType.Byte, cm.getChannelModuleType());
//        assertEquals("CM1-01", cm.getName());
//    }
//
//    @Test
//    public void canConnect_success(
//    ) {
//        SoftwareWordChannelModule cm = new SoftwareWordChannelModule("CM1-1");
//        assertTrue(cm.canConnect(new InputOutputProcessor("IOP0", InventoryManager.FIRST_INPUT_OUTPUT_PROCESSOR_UPI)));
//    }
//
//    @Test
//    public void canConnect_failure(
//    ) throws IllegalAccessException,
//             InstantiationException,
//             InvocationTargetException,
//             NoSuchMethodException {
//        SoftwareWordChannelModule cm = new SoftwareWordChannelModule("CM1-1");
//        assertFalse(cm.canConnect(new FileSystemDiskDevice("DISK0", (short)0)));
//        assertFalse(cm.canConnect(new FileSystemTapeDevice("TAPE0", (short)0)));
//        assertFalse(cm.canConnect(new ByteDiskController("DSKCTL", (short)0)));
//        assertFalse(cm.canConnect(new WordDiskController("DSKCTL", (short)0)));
//        assertFalse(cm.canConnect(new TapeController("TAPCUB", (short)0)));
//        assertFalse(cm.canConnect(new SoftwareByteChannelModule("CM1-0")));
//        assertFalse(cm.canConnect(new SoftwareWordChannelModule("CM1-1")));
//        assertFalse(cm.canConnect(new StaticMainStorageProcessor("MSP0",
//                                                           InventoryManager.FIRST_MAIN_STORAGE_PROCESSOR_UPI,
//                                                           InventoryManager.MAIN_STORAGE_PROCESSOR_SIZE)));
//        assertFalse(cm.canConnect(new InstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI)));
//    }
//
//    @Test
//    public void getByteFromWordCount(
//    ) {
//        //  Quarter-word -> frame
//        assertEquals(4 * 28, SoftwareByteChannelModule.getByteCountFromWordCount(28, ChannelModule.IOTranslateFormat.A));
//        assertEquals(4 * 28, SoftwareByteChannelModule.getByteCountFromWordCount(28, ChannelModule.IOTranslateFormat.D));
//
//        //  Sixth-word -> frame
//        assertEquals(6 * 28, SoftwareByteChannelModule.getByteCountFromWordCount(28, ChannelModule.IOTranslateFormat.B));
//
//        //  2 words -> 9 frames
//        assertEquals(128 - 2, SoftwareByteChannelModule.getByteCountFromWordCount(28, ChannelModule.IOTranslateFormat.C));
//        assertEquals(8192 - 128, SoftwareByteChannelModule.getByteCountFromWordCount(1792, ChannelModule.IOTranslateFormat.C));
//    }
//
//    @Test
//    public void threadAlive_false_1(
//    ) {
//        TestModule cm = new TestModule("CM1-01");
//        assertFalse(cm.isWorkerActive());
//    }
//
//    @Test
//    public void threadAlive_false_2(
//    ) {
//        TestModule cm = new TestModule("CM1-01");
//        cm.initialize();
//
//        try {
//            Thread.sleep(1000);
//        } catch (Exception ex) {
//        }
//
//        cm.terminate();
//        assertFalse(cm.isWorkerActive());
//    }
//
//    @Test
//    public void threadAlive_true(
//    ) {
//        TestModule cm = new TestModule("CM1-01");
//        cm.initialize();
//
//        try {
//            Thread.sleep(1000);
//        } catch (Exception ex) {
//        }
//
//        assertTrue(cm.isWorkerActive());
//        cm.terminate();
//    }
}
