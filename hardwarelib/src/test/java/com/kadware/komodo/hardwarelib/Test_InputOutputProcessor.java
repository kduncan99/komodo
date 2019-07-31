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

    //TODO need single-buffer read, scatter read, gather write, and maybe more negative test cases

    @Test
    public void gatherWrite(
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             UPINotAssignedException {
        setup();

        long[] baseData0 = new long[80];
        for (int bdx = 0; bdx < baseData0.length; ++bdx) {
            baseData0[bdx] = _random.nextLong() & 0_777777_777777L;
        }

        long[] baseData1 = new long[100];
        for (int bdx = 0; bdx < baseData1.length; ++bdx) {
            baseData1[bdx] = _random.nextLong() & 0_777777_777777L;
        }

        long[] baseData2 = new long[44];
        for (int bdx = 0; bdx < baseData2.length; ++bdx) {
            baseData2[bdx] = _random.nextLong() & 0_777777_777777L;
        }

        int dataSegment0 = _msp.createSegment(baseData0.length);
        ArraySlice dataStorage0 = _msp.getStorage(dataSegment0);
        dataStorage0.load(baseData0);
        AbsoluteAddress dataAddress0 = new AbsoluteAddress(_msp._upiIndex, dataSegment0, 0);

        int dataSegment1 = _msp.createSegment(baseData1.length);
        ArraySlice dataStorage1 = _msp.getStorage(dataSegment1);
        dataStorage1.load(baseData1);
        AbsoluteAddress dataAddress1 = new AbsoluteAddress(_msp._upiIndex, dataSegment1, 0);

        int dataSegment2 = _msp.createSegment(baseData2.length);
        ArraySlice dataStorage2 = _msp.getStorage(dataSegment2);
        dataStorage2.load(baseData2);
        AbsoluteAddress dataAddress2 = new AbsoluteAddress(_msp._upiIndex, dataSegment2, 0);

        int acwSegment = _msp.createSegment(28);
        ArraySlice acwStorage = _msp.getStorage(acwSegment);
        AccessControlWord.populate(acwStorage, 0, dataAddress0, baseData0.length, AccessControlWord.AddressModifier.Increment);
        AccessControlWord.populate(acwStorage, 3, dataAddress1, baseData1.length, AccessControlWord.AddressModifier.Increment);
        AccessControlWord.populate(acwStorage, 6, dataAddress2, baseData2.length, AccessControlWord.AddressModifier.Increment);
        AccessControlWord[] acws = {
            new AccessControlWord(acwStorage, 0),
            new AccessControlWord(acwStorage, 3),
            new AccessControlWord(acwStorage, 6)
        };

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
        assertEquals(baseData0.length + baseData1.length + baseData2.length, _cm._lastBuffer._array.length);
        assertArrayEquals(baseData0, Arrays.copyOfRange(_cm._lastBuffer._array,
                                                        0,
                                                        baseData0.length));
        assertArrayEquals(baseData1, Arrays.copyOfRange(_cm._lastBuffer._array,
                                                        baseData0.length,
                                                        baseData0.length + baseData1.length));
        assertArrayEquals(baseData2, Arrays.copyOfRange(_cm._lastBuffer._array,
                                                        baseData0.length + baseData1.length,
                                                        baseData0.length + baseData1.length + baseData2.length));

        teardown();
    }

}
