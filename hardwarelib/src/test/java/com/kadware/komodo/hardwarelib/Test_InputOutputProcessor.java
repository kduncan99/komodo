/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.BlockId;
import com.kadware.komodo.hardwarelib.exceptions.*;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for ByteChannelModule class
 */
@SuppressWarnings("Duplicates")
public class Test_InputOutputProcessor {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Stub classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static class TestInstructionProcessor extends InstructionProcessor {

        TestInstructionProcessor() {
            super("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI_INDEX);
            try {
                InventoryManager.getInstance().addInstructionProcessor(this);
            } catch (NodeNameConflictException | UPIConflictException ex) {
            }
        }

        @Override
        public void run() {
            while (!_workerTerminate) {
                try {
                    synchronized (this) { wait(); }
                } catch (InterruptedException ex) {
                    System.out.println("Caught " + ex.getMessage());
                }
            }
        }
    }

    private static class TestChannelModule extends ChannelModule {

        private ArraySlice _lastBuffer = null;

        private class TestTracker extends Tracker {
            TestTracker(
                final Processor source,
                final InputOutputProcessor ioProcessor,
                final ChannelProgram channelProgram,
                final ArraySlice buffer
            ) {
                super(source, ioProcessor, channelProgram, buffer);
                _lastBuffer = buffer;
            }
        }

        TestChannelModule() {
            super(ChannelModuleType.Byte, "CM0");
        }

        protected Tracker createTracker(
            final Processor source,
            final InputOutputProcessor ioProcessor,
            final ChannelProgram channelProgram,
            final ArraySlice buffer
        ) {
            return new TestTracker(source, ioProcessor, channelProgram, buffer);
        }

        public void run() {
            while (!_workerTerminate) {
                boolean sleepFlag = true;
                synchronized (this) {
                    Iterator<Tracker> iter = _trackers.iterator();
                    while (iter.hasNext()) {
                        TestTracker tracker = (TestTracker) iter.next();
                        tracker._channelProgram.setChannelStatus(ChannelStatus.Successful);
                        tracker._ioProcessor.finalizeIo(tracker._channelProgram, tracker._source);
                        iter.remove();
                        sleepFlag = false;
                    }
                }

                if (sleepFlag) {
                    try {
                        synchronized (this) {
                            wait(100);
                        }
                    } catch (InterruptedException ex) {
                        System.out.println("Caught " + ex.getMessage());
                    }
                }
            }
        }
    }

    private static class TestDevice extends Device {

        private TestDevice(
        ) {
            super(DeviceType.Disk, DeviceModel.FileSystemDisk, "DEV0");
        }

        @Override
        public boolean canConnect( Node ancestor ) { return true; }

        @Override
        public void clear() {}

        @Override
        public boolean handleIo(DeviceIOInfo ioInfo) { return false; }

        @Override
        public boolean hasByteInterface() { return true; }

        @Override
        public boolean hasWordInterface() { return true; }

        @Override
        public void initialize() {}

        @Override
        public void terminate() {}

        @Override
        public void writeBuffersToLog(DeviceIOInfo ioInfo) {}
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  members
    //  ----------------------------------------------------------------------------------------------------------------------------

    private TestChannelModule _cm = null;
    private int _cmIndex = 0;
    private TestDevice _dev = null;
    private int _devIndex = 0;
    private InstructionProcessor _ip = null;
    private InputOutputProcessor _iop = null;
    private MainStorageProcessor _msp = null;
    private final Random _random = new Random(System.currentTimeMillis());

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  useful methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    private void setup(
    ) throws CannotConnectException,
             MaxNodesException {
        _ip = InventoryManager.getInstance().createInstructionProcessor();
        _iop = InventoryManager.getInstance().createInputOutputProcessor();
        _msp = InventoryManager.getInstance().createMainStorageProcessor();
        _cm = new TestChannelModule();
        _dev = new TestDevice();

        _cmIndex = Math.abs(_random.nextInt()) % 6;
        _devIndex = Math.abs(_random.nextInt()) % 32;
        Node.connect(_iop, _cmIndex, _cm);
        Node.connect(_cm, _devIndex, _dev);
        _cm.initialize();
        _dev.initialize();
    }

