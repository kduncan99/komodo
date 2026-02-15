/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib;

import com.bearsnake.komodo.utslib.exceptions.UTSBufferOverflowException;
import com.bearsnake.komodo.utslib.exceptions.UTSCoordinateException;
import com.bearsnake.komodo.utslib.exceptions.UTSFunctionKeyException;
import com.bearsnake.komodo.utslib.primitives.UTSPrimitive;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.bearsnake.komodo.baselib.Constants.*;

/**
 * An extension (of sorts) of the ByteBuffer class.
 * Used for constructing and parsing UTS protocol streams.
 * Note that this buffer cannot contain NUL nor SYN characters.
 * Both of these characters are dropped at character ingest, so that they
 * do not have to be considered during extraction.
 */
public class UTSByteBuffer {

    private byte[] _buffer;
    private int _limit;
    private int _pointer;

    public UTSByteBuffer(final byte[] buffer) {
        _buffer = buffer.clone();
        _limit = _buffer.length;
        _pointer = 0;
        removeNulAndSyn();
    }

    public UTSByteBuffer(final byte[] buffer,
                         final int offset,
                         final int length) {
        _buffer = Arrays.copyOfRange(buffer, offset, offset + length);
        _limit = _buffer.length;
        _pointer = 0;
        removeNulAndSyn();
    }

    public UTSByteBuffer(final int length) {
        _buffer = new byte[length];
        _limit = _buffer.length;
        _pointer = 0;
    }

    /**
     * Removes ASCII_NUL and ASCII_SYN bytes from the buffer, shifting all content downward as necessary.
     * Updates the limit accordingly.
     */
    void removeNulAndSyn() {
        int target = 0;
        for (int source = 0; source < _limit; source++) {
            byte b = _buffer[source];
            if (b != ASCII_NUL && b != ASCII_SYN) {
                _buffer[target++] = b;
            }
        }
        _limit = target;
    }

    /**
     * Indicates whether the pointer has reached the limit.
     */
    public boolean atEnd() {
        return _pointer >= _limit;
    }

    public void checkAtEnd() throws UTSBufferOverflowException {
        if (atEnd()) {
            throw new UTSBufferOverflowException();
        }
    }

