/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import com.kadware.komodo.baselib.exceptions.InvalidArgumentRuntimeException;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class Test_ArraySlice {

    private static class TestLogger extends LoggerStub {

        final List<String> _messages = new LinkedList<>();

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
    public void testConstructor1() {
        long[] base = new long[8];
        for (int bx = 0; bx < 8; ++bx) {
            base[bx] = bx;
        }

        ArraySlice slice = new ArraySlice(base);
        for (int sx = 0; sx < 8; ++sx) {
            assertEquals(sx, slice.get(sx));
        }
    }

    @Test
    public void testConstructor2_okay1() {
        long[] base = new long[8];
        for (int bx = 0; bx < 8; ++bx) {
            base[bx] = bx;
        }

        ArraySlice slice = new ArraySlice(base, 2, 4);
        for (int sx = 0; sx < 4; ++sx) {
            assertEquals(sx + 2, slice.get(sx));
        }
    }

    @Test
    public void testConstructor2_okay2() {
        long[] base = new long[8];
        for (int bx = 0; bx < 8; ++bx) {
            base[bx] = bx;
        }

        ArraySlice slice = new ArraySlice(base, 2, 6);
        for (int sx = 0; sx < 6; ++sx) {
            assertEquals(sx + 2, slice.get(sx));
        }
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void testConstructor2_error1() {
        long[] base = new long[8];
        new ArraySlice(base, -2, 4);
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void testConstructor2_error2() {
        long[] base = new long[8];
        new ArraySlice(base, 2, 8);
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void testConstructor2_error3() {
        long[] base = new long[8];
        new ArraySlice(base, 256, 8);
    }

    @Test
    public void testConstructor3() {
        long[] base = new long[8];
        for (int bx = 0; bx < 8; ++bx) {
            base[bx] = bx;
        }

        ArraySlice slice1 = new ArraySlice(base, 2, 4);
        ArraySlice slice2 = new ArraySlice(slice1, 1, 2);
        for (int sx = 0; sx < 2; ++sx) {
            assertEquals(sx + 3, slice2.get(sx));
        }
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void testConstructor3_error1() {
        long[] base = new long[8];
        ArraySlice slice1 = new ArraySlice(base, 2, 4);
        new ArraySlice(slice1, -2, 4);
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void testConstructor3_error2() {
        long[] base = new long[8];
        ArraySlice slice1 = new ArraySlice(base, 2, 4);
        new ArraySlice(slice1, 2, 4);
    }

    @Test(expected = InvalidArgumentRuntimeException.class)
    public void testConstructor3_error3() {
        long[] base = new long[8];
        ArraySlice slice1 = new ArraySlice(base, 2, 4);
        new ArraySlice(slice1, 29999, 4);
    }

    @Test
    public void testCoherency() {
        //  Ensure the base array acted upon by one slice shows updated values in a different slice.
        //  Also tests get() and set()
        long[] base = new long[16];
        ArraySlice slice1 = new ArraySlice(base, 4, 8);
        ArraySlice slice2 = new ArraySlice(base, 2, 10);
        for (int ax = 0; ax < 8; ++ax) {
            slice1.set(ax, ax);
        }
        for (int ax = 2; ax < 10; ++ax) {
            assertEquals(ax - 2, slice2.get(ax));
        }
    }

    @Test
    public void testGetAll() {
        long[] base = { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
        long[] comp = { 30, 40, 50, 60, 70 };

        ArraySlice slice1 = new ArraySlice(base, 1, 8);
        ArraySlice slice2 = new ArraySlice(slice1, 1, 5);
        assertArrayEquals(comp, slice2.getAll());
    }

    @Test
    public void testLoad() {
        long[] base = { 2, 4, 6, 8, 10, 12, 14, 16, 18, 20 };
        long[] comp = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 8, 10, 12, 14, 16,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        ArraySlice slice = new ArraySlice(new long[32]);
        slice.load(base, 2, 6, 10);
        long[] view = slice.getAll();
        assertArrayEquals(comp, view);
    }

    //  minalib logging

    @Test
    public void testLog() {
        TestLogger logger = new TestLogger();

        ArraySlice array = new ArraySlice(new long[9]);
        array.set(0, 0_000_010_020_040L);
        array.set(1, 0_101_102_103_104L);
        array.set(2, 0_105_106_107_110L);
        array.set(3, 0_176_177_200_777L);
        array.set(4, 0);
        array.set(5, 0_00_06_30_14_05_13L);
        array.set(6, 0_16_21_12_75_05_05L);
        array.set(7, 0);
        array.set(8, 0);

        String[] comp = {
            "INFO:--[ Testing ]--",
            "INFO:000000:000010020040 101102103104 105106107110 176177200777  @@C]@) CD]CT^ C(AC9C J1_KB_  ...  ABCD EFGH ~...",
            "INFO:000004:000000000000 000630140513 162112750505 000000000000  @@@@@@ @ASG F ILE.   @@@@@@  .... ..`. rJ.. ....",
            "INFO:000010:000000000000                                         @@@@@@                       ....               ",
            };

        array.logMultiFormat(logger, Level.INFO, "Testing");
        String[] real = logger._messages.toArray(new String[0]);

        assertEquals(comp.length, real.length);
        for (int ax = 0; ax < comp.length; ++ax) {
            assertEquals(comp[ax], real[ax]);
        }
    }

    //  character conversions

    @Test
    public void toASCII() {
        ArraySlice array = new ArraySlice(new long[3]);
        array.set(0, 0_110_105_114_114L);
        array.set(1, 0_117_040_127_117L);
        array.set(2, 0_122_114_104_040L);
        assertEquals("HELLO WORLD ", array.toASCII(false));
    }

    @Test
    public void toASCII_Delimited() {
        ArraySlice array = new ArraySlice(new long[3]);
        array.set(0, 0_110_105_114_114L);
        array.set(1, 0_117_040_127_117L);
        array.set(2, 0_122_114_104_040L);
        assertEquals("HELL O WO RLD ", array.toASCII(true));
    }

    @Test
    public void toFieldata() {
        ArraySlice array = new ArraySlice(new long[2]);
        array.set(0, 0_06_07_10_11_12_13L);
        array.set(1, 0_14_15_16_17_20_21L);
        assertEquals("ABCDEFGHIJKL", array.toFieldata(false));
    }

    @Test
    public void toFieldata_Delimited() {
        ArraySlice array = new ArraySlice(new long[2]);
        array.set(0, 0_06_07_10_11_12_13L);
        array.set(1, 0_14_15_16_17_20_21L);
        assertEquals("ABCDEF GHIJKL", array.toFieldata(true));
    }

    @Test
    public void toOctal() {
        ArraySlice array = new ArraySlice(new long[2]);
        array.set(0, 0_06_07_10_11_12_13L);
        array.set(1, 0_14_15_16_17_20_21L);
        assertEquals("060710111213141516172021", array.toOctal(false));
    }

    @Test
    public void toOctal_Delimited() {
        ArraySlice array = new ArraySlice(new long[2]);
        array.set(0, 0_06_07_10_11_12_13L);
        array.set(1, 0_14_15_16_17_20_21L);
        assertEquals("060710111213 141516172021", array.toOctal(true));
    }

    //  type A pack and unpack -----------------------------------------------------------------------------------------------------

    @Test
    public void translateFromA(
    ) {
        byte[] byteBuffer = {
            (byte)'H', (byte)'e', (byte)'l', (byte)'l', (byte)'o', (byte)' ',
            (byte)'W', (byte)'o', (byte)'r', (byte)'l', (byte)'d', (byte)'!'
        };

        ArraySlice as = new ArraySlice(new long[3]);
        as.unpackQuarterWords(byteBuffer);
        assertEquals("Hello World!", as.toASCII(false));
    }

    @Test
    public void translateFromA_partialWord(
    ) {
        byte[] byteBuffer = {
            (byte)'H', (byte)'e', (byte)'l', (byte)'l', (byte)'o', (byte)' ',
            (byte)'D', (byte)'o', (byte)'r', (byte)'k'
        };

        ArraySlice as = new ArraySlice(new long[3]);
        as.unpackQuarterWords(byteBuffer);
        assertEquals("Hello Dork..", as.toASCII(false));
    }

    //  TODO more type A

    //  type B pack and unpack -----------------------------------------------------------------------------------------------------

    @Test
    public void translateFromB(
    ) {
        byte[] byteBuffer = {
            (byte)015, (byte)012, (byte)021, (byte)021, (byte)024, (byte)05,
            (byte)034, (byte)024, (byte)027, (byte)021, (byte)011, (byte)055
        };

        ArraySlice as = new ArraySlice(new long[2]);
        as.unpackSixthWords(byteBuffer);
        assertEquals("HELLO WORLD!", as.toFieldata(false));
    }

    @Test
    public void translateFromB_partialWord(
    ) {
        byte[] byteBuffer = {
            (byte)015, (byte)012, (byte)021, (byte)021, (byte)024, (byte)05,
            (byte)011, (byte)024, (byte)027, (byte)020
        };

        ArraySlice as = new ArraySlice(new long[2]);
        as.unpackSixthWords(byteBuffer);
        assertEquals("HELLO DORK@@", as.toFieldata(false));
    }

    //  TODO more type B

    //  type C pack and unpack -----------------------------------------------------------------------------------------------------

    @Test
    public void pack_Normal() {
        long[] rawSource = {
            0_010203_040506L,
            0_222324_252627L,
            0_776655_443322L,
            0_454545_545454L,
        };

        ArraySlice source = new ArraySlice(rawSource);

        byte[] comp = {
            0, 0, 0, 0,
            (byte)0x04, (byte)0x20, (byte)0xc4, (byte)0x14, (byte)0x64, (byte)0x93, (byte)0x51, (byte)0x55, (byte)0x97,
            (byte)0xff, (byte)0x6b, (byte)0x64, (byte)0x6d, (byte)0x29, (byte)0x65, (byte)0x96, (byte)0xcb, (byte)0x2c,
            0, 0, 0, 0
        };

        byte[] dest = new byte[comp.length];

        assertEquals(4, source.pack(dest, 4));
        assertArrayEquals(comp, dest);
    }

    @Test
    public void pack_Truncate_Aligned() {
        long[] rawSource = {
            0_010203_040506L,
            0_222324_252627L,
            0_776655_443322L,
            0_454545_545454L,
            };

        ArraySlice source = new ArraySlice(rawSource);

        byte[] comp = {
            (byte)0x04, (byte)0x20, (byte)0xc4, (byte)0x14, (byte)0x64, (byte)0x93, (byte)0x51, (byte)0x55, (byte)0x97,
            };

        byte[] dest = new byte[comp.length];

        assertEquals(2, source.pack(dest));
        assertArrayEquals(comp, dest);
    }

    @Test
    public void pack_Truncate_Nonaligned_1() {
        long[] rawSource = {
            0_010203_040506L,
            0_222324_252627L,
            0_776655_443322L,
            0_454545_545454L,
            };

        ArraySlice source = new ArraySlice(rawSource);

        byte[] comp = {
            (byte)0x04, (byte)0x20, (byte)0xc4, (byte)0x14, (byte)0x64, (byte)0x93, (byte)0x51, (byte)0x55, (byte)0x97,
            (byte)0xff, (byte)0x6b, (byte)0x64, (byte)0x6d, (byte)0x29, (byte)0x65,
            };

        byte[] dest = new byte[comp.length];

        assertEquals(3, source.pack(dest));
        assertArrayEquals(comp, dest);
    }

    @Test
    public void pack_Truncate_Nonaligned_2() {
        long[] rawSource = {
            0_010203_040506L,
            0_222324_252627L,
            0_776655_443322L,
            0_454545_545454L,
            };

        ArraySlice source = new ArraySlice(rawSource);

        byte[] comp = {
            (byte)0x04, (byte)0x20, (byte)0xc4, (byte)0x14, (byte)0x64, (byte)0x93, (byte)0x51, (byte)0x55, (byte)0x97,
            (byte)0xff,
            };

        byte[] dest = new byte[comp.length];

        assertEquals(2, source.pack(dest, 0));
        assertArrayEquals(comp, dest);
    }

    @Test
    public void pack_Nothing() {
        long[] rawSource = {
            0_010203_040506L,
            0_222324_252627L,
            0_776655_443322L,
            0_454545_545454L,
            };

        ArraySlice source = new ArraySlice(rawSource);

        byte[] comp = {
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            };

        byte[] dest = new byte[comp.length];

        assertEquals(0, source.pack(dest, comp.length));
        assertArrayEquals(comp, dest);
    }

    @Test
    public void unpack_Normal() {
        byte[] source = {
            '*', '*', '*',
            'H', 'E', 'L', 'L', 'O', '1', '2', '3', '4',
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
            '*', '*', '*',
            };

        long[] comp = {
            0_220425_142304L,
            0_746114_431464L,
            0_100200_401002L,
            0_004010_020040L,
        };

        ArraySlice array = new ArraySlice(new long[comp.length]);
        assertEquals(18, array.unpack(source, 3, 18));

        long[] result = new long[array.getSize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.get(rx);
        }

        assertArrayEquals(comp, result);
    }

    @Test
    public void unpack_Truncated() {
        byte[] source = {
            '*', '*', '*',
            'H', 'E', 'L', 'L', 'O', '1', '2', '3', '4',
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
            '*', '*', '*',
        };

        long[] comp = {
            0_220425_142304L,
            0_746114_431464L,
            0_100200_401002L,
        };

        ArraySlice array = new ArraySlice(new long[comp.length]);
        assertEquals(13, array.unpack(source, 3, 18));

        long[] result = new long[array.getSize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.get(rx);
        }

        assertArrayEquals(comp, result);
    }

    @Test
    public void unpack_Misaligned_Into_0s_1() {
        byte[] source = { 'H' };

        long[] comp = {
            0_220000_000000L,
            0,
            0,
            0,
            };

        ArraySlice array = new ArraySlice(new long[comp.length]);
        assertEquals(1, array.unpack(source, 0, 5));

        long[] result = new long[array.getSize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.get(rx);
        }

        assertArrayEquals(comp, result);
    }

    @Test
    public void unpack_Misaligned_Into_0s_2() {
        byte[] source = { 'H', 'E', 'L', 'L', 'O' };

        long[] comp = {
            0_220425_142304L,
            0_740000_000000L,
            0,
            0,
            };

        ArraySlice array = new ArraySlice(new long[comp.length]);
        assertEquals(5, array.unpack(source, 0, 5));

        long[] result = new long[array.getSize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.get(rx);
        }

        assertArrayEquals(comp, result);
    }

    @Test
    public void unpack_Misaligned_Into_7s_1() {
        byte[] source = { 'H' };

        long[] comp = { 0_221777_777777L, 0_777777_777777L, 0_777777_777777L, 0_777777_777777L, };
        long[] raw = { 0_777777_777777L, 0_777777_777777L, 0_777777_777777L, 0_777777_777777L, };
        ArraySlice array = new ArraySlice(raw);
        assertEquals(1, array.unpack(source, 0, 5));

        long[] result = new long[array.getSize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.get(rx);
        }

        assertArrayEquals(comp, result);
    }

    @Test
    public void unpack_Misaligned_Into_7s_2() {
        byte[] source = { 'H', 'E', 'L', 'L', 'O', '1', '2' };

        long[] comp = {
            0_220425_142304L,
            0_746114_577777L,
            0_777777_777777L,
            0_777777_777777L
        };

        long[] raw = { 0_777777_777777L, 0_777777_777777L, 0_777777_777777L, 0_777777_777777L, };
        ArraySlice array = new ArraySlice(raw);
        assertEquals(7, array.unpack(source, 0, 7));

        long[] result = new long[array.getSize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.get(rx);
        }

        assertArrayEquals(comp, result);
    }

    @Test
    public void unpack_Nothing_1() {
        byte[] source = {
            '*', '*', '*',
            'H', 'E', 'L', 'L', 'O', '1', '2', '3', '4',
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
            '*', '*', '*',
            };

        long[] comp = { 0, 0, 0, 0 };

        ArraySlice array = new ArraySlice(new long[comp.length]);
        assertEquals(0, array.unpack(source, 0, 0));

        long[] result = new long[array.getSize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.get(rx);
        }

        assertArrayEquals(comp, result);
    }

    @Test
    public void unpack_Nothing_2() {
        byte[] source = {
            '*', '*', '*',
            'H', 'E', 'L', 'L', 'O', '1', '2', '3', '4',
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
            '*', '*', '*',
            };

        long[] comp = { 0, 0, 0, 0 };

        ArraySlice array = new ArraySlice(new long[comp.length]);
        assertEquals(0, array.unpack(source, source.length, 1));

        long[] result = new long[array.getSize()];
        for (int rx = 0; rx < result.length; ++rx) {
            result[rx] = array.get(rx);
        }

        assertArrayEquals(comp, result);
    }
}