    private void teardown(
    ) throws UPINotAssignedException {
        _dev.terminate();
        _dev = null;
        _cm.terminate();
        _cm = null;
        InventoryManager.getInstance().deleteProcessor(_ip._upiIndex);
        InventoryManager.getInstance().deleteProcessor(_iop._upiIndex);
        InventoryManager.getInstance().deleteProcessor(_msp._upiIndex);
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  unit tests
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void create() {
        InputOutputProcessor iop = new InputOutputProcessor("IOP0", 2);
        assertEquals(NodeCategory.Processor, iop._category);
        assertEquals(2, iop._upiIndex);
        assertEquals("IOP0", iop._name);
    }

    @Test
    public void canConnect_failure(
    ) throws CannotConnectException,
             MaxNodesException,
             UPINotAssignedException{
        setup();
        InputOutputProcessor iop = new InputOutputProcessor("IOP0", 2);
        assertFalse(iop.canConnect(new FileSystemDiskDevice("DISK0")));
        assertFalse(iop.canConnect(new FileSystemTapeDevice("TAPE0")));
        assertFalse(iop.canConnect(new ByteChannelModule("CM1-0")));
        assertFalse(iop.canConnect(new WordChannelModule("CM1-1")));
        assertFalse(iop.canConnect(_msp));
        assertFalse(iop.canConnect(_ip));
        teardown();
    }

    @Test
    public void threadAlive_true(
    ) throws CannotConnectException,
             MaxNodesException,
             UPINotAssignedException{
        setup();

        try {
            Thread.sleep(1000);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        assertTrue(_iop._workerThread.isAlive());
        teardown();
    }

    @Test
    public void unconfiguredChannelModule(
    ) throws CannotConnectException,
             MaxNodesException,
             UPINotAssignedException {
        setup();

        ChannelProgram cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                        .setChannelModuleIndex(_cmIndex + 1)
                                                        .setDeviceAddress(5)
                                                        .setIOFunction(IOFunction.Reset)
                                                        .build();
        boolean scheduled = _iop.startIO(_ip, cp);
        if (scheduled) {
            while (cp.getChannelStatus() == ChannelStatus.InProgress) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    System.out.println("Caught " + ex.getMessage());
                }
            }
        }
        assertEquals(ChannelStatus.UnconfiguredChannelModule, cp.getChannelStatus());
        teardown();
    }

    @Test
    public void simpleWrite(
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             UPINotAssignedException {
        setup();

        long[] baseData = new long[224];
        for (int bdx = 0; bdx < baseData.length; ++bdx) {
            baseData[bdx] = _random.nextLong() & 0_777777_777777L;
        }

        int dataSegment = _msp.createSegment(baseData.length);
        ArraySlice dataStorage = _msp.getStorage(dataSegment);
        dataStorage.load(baseData);
        AbsoluteAddress dataAddress = new AbsoluteAddress(_msp._upiIndex, dataSegment, 0);

        int acwSegment = _msp.createSegment(28);
        ArraySlice acwStorage = _msp.getStorage(acwSegment);
        AccessControlWord.populate(acwStorage, 0, dataAddress, baseData.length, AccessControlWord.AddressModifier.Increment);
        AccessControlWord[] acws = { new AccessControlWord(acwStorage, 0) };

        ChannelProgram cp = new ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                        .setChannelModuleIndex(_cmIndex)
                                                        .setDeviceAddress(_devIndex)
                                                        .setIOFunction(IOFunction.Write)
                                                        .setAccessControlWords(acws)
                                                        .build();
        boolean scheduled = _iop.startIO(_ip, cp);
        assert(scheduled);
        while (cp.getChannelStatus() == ChannelStatus.InProgress) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                System.out.println("Caught " + ex.getMessage());
            }
        }
        assertEquals(ChannelStatus.Successful, cp.getChannelStatus());
        assertArrayEquals(baseData, _cm._lastBuffer._array);

        teardown();
    }

    //TODO need single-buffer, scatter-gather, and a bunch of negative test cases
}