    /**
     * Compares the readable portion of the buffer to the given buffer.
     * This consists of the data between the current pointer and the limit.
     * @param buffer the buffer to compare to
     * @return true if the buffers are equal, false otherwise
     */
    public boolean equalsBuffer(final byte[] buffer) {
        if (buffer.length != _limit - _pointer) {
            return false;
        }
        for (int i = 0; i < buffer.length; i++) {
            if (_buffer[_pointer + i] != buffer[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Expands the backing buffer by doubling its size.
     */
    public void expand() {
        var newBuffer = new byte[_buffer.length * 2];
        System.arraycopy(_buffer, 0, newBuffer, 0, _buffer.length);
        _buffer = newBuffer;
    }

    /**
     * Retrieves the byte at the given index, constrained by the limit.
     * @param index the index of the byte to retrieve
     * @return the byte at the given index, or null if the index is out of bounds
     */
    public Byte get(final int index) {
        if (index < 0 || index >= _limit) {
            return null;
        } else {
            return _buffer[index];
        }
    }

    /**
     * Retrieves the backing buffer - not generally useful, but it is available
     * @return the backing buffer
     */
    public byte[] getBackingBuffer() {
        return _buffer;
    }

    /**
     * Retrieves a subset of the backing buffer constrained by the pointer and the limit.
     * @return a subset of the backing buffer
     */
    public byte[] getBuffer() {
        return Arrays.copyOfRange(_buffer, _pointer, _limit);
    }

    /**
     * Reads the next one or two bytes expecting them to represent a coordinate in UTS format.
     * @return screen coordinate ranging from 1 to 256
     * @throws UTSCoordinateException If the coordinate is invalid
     * @throws UTSBufferOverflowException If we run out of bytes to read
     */
    public int getCoordinate() throws UTSCoordinateException, UTSBufferOverflowException {
        var ch = getNext();
        if ((ch >= 0x20) && (ch <= 0x6f)) {
            return (ch - 0x20 + 1);
        } else if (ch >= 0x75) {
            var ch2 = (byte) getNext();
            if (ch2 >= 70) {
                return 81 + ((ch - 0x75) << 4) + (ch2 & 0x0F);
            }
        }

        throw new UTSCoordinateException("Invalid coordinate");
    }

    /**
     * Retrieves the limit of the buffer (that point at which no further get will succeed).
     * The limit does not limit puts, but is set by them (excepting the put with index).
     * @return the limit of the buffer
     */
    public int getLimit() {
        return _limit;
    }

    /**
     * Retrieves the next byte in the buffer, advancing the pointer.
     * @return the next byte in the buffer, or null if at the end of the buffer
     */
    public Byte getNext() throws UTSBufferOverflowException {
        checkAtEnd();
        return _buffer[_pointer++];
    }

    /**
     * Retrieves the current pointer within the buffer.
     * @return the current pointer within the buffer
     */
    public int getPointer() {
        return _pointer;
    }

    /**
     * Returns the number of bytes remaining in the buffer, constrained by the limit.
     */
    public int getRemaining() {
        return _buffer.length - _pointer;
    }

    /**
     * Intended for use while reading - returns the limit value.
     * @return the readable size of the buffer
     */
    public int getSize() {
        return _limit;
    }

    /**
     * Retrieves the byte that would be returned from the next get operation.
     * @return the byte that would be returned from the next get operation, or null if at the end of the buffer
     */
    public Byte peekNext() throws UTSBufferOverflowException {
        checkAtEnd();
        return _buffer[_pointer];
    }

    /**
     * Puts a byte at the current position in the buffer, advancing the pointer.
     * Expands the buffer if necessary; Sets the limit to the new value of the pointer
     * Ignores NUL and SYN bytes.
     * @param b the byte to put
     * @return this buffer
     */
    public UTSByteBuffer put(final byte b) {
        if ((b != ASCII_NUL) && (b != ASCII_SYN)) {
            if (_pointer >= _buffer.length) {
                expand();
            }
            _buffer[_pointer++] = b;
            _limit = _pointer;
        }
        return this;
    }

    /**
     * Puts a byte at the specified index in the buffer. Ignores NUL and SYN bytes.
     * @param index the index at which to put the byte
     * @param b the byte to put
     * @return this buffer
     * @throws UTSBufferOverflowException if the index is out of bounds
     */
    public UTSByteBuffer put(final int index, final byte b) throws UTSBufferOverflowException {
        if (index < 0 || index >= _buffer.length) {
            throw new UTSBufferOverflowException();
        }
        if ((b != ASCII_NUL) && (b != ASCII_SYN)) {
            _buffer[index] = b;
        }
        return this;
    }

    /**
     * Copies the content of a source buffer to this buffer starting at the current pointer.
     * Advances the current pointer accordingly and adjusts the limit as necessary.
     * Ignores NUL and SYN bytes.
     * @param buffer the source buffer
     * @return this buffer
     */
    public UTSByteBuffer put(final byte[] buffer) {
        return put(buffer, 0, buffer.length);
    }

    /**
     * Copies the content of a source buffer, subject to the given offset and length,
     * to this buffer starting at the current pointer.
     * Advances the current pointer accordingly and adjusts the limit as necessary.
     * Ignores NUL and SYN bytes.
     * @param buffer the source buffer
     * @param offset the offset within the source buffer
     * @param length the number of bytes to copy
     * @return this buffer
     */
    public UTSByteBuffer put(final byte[] buffer,
                             final int offset,
                             final int length) {
        var newLength = _pointer + length;
        while (newLength > _buffer.length) {
            expand();
        }
        for (int i = 0; i < length; i++) {
            var ch = buffer[offset + i];
            if ((ch != ASCII_NUL) && (ch != ASCII_SYN)) {
                _buffer[_pointer++] = ch;
            }
        }
        _limit = _pointer;
        return this;
    }

    /**
     * Puts codes corresponding to the provided primitive into the buffer.
     * @param primitive the primitive to put
     * @return this buffer
     */
    public UTSByteBuffer put(final UTSPrimitive primitive) throws UTSCoordinateException {
        primitive.serialize(this);
        return this;
    }

    /**
     * Puts a coordinate into the buffer, in UTS format
     * @param coordinate the coordinate to put, ranging from 1 to 256 inclusive
     * @return this buffer
     */
    public UTSByteBuffer putCoordinate(final int coordinate) throws UTSCoordinateException {
        if (coordinate < 0 || coordinate > 256) {
            throw new UTSCoordinateException(coordinate);
        }
        if (coordinate <= 80) {
            put((byte) (coordinate + 31));
        } else {
            var slop = coordinate - 81;
            put((byte) ((slop >> 4) + 0x75));
            put((byte) (slop & 0x0F));
        }
        return this;
    }

    public UTSByteBuffer putCursorScanLeft() {
        put(ASCII_ESC).put((byte) 'g');
        return this;
    }

    public UTSByteBuffer putCursorScanUp() {
        put(ASCII_ESC).put((byte) 'f');
        return this;
    }

    /**
     * Puts a cursor-to-home sequence into the buffer.
     * @return this buffer
     */
    public UTSByteBuffer putCursorToHome() {
        put(ASCII_ESC).put((byte) 'e');
        return this;
    }

    public UTSByteBuffer putDeleteLine() {
        put(ASCII_ESC).put((byte) 'k');
        return this;
    }

    /**
     * Puts an erase display sequence into the buffer.
     * @return this buffer
     */
    public UTSByteBuffer putEraseDisplay() {
        put(ASCII_ESC).put((byte) 'M');
        return this;
    }

    /**
     * Converts the provided function key ordinal into the byte code representing that function key
     * when it is found in a function key sequence.
     * @param fkey the function key ordinal, from 1 to 22 inclusive
     * @return this buffer
     * @throws UTSFunctionKeyException if the provided ordinal is out of range
     */
    public UTSByteBuffer putFunctionKeyCode(final int fkey) throws UTSFunctionKeyException {
        if ((fkey < 0) || (fkey > 22)) {
            throw new UTSFunctionKeyException(fkey);
        }

        switch (fkey) {
            case 1 -> put((byte) 0x37);
            case 2 -> put((byte) 0x47);
            case 3 -> put((byte) 0x57);
            case 4 -> put((byte) 0x67);
            default -> put((byte) (fkey - 5 + 0x20));
        }

        return this;
    }

    /**
     * Puts a lock-keyboard sequence into the buffer.
     * @return this buffer
     */
    public UTSByteBuffer putLockKeyboard() {
        put(ASCII_ESC).put(ASCII_DC4);
        return this;
    }

    public UTSByteBuffer putSendCursorPosition() {
        put(ASCII_ESC).put((byte) 'T');
        return this;
    }

    /**
     * Writes the indicated number of spaces into the buffer.
     * @param count the number of spaces to write
     * @return this buffer
     */
    public UTSByteBuffer putSpaces(final int count) {
        while (getRemaining() < getPointer() + count) {
            expand();
        }
        for (int i = 0; i < count; i++) {
            _buffer[_pointer++] = ASCII_SP;
        }
        _limit = _pointer;
        return this;
    }

    /**
     * Copies the content of a string to this buffer starting at the current pointer.
     * Advances the current pointer accordingly and adjusts the limit as necessary.
     * @param string the string to copy
     * @return this buffer
     */
    public UTSByteBuffer putString(final String string) {
        return put(string.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Puts an unlock-keyboard sequence into the buffer.
     * @return this buffer
     */
    public UTSByteBuffer putUnlockKeyboard() {
        put(ASCII_ESC).put((byte) 'L');
        return this;
    }

    /**
     * Resets the pointer to zero - does nothing with the limit.
     * Used by recipients prior to reading the buffer after the sender has written to it.
     * @return this buffer
     */
    public UTSByteBuffer reset() {
        _pointer = 0;
        return this;
    }

    /**
     * Sets the limit of the buffer.
     * If the pointer is beyond the limit, it is set to the limit.
     * @param limit the new value which must be between 0 and the current buffer length.
     */
    public void setLimit(final int limit) {
        if (limit >= 0 && limit <= _buffer.length) {
            _limit = limit;
            if (_pointer > _limit) {
                _pointer = _limit;
            }
        }
    }

    /**
     * Sets the pointer to the given position.
     * @param position a value from zero to the limit
     * @return this buffer
     */
    public UTSByteBuffer setPointer(final int position) {
        if (position >= 0 && position <= _limit) {
            _pointer = position;
        }
        return this;
    }

    /**
     * Skips the indicated number of bytes, restricted by the current limit
     * @param count number of bytes to be skipped
     */
    public void skip(final int count) {
        _pointer = Math.max(_pointer + count, _limit);
    }

    /**
     * Skips one byte if we are not at the limit
     */
    public void skipNext() {
        if (!atEnd()) {
            _pointer++;
        }
    }
}
