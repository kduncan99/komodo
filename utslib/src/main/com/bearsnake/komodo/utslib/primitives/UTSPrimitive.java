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
public class UTSPrimitive {

    private final UTSPrimitiveType _type;

    public UTSPrimitive(final UTSPrimitiveType type) {
        _type = type;
    }

    public UTSPrimitiveType getType() {
        return _type;
    }

    /**
     * A sub-function sliced out of deserializePrimitive() for clarity
     * @param source The buffer to deserialize from.
     * @return The deserialized primitive.
     * @throws UTSInvalidEscapeSequenceException If an invalid escape sequence is encountered.
     * @throws UTSBufferOverflowException If the buffer is overflowed.
     */
    private static UTSPrimitive deserializeEscapePrimitive(final UTSByteBuffer source)
        throws UTSInvalidEscapeSequenceException, UTSBufferOverflowException {
        return switch (source.getNext()) {
            case ASCII_HT -> new UTSPrimitive(UTSPrimitiveType.TAB_SET);
            case ASCII_DC1 -> new UTSPrimitive(UTSPrimitiveType.TRANSMIT_ALL_FIELDS);
            case ASCII_DC2 -> new UTSPrimitive(UTSPrimitiveType.PRINT_TRANSPARENT);
            case ASCII_DC4 -> new UTSPrimitive(UTSPrimitiveType.LOCK_KEYBOARD);
            case 'D' -> new UTSPrimitive(UTSPrimitiveType.INSERT_IN_DISPLAY);
            case 'E' -> new UTSPrimitive(UTSPrimitiveType.TRANSFER_CHANGED_FIELDS);
            case 'F' -> new UTSPrimitive(UTSPrimitiveType.TRANSFER_VARIABLE_FIELDS);
            case 'G' -> new UTSPrimitive(UTSPrimitiveType.TRANSFER_ALL_FIELDS);
            case 'H' -> new UTSPrimitive(UTSPrimitiveType.PRINT_FORM);
            case 'K' -> new UTSPrimitive(UTSPrimitiveType.ERASE_TO_END_OF_FIELD);
            case 'L' -> new UTSPrimitive(UTSPrimitiveType.UNLOCK_KEYBOARD);
            case 'M' -> new UTSPrimitive(UTSPrimitiveType.ERASE_DISPLAY);
            case 'Y' -> new UTSPrimitive(UTSPrimitiveType.ADD_EMPHASIS);
            case 'Z' -> new UTSPrimitive(UTSPrimitiveType.DELETE_EMPHASIS);
            case 'T' -> new UTSPrimitive(UTSPrimitiveType.SEND_CURSOR_ADDRESS);
            case '[' -> new UTSPrimitive(UTSPrimitiveType.PUT_ESCAPE);
            case 'a' -> new UTSPrimitive(UTSPrimitiveType.ERASE_UNPROTECTED_DATA);
            case 'b' -> new UTSPrimitive(UTSPrimitiveType.ERASE_TO_END_OF_LINE);
            case 'c' -> new UTSPrimitive(UTSPrimitiveType.DELETE_IN_LINE);
            case 'd' -> new UTSPrimitive(UTSPrimitiveType.INSERT_IN_LINE);
            case 'e' -> new UTSPrimitive(UTSPrimitiveType.CURSOR_TO_HOME);
            case 'f' -> new UTSPrimitive(UTSPrimitiveType.SCAN_UP);
            case 'g' -> new UTSPrimitive(UTSPrimitiveType.SCAN_LEFT);
            case 'h' -> new UTSPrimitive(UTSPrimitiveType.SCAN_RIGHT);
            case 'i' -> new UTSPrimitive(UTSPrimitiveType.SCAN_DOWN);
            case 'k' -> new UTSPrimitive(UTSPrimitiveType.DELETE_LINE);
            case 'u' -> new UTSPrimitive(UTSPrimitiveType.CLEAR_CHANGED_BITS);
            case 'y' -> new UTSPrimitive(UTSPrimitiveType.LINE_DUPLICATION);
            case 'z' -> new UTSPrimitive(UTSPrimitiveType.BACKWARD_TAB);
            default -> throw new UTSInvalidEscapeSequenceException();
        };
    }

    /**
     * Deserializes a primitive from the current pointer in the given buffer.
     * We use this quasi-brute-force method in order to speed things up,
     * as opposed to trying to parse two-byte sequences which mostly all start with ASCII_ESC.
     * @param source The buffer to deserialize from.
     * @return The deserialized primitive, or null if the buffer is at its end.
     * @throws UTSCoordinateException If a coordinate-related error occurs during deserialization.
     * @throws UTSIncompleteEscapeSequenceException If an escape sequence is incomplete.
     * @throws UTSIncompleteFCCSequenceException If an FCC sequence is incomplete.
     * @throws UTSInvalidEscapeSequenceException If an invalid escape sequence is encountered.
     * @throws UTSInvalidFCCSequenceException If an invalid FCC sequence is encountered.
     */
    public static UTSPrimitive deserializePrimitive(final UTSByteBuffer source)
        throws UTSCoordinateException,
               UTSIncompleteEscapeSequenceException,
               UTSIncompleteFCCSequenceException,
               UTSInvalidEscapeSequenceException,
               UTSInvalidFCCSequenceException {
        if (source.atEnd()) {
            return null;
        }

        try {
            UTSPrimitive prim = UTSCursorPositionPrimitive.deserializePrimitive(source);
            if (prim == null) {
                prim = UTSFCCSequencePrimitive.deserializePrimitive(source);
            }
            if (prim == null) {
                prim = UTSImmediateFCCSequencePrimitive.deserializePrimitive(source);
            }
            if (prim == null) {
                prim = UTSCreateEmphasisPrimitive.deserializePrimitive(source);
            }
            if (prim == null) {
                var ptr = source.getPointer();
                prim = switch (source.getNext()) {
                    case ASCII_DC1 -> new UTSPrimitive(UTSPrimitiveType.TRANSMIT_VARIABLE_FIELDS);
                    case ASCII_DC2 -> new UTSPrimitive(UTSPrimitiveType.PRINT_ALL);
                    case ASCII_DC4 -> new UTSPrimitive(UTSPrimitiveType.LOCK_KEYBOARD);
                    case ASCII_ESC -> deserializeEscapePrimitive(source);
                    default -> null;
                };

                if (prim == null) {
                    source.setPointer(ptr);
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
