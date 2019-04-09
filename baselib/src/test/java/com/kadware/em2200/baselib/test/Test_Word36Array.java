/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib.test;

import java.util.LinkedList;
import java.util.List;

import com.kadware.em2200.baselib.LoggerStub;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import static org.junit.Assert.*;
import org.junit.Test;

import com.kadware.em2200.baselib.Word36;
import com.kadware.em2200.baselib.Word36Array;
import java.util.Arrays;

public class Test_Word36Array {

    private static class TestLogger extends LoggerStub {

        public final List<String> _messages = new LinkedList<>();

        @Override
        public void logMessage(
            final String string,
            final Level level,
            final Marker marker,
            final Message msg,
            final Throwable throwable
        ) {
            String str = String.format("%s:%s", level.toString(), msg.getFormattedMessage());
            _messages.add(str);
        }
    }

    @Test
    public void construction(
    ) {
        Word36Array array = new Word36Array(10);
        assertEquals(10, array.getArraySize());
    }

    @Test
    public void access(
    ) {
        Word36Array array = new Word36Array(10);
        array.setWord36(4, new Word36(0_700700_700700l));
        assertEquals(0_700700_700700l, array.getWord36(4).getW());
    }

    @Test
    public void toASCII(
    ) {
        Word36Array array = new Word36Array(3);
        array.setWord36(0, new Word36(0_110_105_114_114l));
        array.setWord36(1, new Word36(0_117_040_127_117l));
        array.setWord36(2, new Word36(0_122_114_104_040l));
        assertEquals("HELLO WORLD ", array.toASCII(false));
    }

    @Test
    public void toASCII_Deliited(
    ) {
        Word36Array array = new Word36Array(3);
        array.setWord36(0, new Word36(0_110_105_114_114l));
        array.setWord36(1, new Word36(0_117_040_127_117l));
        array.setWord36(2, new Word36(0_122_114_104_040l));
        assertEquals("HELL O WO RLD ", array.toASCII(true));
    }

    @Test
    public void toFieldata(
    ) {
        Word36Array array = new Word36Array(2);
        array.setWord36(0, new Word36(0_06_07_10_11_12_13l));
        array.setWord36(1, new Word36(0_14_15_16_17_20_21l));
        assertEquals("ABCDEFGHIJKL", array.toFieldata(false));
    }

    @Test
    public void toFieldata_Delimited(
    ) {
        Word36Array array = new Word36Array(2);
        array.setWord36(0, new Word36(0_06_07_10_11_12_13l));
        array.setWord36(1, new Word36(0_14_15_16_17_20_21l));
        assertEquals("ABCDEF GHIJKL", array.toFieldata(true));
    }

    @Test
    public void toOctal(
    ) {
        Word36Array array = new Word36Array(2);
        array.setWord36(0, new Word36(0_06_07_10_11_12_13l));
        array.setWord36(1, new Word36(0_14_15_16_17_20_21l));
        assertEquals("060710111213141516172021", array.toOctal(false));
    }

    @Test
    public void toOctal_Delimited(
    ) {
        Word36Array array = new Word36Array(2);
        array.setWord36(0, new Word36(0_06_07_10_11_12_13l));
        array.setWord36(1, new Word36(0_14_15_16_17_20_21l));
        assertEquals("060710111213 141516172021", array.toOctal(true));
    }

    @Test
    public void load(
    ) {
        long original[] = new long[4];
        Word36Array array = new Word36Array(original);

        long source[] = { 0_070707_070707l, 0_707070_707070l };
        long comp[] = { 0, 0_070707_070707l, 0_707070_707070l, 0 };

        array.load(1, source);

        assertTrue(Arrays.equals(comp, original));
    }

