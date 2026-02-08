/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.network;

import com.bearsnake.komodo.kutelib.Constants;
import com.bearsnake.komodo.kutelib.TransmitMode;
import com.bearsnake.komodo.kutelib.exceptions.BufferOverflowException;
import com.bearsnake.komodo.kutelib.exceptions.CoordinateException;
import com.bearsnake.komodo.kutelib.exceptions.FCCSequenceException;
import com.bearsnake.komodo.kutelib.exceptions.FunctionKeyException;
import com.bearsnake.komodo.kutelib.panes.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.bearsnake.komodo.kutelib.Constants.*;

public class UTSByteBuffer {

    private byte[] _buffer;
    private int _limit;
    private int _pointer;

    public UTSByteBuffer(final byte[] buffer) {
        _buffer = buffer.clone();
        _limit = _buffer.length;
        _pointer = 0;
    }

    public UTSByteBuffer(final byte[] buffer,
                         final int offset,
                         final int length) {
        _buffer = Arrays.copyOfRange(buffer, offset, offset + length);
        _limit = _buffer.length;
        _pointer = 0;
    }

    public UTSByteBuffer(final int length) {
        _buffer = new byte[length];
        _limit = _buffer.length;
        _pointer = 0;
    }

    /**
     * Indicates whether the pointer has reached the limit.
     */
    public boolean atEnd() {
        return _pointer >= _limit;
    }

