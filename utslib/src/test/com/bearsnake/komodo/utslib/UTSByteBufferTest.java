package com.bearsnake.komodo.utslib;

import org.junit.Test;

import static com.bearsnake.komodo.baselib.Constants.ASCII_SYN;
import static org.junit.Assert.*;

public class UTSByteBufferTest {

    @Test
    public void testConstructorWithSize() {
        UTSByteBuffer buffer = new UTSByteBuffer(10);
        assertNotNull(buffer);
        assertEquals(10, buffer.getBuffer().length);
    }

    @Test
    public void testConstructorWithArray() {
        byte[] buffer = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A};
        UTSByteBuffer byteBuffer = new UTSByteBuffer(buffer);
        assertNotNull(byteBuffer);
        assertNotEquals(buffer, byteBuffer.getBuffer());
        assertArrayEquals(buffer, byteBuffer.getBuffer());
    }

    @Test
    public void testConstructorWithArrayAndOffsetAndLength() {
        byte[] buffer = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A};
        byte[] expected = {0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        UTSByteBuffer byteBuffer = new UTSByteBuffer(buffer, 2, 6);
        assertNotNull(byteBuffer);
        assertArrayEquals(expected, byteBuffer.getBuffer());
    }

    @Test
    public void testPutByte() {
        UTSByteBuffer buffer = new UTSByteBuffer(10);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x02);
        buffer.put((byte) 0x03);
        buffer.setPointer(0);
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03}, buffer.getBuffer());
    }

    @Test
    public void testPutBuffer() {
        UTSByteBuffer buffer = new UTSByteBuffer(30);
        byte[] buffer1 = {0x01, 0x02, 0x03};
        byte[] buffer2 = {0x04, 0x05, 0x06};
        buffer.put(buffer1);
        buffer.put(buffer2);
        buffer.setPointer(0);
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06}, buffer.getBuffer());
    }

    @Test
    public void testPutBufferSubset() {
        UTSByteBuffer buffer = new UTSByteBuffer(30);
        byte[] buffer1 = {0x01, 0x02, 0x03, 0x04};
        byte[] buffer2 = {0x05, 0x06, 0x07, 0x08};
        buffer.put(buffer1, 1, 2);
        buffer.put(buffer2, 2, 1);
        buffer.setPointer(0);
        assertArrayEquals(new byte[]{0x02, 0x03, 0x07}, buffer.getBuffer());
    }

    @Test
    public void testPutString() {
        UTSByteBuffer buffer = new UTSByteBuffer(10);
        buffer.put((byte) 0x30);
        buffer.putString("Hello World");
        buffer.setPointer(0);
        assertArrayEquals("0Hello World".getBytes(), buffer.getBuffer());
    }

    @Test
    public void testEquals() {
        UTSByteBuffer buffer = new UTSByteBuffer(50);
        buffer.put(new byte[]{0x01, 0x02});
        var mainBuffer = new byte[]{0x03, 0x04, 0x05, 0x06, 0x30, 0x32, 0x7F};
        buffer.put(mainBuffer);
        buffer.setPointer(2);
        assertTrue(buffer.equalsBuffer(mainBuffer));
    }

    @Test
    public void testRemoveNulAndSyn_1() {
        UTSByteBuffer buffer = new UTSByteBuffer(new byte[]{});
        byte[] expected = new byte[]{};
        buffer.removeNulAndSyn();
        assertArrayEquals(expected, buffer.getBuffer());
    }

    @Test
    public void testRemoveNulAndSyn_2() {
        UTSByteBuffer buffer = new UTSByteBuffer(new byte[]{0x00, 0x00, 0x40, 0x41, 0x00, 0x42, 0x00});
        byte[] expected = new byte[]{0x40, 0x41, 0x42};
        buffer.removeNulAndSyn();
        assertArrayEquals(expected, buffer.getBuffer());
    }

    @Test
    public void testRemoveNulAndSyn_3() {
        UTSByteBuffer buffer = new UTSByteBuffer(new byte[]{0x40, ASCII_SYN, 0x41, 0x00, 0x42, ASCII_SYN, ASCII_SYN, 0x00, 0x43});
        byte[] expected = new byte[]{0x40, 0x41, 0x42, 0x43};
        buffer.removeNulAndSyn();
        assertArrayEquals(expected, buffer.getBuffer());
    }
}
