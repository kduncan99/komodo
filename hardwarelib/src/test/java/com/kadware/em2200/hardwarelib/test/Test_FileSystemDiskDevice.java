/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;
import org.junit.*;
import org.junit.rules.ExpectedException;

import com.kadware.komodo.baselib.types.*;
import com.kadware.em2200.hardwarelib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;

/**
 * Unit tests for Device class
 */
public class Test_FileSystemDiskDevice {

    @Rule
    public ExpectedException _exception = ExpectedException.none();

    public static class TestDevice extends FileSystemDiskDevice {

        public static class ScratchPad extends FileSystemDiskDevice.ScratchPad {

            public ScratchPad() {}

            public ScratchPad(
                final PrepFactor prepFactor,
                final BlockSize blockSize,
                final BlockCount blockCount
            ) {
                super(prepFactor, blockSize, blockCount);
            }
        }

        public Node _signalSource = null;

        public TestDevice(
            final String name,
            final short subsystemId
        ) {
            super(name, subsystemId);
        }

        @Override
        public long calculateByteOffset(
            final BlockId blockId
        ) {
            return super.calculateByteOffset(blockId);
        }

        @Override
        public void signal(
            final Node source
        ) {
            _signalSource = source;
        }
    }

    public static int nextFileIndex = 1;

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
                    //????
                }
            }
        }
    }

    @Test
    public void create(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", (short)10);
        assertEquals("DISK0", d.getName());
        assertEquals(10, d.getSubsystemIdentifier());
        assertEquals(Node.Category.Device, d.getCategory());
        assertEquals(Device.DeviceModel.FileSystemDisk, d.getDeviceModel());
        assertEquals(Device.DeviceType.Disk, d.getDeviceType());
    }

    @Test
    public void calculateByteOffset(
    ) {
        TestDevice d = new TestDevice("TEST", (short)0);
        d.setBlockSize(new BlockSize(256));
        assertEquals(3 * 256, d.calculateByteOffset(new BlockId(2)));
    }

    @Test
    public void calculateByteOffset_reallyBig(
    ) {
        TestDevice d = new TestDevice("TEST", (short)0);
        d.setBlockSize(new BlockSize(256));
        assertEquals(0x80000001l * 256, d.calculateByteOffset(new BlockId(0x80000000l)));
    }

    @Test
    public void canConnect_success(
    ) {
        ByteDiskController c = new ByteDiskController("DSKCUA", (short)0);
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", (short)0);
        assertTrue(d.canConnect(c));
    }

    @Test
    public void canConnect_failure(
    ) throws IllegalAccessException,
             InstantiationException,
             NoSuchMethodException {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", (short)0);
        assertFalse(d.canConnect(new FileSystemDiskDevice("DISK1", (short)0)));
        assertFalse(d.canConnect(new FileSystemTapeDevice("TAPE0", (short)0)));
        assertFalse(d.canConnect(new WordDiskController("DSKCUB", (short)0)));
        assertFalse(d.canConnect(new TapeController("TAPCUA", (short)0)));
//????        assertFalse(d.canConnect(new SoftwareByteChannelModule("CM1-0")));
//????        assertFalse(d.canConnect(new SoftwareWordChannelModule("CM1-1")));
        assertFalse(d.canConnect(new StaticMainStorageProcessor("MSP0",
                                                                InventoryManager.FIRST_MAIN_STORAGE_PROCESSOR_UPI,
                                                                InventoryManager.MAIN_STORAGE_PROCESSOR_SIZE)));
        assertFalse(d.canConnect(new InputOutputProcessor("IOP0", InventoryManager.FIRST_INPUT_OUTPUT_PROCESSOR_UPI)));
        assertFalse(d.canConnect(new InstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI)));
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
    public void getPackName(
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

        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        String s = d.getPackName();
        assertEquals(fileName, d.getPackName());
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void getPackName_notMounted(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", (short)0);
        assertEquals("", d.getPackName());
    }

    @Test
    public void hasByteInterface(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", (short)0);
        assertTrue(d.hasByteInterface());
    }

    @Test
    public void hasWordInterface(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", (short)0);
        assertFalse(d.hasWordInterface());
    }

    @Test
    public void ioGetInfo_successful(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockCount blockCount = new BlockCount(10000);
        BlockSize blockSize = new BlockSize(8192);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        short subSystemId = 10;
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", subSystemId);
        d.mount(fileName);
        d.setReady(true);

        byte[] buffer = new byte[blockSize.getValue()];
        Device.IOInfo ioInfo = new Device.IOInfo(d, Device.IOFunction.GetInfo, buffer, blockSize.getValue());
        d.handleIo(ioInfo);
        assertEquals(Device.IOStatus.Successful, ioInfo.getStatus());

        DiskDevice.DiskDeviceInfo ddInfo = new DiskDevice.DiskDeviceInfo();
        ddInfo.deserialize(ByteBuffer.wrap(buffer));
        assertEquals(DiskDevice.DeviceModel.FileSystemDisk, ddInfo.getDeviceModel());
        assertEquals(DiskDevice.DeviceType.Disk, ddInfo.getDeviceType());
        assertEquals(subSystemId, ddInfo.getSubsystemIdentifier());
        assertTrue(ddInfo.isReady());
        assertTrue(ddInfo.isMounted());
        assertFalse(ddInfo.isWriteProtected());
        assertTrue(ddInfo.getUnitAttention());
        assertEquals(blockCount, ddInfo.getBlockCount());
        assertEquals(blockSize, ddInfo.getBlockSize());

        //  make sure UA is cleared
        assertFalse(d.getUnitAttentionFlag());
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioGetInfo_failed_bufferTooSmall(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockCount blockCount = new BlockCount(10000);
        BlockSize blockSize = new BlockSize(8192);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        short subSystemId = 10;
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", subSystemId);
        d.mount(fileName);
        d.setReady(true);

        byte[] buffer = new byte[10];
        Device.IOInfo ioInfo = new Device.IOInfo(d, Device.IOFunction.GetInfo, buffer, blockSize.getValue());
        d.handleIo(ioInfo);
        assertEquals(Device.IOStatus.BufferTooSmall, ioInfo.getStatus());

        //  make sure UA is still set
        assertTrue(d.getUnitAttentionFlag());

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioGetInfo_failed_invalidBlockSize(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockCount blockCount = new BlockCount(10000);
        BlockSize blockSize = new BlockSize(8192);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        short subSystemId = 10;
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", subSystemId);
        d.mount(fileName);
        d.setReady(true);

        byte[] buffer = new byte[blockSize.getValue()];
        Device.IOInfo ioInfo = new Device.IOInfo(d, Device.IOFunction.GetInfo, buffer, 10);
        d.handleIo(ioInfo);
        assertEquals(Device.IOStatus.InvalidBlockSize, ioInfo.getStatus());

        //  make sure UA is still set
        assertTrue(d.getUnitAttentionFlag());

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioRead_fail_notReady(
    ) throws Exception {
        TestDevice d = new TestDevice("TEST", (short)0);

        BlockSize blockSize = new BlockSize(128);
        BlockId blockId = new BlockId(5);
        byte[] readBuffer = new byte[(int)blockSize.getValue()];

        Device.IOInfo ioInfoRead = new Device.IOInfo(null, Device.IOFunction.Read, readBuffer, blockId, blockSize.getValue());
        d.handleIo(ioInfoRead);
        assertEquals(Device.IOStatus.NotReady, ioInfoRead.getStatus());
    }

    @Test
    public void ioRead_fail_bufferTooSmall(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockSize blockSize = new BlockSize(128);
        PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        BlockCount blockCount = new BlockCount(10000 * (prepFactor.getBlocksPerTrack()));
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        d.setReady(true);

        byte[] infoBuffer = new byte[128];
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo(null,
                                                        Device.IOFunction.GetInfo,
                                                        infoBuffer,
                                                        128);
        d.handleIo(ioInfoGetInfo);

        byte[] readBuffer = new byte[10];
        BlockId blockId = new BlockId(5);

        Device.IOInfo ioInfoRead = new Device.IOInfo(null,
                                                     Device.IOFunction.Read,
                                                     readBuffer,
                                                     blockId,
                                                     blockSize.getValue());
        d.handleIo(ioInfoRead);
        assertEquals(Device.IOStatus.BufferTooSmall, ioInfoRead.getStatus());

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioRead_fail_invalidBlockSize(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockSize blockSize = new BlockSize(128);
        PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        BlockCount blockCount = new BlockCount(10000 * (prepFactor.getBlocksPerTrack()));
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        d.setReady(true);

        byte[] infoBuffer = new byte[128];
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo(null, Device.IOFunction.GetInfo, infoBuffer, 128);
        d.handleIo(ioInfoGetInfo);

        byte[] readBuffer = new byte[(int)blockSize.getValue()];
        BlockId blockId = new BlockId(5);

        Device.IOInfo ioInfoRead = new Device.IOInfo(null,
                                                     Device.IOFunction.Read,
                                                     readBuffer,
                                                     blockId,
                                                     blockSize.getValue() - 1);
        d.handleIo(ioInfoRead);
        assertEquals(Device.IOStatus.InvalidBlockSize, ioInfoRead.getStatus());
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

        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        d.setReady(true);

        byte[] infoBuffer = new byte[128];
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo(null, Device.IOFunction.GetInfo, infoBuffer, 128);
        d.handleIo(ioInfoGetInfo);

        byte[] readBuffer = new byte[(int)blockSize.getValue()];
        //long blockId = 5;

        Device.IOInfo ioInfoRead = new Device.IOInfo(null,
                                                     Device.IOFunction.Read,
                                                     readBuffer,
                                                     new BlockId(blockCount.getValue()),
                                                     blockSize.getValue());
        d.handleIo(ioInfoRead);
        assertEquals(Device.IOStatus.InvalidBlockId, ioInfoRead.getStatus());

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

        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        d.setReady(true);

        byte[] infoBuffer = new byte[128];
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo(null, Device.IOFunction.GetInfo, infoBuffer, 128);
        d.handleIo(ioInfoGetInfo);

        byte[] readBuffer = new byte[(int)(2 * blockSize.getValue())];
        Device.IOInfo ioInfoRead = new Device.IOInfo(null,
                                                     Device.IOFunction.Read,
                                                     readBuffer,
                                                     new BlockId(blockCount.getValue() - 1),
                                                     2 * blockSize.getValue());
        d.handleIo(ioInfoRead);
        assertEquals(Device.IOStatus.InvalidBlockCount, ioInfoRead.getStatus());

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

        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        d.setReady(true);

        byte[] readBuffer = new byte[(int)blockSize.getValue()];
        BlockId blockId = new BlockId(5);

        Device.IOInfo ioInfoRead = new Device.IOInfo(null,
                                                     Device.IOFunction.Read,
                                                     readBuffer,
                                                     blockId,
                                                     blockSize.getValue());
        d.handleIo(ioInfoRead);
        assertEquals(Device.IOStatus.UnitAttention, ioInfoRead.getStatus());

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

        short subSystemId = 10;
        FileSystemDiskDevice d = new FileSystemDiskDevice("TEST", subSystemId);
        d.mount(fileName);
        d.setReady(true);

        byte[] buffer = new byte[(int)blockSize.getValue()];
        Device.IOInfo ioInfo = new Device.IOInfo(d, Device.IOFunction.Reset, buffer, blockSize.getValue());
        d.handleIo(ioInfo);
        assertEquals(Device.IOStatus.Successful, ioInfo.getStatus());

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioReset_failed_notReady(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", (short)0);
        Device.IOInfo ioInfo = new Device.IOInfo(d, Device.IOFunction.Reset);
        d.handleIo(ioInfo);
        assertEquals(Device.IOStatus.NotReady, ioInfo.getStatus());
    }

    @Test
    public void ioStart_failed_badFunction(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", (short)0);

        Device.IOInfo ioInfo1 = new Device.IOInfo(d, Device.IOFunction.Rewind);
        d.handleIo(ioInfo1);
        assertEquals(Device.IOStatus.InvalidFunction, ioInfo1.getStatus());

        Device.IOInfo ioInfo2 = new Device.IOInfo(d, Device.IOFunction.Close);
        d.handleIo(ioInfo2);
        assertEquals(Device.IOStatus.InvalidFunction, ioInfo2.getStatus());

        Device.IOInfo ioInfo3 = new Device.IOInfo(d, Device.IOFunction.MoveBlockBackward);
        d.handleIo(ioInfo3);
        assertEquals(Device.IOStatus.InvalidFunction, ioInfo3.getStatus());
    }

    @Test
    public void ioStart_none(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", (short)0);
        Device.IOInfo ioInfo = new Device.IOInfo(d, Device.IOFunction.None);
        d.handleIo(ioInfo);
        assertEquals(Device.IOStatus.Successful, ioInfo.getStatus());
    }

    @Test
    public void ioStart_checkNoSignal(
    ) {
        TestDevice d = new TestDevice("TEST", (short)0);
        Device.IOInfo ioInfo3 = new Device.IOInfo(null, Device.IOFunction.None);
        d.handleIo(ioInfo3);
        assertEquals(null, d._signalSource);
    }

    @Test
    public void ioStart_checkSignal(
    ) {
        TestDevice d = new TestDevice("TEST", (short)0);
        Device.IOInfo ioInfo3 = new Device.IOInfo(d, Device.IOFunction.None);
        d.handleIo(ioInfo3);
        assertEquals(d, d._signalSource);
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

        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        d.setReady(true);
        Device.IOInfo ioInfo = new Device.IOInfo(d, Device.IOFunction.Unload);
        d.handleIo(ioInfo);
        assertEquals(Device.IOStatus.Successful, ioInfo.getStatus());
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

        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        d.setReady(false);
        Device.IOInfo ioInfo = new Device.IOInfo(d, Device.IOFunction.Unload);
        d.handleIo(ioInfo);
        assertEquals(Device.IOStatus.NotReady, ioInfo.getStatus());

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

        for (BlockSize blockSize : blockSizes) {
            PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
            BlockCount blockCount = new BlockCount(10000 * prepFactor.getBlocksPerTrack());
            FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

            //  set up the device and eat the UA
            TestDevice d = new TestDevice("TEST", (short)0);
            d.mount(fileName);
            d.setReady(true);
            byte[] infoBuffer = new byte[128];
            Device.IOInfo ioInfoGetInfo = new Device.IOInfo(null, Device.IOFunction.GetInfo, infoBuffer, 128);
            d.handleIo(ioInfoGetInfo);

            for (int x = 0; x < 16; ++x) {
                long blockIdVal = 0;
                while (blockIdVal <= 0) {
                    blockIdVal = r.nextInt() % blockCount.getValue();
                }
                BlockId blockId = new BlockId(blockIdVal);

                int ioBlockCount = 0;
                while (ioBlockCount <= 0) {
                    ioBlockCount = r.nextInt() % 4;
                }

                int bufferSize = ioBlockCount * blockSize.getValue();
                byte[] writeBuffer = new byte[bufferSize];
                r.nextBytes(writeBuffer);

                Device.IOInfo ioInfoWrite = new Device.IOInfo(null,
                                                              Device.IOFunction.Write,
                                                              writeBuffer,
                                                              blockId,
                                                              bufferSize);
                d.handleIo(ioInfoWrite);
                assertEquals(Device.IOStatus.Successful, ioInfoWrite.getStatus());

                byte[] readBuffer = new byte[bufferSize];
                Device.IOInfo ioInfoRead = new Device.IOInfo(null,
                                                             Device.IOFunction.Read,
                                                             readBuffer,
                                                             blockId,
                                                             bufferSize);
                d.handleIo(ioInfoRead);
                assertEquals(Device.IOStatus.Successful, ioInfoRead.getStatus());

                assertTrue(Arrays.equals(writeBuffer, readBuffer));
            }

            d.unmount();
            deleteTestFile(fileName);
        }
    }

    @Test
    public void ioWrite_fail_notReady(
    ) {
        TestDevice d = new TestDevice("TEST", (short)0);
        int bufferSize = 128;
        BlockId blockId = new BlockId(5);
        byte[] writeBuffer = new byte[bufferSize];
        Device.IOInfo ioInfoWrite = new Device.IOInfo(null,
                                                      Device.IOFunction.Write,
                                                      writeBuffer, blockId, bufferSize);
        d.handleIo(ioInfoWrite);
        assertEquals(Device.IOStatus.NotReady, ioInfoWrite.getStatus());
    }

    @Test
    public void ioWrite_fail_bufferTooSmall(
    ) throws Exception {
        String fileName = getTestFileName();
        BlockSize blockSize = new BlockSize(128);
        PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        BlockCount blockCount = new BlockCount(10000 * (prepFactor.getBlocksPerTrack()));
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);
        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        d.setReady(true);
        byte[] infoBuffer = new byte[128];
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo(null,
                                                        Device.IOFunction.GetInfo,
                                                        infoBuffer,
                                                        128);
        d.handleIo(ioInfoGetInfo);

        byte[] writeBuffer = new byte[10];
        BlockId blockId = new BlockId(5);

        Device.IOInfo ioInfoWrite = new Device.IOInfo(null, Device.IOFunction.Write, writeBuffer, blockId, blockSize.getValue());
        d.handleIo(ioInfoWrite);
        assertEquals(Device.IOStatus.BufferTooSmall, ioInfoWrite.getStatus());

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

        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        d.setReady(true);

        byte[] infoBuffer = new byte[128];
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo(null,
                                                        Device.IOFunction.GetInfo,
                                                        infoBuffer,
                                                        128);
        d.handleIo(ioInfoGetInfo);

        byte[] writeBuffer = new byte[(int)blockSize.getValue()];
        BlockId blockId = new BlockId(5);

        Device.IOInfo ioInfoWrite = new Device.IOInfo(null,
                                                      Device.IOFunction.Write,
                                                      writeBuffer,
                                                      blockId,
                                                      blockSize.getValue() - 1);
        d.handleIo(ioInfoWrite);
        assertEquals(Device.IOStatus.InvalidBlockSize, ioInfoWrite.getStatus());

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

        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        d.setReady(true);

        byte[] infoBuffer = new byte[128];
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo(null,
                                                        Device.IOFunction.GetInfo,
                                                        infoBuffer,
                                                        128);
        d.handleIo(ioInfoGetInfo);

        byte[] writeBuffer = new byte[(int)blockSize.getValue()];
        Device.IOInfo ioInfoWrite = new Device.IOInfo(null,
                                                      Device.IOFunction.Write,
                                                      writeBuffer,
                                                      new BlockId(blockCount.getValue()),
                                                      blockSize.getValue());
        d.handleIo(ioInfoWrite);
        assertEquals(Device.IOStatus.InvalidBlockId, ioInfoWrite.getStatus());

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

        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        d.setReady(true);

        byte[] infoBuffer = new byte[128];
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo(null,
                                                        Device.IOFunction.GetInfo,
                                                        infoBuffer,
                                                        128);
        d.handleIo(ioInfoGetInfo);

        byte[] writeBuffer = new byte[2 * blockSize.getValue()];
        Device.IOInfo ioInfoWrite = new Device.IOInfo(null,
                                                      Device.IOFunction.Write,
                                                      writeBuffer,
                                                      new BlockId(blockCount.getValue() - 1),
                                                      2 * blockSize.getValue());
        d.handleIo(ioInfoWrite);
        assertEquals(Device.IOStatus.InvalidBlockCount, ioInfoWrite.getStatus());

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

        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        d.setReady(true);

        byte[] writeBuffer = new byte[(int)blockSize.getValue()];
        BlockId blockId = new BlockId(5);
        Device.IOInfo ioInfoWrite = new Device.IOInfo(null,
                                                      Device.IOFunction.Write,
                                                      writeBuffer,
                                                      blockId,
                                                      blockSize.getValue());
        d.handleIo(ioInfoWrite);
        assertEquals(Device.IOStatus.UnitAttention, ioInfoWrite.getStatus());

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

        //  set up the device and eat the UA
        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        d.setReady(true);
        d.setIsWriteProtected(true);

        byte[] infoBuffer = new byte[128];
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo(null,
                                                        Device.IOFunction.GetInfo,
                                                        infoBuffer,
                                                        128);
        d.handleIo(ioInfoGetInfo);

        byte[] writeBuffer = new byte[(int)blockSize.getValue()];
        BlockId blockId = new BlockId(5);

        Device.IOInfo ioInfoWrite = new Device.IOInfo(null,
                                                      Device.IOFunction.Write,
                                                      writeBuffer,
                                                      blockId,
                                                      blockSize.getValue());
        d.handleIo(ioInfoWrite);
        assertEquals(Device.IOStatus.WriteProtected, ioInfoWrite.getStatus());

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

        TestDevice d = new TestDevice("TEST", (short)0);
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

        TestDevice d = new TestDevice("TEST", (short)0);
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

        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        assertFalse(d.mount("BLAH.pack"));
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void mount_failed_noFile(
    ) {
        TestDevice d = new TestDevice("TEST", (short)0);
        assertFalse(d.mount("/blah/blah/blah/FOO.pack"));
    }

    @Test
    public void mount_failed_noScratchPad(
    ) throws IOException {
        String fileName = getTestFileName();
        Files.deleteIfExists(FileSystems.getDefault().getPath(fileName));
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        file.close();
        TestDevice d = new TestDevice("TEST", (short)0);
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

        TestDevice d = new TestDevice("TEST", (short)0);
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

        TestDevice d = new TestDevice("TEST", (short)0);
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

        TestDevice d = new TestDevice("TEST", (short)0);
        assertFalse(d.mount(fileName));
        deleteTestFile(fileName);
    }

    @Test
    public void setReady_false_successful_alreadyFalse(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemDiskDevice.createPack(fileName, new BlockSize(8192), new BlockCount(10000));

        FileSystemDiskDevice d = new FileSystemDiskDevice("TEST", (short)0);
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

        FileSystemDiskDevice d = new FileSystemDiskDevice("TEST", (short)0);
        d.mount(fileName);
        d.setReady(true);
        assertTrue(d.setReady(false));
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void setReady_false_successful_noPack(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", (short)0);
        assertTrue(d.setReady(false));
    }

    @Test
    public void setReady_true_successful(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemDiskDevice.createPack(fileName,
                                        new BlockSize(8192),
                                        new BlockCount(10000));
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", (short)0);
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
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", (short)0);
        d.mount(fileName);
        d.setReady(true);
        assertTrue(d.setReady(true));
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void setReady_true_failed_noPack(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0", (short)0);
        assertFalse(d.setReady(true));
    }

    //???? setWriteProtected

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

        TestDevice d = new TestDevice("TEST", (short)0);
        d.mount(fileName);
        assertTrue(d.unmount());
        deleteTestFile(fileName);
    }

    @Test
    public void unmount_failed(
    ) {
        TestDevice d = new TestDevice("TEST", (short)0);
        assertFalse(d.unmount());
    }
}
