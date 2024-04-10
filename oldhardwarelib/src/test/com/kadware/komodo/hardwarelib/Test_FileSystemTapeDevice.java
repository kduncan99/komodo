/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.*;
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
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for FileSystemDiskDevice class
 */
public class Test_FileSystemTapeDevice {

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

    public static class TestDevice extends FileSystemTapeDevice {

        TestDevice() { super("TEST"); }
    }

    private static int nextFileIndex = 1;
    private static final Random _random = new Random(System.currentTimeMillis());

    /**
     * Prepends the system-wide temporary path to the given base name if found
     * @return cooked path/file name
     */
    private static String getTestFileName(
    ) {
        String pathName = System.getProperty("java.io.tmpdir");
        return String.format("%sTEST%04d.vol", pathName == null ? "" : pathName, nextFileIndex++);
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
        FileSystemTapeDevice d = new FileSystemTapeDevice("TAPE0");
        assertEquals("TAPE0", d._name);
        assertEquals(NodeCategory.Device, d._category);
        Assert.assertEquals(NodeModel.FileSystemTape, d._deviceNodeModel);
        assertEquals(NodeType.Tape, d._deviceNodeType);
    }

    @Test
    public void canConnect_success(
    ) {
        ByteChannelModule cm = new ByteChannelModule("CM0");
        FileSystemDiskDevice d = new FileSystemDiskDevice("TAPE0");
        assertTrue(d.canConnect(cm));
    }

