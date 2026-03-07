/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.primitives;

import com.bearsnake.komodo.utslib.UTSByteBuffer;
import com.bearsnake.komodo.utslib.exceptions.*;

import static com.bearsnake.komodo.baselib.Constants.*;

/**
 * Base class for primitives of a type which has a pattern.
 * Types with null patterns require special subclasses of this class.
 */
public class Primitive {

    private final PrimitiveType _type;

    public Primitive(final PrimitiveType type) {
        _type = type;
    }

    public PrimitiveType getType() {
        return _type;
    }

    /**
     * A sub-function sliced out of deserializePrimitive() for clarity
     * @param source The buffer to deserialize from.
     * @param emphasisSupported Whether emphasis is supported by the terminal.
     * @param colorSupported Whether color is supported by the terminal.
     * @return The deserialized primitive.
     * @throws UTSInvalidEscapeSequenceException If an invalid escape sequence is encountered.
     * @throws UTSBufferOverflowException If the buffer is overflowed.
     */
    private static Primitive deserializeEscapePrimitive(final UTSByteBuffer source,
                                                        final boolean emphasisSupported,
                                                        final boolean colorSupported)
        throws UTSInvalidEscapeSequenceException, UTSBufferOverflowException {
        switch (source.getNext()) {
            case ASCII_HT: return new Primitive(PrimitiveType.TAB_SET);
            case ASCII_DC1: return new Primitive(PrimitiveType.TRANSMIT_ALL_FIELDS);
            case ASCII_DC2: return new Primitive(PrimitiveType.PRINT_TRANSPARENT);
            case ASCII_DC4: return new Primitive(PrimitiveType.LOCK_KEYBOARD);
            case 'D': return new Primitive(PrimitiveType.INSERT_IN_DISPLAY);
            case 'E': return new Primitive(PrimitiveType.TRANSFER_CHANGED_FIELDS);
            case 'F': return new Primitive(PrimitiveType.TRANSFER_VARIABLE_FIELDS);
            case 'G': return new Primitive(PrimitiveType.TRANSFER_ALL_FIELDS);
            case 'H': return new Primitive(PrimitiveType.PRINT_FORM);
            case 'K': return new Primitive(PrimitiveType.ERASE_TO_END_OF_FIELD);
            case 'L': return new Primitive(PrimitiveType.UNLOCK_KEYBOARD);
            case 'M': return new Primitive(PrimitiveType.ERASE_DISPLAY);
            case 'T': return new Primitive(PrimitiveType.SEND_CURSOR_ADDRESS);
            case 'X': {
                // ESC X xx where xx is a two-byte hex value
                if (source.getRemaining() < 2) {
                    throw new IllegalArgumentException("Not enough bytes for ESC X primitive");
                }

                final int b1 = Character.toUpperCase(source.getNext());
                final int b2 = Character.toUpperCase(source.getNext());
                if ((b1 < 'A') || (b1 > 'F') || (b2 < 'A') || (b2 > 'F')) {
                    throw new IllegalArgumentException("Invalid ESC X value");
                }

                byte value = (byte)(((b1 - 'A') << 4) | (b2 - 'A'));
                return new PutCharacterHexPrimitive(value);
            }
            case 'Y': {
                if (emphasisSupported) {
                    return new Primitive(PrimitiveType.ADD_EMPHASIS);
                }
                break;
            }
            case 'Z': {
                // Check color first, then emphasis. This supports the unlikely situation where both are supported.
                if (colorSupported) {
                    if (source.peekNext() == 'd') {
                        source.skipNext();
                        return new Primitive(PrimitiveType.REPORT_COLOR_FCC_ENABLE);
                    } else if (source.peekNext() == 'e') {
                        source.skipNext();
                        return new Primitive(PrimitiveType.REPORT_COLOR_FCC_DISABLE);
                    }
                }
                if (emphasisSupported) {
                    return new Primitive(PrimitiveType.DELETE_EMPHASIS);
                }
                break;
            }
            case '[': return new Primitive(PrimitiveType.PUT_ESCAPE);
            case '{': {
                // ESC { nnn } where nnn is one to three-byte decimal value
                int digits = 0;
                int value = 0;
                while (source.peekNext() != '}') {
                    int ch = source.getNext();
                    if ((ch < '0') || (ch > '9')) {
                        throw new IllegalArgumentException("Invalid ESC { value");
                    }
                    value = (value * 10) + (ch - '0');
                    digits++;
                }
                source.skipNext();

                if ((value > 127) || (digits < 1) || (digits > 3)) {
                    throw new IllegalArgumentException("Invalid ESC { value");
                }
                return new PutCharacterDecimalPrimitive((byte)value);
            }
            case 'a': return new Primitive(PrimitiveType.ERASE_UNPROTECTED_DATA);
            case 'b': return new Primitive(PrimitiveType.ERASE_TO_END_OF_LINE);
            case 'c': return new Primitive(PrimitiveType.DELETE_IN_LINE);
            case 'd': return new Primitive(PrimitiveType.INSERT_IN_LINE);
            case 'e': return new Primitive(PrimitiveType.CURSOR_TO_HOME);
            case 'f': return new Primitive(PrimitiveType.SCAN_UP);
            case 'g': return new Primitive(PrimitiveType.SCAN_LEFT);
            case 'h': return new Primitive(PrimitiveType.SCAN_RIGHT);
            case 'i': return new Primitive(PrimitiveType.SCAN_DOWN);
            case 'j': return new Primitive(PrimitiveType.INSERT_LINE);
            case 'k': return new Primitive(PrimitiveType.DELETE_LINE);
            case 'u': return new Primitive(PrimitiveType.CLEAR_CHANGED_BITS);
            case 'y': return new Primitive(PrimitiveType.LINE_DUPLICATION);
            case 'z': return new Primitive(PrimitiveType.BACKWARD_TAB);
        }
        throw new UTSInvalidEscapeSequenceException();
    }

