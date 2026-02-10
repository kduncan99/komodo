/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.uts;

import com.bearsnake.komodo.kutelib.Emphasis;
import com.bearsnake.komodo.kutelib.exceptions.*;

import static com.bearsnake.komodo.kutelib.Constants.ASCII_ESC;

/**
 * For Create/Replace Emphasis function
 *  ESC 0x2x
 */
public class UTSCreateEmphasisPrimitive extends UTSPrimitive {

    private final Emphasis _emphasis;

    public UTSCreateEmphasisPrimitive(final Emphasis emphasis) {
        super(UTSPrimitiveType.CREATE_REPLACE_EMPHASIS);
        _emphasis = emphasis;
    }

    public UTSCreateEmphasisPrimitive(final boolean columnSeparator,
                                      final boolean strikeThrough,
                                      final boolean underscore) {
        super(UTSPrimitiveType.CREATE_REPLACE_EMPHASIS);
        _emphasis = new Emphasis(columnSeparator, strikeThrough, underscore);
    }

    public boolean allFlagsClear() { return _emphasis.allFlagsClear(); }
    public boolean getColumnSeparator() { return _emphasis.isColumnSeparator(); }
    public boolean getStrikeThrough() { return _emphasis.isStrikeThrough(); }
    public boolean getUnderscore() { return _emphasis.isUnderscore(); }

    public static UTSCreateEmphasisPrimitive deserializePrimitive(final UTSByteBuffer source) {
        var pointer = source.getPointer();
        try {
            if (source.atEnd() || (source.getNext() != ASCII_ESC)) {
                source.setPointer(pointer);
                return null;
            }

            var ch = source.getNext();
            if ((ch < 0x20) || (ch > 0x2F)) {
                source.setPointer(pointer);
                return null;
            }

            return new UTSCreateEmphasisPrimitive(new Emphasis(ch));
        } catch (BufferOverflowException ex) {
            source.setPointer(pointer);
            return null;
        }
    }

    @Override
    public void serialize(final UTSByteBuffer destination) throws CoordinateException {
        destination.put(ASCII_ESC).put(_emphasis.getCode());
    }

    @Override
    public String toString() {
        return getType().getToken() + _emphasis.toString();
    }
}
