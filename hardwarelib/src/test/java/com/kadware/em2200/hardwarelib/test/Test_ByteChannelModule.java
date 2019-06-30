/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.test;

/**
 * Unit tests for ByteChannelModule class
 */
public class Test_ByteChannelModule {

//    public static class TestModule extends SoftwareByteChannelModule {
//
//        public static class TestTracker extends ByteTracker {
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
//        SoftwareByteChannelModule cm = new SoftwareByteChannelModule("CM1-0");
//        assertTrue(cm.canConnect(new InputOutputProcessor("IOP0", InventoryManager.FIRST_INPUT_OUTPUT_PROCESSOR_UPI)));
//    }
//
//    @Test
//    public void canConnect_failure(
//    ) throws IllegalAccessException,
//             InstantiationException,
//             NoSuchMethodException {
//        SoftwareByteChannelModule cm = new SoftwareByteChannelModule("CM1-0");
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
//
//    @Test
//    public void translateFromA(
//    ) {
//        TestModule cm = new TestModule("CM1-01");
//
//        byte[] byteBuffer = { (byte)'H', (byte)'e', (byte)'l', (byte)'l', (byte)'o', (byte)' ',
//                              (byte)'W', (byte)'o', (byte)'r', (byte)'l', (byte)'d', (byte)'!' };
//
//        Word36Array wordBuffer = new Word36Array(3);
//        TestModule.ChannelProgram program = new TestModule.ChannelProgram(null);
//        program._accessControlWord = new IOAccessControlWord(wordBuffer, 0, IOAccessControlWord.AddressModifier.Increment);
//        program._wordCount = 3;
//
//        TestModule.TestTracker tracker = new TestModule.TestTracker(program);
//        tracker._conversionBuffer = new TestModule.ConversionBuffer(byteBuffer, true);
//
//        assertEquals(byteBuffer.length % 4, cm.translateFromA(tracker, byteBuffer.length));
//        assertEquals("Hello World!", wordBuffer.toASCII(false));
//        assertEquals(byteBuffer.length, program._bytesTransferred);
//        assertEquals(wordBuffer.getArraySize(), program._wordsTransferred);
//    }
//
//    @Test
//    public void translateFromA_partialWord(
//    ) {
//        TestModule cm = new TestModule("CM1-01");
//
//        byte[] byteBuffer = { (byte)'H', (byte)'e', (byte)'l', (byte)'l', (byte)'o', (byte)' ',
//                              (byte)'D', (byte)'o', (byte)'r', (byte)'k' };
//
//        Word36Array wordBuffer = new Word36Array(3);
//        TestModule.ChannelProgram program = new TestModule.ChannelProgram(null);
//        program._accessControlWord = new IOAccessControlWord(wordBuffer, 0, IOAccessControlWord.AddressModifier.Increment);
//        program._wordCount = 3;
//
//        TestModule.TestTracker tracker = new TestModule.TestTracker(program);
//        tracker._conversionBuffer = new TestModule.ConversionBuffer(byteBuffer, true);
//
//        assertEquals(byteBuffer.length % 4, cm.translateFromA(tracker, byteBuffer.length));
//        assertEquals("Hello Dork..", wordBuffer.toASCII(false));
//        assertEquals(byteBuffer.length, program._bytesTransferred);
//        assertEquals(wordBuffer.getArraySize(), program._wordsTransferred);
//    }
//
//    @Test
//    public void translateFromB(
//    ) {
//        TestModule cm = new TestModule("CM1-01");
//
//        byte[] byteBuffer = { (byte)015, (byte)012, (byte)021, (byte)021, (byte)024, (byte)05,
//                              (byte)034, (byte)024, (byte)027, (byte)021, (byte)011, (byte)055 };
//
//        Word36Array wordBuffer = new Word36Array(2);
//        TestModule.ChannelProgram program = new TestModule.ChannelProgram(null);
//        program._accessControlWord = new IOAccessControlWord(wordBuffer, 0, IOAccessControlWord.AddressModifier.Increment);
//        program._wordCount = 2;
//
//        TestModule.TestTracker tracker = new TestModule.TestTracker(program);
//        tracker._conversionBuffer = new TestModule.ConversionBuffer(byteBuffer, true);
//
//        assertEquals(byteBuffer.length % 6, cm.translateFromB(tracker, byteBuffer.length));
//        assertEquals("HELLO WORLD!", wordBuffer.toFieldata(false));
//        assertEquals(byteBuffer.length, program._bytesTransferred);
//        assertEquals(wordBuffer.getArraySize(), program._wordsTransferred);
//    }
//
//    @Test
//    public void translateFromB_partialWord(
//    ) {
//        TestModule cm = new TestModule("CM1-01");
//
//        byte[] byteBuffer = { (byte)015, (byte)012, (byte)021, (byte)021, (byte)024, (byte)05,
//                              (byte)011, (byte)024, (byte)027, (byte)020 };
//
//        Word36Array wordBuffer = new Word36Array(2);
//        TestModule.ChannelProgram program = new TestModule.ChannelProgram(null);
//        program._accessControlWord = new IOAccessControlWord(wordBuffer, 0, IOAccessControlWord.AddressModifier.Increment);
//        program._wordCount = 2;
//
//        TestModule.TestTracker tracker = new TestModule.TestTracker(program);
//        tracker._conversionBuffer = new TestModule.ConversionBuffer(byteBuffer, true);
//
//        assertEquals(byteBuffer.length % 6, cm.translateFromB(tracker, byteBuffer.length));
//        assertEquals("HELLO DORK@@", wordBuffer.toFieldata(false));
//        assertEquals(byteBuffer.length, program._bytesTransferred);
//        assertEquals(wordBuffer.getArraySize(), program._wordsTransferred);
//    }
//
//    @Test
//    public void translateFromC(
//    ) {
//        //  This minalib is a little different - it requires testing the slop factor,
//        //  which means we have to translate more than 1 block.
//        //  Create two blocks at the smallest prepfactor, of random-ish values, then pack it into a byte array.
//        Random rand = new Random(System.currentTimeMillis());
//        PrepFactor prepFactor = new PrepFactor(28);
//        int wordArraySize = 2 * prepFactor.getValue();
//        Word36Array wordArray = new Word36Array(wordArraySize);
//        for (int wx = 0; wx < wordArray.getArraySize(); ++wx) {
//            wordArray.setValue(wx, rand.nextLong());
//        }
//
//        //  We have to pack in two steps, due to byte slop between the blocks
//        Word36ArraySlice block0 = new Word36ArraySlice(wordArray, 0, 28);
//        Word36ArraySlice block1 = new Word36ArraySlice(wordArray, 28, 28);
//
//        int byteArraySize = 256;
//        byte[] byteBuffer = new byte[byteArraySize];
//        block0.pack(byteBuffer);
//        block1.pack(byteBuffer, 128);
//
//        //  Now do the translation minalib
//        TestModule cm = new TestModule("CM1-01");
//
//        Word36Array resultBuffer = new Word36Array(wordArraySize);
//        TestModule.ChannelProgram program = new TestModule.ChannelProgram(null);
//        program._prepFactor = prepFactor;
//        program._wordCount = wordArraySize;
//        program._accessControlWord = new IOAccessControlWord(resultBuffer, 0, IOAccessControlWord.AddressModifier.Increment);
//
//        TestModule.TestTracker tracker = new TestModule.TestTracker(program);
//        tracker._conversionBuffer = new TestModule.ConversionBuffer(byteBuffer, true);
//
//        assertEquals(0, cm.translateFromC(tracker, byteBuffer.length));
//        assertTrue(wordArray.equals(resultBuffer));
//        assertEquals(252, program._bytesTransferred);
//        assertEquals(wordArray.getArraySize(), program._wordsTransferred);
//    }
//
//    @Test
//    public void translateFromC_tape(
//    ) {
//        //  Same as above, but no prepfactor and no slop to simulate tape IO
//        Random rand = new Random(System.currentTimeMillis());
//        PrepFactor prepFactor = new PrepFactor(28);
//        int wordArraySize = 2 * prepFactor.getValue();
//        Word36Array wordArray = new Word36Array(wordArraySize);
//        for (int wx = 0; wx < wordArray.getArraySize(); ++wx) {
//            wordArray.setValue(wx, rand.nextLong());
//        }
//
//        int byteArraySize = 252;
//        byte[] byteBuffer = new byte[byteArraySize];
//        wordArray.pack(byteBuffer);
//
//        //  Now do the translation minalib
//        TestModule cm = new TestModule("CM1-01");
//
//        Word36Array resultBuffer = new Word36Array(wordArraySize);
//        TestModule.ChannelProgram program = new TestModule.ChannelProgram(null);
//        program._accessControlWord = new IOAccessControlWord(resultBuffer, 0, IOAccessControlWord.AddressModifier.Increment);
//        program._wordCount = wordArraySize;
//
//        TestModule.TestTracker tracker = new TestModule.TestTracker(program);
//        tracker._conversionBuffer = new TestModule.ConversionBuffer(byteBuffer, true);
//
//        assertEquals(0, cm.translateFromC(tracker, byteBuffer.length));
//        assertTrue(wordArray.equals(resultBuffer));
//        assertEquals(252, program._bytesTransferred);
//        assertEquals(wordArray.getArraySize(), program._wordsTransferred);
//    }
//
//    @Test
//    public void translateFromC_partialWord_1(
//    ) {
//        TestModule cm = new TestModule("CM1-01");
//
//        byte[] byteBuffer = { (byte)0x20, (byte)0xff, (byte)0xa5, (byte)0x5a };
//
//        Word36Array wordBuffer = new Word36Array(1);
//        TestModule.ChannelProgram program = new TestModule.ChannelProgram(null);
//        program._accessControlWord = new IOAccessControlWord(wordBuffer, 0, IOAccessControlWord.AddressModifier.Increment);
//        program._wordCount = 1;
//
//        TestModule.TestTracker tracker = new TestModule.TestTracker(program);
//        tracker._conversionBuffer = new TestModule.ConversionBuffer(byteBuffer, true);
//
//        assertEquals(4, cm.translateFromC(tracker, byteBuffer.length));
//        assertEquals(0x20ffa55a0l, wordBuffer.getValue(0));
//        assertEquals(4, program._bytesTransferred);
//        assertEquals(1, program._wordsTransferred);
//    }
//
//    @Test
//    public void translateFromC_partialWord_2(
//    ) {
//        TestModule cm = new TestModule("CM1-01");
//
//        byte[] byteBuffer = { (byte)0x20, (byte)0xff, (byte)0xa5, (byte)0x5a, (byte)0x44 };
//
//        Word36Array wordBuffer = new Word36Array(1);
//        TestModule.ChannelProgram program = new TestModule.ChannelProgram(null);
//        program._accessControlWord = new IOAccessControlWord(wordBuffer, 0, IOAccessControlWord.AddressModifier.Increment);
//        program._wordCount = 1;
//
//        TestModule.TestTracker tracker = new TestModule.TestTracker(program);
//        tracker._conversionBuffer = new TestModule.ConversionBuffer(byteBuffer, true);
//
//        assertEquals(5, cm.translateFromC(tracker, byteBuffer.length));
//        assertEquals(0x20ffa55a4l, wordBuffer.getValue(0));
//        assertEquals(5, program._bytesTransferred);
//        assertEquals(1, program._wordsTransferred);
//    }
//
//    @Test
//    public void translateFromC_partialWord_3(
//    ) {
//        TestModule cm = new TestModule("CM1-01");
//
//        long[] comp = { 0x20ffa55a4l, 0xe92120000l };
//
//        byte[] byteBuffer = { (byte)0x20, (byte)0xff, (byte)0xa5, (byte)0x5a, (byte)0x4e, (byte)0x92, (byte)0x12 };
//
//        Word36Array wordBuffer = new Word36Array(2);
//        TestModule.ChannelProgram program = new TestModule.ChannelProgram(null);
//        program._accessControlWord = new IOAccessControlWord(wordBuffer, 0, IOAccessControlWord.AddressModifier.Increment);
//        program._wordCount = 2;
//
//        TestModule.TestTracker tracker = new TestModule.TestTracker(program);
//        tracker._conversionBuffer = new TestModule.ConversionBuffer(byteBuffer, true);
//
//        assertEquals(7, cm.translateFromC(tracker, byteBuffer.length));
//        assertEquals(comp[0], wordBuffer.getValue(0));
//        assertEquals(comp[1], wordBuffer.getValue(1));
//        assertEquals(7, program._bytesTransferred);
//        assertEquals(2, program._wordsTransferred);
//    }
//
//    @Test
//    public void translateToA(
//    ) {
//        TestModule cm = new TestModule("CM1-01");
//
//        Word36 w0 = new Word36();
//        Word36 w1 = new Word36();
//        Word36 w2 = new Word36();
//        w0.stringToWord36ASCII("Help");
//        w1.stringToWord36ASCII(" Me ");
//        w2.stringToWord36ASCII("@ASG");
//
//        long[] sourceLongs = { w0.getW(), w1.getW(), w2.getW() };
//        Word36Array wordBuffer = new Word36Array(sourceLongs);
//
//        TestModule.ChannelProgram program = new TestModule.ChannelProgram(null);
//        program._accessControlWord = new IOAccessControlWord(wordBuffer, 0, IOAccessControlWord.AddressModifier.Increment);
//        program._wordCount = 3;
//
//        TestModule.TestTracker tracker = new TestModule.TestTracker(program);
//        tracker._conversionBuffer = new TestModule.ConversionBuffer(16);
//
//        byte[] comp = {
//            (byte)'H', (byte)'e', (byte)'l', (byte)'p',
//            (byte)' ', (byte)'M', (byte)'e', (byte)' ',
//            (byte)'@', (byte)'A', (byte)'S', (byte)'G',
//            0, 0, 0, 0,
//        };
//
//        cm.translateToA(tracker, false);
//        assertTrue(Arrays.equals(comp, tracker._conversionBuffer._buffer));
//        assertEquals(12, tracker._channelProgram._bytesTransferred);
//        assertEquals(3, tracker._channelProgram._wordsTransferred);
//    }
//
//    @Test
//    public void translateToA_stop(
//    ) {
//        TestModule cm = new TestModule("CM1-01");
//
//        Word36 w0 = new Word36();
//        Word36 w1 = new Word36();
//        Word36 w2 = new Word36();
//        w0.stringToWord36ASCII("Help");
//        w1.stringToWord36ASCII(" Me ");
//        w1.logicalOr(0400000);
//        w2.stringToWord36ASCII("@ASG");
//
//        long[] sourceLongs = { w0.getW(), w1.getW(), w2.getW() };
//        Word36Array wordBuffer = new Word36Array(sourceLongs);
//
//        TestModule.ChannelProgram program = new TestModule.ChannelProgram(null);
//        program._accessControlWord = new IOAccessControlWord(wordBuffer, 0, IOAccessControlWord.AddressModifier.Increment);
//        program._wordCount = 3;
//
//        TestModule.TestTracker tracker = new TestModule.TestTracker(program);
//        tracker._conversionBuffer = new TestModule.ConversionBuffer(12);
//
//        byte[] comp = {
//            (byte)'H', (byte)'e', (byte)'l', (byte)'p',
//            (byte)' ', (byte)'M', 0, 0,
//            0, 0, 0, 0,
//        };
//
//        cm.translateToA(tracker, false);
//        assertTrue(Arrays.equals(comp, tracker._conversionBuffer._buffer));
//        assertEquals(6, tracker._channelProgram._bytesTransferred);
//        assertEquals(2, tracker._channelProgram._wordsTransferred);
//    }
//
//    @Test
//    public void translateToA_ignoreStop(
//    ) {
//        TestModule cm = new TestModule("CM1-01");
//
//        Word36 w0 = new Word36();
//        Word36 w1 = new Word36();
//        Word36 w2 = new Word36();
//        w0.stringToWord36ASCII("Help");
//        w1.stringToWord36ASCII(" Me ");
//        w1.logicalOr(0400000);
//        w2.stringToWord36ASCII("@ASG");
//
//        long[] sourceLongs = { w0.getW(), w1.getW(), w2.getW() };
//        Word36Array wordBuffer = new Word36Array(sourceLongs);
//
//        TestModule.ChannelProgram program = new TestModule.ChannelProgram(null);
//        program._accessControlWord = new IOAccessControlWord(wordBuffer, 0, IOAccessControlWord.AddressModifier.Increment);
//        program._wordCount = 3;
//
//        TestModule.TestTracker tracker = new TestModule.TestTracker(program);
//        tracker._conversionBuffer = new TestModule.ConversionBuffer(12);
//
//        byte[] comp = {
//            (byte)'H', (byte)'e', (byte)'l', (byte)'p',
//            (byte)' ', (byte)'M', (byte)'e', (byte)' ',
//            (byte)'@', (byte)'A', (byte)'S', (byte)'G',
//        };
//
//        cm.translateToA(tracker, true);
//        assertTrue(Arrays.equals(comp, tracker._conversionBuffer._buffer));
//        assertEquals(12, tracker._channelProgram._bytesTransferred);
//        assertEquals(3, tracker._channelProgram._wordsTransferred);
//    }
//
//    @Test
//    public void translateToB(
//    ) {
//        TestModule cm = new TestModule("CM1-01");
//
//        Word36 w0 = new Word36();
//        Word36 w1 = new Word36();
//        w0.stringToWord36Fieldata("@ASG,T");
//        w1.stringToWord36Fieldata(" TEMP.");
//
//        long[] sourceLongs = { w0.getW(), w1.getW() };
//        Word36Array wordBuffer = new Word36Array(sourceLongs);
//
//        TestModule.ChannelProgram program = new TestModule.ChannelProgram(null);
//        program._accessControlWord = new IOAccessControlWord(wordBuffer, 0, IOAccessControlWord.AddressModifier.Increment);
//        program._wordCount = 2;
//
//        TestModule.TestTracker tracker = new TestModule.TestTracker(program);
//        tracker._conversionBuffer = new TestModule.ConversionBuffer(16);
//
//        byte[] comp = {
//            (byte)Word36.FIELDATA_FROM_ASCII['@'],
//            (byte)Word36.FIELDATA_FROM_ASCII['A'],
//            (byte)Word36.FIELDATA_FROM_ASCII['S'],
//            (byte)Word36.FIELDATA_FROM_ASCII['G'],
//            (byte)Word36.FIELDATA_FROM_ASCII[','],
//            (byte)Word36.FIELDATA_FROM_ASCII['T'],
//            (byte)Word36.FIELDATA_FROM_ASCII[' '],
//            (byte)Word36.FIELDATA_FROM_ASCII['T'],
//            (byte)Word36.FIELDATA_FROM_ASCII['E'],
//            (byte)Word36.FIELDATA_FROM_ASCII['M'],
//            (byte)Word36.FIELDATA_FROM_ASCII['P'],
//            (byte)Word36.FIELDATA_FROM_ASCII['.'],
//            0,
//            0,
//            0,
//            0,
//        };
//
//        cm.translateToB(tracker);
//        assertTrue(Arrays.equals(comp, tracker._conversionBuffer._buffer));
//        assertEquals(12, tracker._channelProgram._bytesTransferred);
//        assertEquals(2, tracker._channelProgram._wordsTransferred);
//    }
//
//    @Test
//    public void translateToC(
//    ) {
//        //  Translate an entire track, presuming disk IO at 448 prepfactor.
//        //  First, create a track of Word36 stuff with random values
//        Random rand = new Random(System.currentTimeMillis());
//        Word36Array wordBuffer = new Word36Array(1792);
//        for (int wx = 0; wx < wordBuffer.getArraySize(); ++wx) {
//            wordBuffer.setValue(wx, rand.nextLong());
//        }
//
//        //  Now pack it into a comparison buffer which is our source of the Right Answer
//        byte[] comp = new byte[8192];
//        for (int block = 0; block < 4; ++block) {
//            int wordOffset = block * 448;
//            int byteOffset = block * 2048;
//            Word36ArraySlice subset = new Word36ArraySlice(wordBuffer, wordOffset, 448);
//            subset.pack(comp, byteOffset);
//        }
//
//        //  Create the channel program and tracker we need for testing
//        TestModule.ChannelProgram program = new TestModule.ChannelProgram(null);
//        program._accessControlWord = new IOAccessControlWord(wordBuffer, 0, IOAccessControlWord.AddressModifier.Increment);
//        program._wordCount = 1792;
//
//        TestModule.TestTracker tracker = new TestModule.TestTracker(program);
//        tracker._conversionBuffer = new TestModule.ConversionBuffer(8192);
//        tracker._channelProgram._prepFactor = new PrepFactor(448);
//
//        //  Convert and verify
//        TestModule cm = new TestModule("CM1-01");
//        cm.translateToC(tracker);
//        assertTrue(Arrays.equals(comp, tracker._conversionBuffer._buffer));
//        assertEquals(1792, tracker._channelProgram._wordsTransferred);
//        assertEquals(8192 - 128, tracker._channelProgram._bytesTransferred);
//    }
//
//    @Test
//    public void translateToC_tape(
//    ) {
//        //  Translate an entire track, presuming tape IO with no slop anywhere.
//        //  First, create a track of Word36 stuff with random values
//        Random rand = new Random(System.currentTimeMillis());
//        Word36Array wordBuffer = new Word36Array(1792);
//        for (int wx = 0; wx < wordBuffer.getArraySize(); ++wx) {
//            wordBuffer.setValue(wx, rand.nextLong());
//        }
//
//        //  Now pack it into a comparison buffer which is our source of the Right Answer
//        byte[] comp = new byte[8192 - 128];
//        wordBuffer.pack(comp);
//
//        //  Create the channel program and tracker we need for testing
//        TestModule.ChannelProgram program = new TestModule.ChannelProgram(null);
//        program._accessControlWord = new IOAccessControlWord(wordBuffer, 0, IOAccessControlWord.AddressModifier.Increment);
//        program._wordCount = 1792;
//
//        TestModule.TestTracker tracker = new TestModule.TestTracker(program);
//        tracker._conversionBuffer = new TestModule.ConversionBuffer(8192 - 128);
//
//        //  Convert and verify
//        TestModule cm = new TestModule("CM1-01");
//        cm.translateToC(tracker);
//        assertTrue(Arrays.equals(comp, tracker._conversionBuffer._buffer));
//        assertEquals(1792, tracker._channelProgram._wordsTransferred);
//        assertEquals(8192 - 128, tracker._channelProgram._bytesTransferred);
//    }
}