    public void checkAtEnd() throws BufferOverflowException {
        if (atEnd()) {
            throw new BufferOverflowException();
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
     * @throws CoordinateException If the coordinate is invalid
     * @throws BufferOverflowException If we run out of bytes to read
     */
    public int getCoordinate() throws CoordinateException, BufferOverflowException {
        var ch = getNext();
        if ((ch >= 0x20) && (ch <= 0x6f)) {
            return (ch - 0x20 + 1);
        } else if (ch >= 0x75) {
            var ch2 = (byte) getNext();
            if (ch2 >= 70) {
                return 81 + ((ch - 0x75) << 4) + (ch2 & 0x0F);
            }
        }

        throw new CoordinateException("Invalid coordinate");
    }

    /**
     * Reads the next five bytes expecting them to represent a cursor position in UTS format.
     * @return Coordinates if successful, null if we do not encounter an initial ESC VT sequence.
     * @throws BufferOverflowException If we run out of bytes to read
     * @throws CoordinateException If the coordinate sequence is invalid
     */
    public Coordinates getCursorPosition() throws BufferOverflowException, CoordinateException {
        if (getRemaining() < 5) {
            return null;
        }

        var oldPos = _pointer;
        var esc = getNext();
        if (esc != ASCII_ESC) {
            _pointer = oldPos;
            return null;
        }

        var vt = getNext();
        if (vt != ASCII_VT) {
            _pointer = oldPos;
            return null;
        }

        // we are now committed to a good return or an exception
        int row = getCoordinate();
        int column = getCoordinate();
        var ch = getNext();
        // some code does not remove NUL bytes before calling here,
        // and it is not uncommon to get one at this point in a valid ESC VT sequence.
        while (ch == ASCII_NUL) {
            ch = getNext();
        }
        if (ch != ASCII_SI) {
            throw new CoordinateException("Missing SI at end of ESC VT sequence");
        }

        return new Coordinates(row, column);
    }

    /**
     * Reads the field control character sequence from the stream and returns a corresponding Field object.
     * If the stream does not contain a valid FCC sequence, returns null.
     * If this is an immediate FCC sequence, the Coordinate attribute of the Field object will be null.
     * @return Field if successful, null if we do not encounter an initial EM sequence.
     * @throws CoordinateException if something is wrong with the sequence
     * @throws BufferOverflowException if we run out of bytes to read
     * @throws FCCSequenceException if the sequence is invalid
     */
    public Field getField()
        throws CoordinateException, FCCSequenceException, BufferOverflowException {
        // Formats:
        //  EM [ O ] M N
        //  US row col M N
        //  US row col O M N C1 [ C2 ]
        var hasCoordinates = false;
        if (peekNext() == ASCII_US) {
            hasCoordinates = true;
        } else if (peekNext() != ASCII_EM) {
            return null;
        }

        skipNext();
        Coordinates coordinates = null;
        if (hasCoordinates) {
            var row = getCoordinate();
            var column = getCoordinate();
            coordinates = new Coordinates(row, column);
        }

        var field = new ExplicitField(coordinates);
        var ch = peekNext();
        Byte oChar = null;
        if ((ch >= 0x20) && (ch <= 0x2F)) {
            oChar = ch;
            skipNext();
        }

        var mChar = getNext();
        var nChar = getNext();
        if ((mChar >= 0x30) && (mChar <= 0x3f) && (nChar >= 0x30) && (nChar <= 0x3f)) {
            // UTS400 compatible FCC sequence
            switch (mChar & 0x03) {
                case 0x00 -> field.setIntensity(Intensity.NORMAL);
                case 0x01 -> field.setIntensity(Intensity.NONE);
                case 0x02 -> field.setIntensity(Intensity.LOW);
                case 0x03 -> field.setBlinking(true);
            }
            field.setChanged((mChar & 0x04) == 0x00);
            field.setTabStop((mChar & 0x08) == 0x00);
            switch (nChar & 0x03) {
                case 0x00 -> {}
                case 0x01 -> field.setAlphabeticOnly(true);
                case 0x02 -> field.setNumericOnly(true);
                case 0x03 -> field.setProtected(true);
            }
            field.setRightJustified((nChar & 0x04) == 0x04);
        } else if ((mChar >= 0x40) && (nChar >= 0x40)) {
            // Expanded FCC sequence
            if ((mChar & 0x01) == 0x01) { field.setIntensity(Intensity.NONE); }
            if ((mChar & 0x02) == 0x02) { field.setIntensity(Intensity.LOW); }
            field.setChanged((mChar & 0x04) == 0x00);
            field.setTabStop((mChar & 0x08) == 0x00);
            field.setProtectedEmphasis((mChar & 0x20) == 0x20);
            switch (nChar & 0x03) {
                case 0x00 -> {}
                case 0x01 -> field.setAlphabeticOnly(true);
                case 0x02 -> field.setNumericOnly(true);
                case 0x03 -> field.setProtected(true);
            }
            field.setRightJustified((nChar & 0x04) == 0x04);
            field.setBlinking((nChar & 0x08) == 0x08);
            field.setReverseVideo((nChar & 0x10) == 0x10);
        } else {
            throw new FCCSequenceException(mChar, nChar);
        }

        if (oChar != null) {
            if (oChar == 0x20) {
                // next char is 0b01gggttt ggg=background color, ttt=text color
                var c1Char = getNext();
                field.setTextColor(UTSColor.fromByte((byte) (c1Char & 0x07)));
                field.setBackgroundColor(UTSColor.fromByte((byte) ((c1Char >> 3) & 0x07)));
            } else if (oChar == 0x21) {
                // next char is text color in lower 3 bits
                var c1Char = getNext();
                field.setTextColor(UTSColor.fromByte((byte) (c1Char & 0x07)));
            } else if (oChar == 0x22) {
                // next char is bg color in lower 3 bits
                var c1Char = getNext();
                field.setBackgroundColor(UTSColor.fromByte((byte) (c1Char & 0x07)));
            } else if (oChar == 0x23) {
                // next chars are text color in lower 3 bits, then bg color in lower 3 bits
                var c1Char = getNext();
                var c2Char = getNext();
                field.setTextColor(UTSColor.fromByte((byte) (c1Char & 0x07)));
                field.setBackgroundColor(UTSColor.fromByte((byte) (c2Char & 0x07)));
            } else {
                // reserved color code - error for now
                throw new FCCSequenceException("Invalid O byte", ch);
            }
        }

        return field;
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
    public Byte getNext() throws BufferOverflowException {
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

    private UTSPrimitive getEscapePrimitive() {
        if (atEnd()) {
            return null;
        }

        return switch (_buffer[_pointer++]) {
            case 'M' -> UTSPrimitive.ERASE_DISPLAY;
            case '[' -> UTSPrimitive.PUT_ESCAPE;
            case 'e' -> UTSPrimitive.CURSOR_TO_HOME;
            default -> null;
        };
    }

    public UTSPrimitive getPrimitive() {
        if (atEnd()) {
            return null;
        }

        var ptr = _pointer;
        var prim = switch (_buffer[_pointer++]) {
            case ASCII_ESC -> getEscapePrimitive();
            default -> null;
        };

        if (prim == null) {
            _pointer = ptr;
        }
        return prim;
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
    public Byte peekNext() throws BufferOverflowException {
        checkAtEnd();
        return _buffer[_pointer];
    }

    /**
     * Puts a byte at the current position in the buffer, advancing the pointer.
     * Expands the buffer if necessary; Sets the limit to the new value of the pointer
     * @param b the byte to put
     * @return this buffer
     */
    public UTSByteBuffer put(final byte b) {
        if (_pointer >= _buffer.length) {
            expand();
        }
        _buffer[_pointer++] = b;
        _limit = _pointer;
        return this;
    }

    /**
     * Puts a byte at the specified index in the buffer.
     * @param index the index at which to put the byte
     * @param b the byte to put
     * @return this buffer
     * @throws BufferOverflowException if the index is out of bounds
     */
    public UTSByteBuffer put(final int index, final byte b) throws BufferOverflowException {
        if (index < 0 || index >= _buffer.length) {
            throw new BufferOverflowException();
        }
        _buffer[index] = b;
        return this;
    }

    /**
     * Copies the content of a source buffer to this buffer starting at the current pointer.
     * Advances the current pointer accordingly and adjusts the limit as necessary.
     * @param buffer the source buffer
     * @return this buffer
     */
    public UTSByteBuffer putBuffer(final byte[] buffer) {
        return putBuffer(buffer, 0, buffer.length);
    }

    /**
     * Copies the content of a source buffer, subject to the given offset and length,
     * to this buffer starting at the current pointer.
     * Advances the current pointer accordingly and adjusts the limit as necessary.
     * @param buffer the source buffer
     * @param offset the offset within the source buffer
     * @param length the number of bytes to copy
     * @return this buffer
     */
    public UTSByteBuffer putBuffer(final byte[] buffer,
                                   final int offset,
                                   final int length) {
        var newLength = _pointer + length;
        while (newLength > _buffer.length) {
            expand();
        }
        for (int i = 0; i < length; i++) {
            _buffer[_pointer++] = buffer[offset + i];
        }
        _limit = _pointer;
        return this;
    }

    /**
     * Puts a coordinate into the buffer, in UTS format
     * @param coordinate the coordinate to put, ranging from 1 to 256 inclusive
     * @return this buffer
     */
    public UTSByteBuffer putCoordinate(final int coordinate) throws CoordinateException {
        if (coordinate < 0 || coordinate > 256) {
            throw new CoordinateException(coordinate);
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

    /**
     * Puts a UTS cursor positioning sequence into the buffer.
     * @param coordinates the coordinates to put
     * @param includeNulByte includes a NUL byte toward the end of the sequence in the manner in which a terminal would do
     * @return this buffer
     * @throws CoordinateException if the row or column are out of range
     */
    public UTSByteBuffer putCursorPositionSequence(final Coordinates coordinates,
                                                   final boolean includeNulByte) throws CoordinateException {
        return putCursorPositionSequence(coordinates.getRow(), coordinates.getColumn(), includeNulByte);
    }

    /**
     * Puts a UTS cursor positioning sequence into the buffer.
     * @param row row coordinates
     * @param column column coordinates
     * @param includeNulByte includes a NUL byte toward the end of the sequence in the manner in which a terminal would do
     * @return this buffer
     * @throws CoordinateException if the row or column are out of range
     */
    public UTSByteBuffer putCursorPositionSequence(final int row,
                                                   final int column,
                                                   final boolean includeNulByte) throws CoordinateException {
        put(ASCII_ESC)
            .put(ASCII_VT)
            .putCoordinate(row)
            .putCoordinate(column);
        if (includeNulByte) {
            put(ASCII_NUL);
        }
        put(ASCII_SI);
        return this;
    }

    public UTSByteBuffer putCursorScanDown() {
        put(Constants.ASCII_ESC).put((byte) 'i');
        return this;
    }

    public UTSByteBuffer putCursorScanLeft() {
        put(Constants.ASCII_ESC).put((byte) 'g');
        return this;
    }

    public UTSByteBuffer putCursorScanRight() {
        put(Constants.ASCII_ESC).put((byte) 'h');
        return this;
    }

    public UTSByteBuffer putCursorScanUp() {
        put(Constants.ASCII_ESC).put((byte) 'f');
        return this;
    }

    /**
     * Puts a cursor-to-home sequence into the buffer.
     * @return this buffer
     */
    public UTSByteBuffer putCursorToHome() {
        put(Constants.ASCII_ESC).put((byte) 'e');
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
        put(Constants.ASCII_ESC).put((byte) 'M');
        return this;
    }

    /**
     * Puts an FCC sequence into the buffer.
     * @param field describes the field
     * @param immediate true for an immediate sequence, false for a sequence with row and column coordinates
     * @param expandedMode true for expanded FCC sequence, false for UTS400 compatible sequence
     * @param colorMode true for color FCC sequence, false for UTS400 compatible sequence (forces expanded mode)
     * @return this buffer
     * @throws CoordinateException if the field coordinates are invalid
     */
    public UTSByteBuffer putFCCSequence(final Field field,
                                        final boolean immediate,
                                        final boolean expandedMode,
                                        final boolean colorMode) throws CoordinateException {
        if (immediate) {
            put(ASCII_EM);
        } else {
            put(ASCII_US);
            putCoordinate(field.getCoordinates().getRow());
            putCoordinate(field.getCoordinates().getColumn());
        }

        // O byte
        var hasColor = field.getBackgroundColor() != null || field.getTextColor() != null;
        Byte oChar = null;
        if (colorMode && hasColor) {
            if (field.getBackgroundColor() == null) {
                oChar = (byte) 0x21;
            } else if (field.getTextColor() == null) {
                oChar = (byte) 0x22;
            } else {
                oChar = (byte) 0x20;
            }
            put(oChar);
        }

        if (!expandedMode && !colorMode) {
            byte m = 0x30;
            byte n = 0x30;

            if (!field.isTabStop()) m |= 0x08;
            if (field.isChanged()) m |= 0x04;
            if (field.isBlinking()) {
                m |= 0x03;
            } else if (field.getIntensity() != Intensity.NONE) {
                m |= 0x01;
            } else if (field.getIntensity() == Intensity.LOW) {
                m |= 0x02;
            }

            if (field.isRightJustified()) n |= 0x04;
            if (field.isAlphabeticOnly()) {
                n |= 01;
            } else if (field.isNumericOnly()) {
                n |= 02;
            } else if (field.isProtected()) {
                n |= 03;
            }

            put(m);
            put(n);
        } else {
            byte m = 0x40;
            byte n = 0x40;

            if (!field.isTabStop()) m |= 0x08;
            if (field.isChanged()) m |= 0x04;
            if (field.getIntensity() == Intensity.LOW) m |= 0x02;
            if (field.getIntensity() == Intensity.NONE) m |= 0x01;

            if (field.isBlinking()) n |= 0x08;
            if (field.isRightJustified()) n |= 0x04;
            if (field.isAlphabeticOnly()) {
                n |= 01;
            } else if (field.isNumericOnly()) {
                n |= 02;
            } else if (field.isProtected()) {
                n |= 03;
            }

            put(m);
            put(n);

            if (oChar != null) {
                switch (oChar) {
                    case 0x20 -> {
                        byte c1 = 0x40;
                        c1 |= field.getTextColor().getByteValue();
                        c1 |= (byte) (field.getBackgroundColor().getByteValue() << 3);
                        put(c1);
                    }
                    case 0x21 -> put((byte) (0x40 | field.getTextColor().getByteValue()));
                    case 0x22 -> put((byte) (0x80 | field.getBackgroundColor().getByteValue()));
                }
            }
        }

        return this;
    }

    public UTSByteBuffer putForceTransmit(final TransmitMode mode) {
        switch (mode) {
            case ALL -> { put(ASCII_ESC); put(ASCII_DC1); }
            case VARIABLE -> put(ASCII_DC1);
            case CHANGED -> { put(ASCII_ESC); put((byte) 't'); }
        }

        return this;
    }

    public UTSByteBuffer putFunctionKeyCode(final int fkey) throws FunctionKeyException {
        if ((fkey < 0) || (fkey > 22)) {
            throw new FunctionKeyException(fkey);
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
        put(Constants.ASCII_ESC).put(ASCII_DC4);
        return this;
    }

    public UTSByteBuffer putSendCursorPosition(final TransmitMode mode) {
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
        return putBuffer(string.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Puts an unlock-keyboard sequence into the buffer.
     * @return this buffer
     */
    public UTSByteBuffer putUnlockKeyboard() {
        put(Constants.ASCII_ESC).put((byte) 'L');
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