    @Test
    public void logBuffer(
    ) {
        TestLogger logger = new TestLogger();

        Word36Array array = new Word36Array(9);
        array.setWord36(0, new Word36(0_000_010_020_040l));
        array.setWord36(1, new Word36(0_101_102_103_104l));
        array.setWord36(2, new Word36(0_105_106_107_110l));
        array.setWord36(3, new Word36(0_176_177_200_777l));
        array.setWord36(4, new Word36(0));
        array.setWord36(5, new Word36(0_00_06_30_14_05_13l));
        array.setWord36(6, new Word36(0_16_21_12_75_05_05l));
        array.setWord36(7, new Word36(0));
        array.setWord36(8, new Word36(0));

        String[] comp = {
            "INFO:--[ Testing ]--",
            "INFO:000000:000010020040 101102103104 105106107110 176177200777  @@C]@) CD]CT^ C(AC9C J1_KB_  ...  ABCD EFGH ~...",
            "INFO:000004:000000000000 000630140513 162112750505 000000000000  @@@@@@ @ASG F ILE.   @@@@@@  .... ..`. rJ.. ....",
            "INFO:000010:000000000000                                         @@@@@@                       ....               ",
        };

        array.logBufferMultiFormat(logger, Level.INFO, "Testing");
        String[] real = logger._messages.toArray(new String[0]);

        assertEquals(comp.length, real.length);
        for (int ax = 0; ax < comp.length; ++ax) {
            assertEquals(comp[ax], real[ax]);
        }
    }

    //  pack and unpack ------------------------------------------------------------------------------------------------------------

    @Test
    public void pack_Normal(
    ) {
        long rawSource[] = {
            0_010203_040506l,
            0_222324_252627l,
            0_776655_443322l,
            0_454545_545454l,
        };

        Word36Array source = new Word36Array(rawSource);

        byte comp[] = {
            0, 0, 0, 0,
            (byte)0x04, (byte)0x20, (byte)0xc4, (byte)0x14, (byte)0x64, (byte)0x93, (byte)0x51, (byte)0x55, (byte)0x97,
            (byte)0xff, (byte)0x6b, (byte)0x64, (byte)0x6d, (byte)0x29, (byte)0x65, (byte)0x96, (byte)0xcb, (byte)0x2c,
            0, 0, 0, 0
        };

        byte dest[] = new byte[comp.length];

        assertEquals(4, source.pack(dest, 4));
        assertTrue(Arrays.equals(comp, dest));
    }

    @Test
    public void pack_Truncate_Aligned(
    ) {
        long rawSource[] = {
            0_010203_040506l,
            0_222324_252627l,
            0_776655_443322l,
            0_454545_545454l,
        };

        Word36Array source = new Word36Array(rawSource);

        byte comp[] = {
            (byte)0x04, (byte)0x20, (byte)0xc4, (byte)0x14, (byte)0x64, (byte)0x93, (byte)0x51, (byte)0x55, (byte)0x97,
        };

        byte dest[] = new byte[comp.length];

        assertEquals(2, source.pack(dest));
        assertTrue(Arrays.equals(comp, dest));
    }

    @Test
    public void pack_Truncate_Nonaligned_1(
    ) {
        long rawSource[] = {
            0_010203_040506l,
            0_222324_252627l,
            0_776655_443322l,
            0_454545_545454l,
        };

        Word36Array source = new Word36Array(rawSource);

        byte comp[] = {
            (byte)0x04, (byte)0x20, (byte)0xc4, (byte)0x14, (byte)0x64, (byte)0x93, (byte)0x51, (byte)0x55, (byte)0x97,
            (byte)0xff, (byte)0x6b, (byte)0x64, (byte)0x6d, (byte)0x29, (byte)0x65,
        };

        byte dest[] = new byte[comp.length];

        assertEquals(3, source.pack(dest));
        assertTrue(Arrays.equals(comp, dest));
    }

    @Test
    public void pack_Truncate_Nonaligned_2(
    ) {
        long rawSource[] = {
            0_010203_040506l,
            0_222324_252627l,
            0_776655_443322l,
            0_454545_545454l,
        };

        Word36Array source = new Word36Array(rawSource);

        byte comp[] = {
            (byte)0x04, (byte)0x20, (byte)0xc4, (byte)0x14, (byte)0x64, (byte)0x93, (byte)0x51, (byte)0x55, (byte)0x97,
            (byte)0xff,
        };

        byte dest[] = new byte[comp.length];

        assertEquals(2, source.pack(dest, 0));
        assertTrue(Arrays.equals(comp, dest));
    }

    @Test
    public void pack_Nothing(
    ) {
        long rawSource[] = {
            0_010203_040506l,
            0_222324_252627l,
            0_776655_443322l,
            0_454545_545454l,
        };

        Word36Array source = new Word36Array(rawSource);

        byte comp[] = {
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        };

        byte dest[] = new byte[comp.length];

        assertEquals(0, source.pack(dest, comp.length));
        assertTrue(Arrays.equals(comp, dest));
    }

