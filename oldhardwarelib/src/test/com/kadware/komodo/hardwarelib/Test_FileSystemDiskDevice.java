/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
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
import static org.junit.Assert.*;

/**
 * Unit tests for FileSystemDiskDevice class
 */
public class Test_FileSystemDiskDevice {

    public static class TestChannelModule extends ChannelModule {

        private final List<Device.IOInfo> _ioList = new LinkedList<>();

        private TestChannelModule() {
            super(ChannelModuleType.Byte, "TESTCM");
        }

        //  Only for satisfying the compiler
        protected Tracker createTracker(
            Processor p,
            InputOutputProcessor iop,
            ChannelProgram cp,
            ArraySlice buffer
        ) {
            return null;
        }

        //  This is the real thing
        void submitAndWait(
            final Device target,
            final Device.IOInfo deviceIoInfo
        ) {
            if (target.handleIo(deviceIoInfo)) {
                synchronized (_ioList) {
                    _ioList.add(deviceIoInfo);
                }

                synchronized (deviceIoInfo) {
                    while (deviceIoInfo._status == IOStatus.InProgress) {
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
                    Iterator<Device.IOInfo> iter = _ioList.iterator();
                    while (iter.hasNext()) {
                        Device.IOInfo ioInfo = iter.next();
                        if (ioInfo._status != IOStatus.InProgress) {
                            iter.remove();
                            ioInfo._status.notify();
                        }
                    }
                }
            }
        }
    }

    public static class TestDevice extends FileSystemDiskDevice {

        TestDevice() { super("TEST"); }

        @Override
        public long calculateByteOffset(
            final long blockId
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


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Tests
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void create(
    ) {
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        assertEquals("DISK0", d._name);
        assertEquals(NodeCategory.Device, d._category);
        Assert.assertEquals(NodeModel.FileSystemDisk, d._deviceNodeModel);
        assertEquals(NodeType.Disk, d._deviceNodeType);
    }

    @Test
    public void calculateByteOffset(
    ) {
        FileSystemDiskDevice d = new TestDevice();
        d._blockSize = 256;
        assertEquals(3 * 256, d.calculateByteOffset(2));
    }

    @Test
    public void calculateByteOffset_reallyBig(
    ) {
        TestDevice d = new TestDevice();
        d._blockSize = 256;
        assertEquals(0x80000001L * 256, d.calculateByteOffset(0x80000000L));
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
                                                          1024 * 1024)));
        assertFalse(d.canConnect(new InputOutputProcessor("IOP0", InventoryManager.FIRST_INPUT_OUTPUT_PROCESSOR_UPI_INDEX)));
        assertFalse(d.canConnect(new InstructionProcessor("IP0", InventoryManager.FIRST_INSTRUCTION_PROCESSOR_UPI_INDEX)));
    }

    @Test
    public void createPack(
    ) throws Exception {
        String fileName = getTestFileName();
        Integer[] blockSizes = { 128, 256, 512, 1024, 2048, 4096, 8192 };

        for (Integer blockSize : blockSizes) {
            long blockCount = 10000 * (8192 / blockSize);
            FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

            //  Make sure we can read the ScratchPad
            RandomAccessFile check = new RandomAccessFile(fileName, "r");
            byte[] buffer = new byte[blockSize];
            check.seek(0);
            assertEquals((int)blockSize, check.read(buffer));
            check.close();

            //  Verify the ScratchPad
            TestDevice.ScratchPad sp = new TestDevice.ScratchPad();
            sp.deserialize(ByteBuffer.wrap(buffer));
            assertEquals(blockCount, sp._blockCount);
            assertEquals((int)blockSize, sp._blockSize);
        }

        deleteTestFile(fileName);
    }

    @Test(expected = FileNotFoundException.class)
    public void createPack_badPath(
    ) throws Exception {
        FileSystemDiskDevice.createPack("/blah/blah/blah/TEST.pack", 8192, 10000);
    }