    @Test
    public void canConnect_failure(
    ) {
        FileSystemTapeDevice d = new FileSystemTapeDevice("TAPE0");
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
    public void createVolume(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemTapeDevice.createVolume(fileName);
        deleteTestFile(fileName);
    }

    @Test(expected = FileNotFoundException.class)
    public void createVolume_badPath(
    ) throws Exception {
        FileSystemTapeDevice.createVolume("/blah/blah/blah/TEST.vol");
    }

    @Test
    public void hasByteInterface(
    ) {
        TestDevice d = new TestDevice();
        assertTrue(d.hasByteInterface());
    }

    @Test
    public void hasWordInterface(
    ) {
        TestDevice d = new TestDevice();
        assertFalse(d.hasWordInterface());
    }

    @Test
    public void ioGetInfo_successful(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemTapeDevice.createVolume(fileName);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        assertTrue(d.setReady(true));

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

        assertTrue(resultIsReady);
        assertTrue(resultIsMounted);
        assertTrue(resultIsWriteProtected);
        assertEquals(NodeModel.FileSystemTape, resultNodeModel);
        assertEquals(NodeType.Tape, resultNodeType);
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
        Device.IOInfo ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.Read)
                                                                          .setBlockId(blockId)
                                                                          .setTransferCount(blockSize)
                                                                          .build();

        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.NotReady, ioInfoRead._status);
    }

    @Test
    public void ioRead_fail_unitAttention(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemTapeDevice.createVolume(fileName);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        assertTrue(d.mount(fileName));
        assertTrue(d.setReady(true));

        Device.IOInfo ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.Read)
                                                                          .setTransferCount(1024)
                                                                          .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.UnitAttention, ioInfoRead._status);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioReset_successful(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemTapeDevice.createVolume(fileName);

        TestChannelModule cm = new TestChannelModule();
        FileSystemTapeDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        Device.IOInfo ioInfo = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                     .setIOFunction(IOFunction.Reset)
                                                                     .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(IOStatus.Successful, ioInfo._status);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioReset_failed_notReady(
    ) {
        TestChannelModule cm = new TestChannelModule();
        FileSystemTapeDevice d = new FileSystemTapeDevice("TAPE0");
        Device.IOInfo ioInfo = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                     .setIOFunction(IOFunction.Reset)
                                                                     .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(IOStatus.NotReady, ioInfo._status);
    }

    @Test
    public void ioStart_failed_badFunction(
    ) {
        TestChannelModule cm = new TestChannelModule();
        FileSystemTapeDevice d = new TestDevice();

        Device.IOInfo[] ioInfos = {
            new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                  .setIOFunction(IOFunction.Close)
                                                        .build(),
        };

        for (Device.IOInfo ioInfo : ioInfos) {
            cm.submitAndWait(d, ioInfo);
            assertEquals(IOStatus.InvalidFunction, ioInfo._status);
        }
    }

    @Test
    public void ioStart_none(
    ) {
        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        Device.IOInfo ioInfo = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                     .setIOFunction(IOFunction.None)
                                                                     .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(IOStatus.Successful, ioInfo._status);
    }

    @Test
    public void ioUnload_successful(
    ) throws IOException {
        String fileName = getTestFileName();
        FileSystemTapeDevice.createVolume(fileName);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        Device.IOInfo ioInfo = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                     .setIOFunction(IOFunction.Unload)
                                                                     .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(IOStatus.Successful, ioInfo._status);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioUnload_failed_notReady(
    ) throws IOException {
        String fileName = getTestFileName();
        FileSystemTapeDevice.createVolume(fileName);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(false);

        Device.IOInfo ioInfo = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                     .setIOFunction(IOFunction.Unload)
                                                                     .build();
        cm.submitAndWait(d, ioInfo);
        assertEquals(IOStatus.NotReady, ioInfo._status);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void io_fileMarks(
    ) throws Exception {
        //  Create a volume, mount it, write three file marks, and rewind it
        String fileName = getTestFileName();
        FileSystemTapeDevice.createVolume(fileName);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);
        d.setIsWriteProtected(false);

        Device.IOInfo ioInfoGet = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                         .setIOFunction(IOFunction.GetInfo)
                                                                         .setTransferCount(128)
                                                                         .build();
        cm.submitAndWait(d, ioInfoGet);
        assertEquals(IOStatus.Successful, ioInfoGet._status);

        Device.IOInfo ioInfoWrite = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.WriteEndOfFile)
                                                                          .build();
        cm.submitAndWait(d, ioInfoWrite);
        assertEquals(IOStatus.Successful, ioInfoWrite._status);
        assertFalse(d._loadPointFlag);
        cm.submitAndWait(d, ioInfoWrite);
        assertEquals(IOStatus.Successful, ioInfoWrite._status);
        cm.submitAndWait(d, ioInfoWrite);
        assertEquals(IOStatus.Successful, ioInfoWrite._status);

        Device.IOInfo ioInfoRewind = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.Rewind)
                                                                           .build();
        cm.submitAndWait(d, ioInfoRewind);
        assertEquals(IOStatus.Successful, ioInfoRewind._status);
        cm.submitAndWait(d, ioInfoRewind);
        assertEquals(IOStatus.EndOfTape, ioInfoRewind._status);
        assertTrue(d._loadPointFlag);

        assertEquals(3, d._miscCount);  //  get info and 2x rewind
        assertEquals(0, d._readCount);
        assertEquals(0, d._readBytes);
        assertEquals(3, d._writeCount); //  3x write mark
        assertEquals(0, d._writeBytes);

        //  Try 3 forward operations - move block, move file, read.
        Device.IOInfo ioInfoMoveBlock = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                              .setIOFunction(IOFunction.MoveBlock)
                                                                              .build();
        cm.submitAndWait(d, ioInfoMoveBlock);
        assertEquals(IOStatus.FileMark, ioInfoMoveBlock._status);

        Device.IOInfo ioInfoMoveFile = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                             .setIOFunction(IOFunction.MoveFile)
                                                                             .build();
        cm.submitAndWait(d, ioInfoMoveFile);
        assertEquals(IOStatus.Successful, ioInfoMoveFile._status);

        Device.IOInfo ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.Read)
                                                                          .setTransferCount(0)
                                                                          .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.FileMark, ioInfoRead._status);

        assertEquals(5, d._miscCount);      //  previous, plus 2 moves
        assertEquals(1, d._readCount);
        assertEquals(0, d._readBytes);
        assertEquals(3, d._filesExtended);
        assertEquals(0, d._blocksExtended);

        //  Try another forward operation - should get loss of position
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.LostPosition, ioInfoRead._status);
        assertEquals(2, d._readCount);

        //  Clear lost position state
        d._lostPositionFlag = false;

        //  Now try 3 backward operations - move block, move file, read.
        ioInfoMoveBlock = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                .setIOFunction(IOFunction.MoveBlockBackward)
                                                                .build();
        cm.submitAndWait(d, ioInfoMoveBlock);
        assertEquals(IOStatus.FileMark, ioInfoMoveBlock._status);

        ioInfoMoveFile = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                               .setIOFunction(IOFunction.MoveFileBackward)
                                                               .build();
        cm.submitAndWait(d, ioInfoMoveFile);
        assertEquals(IOStatus.Successful, ioInfoMoveFile._status);

        ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                            .setIOFunction(IOFunction.ReadBackward)
                                                            .setTransferCount(0)
                                                            .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.FileMark, ioInfoRead._status);
        assertEquals(7, d._miscCount);      //  previous, plus 2 more moves
        assertEquals(3, d._readCount);      //  previous, plus another read
        assertEquals(0, d._readBytes);
        assertEquals(6, d._filesExtended);  //  three more tape marks encountered
        assertEquals(0, d._blocksExtended);

        //  Try one more back operation - should get end-of-tape
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.EndOfTape, ioInfoRead._status);
        assertEquals(4, d._readCount);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_ioRead_successful(
    ) throws Exception {
        //  Create a volume, mount it, write three file marks, and rewind it
        String fileName = getTestFileName();
        FileSystemTapeDevice.createVolume(fileName);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);
        d.setIsWriteProtected(false);

        //  source data buffer
        int blockCount = 30;
        int blockSize = 4096;
        byte[] data = new byte[blockSize];
        _random.nextBytes(data);

        //  create IOInfo blocks
        Device.IOInfo ioInfoGet = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                         .setIOFunction(IOFunction.GetInfo)
                                                                         .setTransferCount(128)
                                                                         .build();

        Device.IOInfo ioInfoMoveBackward = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                                  .setIOFunction(IOFunction.MoveFileBackward)
                                                                                  .setTransferCount(0)
                                                                                  .build();

        Device.IOInfo ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.Read)
                                                                          .setTransferCount(0)
                                                                          .build();

        Device.IOInfo ioInfoReadBackward = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                                  .setIOFunction(IOFunction.ReadBackward)
                                                                                  .setTransferCount(0)
                                                                                  .build();

        Device.IOInfo ioInfoRewind = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.Rewind)
                                                                           .build();

        Device.IOInfo ioInfoWrite = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.Write)
                                                                           .setBuffer(data)
                                                                           .setTransferCount(blockSize)
                                                                           .build();

        Device.IOInfo ioInfoWriteFileMark = new Device.IOInfo.NonTransferBuilder().setSource(cm)
                                                                                  .setIOFunction(IOFunction.WriteEndOfFile)
                                                                                  .build();

        //  eat unit attention
        cm.submitAndWait(d, ioInfoGet);
        assertEquals(IOStatus.Successful, ioInfoGet._status);

        //  write some data blocks, followed by a file mark
        for (int bx = 0; bx < blockCount; ++bx) {
            cm.submitAndWait(d, ioInfoWrite);
            assertEquals(IOStatus.Successful, ioInfoWrite._status);
        }

        cm.submitAndWait(d, ioInfoWriteFileMark);
        assertEquals(IOStatus.Successful, ioInfoWriteFileMark._status);
        assertEquals(1, d._miscCount);
        assertEquals(blockCount + 1, d._writeCount);
        assertEquals(blockCount * blockSize, d._writeBytes);

        //  rewind, then read until we hit end of file
        cm.submitAndWait(d, ioInfoRewind);
        assertEquals(IOStatus.Successful, ioInfoRewind._status);
        assertEquals(2, d._miscCount);

        boolean done = false;
        while (!done) {
            cm.submitAndWait(d, ioInfoRead);
            assertTrue(ioInfoRead._status == IOStatus.Successful
                       || ioInfoRead._status == IOStatus.FileMark);
            if (ioInfoRead._status == IOStatus.FileMark) {
                done = true;
            } else {
                assertArrayEquals(data, ioInfoRead._byteBuffer);
            }
        }

        assertEquals(blockCount + 1, d._readCount);
        assertEquals(blockCount * blockSize, d._readBytes);
        assertEquals(1, d._filesExtended);

        //  Move backward one file mark, then read backward until we hit end of tape
        cm.submitAndWait(d, ioInfoMoveBackward);
        assertEquals(IOStatus.Successful, ioInfoMoveBackward._status);
        assertEquals(3, d._miscCount);
        assertEquals(2, d._filesExtended);

        byte[] reverseData = new byte[data.length];
        for (int sx = data.length - 1, dx = 0; dx < reverseData.length; sx--, dx++) {
            reverseData[dx] = data[sx];
        }

        done = false;
        blockCount = 0;
        while (!done) {
            cm.submitAndWait(d, ioInfoRead);
            assertTrue(ioInfoReadBackward._status == IOStatus.Successful
                       || ioInfoRead._status == IOStatus.FileMark);
            if (ioInfoRead._status == IOStatus.FileMark) {
                done = true;
            } else {
                ++blockCount;
                assertArrayEquals(reverseData, ioInfoRead._byteBuffer);
                assertEquals(blockCount, d._blocksExtended);
            }
        }

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_fail_notReady(
    ) {
        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        int bufferSize = 128;
        byte[] writeBuffer = new byte[bufferSize];

        Device.IOInfo ioInfoWrite = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.Write)
                                                                           .setBuffer(writeBuffer)
                                                                           .setTransferCount(writeBuffer.length)
                                                                           .build();
        cm.submitAndWait(d, ioInfoWrite);
        assertEquals(IOStatus.NotReady, ioInfoWrite._status);
    }

    @Test
    public void ioWrite_fail_invalidBlockSize(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemTapeDevice.createVolume(fileName);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);
        d.setIsWriteProtected(false);

        Device.IOInfo ioInfoGetInfo = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                             .setIOFunction(IOFunction.GetInfo)
                                                                             .setTransferCount(128)
                                                                             .build();
        cm.submitAndWait(d, ioInfoGetInfo);
        assertEquals(IOStatus.Successful, ioInfoGetInfo._status);

        byte[] writeBuffer = new byte[d.getMaxBlockSize() + 1];
        Device.IOInfo ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.Write)
                                                                          .setBuffer(writeBuffer)
                                                                          .setTransferCount(writeBuffer.length)
                                                                          .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.InvalidBlockSize, ioInfoRead._status);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_fail_bufferTooSmall(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemTapeDevice.createVolume(fileName);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);
        d.setIsWriteProtected(false);

        Device.IOInfo ioInfoGetInfo = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                             .setIOFunction(IOFunction.GetInfo)
                                                                             .setTransferCount(128)
                                                                             .build();
        cm.submitAndWait(d, ioInfoGetInfo);
        assertEquals(IOStatus.Successful, ioInfoGetInfo._status);

        byte[] writeBuffer = new byte[1024];
        Device.IOInfo ioInfoRead = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                          .setIOFunction(IOFunction.Write)
                                                                          .setBuffer(writeBuffer)
                                                                          .setTransferCount(2048)
                                                                          .build();
        cm.submitAndWait(d, ioInfoRead);
        assertEquals(IOStatus.BufferTooSmall, ioInfoRead._status);
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_fail_unitAttention(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemTapeDevice.createVolume(fileName);

        TestChannelModule cm = new TestChannelModule();
        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);

        byte[] writeBuffer = new byte[1024];
        long blockId = 5;
        Device.IOInfo ioInfoWrite = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.Write)
                                                                           .setBlockId(blockId)
                                                                           .setBuffer(writeBuffer)
                                                                           .setTransferCount(writeBuffer.length)
                                                                           .build();
        cm.submitAndWait(d, ioInfoWrite);
        assertEquals(IOStatus.UnitAttention, ioInfoWrite._status);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void ioWrite_fail_writeProtected(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemTapeDevice.createVolume(fileName);

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

        byte[] writeBuffer = new byte[1024];
        long blockId = 5;
        Device.IOInfo ioInfoWrite = new Device.IOInfo.ByteTransferBuilder().setSource(cm)
                                                                           .setIOFunction(IOFunction.Write)
                                                                           .setBlockId(blockId)
                                                                           .setBuffer(writeBuffer)
                                                                           .setTransferCount(1024)
                                                                           .build();
        cm.submitAndWait(d, ioInfoWrite);
        assertEquals(IOStatus.WriteProtected, ioInfoWrite._status);

        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void mount_successful(
    ) throws IOException {
        String fileName = getTestFileName();
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        byte[] buffer = new byte[128];
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(FileSystemTapeDevice.MIN_FILE_SIZE);
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
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(FileSystemTapeDevice.MIN_FILE_SIZE);
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
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(FileSystemTapeDevice.MIN_FILE_SIZE);
        sp.serialize(ByteBuffer.wrap(buffer));
        file.write(buffer);
        file.close();

        TestDevice d = new TestDevice();
        d.mount(fileName);
        assertFalse(d.mount("BLAH.vol"));
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void mount_failed_noFile(
    ) {
        TestDevice d = new TestDevice();
        assertFalse(d.mount("/blah/blah/blah/FOO.vol"));
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
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(FileSystemTapeDevice.MIN_FILE_SIZE);
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
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(FileSystemTapeDevice.MIN_FILE_SIZE);
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
        FileSystemTapeDevice.createVolume(fileName, FileSystemTapeDevice.MIN_FILE_SIZE);

        TestDevice d = new TestDevice();
        d.mount(fileName);
        assertTrue(d.setReady(false));
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void setReady_false_successful(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemTapeDevice.createVolume(fileName, FileSystemTapeDevice.MIN_FILE_SIZE);

        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);
        assertTrue(d.setReady(false));
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void setReady_false_successful_noReel(
    ) {
        FileSystemTapeDevice d = new FileSystemTapeDevice("TAPE0");
        assertTrue(d.setReady(false));
    }

    @Test
    public void setReady_true_successful(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemTapeDevice.createVolume(fileName, FileSystemTapeDevice.MIN_FILE_SIZE);

        TestDevice d = new TestDevice();
        d.mount(fileName);
        assertTrue(d.setReady(true));
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void setReady_true_successful_alreadyTrue(
    ) throws Exception {
        String fileName = getTestFileName();
        FileSystemTapeDevice.createVolume(fileName, FileSystemTapeDevice.MIN_FILE_SIZE);

        TestDevice d = new TestDevice();
        d.mount(fileName);
        d.setReady(true);
        assertTrue(d.setReady(true));
        d.unmount();
        deleteTestFile(fileName);
    }

    @Test
    public void setReady_true_failed_noReel(
    ) {
        FileSystemTapeDevice d = new FileSystemTapeDevice("TAPE0");
        assertFalse(d.setReady(true));
    }

    @Test
    public void unmount_successful(
    ) throws IOException {
        String fileName = getTestFileName();
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        byte[] buffer = new byte[128];
        TestDevice.ScratchPad sp = new TestDevice.ScratchPad(FileSystemTapeDevice.MIN_FILE_SIZE);
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