    /**
     * Deserializes a primitive from the current pointer in the given buffer.
     * We use this quasi-brute-force method in order to speed things up,
     * as opposed to trying to parse two-byte sequences which mostly all start with ASCII_ESC.
     * Note on support - generally, either color or emphasis is supported, but not both.
     * @param source The buffer to deserialize from.
     * @param emphasisSupported Whether emphasis is supported by the terminal.
     * @param colorSupported Whether color is supported by the terminal.
     * @return The deserialized primitive, or null if the buffer is at its end.
     * @throws UTSCoordinateException If a coordinate-related error occurs during deserialization.
     * @throws UTSIncompleteEscapeSequenceException If an escape sequence is incomplete.
     * @throws UTSIncompleteFCCSequenceException If an FCC sequence is incomplete.
     * @throws UTSInvalidEscapeSequenceException If an invalid escape sequence is encountered.
     * @throws UTSInvalidFCCSequenceException If an invalid FCC sequence is encountered.
     */
    public static Primitive deserializePrimitive(final UTSByteBuffer source,
                                                 final boolean emphasisSupported,
                                                 final boolean colorSupported)
        throws UTSCoordinateException,
               UTSIncompleteEscapeSequenceException,
               UTSIncompleteFCCSequenceException,
               UTSInvalidEscapeSequenceException,
               UTSInvalidFCCSequenceException {
        if (source.atEnd()) {
            return null;
        }

        try {
            Primitive prim = CursorPositionPrimitive.deserializePrimitive(source);
            if (prim == null) {
                prim = FCCSequencePrimitive.deserializePrimitive(source, emphasisSupported, colorSupported);
            }
            if (prim == null) {
                prim = ImmediateFCCSequencePrimitive.deserializePrimitive(source, emphasisSupported, colorSupported);
            }
            if (prim == null && emphasisSupported) {
                prim = CreateEmphasisPrimitive.deserializePrimitive(source);
            }
            if (prim == null) {
                var idx = source.getIndex();
                switch (source.getNext()) {
                    case ASCII_DC1 ->
                        prim = new Primitive(PrimitiveType.TRANSMIT_VARIABLE_FIELDS);
                    case ASCII_DC2 ->
                        prim = new Primitive(PrimitiveType.PRINT_ALL);
                    case ASCII_DC4 ->
                        prim = new Primitive(PrimitiveType.LOCK_KEYBOARD);
                    case ASCII_ESC ->
                        prim = deserializeEscapePrimitive(source, emphasisSupported, colorSupported);
                }

                if (prim == null) {
                    source.setIndex(idx);
                }
            }
            return prim;
        } catch (UTSBufferOverflowException ex) {
            throw new UTSIncompleteEscapeSequenceException();
        }
    }

    /**
     * Helper function for deserializing a one- or two-byte coordinate.
     * @param source the buffer to deserialize from
     * @return the deserialized row or column number, ranging from 1 to 256 inclusive
     * @throws UTSBufferOverflowException if the buffer is overflowed
     * @throws UTSCoordinateException if the coordinate (or the byte[s] is/are invalid)
     */
    protected static int deserializeCoordinate(final UTSByteBuffer source)
        throws UTSBufferOverflowException, UTSCoordinateException {
        var ch1 = source.getNext();
        if ((ch1 >= 0x20) && (ch1 < 0x70)) {
            return ch1 - 31;
        } else if (ch1 >= 0x75) {
            var ch2 = source.getNext();
            if (ch2 < 0x70) {
                throw new UTSCoordinateException("Invalid double-byte coordinate");
            }
            return (ch1 - 0x75) * 16 + (ch2 & 0x0F) + 81;
        }
        throw new UTSCoordinateException("Invalid single-byte coordinate");
    }

    /**
     * Simple serializer for a basic primitive. More complicated primitives (those with variable content)
     * should override this method and serialize themselves.
     * @param destination the buffer to serialize to
     * @throws UTSCoordinateException if an invalid coordinate is deteected during serialization
     */
    public void serialize(final UTSByteBuffer destination) throws UTSCoordinateException {
        destination.put(_type.getPattern());
    }

    @Override
    public String toString() {
        return _type.getToken();
    }
}