    @Test(expected = InvalidBlockSizeException.class)
    public void createPack_invalidBlockSize(
    ) throws Exception {
        FileSystemDiskDevice.createPack(getTestFileName(), 22, 1000);
    }

    @Test(expected = InvalidTrackCountException.class)
    public void createPack_invalidTrackCount_1(
    ) throws Exception {
        FileSystemDiskDevice.createPack(getTestFileName(), 8192, 9999);
    }

    @Test(expected = InvalidTrackCountException.class)
    public void createPack_invalidTrackCount_2(
    ) throws Exception {
        FileSystemDiskDevice.createPack(getTestFileName(), 8192, 100000);
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
        long blockCount = 10000;
        int blockSize = 8192;
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        d.mount(fileName);
        d.setReady(true);

        Device.IOInfo ioInfo = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                      .setIOFunction(IOFunction.GetInfo)
                                                                      .setTransferCount(128)
                                                                      .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(IOStatus.Successful, ioInfo._status);
        ArraySlice as = new ArraySlice(new long[28]);
        as.unpack(ioInfo._byteBuffer, false);

        int flags = (int) Word36.getS1(as.get(0));
        boolean resultIsReady = (flags & 040) != 0;
        boolean resultIsMounted = (flags & 004) != 0;
        boolean resultIsWriteProtected = (flags & 002) != 0;
        NodeModel resultNodeModel = NodeModel.getValue((int) Word36.getS2(as.get(0)));
        NodeType resultNodeType = NodeType.getValue((int) Word36.getS3(as.get(0)));
        int resultPrepFactor = (int) Word36.getH2(as.get(6));
        long resultBlockCount = as.get(7);

        assertTrue(resultIsReady);
        assertTrue(resultIsMounted);
        assertFalse(resultIsWriteProtected);
        assertEquals(NodeModel.FileSystemDisk, resultNodeModel);
        assertEquals(NodeType.Disk, resultNodeType);
        assertEquals(blockCount, resultBlockCount);
        assertEquals(PrepFactor.getPrepFactorFromBlockSize(blockSize), resultPrepFactor);
        assertFalse(d._unitAttentionFlag);
        assertEquals(1, d._miscCount);
        assertEquals(0, d._readCount);
        assertEquals(0, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioRead_fail_notReady(
    ) throws Exception {
        String fileName = getTestFileName();
        int blockSize = 128;
        int prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        long blockCount = 10000 * PrepFactor.getBlocksPerTrack(prepFactor);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(false);

        long blockId = 5;
        Device.IOInfo ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.Read)
                                                                          .setBlockId(blockId)
                                                                          .setTransferCount(blockSize)
                                                                          .build();

        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.NotReady, ioInfoRead._status);
        assertEquals(0, d._miscCount);
        assertEquals(1, d._readCount);
        assertEquals(0, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioRead_fail_invalidBlockSize(
    ) throws Exception {
        String fileName = getTestFileName();
        int blockSize = 128;
        int prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        long blockCount = 10000 * PrepFactor.getBlocksPerTrack(prepFactor);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        //  Clear unit attention
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                             .setIOFunction(IOFunction.GetInfo)
                                                                             .setTransferCount(128)
                                                                             .build();
        cm.submitAndWait(d, ioInfoGetInfo);
        long blockId = 5;
        Device.IOInfo ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.Read)
                                                                          .setBlockId(blockId)
                                                                          .setTransferCount(blockSize - 1)
                                                                          .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.InvalidBlockSize, ioInfoRead._status);
        assertEquals(1, d._miscCount);
        assertEquals(1, d._readCount);
        assertEquals(0, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioRead_fail_invalidBlockId(
    ) throws Exception {
        String fileName = getTestFileName();
        int blockSize = 128;
        int prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        long blockCount = 10000 * PrepFactor.getBlocksPerTrack(prepFactor);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        //  Clear unit attention
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                             .setIOFunction(IOFunction.GetInfo)
                                                                             .setTransferCount(128)
                                                                             .build();
        cm.submitAndWait(d, ioInfoGetInfo);

        Device.IOInfo ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.Read)
                                                                          .setBlockId(blockCount)
                                                                          .setTransferCount(blockSize)
                                                                          .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.InvalidBlockId, ioInfoRead._status);
        assertEquals(1, d._miscCount);
        assertEquals(1, d._readCount);
        assertEquals(0, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioRead_fail_invalidBlockCount(
    ) throws Exception {
        String fileName = getTestFileName();
        int blockSize = 128;
        int prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        long blockCount = 10000 * PrepFactor.getBlocksPerTrack(prepFactor);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        //  Clear unit attention
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                             .setIOFunction(IOFunction.GetInfo)
                                                                             .setTransferCount(128)
                                                                             .build();
        cm.submitAndWait(d, ioInfoGetInfo);

        Device.IOInfo ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.Read)
                                                                          .setBlockId(blockCount - 1)
                                                                          .setTransferCount(2 * blockSize)
                                                                          .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.InvalidBlockCount, ioInfoRead._status);
        assertEquals(1, d._miscCount);
        assertEquals(1, d._readCount);
        assertEquals(0, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioRead_fail_unitAttention(
    ) throws Exception {
        String fileName = getTestFileName();
        int blockSize = 128;
        int prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        long blockCount = 10000 * PrepFactor.getBlocksPerTrack(prepFactor);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        long blockId = 5;
        Device.IOInfo ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.Read)
                                                                          .setBlockId(blockId)
                                                                          .setTransferCount(blockSize)
                                                                          .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.UnitAttention, ioInfoRead._status);
        assertEquals(0, d._miscCount);
        assertEquals(1, d._readCount);
        assertEquals(0, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioReset_successful(
    ) throws Exception {
        String fileName = getTestFileName();
        long blockCount = 10000;
        int blockSize = 8192;
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        FileSystemDiskDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        Device.IOInfo ioInfo = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                     .setIOFunction(IOFunction.Reset)
                                                                     .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(IOStatus.Successful, ioInfo._status);
        assertEquals(1, d._miscCount);
        assertEquals(0, d._readCount);
        assertEquals(0, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioReset_failed_notReady(
    ) throws Exception {
        String fileName = getTestFileName();
        long blockCount = 10000;
        int blockSize = 8192;
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        d.mount(fileName);
        d.setReady(false);

        Device.IOInfo ioInfo = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                     .setIOFunction(IOFunction.Reset)
                                                                     .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(IOStatus.NotReady, ioInfo._status);
        assertEquals(1, d._miscCount);
        assertEquals(0, d._readCount);
        assertEquals(0, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioStart_failed_badFunction(
    ) {
        TestChannelModule cm = new TestChannelModule();
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");

        Device.IOInfo[] ioInfos = {
            new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                  .setIOFunction(IOFunction.Close)
                                                        .build(),
            new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                  .setIOFunction(IOFunction.MoveBlock)
                                                        .build(),
            new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                  .setIOFunction(IOFunction.MoveBlockBackward)
                                                        .build(),
            new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                  .setIOFunction(IOFunction.MoveFile)
                                                        .build(),
            new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                  .setIOFunction(IOFunction.MoveFileBackward)
                                                        .build(),
            new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                  .setIOFunction(IOFunction.Rewind)
                                                        .build(),
            new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                  .setIOFunction(IOFunction.RewindInterlock)
                                                        .build(),
            new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                  .setIOFunction(IOFunction.SetMode)
                                                        .build(),
            new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                  .setIOFunction(IOFunction.ReadBackward)
                                                        .build(),
            new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                  .setIOFunction(IOFunction.WriteEndOfFile)
                                                        .build(),
        };

        for (Device.IOInfo ioInfo : ioInfos) {
            cm.submitAndWait(d, ioInfo);
            assertEquals(IOStatus.InvalidFunction, ioInfo._status);
        }

        assertEquals(0, d._miscCount);
        assertEquals(0, d._readCount);
        assertEquals(0, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
    }

    @Test
    public void ioStart_none(
    ) {
        TestChannelModule cm = new TestChannelModule();
        FileSystemDiskDevice d = new FileSystemDiskDevice("DISK0");
        Device.IOInfo ioInfo = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                     .setIOFunction(IOFunction.None)
                                                                     .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(IOStatus.Successful, ioInfo._status);
        assertEquals(1, d._miscCount);
        assertEquals(0, d._readCount);
        assertEquals(0, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
    }

    @Test
    public void ioUnload_successful(
    ) throws IOException {
        String fileName = getTestFileName();
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        byte[] buffer = new byte[128];
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(1792, 8192, 10000);
        sp.serialize(ByteBuffer.wrap(buffer));
        file.write(buffer);
        file.close();

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);
        Device.IOInfo ioInfo = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                     .setIOFunction(IOFunction.Unload)
                                                                     .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(IOStatus.Successful, ioInfo._status);
        assertEquals(1, d._miscCount);
        assertEquals(0, d._readCount);
        assertEquals(0, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioUnload_failed_notReady(
    ) throws IOException {
        String fileName = getTestFileName();
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        byte[] buffer = new byte[128];
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(1792, 8192, 10000);
        sp.serialize(ByteBuffer.wrap(buffer));
        file.write(buffer);
        file.close();

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(false);
        Device.IOInfo ioInfo = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                     .setIOFunction(IOFunction.Unload)
                                                                     .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(IOStatus.NotReady, ioInfo._status);
        assertEquals(1, d._miscCount);
        assertEquals(0, d._readCount);
        assertEquals(0, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_ioRead_successful(
    ) throws Exception {
        Random r = new Random((int)System.currentTimeMillis());
        String fileName = getTestFileName();
        Integer[] blockSizes = { 128, 256, 512, 1024, 2048, 4096, 8192 };

        for (Integer blockSize : blockSizes) {
            int prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
            long blockCount = 10000 * PrepFactor.getBlocksPerTrack(prepFactor);
            FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

            //  set up the device and eat the UA
            TestChannelModule cm = new TestChannelModule();
            TestDevice d = new TestDevice();
            d.mount(fileName);
            d.setReady(true);
            Device.IOInfo ioInfo = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.GetInfo)
                                                                          .setTransferCount(128)
                                                                          .build();
            cm.submitAndWait(d, ioInfo);
            long expBytes = 0;
            for (int x = 0; x < 16; ++x) {
                long blockIdVal = r.nextInt() % blockCount;
                if (blockIdVal < 0) {
                    blockIdVal = -blockIdVal;
                }

                //  note - we purposely allow block count of zero
                int ioBlockCount = r.nextInt() % 4;
                if (ioBlockCount < 0) {
                    ioBlockCount = -ioBlockCount;
                }

                int bufferSize = ioBlockCount * blockSize;
                expBytes += bufferSize;
                byte[] writeBuffer = new byte[bufferSize];
                r.nextBytes(writeBuffer);

                Device.IOInfo ioInfoWrite = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                                   .setIOFunction(IOFunction.Write)
                                                                                   .setBlockId(blockIdVal)
                                                                                   .setBuffer(writeBuffer)
                                                                                   .setTransferCount(bufferSize)
                                                                                   .build();
                cm.submitAndWait(d, ioInfoWrite);
                assertEquals(IOStatus.Successful, ioInfoWrite._status);
                assertEquals(bufferSize, ioInfoWrite._transferredCount);

                Device.IOInfo ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                                  .setIOFunction(IOFunction.Read)
                                                                                  .setBlockId(blockIdVal)
                                                                                  .setTransferCount(bufferSize)
                                                                                  .build();
                cm.submitAndWait(d, ioInfoRead);
                assertEquals(IOStatus.Successful, ioInfoRead._status);
                assertEquals(bufferSize, ioInfoRead._transferredCount);
                assertArrayEquals(writeBuffer, ioInfoRead._byteBuffer);
            }

            assertEquals(1, d._miscCount);
            assertEquals(16, d._readCount);
            assertEquals(16, d._writeCount);
            assertEquals(expBytes, d._readBytes);
            assertEquals(expBytes, d._writeBytes);
            d.unmount();
            deleteTestFile(fileName);
        }
    }

    @Test
    public void ioWrite_fail_notReady(
    ) throws Exception {
        String fileName = getTestFileName();
        int blockSize = 128;
        int prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        long blockCount = 10000 * PrepFactor.getBlocksPerTrack(prepFactor);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(false);

        int bufferSize = 128;
        long blockId = 5;
        byte[] writeBuffer = new byte[bufferSize];
        Device.IOInfo ioInfoWrite = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.Write)
                                                                           .setBlockId(blockId)
                                                                           .setBuffer(writeBuffer)
                                                                           .setTransferCount(writeBuffer.length)
                                                                           .build();
        cm.submitAndWait(d, ioInfoWrite);
        assertEquals(IOStatus.NotReady, ioInfoWrite._status);
        assertEquals(0, d._miscCount);
        assertEquals(0, d._readCount);
        assertEquals(1, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_fail_bufferTooSmall(
    ) throws Exception {
        String fileName = getTestFileName();
        int blockSize = 128;
        int prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        long blockCount = 10000 * PrepFactor.getBlocksPerTrack(prepFactor);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        Device.IOInfo ioInfo = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                      .setIOFunction(IOFunction.GetInfo)
                                                                      .setTransferCount(128)
                                                                      .build();
        cm.submitAndWait(d, ioInfo);

        byte[] writeBuffer = new byte[10];
        long blockId = 5;
        Device.IOInfo ioInfoWrite = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.Write)
                                                                           .setBlockId(blockId)
                                                                           .setBuffer(writeBuffer)
                                                                           .setTransferCount(blockSize)
                                                                           .build();
        cm.submitAndWait(d, ioInfoWrite);
        assertEquals(IOStatus.BufferTooSmall, ioInfoWrite._status);
        assertEquals(1, d._miscCount);
        assertEquals(0, d._readCount);
        assertEquals(1, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_fail_invalidBlockSize(
    ) throws Exception {
        String fileName = getTestFileName();
        int blockSize = 128;
        int prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        long blockCount = 10000 * PrepFactor.getBlocksPerTrack(prepFactor);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        //  Clear unit attention
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                             .setIOFunction(IOFunction.GetInfo)
                                                                             .setTransferCount(128)
                                                                             .build();
        cm.submitAndWait(d, ioInfoGetInfo);

        byte[] readBuffer = new byte[blockSize];
        long blockId = 5;

        Device.IOInfo ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.Write)
                                                                          .setBlockId(blockId)
                                                                          .setBuffer(readBuffer)
                                                                          .setTransferCount(blockSize - 1)
                                                                          .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.InvalidBlockSize, ioInfoRead._status);
        assertEquals(1, d._miscCount);
        assertEquals(0, d._readCount);
        assertEquals(1, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_fail_invalidBlockId(
    ) throws Exception {
        String fileName = getTestFileName();
        int blockSize = 128;
        int prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        long blockCount = 10000 * PrepFactor.getBlocksPerTrack(prepFactor);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        //  Clear unit attention
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                             .setIOFunction(IOFunction.GetInfo)
                                                                             .setTransferCount(128)
                                                                             .build();
        cm.submitAndWait(d, ioInfoGetInfo);

        byte[] readBuffer = new byte[blockSize];
        Device.IOInfo ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.Write)
                                                                          .setBlockId(blockCount)
                                                                          .setBuffer(readBuffer)
                                                                          .setTransferCount(blockSize)
                                                                          .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.InvalidBlockId, ioInfoRead._status);
        assertEquals(1, d._miscCount);
        assertEquals(0, d._readCount);
        assertEquals(1, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_fail_invalidBlockCount(
    ) throws Exception {
        String fileName = getTestFileName();
        int blockSize = 128;
        int prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        long blockCount = 10000 * PrepFactor.getBlocksPerTrack(prepFactor);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        //  Clear unit attention
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                             .setIOFunction(IOFunction.GetInfo)
                                                                             .setTransferCount(128)
                                                                             .build();
        cm.submitAndWait(d, ioInfoGetInfo);

        byte[] readBuffer = new byte[2 * blockSize];
        Device.IOInfo ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.Write)
                                                                          .setBlockId(blockCount - 1)
                                                                          .setBuffer(readBuffer)
                                                                          .setTransferCount(2 * blockSize)
                                                                          .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.InvalidBlockCount, ioInfoRead._status);
        assertEquals(1, d._miscCount);
        assertEquals(0, d._readCount);
        assertEquals(1, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_fail_unitAttention(
    ) throws Exception {
        String fileName = getTestFileName();
        int blockSize = 128;
        int prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        long blockCount = 10000 * PrepFactor.getBlocksPerTrack(prepFactor);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        byte[] writeBuffer = new byte[blockSize];
        long blockId = 5;
        Device.IOInfo ioInfoWrite = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.Write)
                                                                           .setBlockId(blockId)
                                                                           .setBuffer(writeBuffer)
                                                                           .setTransferCount(blockSize)
                                                                           .build();
        cm.submitAndWait(d, ioInfoWrite);
        assertEquals(IOStatus.UnitAttention, ioInfoWrite._status);
        assertEquals(0, d._miscCount);
        assertEquals(0, d._readCount);
        assertEquals(1, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_fail_writeProtected(
    ) throws Exception {
        String fileName = getTestFileName();
        int blockSize = 128;
        int prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        long blockCount = 10000 * PrepFactor.getBlocksPerTrack(prepFactor);
        FileSystemDiskDevice.createPack(fileName, blockSize, blockCount);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);
        d.setIsWriteProtected(true);

        //  Clear unit attention
        Device.IOInfo ioInfoGetInfo = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                             .setIOFunction(IOFunction.GetInfo)
                                                                             .setTransferCount(128)
                                                                             .build();
        cm.submitAndWait(d, ioInfoGetInfo);

        byte[] writeBuffer = new byte[blockSize];
        long blockId = 5;
        Device.IOInfo ioInfoWrite = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.Write)
                                                                           .setBlockId(blockId)
                                                                           .setBuffer(writeBuffer)
                                                                           .setTransferCount(blockSize)
                                                                           .build();
        cm.submitAndWait(d, ioInfoWrite);
        assertEquals(IOStatus.WriteProtected, ioInfoWrite._status);
        assertEquals(1, d._miscCount);
        assertEquals(0, d._readCount);
        assertEquals(1, d._writeCount);
        assertEquals(0, d._readBytes);
        assertEquals(0, d._writeBytes);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void mount_successful(
    ) throws IOException {
        String fileName = getTestFileName();
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        byte[] buffer = new byte[128];
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(1792, 8192, 10000);
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
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(1792, 8192, 10000);
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
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(1792, 8192, 10000);
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
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(1792, 8192, 10000);
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
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(1792, 8192, 10000);
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
        FileSystemDiskDevice.createPack(fileName, 8192, 10000);

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
        FileSystemDiskDevice.createPack(fileName, 8192, 10000);

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
        FileSystemDiskDevice.createPack(fileName, 8192, 10000);
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
        FileSystemDiskDevice.createPack(fileName, 8192, 10000);
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
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(1792, 8192, 10000);
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