    @Test
    public void unpack_Normal(
    ) {
        byte source[] = {
            '*', '*', '*',
            'H', 'E', 'L', 'L', 'O', '1', '2', '3', '4',
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
            '*', '*', '*',
        };

        long comp[] = {
            0_220425_142304l,
            0_746114_431464l,
            0_100200_401002l,
            0_004010_020040l,
        };

        Word36Array array = new Word36Array(comp.length);
        assertEquals(18, array.unpack(source, 3, 18));

        long result[] = new long[array.getArraySize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.getValue(rx);
        }

        assertTrue(Arrays.equals(comp, result));
    }

    @Test
    public void unpack_Truncated(
    ) {
        byte source[] = {
            '*', '*', '*',
            'H', 'E', 'L', 'L', 'O', '1', '2', '3', '4',
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
            '*', '*', '*',
        };

        long comp[] = {
            0_220425_142304l,
            0_746114_431464l,
            0_100200_401002l
        };

        Word36Array array = new Word36Array(comp.length);
        assertEquals(13, array.unpack(source, 3, 18));

        long result[] = new long[array.getArraySize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.getValue(rx);
        }

        assertTrue(Arrays.equals(comp, result));
    }

    @Test
    public void unpack_Misaligned_Into_0s_1(
    ) {
        byte source[] = { 'H' };

        long comp[] = {
            0_220000_000000l,
            0,
            0,
            0,
        };

        Word36Array array = new Word36Array(comp.length);
        assertEquals(1, array.unpack(source, 0, 5));

        long result[] = new long[array.getArraySize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.getValue(rx);
        }

        assertTrue(Arrays.equals(comp, result));
    }

    @Test
    public void unpack_Misaligned_Into_0s_2(
    ) {
        byte source[] = { 'H', 'E', 'L', 'L', 'O' };

        long comp[] = {
            0_220425_142304l,
            0_740000_000000l,
            0,
            0,
        };

        Word36Array array = new Word36Array(comp.length);
        assertEquals(5, array.unpack(source, 0, 5));

        long result[] = new long[array.getArraySize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.getValue(rx);
        }

        assertTrue(Arrays.equals(comp, result));
    }

    @Test
    public void unpack_Misaligned_Into_7s_1(
    ) {
        byte source[] = { 'H' };

        long comp[] = { 0_221777_777777l, 0_777777_777777l, 0_777777_777777l, 0_777777_777777l };
        long raw[] = { 0_777777_777777l, 0_777777_777777l, 0_777777_777777l, 0_777777_777777l };
        Word36Array array = new Word36Array(raw);
        assertEquals(1, array.unpack(source, 0, 5));

        long result[] = new long[array.getArraySize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.getValue(rx);
        }

        assertTrue(Arrays.equals(comp, result));
    }

    @Test
    public void unpack_Misaligned_Into_7s_2(
    ) {
        byte source[] = { 'H', 'E', 'L', 'L', 'O', '1', '2' };

        long comp[] = {
            0_220425_142304l,
            0_746114_577777l,
            0_777777_777777l,
            0_777777_777777l
        };

        long raw[] = { 0_777777_777777l, 0_777777_777777l, 0_777777_777777l, 0_777777_777777l };
        Word36Array array = new Word36Array(raw);
        assertEquals(7, array.unpack(source, 0, 7));

        long result[] = new long[array.getArraySize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.getValue(rx);
        }

        assertTrue(Arrays.equals(comp, result));
    }

    @Test
    public void unpack_Nothing_1(
    ) {
        byte source[] = {
            '*', '*', '*',
            'H', 'E', 'L', 'L', 'O', '1', '2', '3', '4',
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
            '*', '*', '*',
        };

        long comp[] = { 0, 0, 0, 0 };

        Word36Array array = new Word36Array(comp.length);
        assertEquals(0, array.unpack(source, 0, 0));

        long result[] = new long[array.getArraySize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.getValue(rx);
        }

        assertTrue(Arrays.equals(comp, result));
    }

    @Test
    public void unpack_Nothing_2(
    ) {
        byte source[] = {
            '*', '*', '*',
            'H', 'E', 'L', 'L', 'O', '1', '2', '3', '4',
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
            '*', '*', '*',
        };

        long comp[] = { 0, 0, 0, 0 };

        Word36Array array = new Word36Array(comp.length);
        assertEquals(0, array.unpack(source, source.length, 1));

        long result[] = new long[array.getArraySize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.getValue(rx);
        }

        assertTrue(Arrays.equals(comp, result));
    }
}
