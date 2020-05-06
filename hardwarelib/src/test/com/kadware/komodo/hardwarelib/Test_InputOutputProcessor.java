/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.Credentials;
import com.kadware.komodo.hardwarelib.exceptions.*;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ByteChannelModule class
 */
@SuppressWarnings("Duplicates")
public class Test_InputOutputProcessor {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Stub classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static class TestSystemProcessor extends SystemProcessor {

        TestSystemProcessor() {
            super("SP0", null, null, new Credentials("test", "test"));
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

        //  For now, we do not set up an actual fully-formatted communications area in storage,
        //  only the individual slots for each source/destination combination.
        //  We hard-code the size of each slot to 2-words, to contain an absolute address.
        void setupUPICommunications(
            final MainStorageProcessor msp
        ) throws AddressingExceptionInterrupt {
            int slotSize = 2;
            List<Processor> processors = InventoryManager.getInstance().getProcessors();
            int processorCount = processors.size();
            int areas = processorCount * processorCount;
            int segmentIndex = msp.createSegment(areas * slotSize);
            int offset = 0;
            for (Processor source : processors) {
                for (Processor destination : processors) {
                    UPIIndexPair pair = new UPIIndexPair(source._upiIndex, destination._upiIndex);
                    AbsoluteAddress addr = new AbsoluteAddress(msp._upiIndex, segmentIndex, offset);
                    _upiCommunicationLookup.put(pair, addr);
                    offset += slotSize;
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
                if (channelProgram.getFunction().isWriteFunction()) {
                    _lastBuffer = buffer;
                } else if (channelProgram.getFunction().isReadFunction()) {
                    _lastBuffer = buffer;
                    Random r = new Random(System.currentTimeMillis());
                    for (int bx = 0; bx < buffer._array.length; ++bx) {
                        buffer._array[bx] = r.nextLong() & 0_777777_777777L;
                    }
                } else {
                    _lastBuffer = null;
                }
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
            super(Type.Disk, Model.FileSystemDisk, "DEV0");
        }

        @Override
        public boolean canConnect( Node ancestor ) { return true; }

        @Override
        public void clear() {}

        @Override
        public boolean handleIo(
            IOInfo ioInfo
        ) {
            return false;
        }

        @Override
        public boolean hasByteInterface() { return true; }

        @Override
        public boolean hasWordInterface() { return true; }

        @Override
        public void initialize() {}

        @Override
        public void terminate() {}

        @Override
        public void writeBuffersToLog(IOInfo ioInfo) {}
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
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             UPIConflictException,
             NodeNameConflictException {
        InventoryManager im = InventoryManager.getInstance();
        TestSystemProcessor sp = new TestSystemProcessor();
        sp.initialize();
        im.addSystemProcessor(sp);

        _ip = im.createInstructionProcessor("IP0");
        _iop = im.createInputOutputProcessor("IOP0");
        _msp = im.createMainStorageProcessor("MSP0", 1024 * 1024);
        _cm = new TestChannelModule();
        _dev = new TestDevice();

        _cmIndex = Math.abs(_random.nextInt()) % 6;
        _devIndex = Math.abs(_random.nextInt()) % 32;
        Node.connect(_iop, _cmIndex, _cm);
        Node.connect(_cm, _devIndex, _dev);
        _cm.initialize();
        _dev.initialize();

        sp.setupUPICommunications(_msp);
    }

    private void teardown(
    ) {
        _dev.terminate();
        _dev = null;
        _cm.terminate();
        _cm = null;
        InventoryManager.getInstance().clearConfiguration();
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  unit tests
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void create() {
        InputOutputProcessor iop = new InputOutputProcessor("IOP0", 2);
        assertEquals(Node.NodeCategory.Processor, iop._category);
        assertEquals(2, iop._upiIndex);
        assertEquals("IOP0", iop._name);
    }

    @Test
    public void canConnect_failure(
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException {
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
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException {
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
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException {
        setup();

        ChannelModule.ChannelProgram cp = new ChannelModule.ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                                                    .setChannelModuleIndex(_cmIndex + 1)
                                                                                    .setDeviceAddress(5)
                                                                                    .setIOFunction(Device.IOFunction.Reset)
                                                                                    .build();
        boolean scheduled = _iop.startIO(_ip, cp);
        if (scheduled) {
            while (cp.getChannelStatus() == ChannelModule.ChannelStatus.InProgress) {
                Thread.onSpinWait();
            }
        }
        assertEquals(ChannelModule.ChannelStatus.UnconfiguredChannelModule, cp.getChannelStatus());
        teardown();
    }

    @Test
    public void simpleRead(
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException {
        setup();

        int blockSize = 224;
        int dataSegment = _msp.createSegment(blockSize);
        ArraySlice dataStorage = _msp.getStorage(dataSegment);
        AbsoluteAddress dataAddress = new AbsoluteAddress(_msp._upiIndex, dataSegment, 0);

        int acwSegment = _msp.createSegment(28);
        ArraySlice acwStorage = _msp.getStorage(acwSegment);
        AccessControlWord.populate(acwStorage,
                                   0,
                                   dataAddress,
                                   blockSize,
                                   AccessControlWord.AddressModifier.Increment);
        AccessControlWord[] acws = { new AccessControlWord(acwStorage, 0) };

        long blockId = 0;
        ChannelModule.ChannelProgram cp = new ChannelModule.ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                                                    .setChannelModuleIndex(_cmIndex)
                                                                                    .setDeviceAddress(_devIndex)
                                                                                    .setIOFunction(Device.IOFunction.Read)
                                                                                    .setBlockId(blockId)
                                                                                    .setAccessControlWords(acws)
                                                                                    .build();
        boolean scheduled = _iop.startIO(_ip, cp);
        assert(scheduled);
        while (cp.getChannelStatus() == ChannelModule.ChannelStatus.InProgress) {
            Thread.onSpinWait();
        }

        assertEquals(ChannelModule.ChannelStatus.Successful, cp.getChannelStatus());
        assertArrayEquals(_cm._lastBuffer._array, dataStorage._array);

        teardown();
    }

    @Test
    public void simpleWrite(
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException {
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

        long blockId = 0;
        ChannelModule.ChannelProgram cp = new ChannelModule.ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                                                    .setChannelModuleIndex(_cmIndex)
                                                                                    .setDeviceAddress(_devIndex)
                                                                                    .setBlockId(blockId)
                                                                                    .setIOFunction(Device.IOFunction.Write)
                                                                                    .setAccessControlWords(acws)
                                                                                    .build();
        boolean scheduled = _iop.startIO(_ip, cp);
        assert(scheduled);
        while (cp.getChannelStatus() == ChannelModule.ChannelStatus.InProgress) {
            Thread.onSpinWait();
        }
        assertEquals(ChannelModule.ChannelStatus.Successful, cp.getChannelStatus());
        assertArrayEquals(baseData, _cm._lastBuffer._array);

        teardown();
    }

    //TODO need read backward (not backward ACW)
    //TODO need read/write backward, no-increment, and skip ACWs
    //TODO need more negative test cases

    @Test
    public void gatherWrite(
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException {
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

        ChannelModule.ChannelProgram cp = new ChannelModule.ChannelProgram.Builder().setIopUpiIndex(_iop._upiIndex)
                                                                                    .setChannelModuleIndex(_cmIndex)
                                                                                    .setDeviceAddress(_devIndex)
                                                                                    .setIOFunction(Device.IOFunction.Write)
                                                                                    .setAccessControlWords(acws)
                                                                                    .build();
        boolean scheduled = _iop.startIO(_ip, cp);
        assert(scheduled);
        while (cp.getChannelStatus() == ChannelModule.ChannelStatus.InProgress) {
            Thread.onSpinWait();
        }

        assertEquals(ChannelModule.ChannelStatus.Successful, cp.getChannelStatus());
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

    @Test
    public void scatterRead(
    ) throws AddressingExceptionInterrupt,
             CannotConnectException,
             MaxNodesException,
             NodeNameConflictException,
             UPIConflictException {
        setup();
        //TODO
        teardown();
    }
}
