/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.*;
import com.kadware.komodo.hardwarelib.exceptions.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.junit.*;
import org.junit.rules.ExpectedException;
import static org.junit.Assert.*;

/**
 * Unit tests for FileSystemDiskDevice class
 */
public class Test_FileSystemDiskDevice {

    @Rule
    public ExpectedException _exception = ExpectedException.none();

    public static class TestChannelModule extends ChannelModule {

        private final List<DeviceIOInfo> _ioList = new LinkedList<>();

        private TestChannelModule() {
            super(ChannelModuleType.Byte, "TESTCM");
        }

        //  Only for satisfying the compiler
        protected Tracker createTracker(
            Processor p,
            InputOutputProcessor iop,
            ChannelProgram cp
        ) {
            return null;
        }

        //  This is the real thing
        void submitAndWait(
            final Device target,
            final DeviceIOInfo deviceIoInfo
        ) {
            if (target.handleIo(deviceIoInfo)) {
                synchronized (_ioList) {
                    _ioList.add(deviceIoInfo);
                }

                synchronized (deviceIoInfo) {
                    while (deviceIoInfo._status == DeviceStatus.InProgress) {
                        try {
                            deviceIoInfo.wait(1);
                        } catch (InterruptedException ex) {
                            System.out.println(ex.getMessage());
                        }
                    }
                }
            }
        }

        public void run() {
            while (!_workerTerminate) {
                synchronized (_workerThread) {
                    try {
                        _workerThread.wait(1000);
                    } catch (InterruptedException ex) {
                        System.out.println(ex.getMessage());
                    }
                }

                synchronized (_ioList) {
                    Iterator<DeviceIOInfo> iter = _ioList.iterator();
                    while (iter.hasNext()) {
                        DeviceIOInfo ioInfo = iter.next();
                        if (ioInfo._status != DeviceStatus.InProgress) {
                            iter.remove();
                            ioInfo._status.notify();
                        }
                    }
                }
            }
        }
    }

    public static class TestDevice extends FileSystemDiskDevice {

        static class ScratchPad extends FileSystemDiskDevice.ScratchPad {

            ScratchPad() {}

            ScratchPad(
                final PrepFactor prepFactor,
                final BlockSize blockSize,
                final BlockCount blockCount
            ) {
                super(prepFactor, blockSize, blockCount);
            }
        }

        TestDevice() { super("TEST"); }

        @Override
        public long calculateByteOffset(
            final BlockId blockId
        ) {
            return super.calculateByteOffset(blockId);
        }
    }

    private static int nextFileIndex = 1;

    /**
     * Prepends the system-wide temporary path to the given base name if found
     * @return cooked path/file name
     */
    private static String getTestFileName(
    ) {
        String pathName = System.getProperty("java.io.tmpdir");
        return String.format("%sTEST%04d.pack", pathName == null ? "" : pathName, nextFileIndex++);
    }

    /**
     * For some reason, the delete occasionally fails with the file assigned to another process.
     * I don't know why, so we do this just in case.
     */
    private static void deleteTestFile(
        final String fileName
    ) {
        boolean done = false;
        while (!done) {
            try {
                Files.delete(FileSystems.getDefault().getPath(fileName));
                done = true;
            } catch (Exception ex) {
                System.out.println("Retrying delete...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex2) {
                    System.out.println(ex.getMessage());
                }
            }
        }
    }

    @Test
    public void create(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        assertEquals("DISK0", d._name);
        assertEquals(NodeCategory.Device, d._category);
        Assert.assertEquals(DeviceModel.FileSystemDisk, d._deviceModel);
        assertEquals(DeviceType.Disk, d._deviceType);
    }

    @Test
    public void calculateByteOffset(
    ) {
        FileSystemDiskDevice d = new TestDevice();
        d._blockSize = new BlockSize(256);
        assertEquals(3 * 256, d.calculateByteOffset(new BlockId(2)));
    }

    @Test
    public void calculateByteOffset_reallyBig(
    ) {
        TestDevice d = new TestDevice();
        d._blockSize = new BlockSize(256);
        assertEquals(0x80000001L * 256, d.calculateByteOffset(new BlockId(0x80000000L)));
    }

    @Test
    public void canConnect_success(
    ) {
        ByteChannelModule cm = new ByteChannelModule("CM0");
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        assertTrue(d.canConnect(cm));
    }

