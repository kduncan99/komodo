package com.bearsnake.komodo.kutelib.network;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class UTSByteBufferTest {

    @Test
    void testConstructorWithSize() {
        UTSByteBuffer buffer = new UTSByteBuffer(10);
        assertNotNull(buffer);
        assertEquals(10, buffer.getBuffer().length);
    }

    @Test
    void testConstructorWithArray() {
        byte[] buffer = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A};
        UTSByteBuffer byteBuffer = new UTSByteBuffer(buffer);
        assertNotNull(byteBuffer);
        assertNotEquals(buffer, byteBuffer.getBuffer());
        assertArrayEquals(buffer, byteBuffer.getBuffer());
    }

    @Test
    void testConstructorWithArrayAndOffsetAndLength() {
        byte[] buffer = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A};
        byte[] expected = {0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        UTSByteBuffer byteBuffer = new UTSByteBuffer(buffer, 2, 6);
        assertNotNull(byteBuffer);
        assertArrayEquals(expected, byteBuffer.getBuffer());
    }

    @Test
    void testPutByte() {
        UTSByteBuffer buffer = new UTSByteBuffer(10);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x02);
        buffer.put((byte) 0x03);
        buffer.setPointer(0);
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03}, buffer.getBuffer());
    }

    @Test
    void testPutBuffer() {
        UTSByteBuffer buffer = new UTSByteBuffer(30);
        byte[] buffer1 = {0x01, 0x02, 0x03};
        byte[] buffer2 = {0x04, 0x05, 0x06};
        buffer.putBuffer(buffer1);
        buffer.putBuffer(buffer2);
        buffer.setPointer(0);
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06}, buffer.getBuffer());
    }

    @Test
    void testPutBufferSubset() {
        UTSByteBuffer buffer = new UTSByteBuffer(30);
        byte[] buffer1 = {0x01, 0x02, 0x03, 0x04};
        byte[] buffer2 = {0x05, 0x06, 0x07, 0x08};
        buffer.putBuffer(buffer1, 1, 2);
        buffer.putBuffer(buffer2, 2, 1);
        buffer.setPointer(0);
        assertArrayEquals(new byte[]{0x02, 0x03, 0x07}, buffer.getBuffer());
    }

    @Test
    void testPutString() {
        UTSByteBuffer buffer = new UTSByteBuffer(10);
        buffer.put((byte) 0x30);
        buffer.putString("Hello World");
        buffer.setPointer(0);
        assertArrayEquals("0Hello World".getBytes(), buffer.getBuffer());
    }

    @Test
    void testRemoveNulBytes() {
        UTSByteBuffer buffer = new UTSByteBuffer(20);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x02);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x03);
        buffer.put((byte) 0x00);
        buffer.setPointer(2);
        buffer.removeNulBytes(false);
        System.out.println(Arrays.toString(buffer.getBuffer()));
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03}, buffer.getBuffer());
        assertEquals(2, buffer.getPointer());
        assertEquals(5, buffer.getLimit());
    }

    @Test
    void testEquals() {
        UTSByteBuffer buffer = new UTSByteBuffer(50);
        buffer.putBuffer(new byte[]{0x01, 0x02});
        var mainBuffer = new byte[]{0x03, 0x04, 0x05, 0x06, 0x30, 0x32, 0x7F};
        buffer.putBuffer(mainBuffer);
        buffer.setPointer(2);
        assertTrue(buffer.equalsBuffer(mainBuffer));
    }
}