    @Test
    public void canConnect_failure(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        assertFalse(d.canConnect(new FileSystemDiskDevice("DISK1")));
        assertFalse(d.canConnect(new FileSystemTapeDevice("TAPE0")));
        assertFalse(d.canConnect(new WordChannelModule("CM1-1")));
        assertFalse(d.canConnect(new MainStorageProcessor("MSP0",
                                                          InventoryManager.FIRST_MAIN_STORAGE_PROCESSOR_UPI_INDEX,
                                                          InventoryManager.MAIN_STORAGE_PROCESSOR_SIZE)));
        assertFalse(d.canConnect(new InputOutputProcessor("IOP0", InventoryManager.FIRST_INPUT_OUTPUT_PROCESSOR_UPI_INDEX)));
        assertFalse(d.canConnect(new InstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI_INDEX)));
    }

    @Test
    public void createPack(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockSize[] blockSizes = {
            new BlockSize(128),
            new BlockSize(256),
            new BlockSize(512),
            new BlockSize(1024),
            new BlockSize(2048),
            new BlockSize(4096),
            new BlockSize(8192)
        };

        for (BlockSize blockSize : blockSizes) {
            BlockCount blockCount = new BlockCount(10000 * (8192 / blockSize.getValue()));
            FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

            //  Make sure we can read the ScratchPad
            RandomAccessFile check = new RandomAccessFile(fileName, "r");
            byte[] buffer = new byte[blockSize.getValue()];
            check.seek(0);
            assertEquals(blockSize.getValue(), check.read(buffer));
            check.close();

            //  Verify the ScratchPad
            TestDevice.ScratchPad sp = new TestDevice.ScratchPad();
            sp.deserialize(ByteBuffer.wrap(buffer));
            assertEquals(blockCount, sp._blockCount);
            assertEquals(blockSize, sp._blockSize);
        }

        deleteTestFile(fileName);
    }

    @Test
    public void createPack_badPath(
    ) throws Exception {
        _exception.expect(FileNotFoundException.class);
        FileSystemDiskDevice.createPack("/blah/blah/blah/TEST.pack",
                                        new BlockSize(8192),
                                        new BlockCount(10000));
    }

    @Test
    public void createPack_invalidBlockSize(
    ) throws Exception {
        _exception.expect(InvalidBlockSizeException.class);
        FileSystemDiskDevice.createPack(getTestFileName(),
                                        new BlockSize(22),
                                        new BlockCount(1000));
    }

    @Test
    public void createPack_invalidTrackCount_1(
    ) throws Exception {
        _exception.expect(InvalidTrackCountException.class);
        FileSystemDiskDevice.createPack(getTestFileName(),
                                        new BlockSize(8192),
                                        new BlockCount(9999));
    }

    @Test
    public void createPack_invalidTrackCount_2(
    ) throws Exception {
        _exception.expect(InvalidTrackCountException.class);
        FileSystemDiskDevice.createPack(getTestFileName(),
                                        new BlockSize(8192),
                                        new BlockCount(100000));
    }

    @Test
    public void hasByteInterface(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        assertTrue(d.hasByteInterface());
    }

    @Test
    public void hasWordInterface(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        assertFalse(d.hasWordInterface());
    }

    @Test
    public void ioGetInfo_successful(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockCount blockCount = new BlockCount(10000);
        BlockSize blockSize = new BlockSize(8192);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        d.mount(fileName);
        d.setReady(true);

        DeviceIOInfo ioInfo = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                    .setIOFunction(IOFunction.GetInfo)
                                                                    .setTransferCount(128)
                                                                    .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(DeviceStatus.Successful, ioInfo._status);
        ArraySlice as = new ArraySlice(new long[28]);
        as.unpack(ioInfo._byteBuffer.array());

        int flags = (int) Word36.getS1(as.get(0));
        boolean resultIsReady = (flags & 01) != 0;
        boolean resultIsMounted = (flags & 02) != 0;
        boolean resultIsWriteProtected = (flags & 04) != 0;
        DeviceModel resultModel = DeviceModel.getValue((int) Word36.getS2(as.get(0)));
        DeviceType resultType = DeviceType.getValue((int) Word36.getS3(as.get(0)));
        PrepFactor resultPrepFactor = new PrepFactor((int) Word36.getH2(as.get(0)));
        long resultBlockCount = as.get(1);

        assertTrue(resultIsReady);
        assertTrue(resultIsMounted);
        assertFalse(resultIsWriteProtected);
        assertEquals(DeviceModel.FileSystemDisk, resultModel);
        assertEquals(DeviceType.Disk, resultType);
        assertEquals(blockCount.getValue(), resultBlockCount);
        assertEquals(PrepFactor.getPrepFactorFromBlockSize(blockSize), resultPrepFactor);

        assertFalse(d._unitAttentionFlag);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioRead_fail_notReady(
    ) {
        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();

        long blockId = 5;
        int blockSize = 128;
        DeviceIOInfo ioInfoRead = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                        .setIOFunction(IOFunction.Read)
                                                                        .setBlockId(blockId)
                                                                        .setTransferCount(blockSize)
                                                                        .build();

        cm.submitAndWait(d, ioInfoRead);
        assertEquals(DeviceStatus.NotReady, ioInfoRead._status);
    }

    @Test
    public void ioRead_fail_invalidBlockSize(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockSize blockSize = new BlockSize(128);
        PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        BlockCount blockCount = new BlockCount(10000 * (prepFactor.getBlocksPerTrack()));
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        //  Clear unit attention
        DeviceIOInfo ioInfoGetInfo = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.GetInfo)
                                                                           .setTransferCount(128)
                                                                           .build();
        cm.submitAndWait(d, ioInfoGetInfo);
        BlockId blockId = new BlockId(5);
        DeviceIOInfo ioInfoRead = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                        .setIOFunction(IOFunction.Read)
                                                                        .setBlockId(blockId.getValue())
                                                                        .setTransferCount(blockSize.getValue() - 1)
                                                                        .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(DeviceStatus.InvalidBlockSize, ioInfoRead._status);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioRead_fail_invalidBlockId(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockSize blockSize = new BlockSize(128);
        PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        BlockCount blockCount = new BlockCount(10000 * (prepFactor.getBlocksPerTrack()));
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        //  Clear unit attention
        DeviceIOInfo ioInfoGetInfo = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.GetInfo)
                                                                           .setTransferCount(128)
                                                                           .build();
        cm.submitAndWait(d, ioInfoGetInfo);

        DeviceIOInfo ioInfoRead = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                        .setIOFunction(IOFunction.Read)
                                                                        .setBlockId(blockCount.getValue())
                                                                        .setTransferCount(blockSize.getValue())
                                                                        .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(DeviceStatus.InvalidBlockId, ioInfoRead._status);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioRead_fail_invalidBlockCount(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockSize blockSize = new BlockSize(128);
        PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        BlockCount blockCount = new BlockCount(10000 * (prepFactor.getBlocksPerTrack()));
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        //  Clear unit attention
        DeviceIOInfo ioInfoGetInfo = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.GetInfo)
                                                                           .setTransferCount(128)
                                                                           .build();
        cm.submitAndWait(d, ioInfoGetInfo);

        DeviceIOInfo ioInfoRead = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                        .setIOFunction(IOFunction.Read)
                                                                        .setBlockId(blockCount.getValue() - 1)
                                                                        .setTransferCount(2 * blockSize.getValue())
                                                                        .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(DeviceStatus.InvalidBlockCount, ioInfoRead._status);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioRead_fail_unitAttention(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockSize blockSize = new BlockSize(128);
        PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        BlockCount blockCount = new BlockCount(10000 * (prepFactor.getBlocksPerTrack()));
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        long blockId = 5;
        DeviceIOInfo ioInfoRead = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                        .setIOFunction(IOFunction.Read)
                                                                        .setBlockId(blockId)
                                                                        .setTransferCount(blockSize.getValue())
                                                                        .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(DeviceStatus.UnitAttention, ioInfoRead._status);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioReset_successful(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockCount blockCount = new BlockCount(10000);
        BlockSize blockSize = new BlockSize(8192);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        FileSystemDiskDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        DeviceIOInfo ioInfo = new DeviceIOInfo.NonTransferBuilder().setSource(cm)
                                                                   .setIOFunction(IOFunction.Reset)
                                                                   .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(DeviceStatus.Successful, ioInfo._status);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioReset_failed_notReady(
    ) {
        TestChannelModule cm = new TestChannelModule();
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        DeviceIOInfo ioInfo = new DeviceIOInfo.NonTransferBuilder().setSource(cm)
                                                                   .setIOFunction(IOFunction.Reset)
                                                                   .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(DeviceStatus.NotReady, ioInfo._status);
    }

    @Test
    public void ioStart_failed_badFunction(
    ) {
        TestChannelModule cm = new TestChannelModule();
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");

        DeviceIOInfo[] ioInfos = {
            new DeviceIOInfo.NonTransferBuilder().setSource(cm)
                                                 .setIOFunction(IOFunction.Close)
                                                 .build(),
            new DeviceIOInfo.NonTransferBuilder().setSource(cm)
                                                 .setIOFunction(IOFunction.MoveBlock)
                                                 .build(),
            new DeviceIOInfo.NonTransferBuilder().setSource(cm)
                                                 .setIOFunction(IOFunction.MoveBlockBackward)
                                                 .build(),
            new DeviceIOInfo.NonTransferBuilder().setSource(cm)
                                                 .setIOFunction(IOFunction.MoveFile)
                                                 .build(),
            new DeviceIOInfo.NonTransferBuilder().setSource(cm)
                                                 .setIOFunction(IOFunction.MoveFileBackward)
                                                 .build(),
            new DeviceIOInfo.NonTransferBuilder().setSource(cm)
                                                 .setIOFunction(IOFunction.Rewind)
                                                 .build(),
            new DeviceIOInfo.NonTransferBuilder().setSource(cm)
                                                 .setIOFunction(IOFunction.RewindInterlock)
                                                 .build(),
            new DeviceIOInfo.NonTransferBuilder().setSource(cm)
                                                 .setIOFunction(IOFunction.ReadBackward)
                                                 .build(),
            new DeviceIOInfo.NonTransferBuilder().setSource(cm)
                                                 .setIOFunction(IOFunction.WriteEndOfFile)
                                                 .build(),
        };

        for (DeviceIOInfo ioInfo : ioInfos) {
            cm.submitAndWait(d, ioInfo);
            assertEquals(DeviceStatus.InvalidFunction, ioInfo._status);
        }
    }

    @Test
    public void ioStart_none(
    ) {
        TestChannelModule cm = new TestChannelModule();
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        DeviceIOInfo ioInfo = new DeviceIOInfo.NonTransferBuilder().setSource(cm)
                                                                   .setIOFunction(IOFunction.None)
                                                                   .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(DeviceStatus.Successful, ioInfo._status);
    }

    @Test
    public void ioUnload_successful(
    ) throws IOException {
        String fileName = getTestFileName();
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        byte[] buffer = new byte[128];
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(new PrepFactor(1792),
                                                             new BlockSize(8192),
                                                             new BlockCount(10000));
        sp.serialize(ByteBuffer.wrap(buffer));
        file.write(buffer);
        file.close();

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);
        DeviceIOInfo ioInfo = new DeviceIOInfo.NonTransferBuilder().setSource(cm)
                                                                   .setIOFunction(IOFunction.Unload)
                                                                   .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(DeviceStatus.Successful, ioInfo._status);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioUnload_failed_notReady(
    ) throws IOException {
        String fileName = getTestFileName();
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        byte[] buffer = new byte[128];
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(new PrepFactor(1792),
                                                             new BlockSize(8192),
                                                             new BlockCount(10000));
        sp.serialize(ByteBuffer.wrap(buffer));
        file.write(buffer);
        file.close();

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(false);
        DeviceIOInfo ioInfo = new DeviceIOInfo.NonTransferBuilder().setSource(cm)
                                                                   .setIOFunction(IOFunction.Unload)
                                                                   .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(DeviceStatus.NotReady, ioInfo._status);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_ioRead_successful(
    ) throws Exception {
        Random r = new Random((int)System.currentTimeMillis());
        String fileName = getTestFileName();
        BlockSize[] blockSizes = {
            new BlockSize(128),
            new BlockSize(256),
            new BlockSize(512),
            new BlockSize(1024),
            new BlockSize(2048),
            new BlockSize(4096),
            new BlockSize(8192)
        };

        //  There is some delay per iteration - it is over a second delay during unmount().
        //  I think this is normal and acceptable.  I think.
        for (BlockSize blockSize : blockSizes) {
            PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
            BlockCount blockCount = new BlockCount(10000 * prepFactor.getBlocksPerTrack());
            FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

            //  set up the device and eat the UA
            TestChannelModule cm = new TestChannelModule();
            TestDevice d = new TestDevice();
            d.mount(fileName);
            d.setReady(true);
            DeviceIOInfo ioInfo = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                        .setIOFunction(IOFunction.GetInfo)
                                                                        .setTransferCount(128)
                                                                        .build();
            cm.submitAndWait(d, ioInfo);

            for (int x = 0; x < 16; ++x) {
                long blockIdVal = r.nextInt() % blockCount.getValue();
                if (blockIdVal < 0) {
                    blockIdVal = 0 - blockIdVal;
                }

                //  note - we purposely allow block count of zero
                int ioBlockCount = r.nextInt() % 4;
                if (ioBlockCount < 0) {
                    ioBlockCount = 0 - ioBlockCount;
                }

                int bufferSize = ioBlockCount * blockSize.getValue();
                byte[] writeBuffer = new byte[bufferSize];
                r.nextBytes(writeBuffer);

                DeviceIOInfo ioInfoWrite = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                                 .setIOFunction(IOFunction.Write)
                                                                                 .setBlockId(blockIdVal)
                                                                                 .setBuffer(ByteBuffer.wrap(writeBuffer))
                                                                                 .setTransferCount(bufferSize)
                                                                                 .build();
                cm.submitAndWait(d, ioInfoWrite);
                assertEquals(DeviceStatus.Successful, ioInfoWrite._status);

                DeviceIOInfo ioInfoRead = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                                .setIOFunction(IOFunction.Read)
                                                                                .setBlockId(blockIdVal)
                                                                                .setTransferCount(bufferSize)
                                                                                .build();
                cm.submitAndWait(d, ioInfoRead);
                assertEquals(DeviceStatus.Successful, ioInfoRead._status);
                assertArrayEquals(writeBuffer, ioInfoRead._byteBuffer.array());
            }

            d.unmount();
            deleteTestFile(fileName);
        }
    }

    @Test
    public void ioWrite_fail_notReady(
    ) {
        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        int bufferSize = 128;
        long blockId = 5;
        byte[] writeBuffer = new byte[bufferSize];
        DeviceIOInfo ioInfoWrite = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                         .setIOFunction(IOFunction.Write)
                                                                         .setBlockId(blockId)
                                                                         .setBuffer(ByteBuffer.wrap(writeBuffer))
                                                                         .setTransferCount(writeBuffer.length)
                                                                         .build();
        cm.submitAndWait(d, ioInfoWrite);
        assertEquals(DeviceStatus.NotReady, ioInfoWrite._status);
    }

    @Test
    public void ioWrite_fail_bufferTooSmall(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockSize blockSize = new BlockSize(128);
        PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        BlockCount blockCount = new BlockCount(10000 * (prepFactor.getBlocksPerTrack()));
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        DeviceIOInfo ioInfo = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                    .setIOFunction(IOFunction.GetInfo)
                                                                    .setTransferCount(128)
                                                                    .build();
        cm.submitAndWait(d, ioInfo);

        byte[] writeBuffer = new byte[10];
        long blockId = 5;
        DeviceIOInfo ioInfoWrite = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                         .setIOFunction(IOFunction.Write)
                                                                         .setBlockId(blockId)
                                                                         .setBuffer(ByteBuffer.wrap(writeBuffer))
                                                                         .setTransferCount(blockSize.getValue())
                                                                         .build();
        cm.submitAndWait(d, ioInfoWrite);
        assertEquals(DeviceStatus.BufferTooSmall, ioInfoWrite._status);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_fail_invalidBlockSize(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockSize blockSize = new BlockSize(128);
        PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        BlockCount blockCount = new BlockCount(10000 * (prepFactor.getBlocksPerTrack()));
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        //  Clear unit attention
        DeviceIOInfo ioInfoGetInfo = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.GetInfo)
                                                                           .setTransferCount(128)
                                                                           .build();
        cm.submitAndWait(d, ioInfoGetInfo);

        byte[] readBuffer = new byte[blockSize.getValue()];
        BlockId blockId = new BlockId(5);

        DeviceIOInfo ioInfoRead = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                        .setIOFunction(IOFunction.Write)
                                                                        .setBlockId(blockId.getValue())
                                                                        .setBuffer(ByteBuffer.wrap(readBuffer))
                                                                        .setTransferCount(blockSize.getValue() - 1)
                                                                        .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(DeviceStatus.InvalidBlockSize, ioInfoRead._status);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_fail_invalidBlockId(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockSize blockSize = new BlockSize(128);
        PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        BlockCount blockCount = new BlockCount(10000 * (prepFactor.getBlocksPerTrack()));
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        //  Clear unit attention
        DeviceIOInfo ioInfoGetInfo = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.GetInfo)
                                                                           .setTransferCount(128)
                                                                           .build();
        cm.submitAndWait(d, ioInfoGetInfo);

        byte[] readBuffer = new byte[blockSize.getValue()];
        DeviceIOInfo ioInfoRead = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                        .setIOFunction(IOFunction.Write)
                                                                        .setBlockId(blockCount.getValue())
                                                                        .setBuffer(ByteBuffer.wrap(readBuffer))
                                                                        .setTransferCount(blockSize.getValue())
                                                                        .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(DeviceStatus.InvalidBlockId, ioInfoRead._status);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_fail_invalidBlockCount(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockSize blockSize = new BlockSize(128);
        PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        BlockCount blockCount = new BlockCount(10000 * (prepFactor.getBlocksPerTrack()));
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        //  Clear unit attention
        DeviceIOInfo ioInfoGetInfo = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.GetInfo)
                                                                           .setTransferCount(128)
                                                                           .build();
        cm.submitAndWait(d, ioInfoGetInfo);

        byte[] readBuffer = new byte[2 * blockSize.getValue()];
        DeviceIOInfo ioInfoRead = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                        .setIOFunction(IOFunction.Write)
                                                                        .setBlockId(blockCount.getValue() - 1)
                                                                        .setBuffer(ByteBuffer.wrap(readBuffer))
                                                                        .setTransferCount(2 * blockSize.getValue())
                                                                        .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(DeviceStatus.InvalidBlockCount, ioInfoRead._status);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_fail_unitAttention(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockSize blockSize = new BlockSize(128);
        PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        BlockCount blockCount = new BlockCount(10000 * (prepFactor.getBlocksPerTrack()));
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        byte[] writeBuffer = new byte[blockSize.getValue()];
        long blockId = 5;
        DeviceIOInfo ioInfoWrite = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                         .setIOFunction(IOFunction.Write)
                                                                         .setBlockId(blockId)
                                                                         .setBuffer(ByteBuffer.wrap(writeBuffer))
                                                                         .setTransferCount(blockSize.getValue())
                                                                         .build();
        cm.submitAndWait(d, ioInfoWrite);
        assertEquals(DeviceStatus.UnitAttention, ioInfoWrite._status);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_fail_writeProtected(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockSize blockSize = new BlockSize(128);
        PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        BlockCount blockCount = new BlockCount(10000 * (prepFactor.getBlocksPerTrack()));
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);
        d.setIsWriteProtected(true);

        //  Clear unit attention
        DeviceIOInfo ioInfoGetInfo = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.GetInfo)
                                                                           .setTransferCount(128)
                                                                           .build();
        cm.submitAndWait(d, ioInfoGetInfo);

        byte[] writeBuffer = new byte[blockSize.getValue()];
        long blockId = 5;
        DeviceIOInfo ioInfoWrite = new DeviceIOInfo.ByteTransferBuilder().setSource(cm)
                                                                         .setIOFunction(IOFunction.Write)
                                                                         .setBlockId(blockId)
                                                                         .setBuffer(ByteBuffer.wrap(writeBuffer))
                                                                         .setTransferCount(blockSize.getValue())
                                                                         .build();
        cm.submitAndWait(d, ioInfoWrite);
        assertEquals(DeviceStatus.WriteProtected, ioInfoWrite._status);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void mount_successful(
    ) throws IOException {
        String fileName = getTestFileName();
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        byte[] buffer = new byte[128];
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(new PrepFactor(1792),
                                                             new BlockSize(8192),
                                                             new BlockCount(10000));
        sp.serialize(ByteBuffer.wrap(buffer));
        file.write(buffer);
        file.close();

        TestDevice d = new TestDevice();
        assertTrue(d.mount(fileName));
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void mount_successful_scratchPadWrongMinorVersion(
    ) throws IOException {
        String fileName = getTestFileName();
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        byte[] buffer = new byte[128];
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(new PrepFactor(1792),
                                                             new BlockSize(8192),
                                                             new BlockCount(10000));
        sp._minorVersion = -1;
        sp.serialize(ByteBuffer.wrap(buffer));
        file.write(buffer);
        file.close();

        TestDevice d = new TestDevice();
        assertTrue(d.mount(fileName));
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void mount_failed_alreadyMounted(
    ) throws IOException {
        String fileName = getTestFileName();
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        byte[] buffer = new byte[128];
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(new PrepFactor(1792),
                                                             new BlockSize(8192),
                                                             new BlockCount(10000));
        sp.serialize(ByteBuffer.wrap(buffer));
        file.write(buffer);
        file.close();

        TestDevice d = new TestDevice();
        d.mount(fileName);
        assertFalse(d.mount("BLAH.pack"));
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void mount_failed_noFile(
    ) {
        TestDevice d = new TestDevice();
        assertFalse(d.mount("/blah/blah/blah/FOO.pack"));
    }

    @Test
    public void mount_failed_noScratchPad(
    ) throws IOException {
        String fileName = getTestFileName();
        Files.deleteIfExists(FileSystems.getDefault().getPath(fileName));
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        file.close();
        TestDevice d = new TestDevice();
        assertFalse(d.mount(fileName));
        deleteTestFile(fileName);
    }

    @Test
    public void mount_failed_incompleteScratchPad(
    ) throws IOException {
        String fileName = getTestFileName();
        Files.deleteIfExists(FileSystems.getDefault().getPath(fileName));
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        byte[] buffer = { 0, 0, 0, 0 };
        file.write(buffer);
        file.close();

        TestDevice d = new TestDevice();
        assertFalse(d.mount(fileName));
        deleteTestFile(fileName);
    }

    @Test
    public void mount_failed_scratchPadWrongIdentifier(
    ) throws IOException {
        String fileName = getTestFileName();
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        byte[] buffer = new byte[128];
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(new PrepFactor(1792),
                                                             new BlockSize(8192),
                                                             new BlockCount(10000));
        sp._identifier = "BadDog";
        sp.serialize(ByteBuffer.wrap(buffer));
        file.write(buffer);
        file.close();

        TestDevice d = new TestDevice();
        assertFalse(d.mount(fileName));
        deleteTestFile(fileName);
    }

    @Test
    public void mount_failed_scratchPadWrongMajorVersion(
    ) throws IOException {
        String fileName = getTestFileName();
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        byte[] buffer = new byte[128];
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(new PrepFactor(1792),
                                                             new BlockSize(8192),
                                                             new BlockCount(10000));
        sp._majorVersion = -1;
        sp.serialize(ByteBuffer.wrap(buffer));
        file.write(buffer);
        file.close();

        TestDevice d = new TestDevice();
        assertFalse(d.mount(fileName));
        deleteTestFile(fileName);
    }

    @Test
    public void setReady_false_successful_alreadyFalse(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemDiskDevice.createPack(fileName, new BlockSize(8192), new BlockCount(10000));

        FileSystemDiskDevice d = new TestDevice();
        d.mount(fileName);
        assertTrue(d.setReady(false));
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void setReady_false_successful(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemDiskDevice.createPack(fileName, new BlockSize(8192), new BlockCount(10000));

        FileSystemDiskDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);
        assertTrue(d.setReady(false));
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void setReady_false_successful_noPack(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        assertTrue(d.setReady(false));
    }

    @Test
    public void setReady_true_successful(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemDiskDevice.createPack(fileName,
                                        new BlockSize(8192),
                                        new BlockCount(10000));
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        d.mount(fileName);
        assertTrue(d.setReady(true));
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void setReady_true_successful_alreadyTrue(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemDiskDevice.createPack(fileName,
                                        new BlockSize(8192),
                                        new BlockCount(10000));
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        d.mount(fileName);
        d.setReady(true);
        assertTrue(d.setReady(true));
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void setReady_true_failed_noPack(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        assertFalse(d.setReady(true));
    }

    @Test
    public void unmount_successful(
    ) throws IOException {
        String fileName = getTestFileName();
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        byte[] buffer = new byte[128];
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(new PrepFactor(1792),
                                                             new BlockSize(8192),
                                                             new BlockCount(10000));
        sp.serialize(ByteBuffer.wrap(buffer));
        file.write(buffer);
        file.close();

        TestDevice d = new TestDevice();
        d.mount(fileName);
        assertTrue(d.unmount());
        deleteTestFile(fileName);
    }

    @Test
    public void unmount_failed(
    ) {
        TestDevice d = new TestDevice();
        assertFalse(d.unmount());
    }
}
