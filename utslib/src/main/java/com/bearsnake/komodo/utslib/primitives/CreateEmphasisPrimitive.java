/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.primitives;

import com.bearsnake.komodo.utslib.Emphasis;
import com.bearsnake.komodo.utslib.UTSByteBuffer;
import com.bearsnake.komodo.utslib.exceptions.UTSBufferOverflowException;
import com.bearsnake.komodo.utslib.exceptions.UTSCoordinateException;

import static com.bearsnake.komodo.baselib.Constants.ASCII_ESC;

/**
 * For Create/Replace Emphasis function
 *  ESC 0x2x
 */
public class CreateEmphasisPrimitive extends Primitive {

    private final Emphasis _emphasis;

    public CreateEmphasisPrimitive(final Emphasis emphasis) {
        super(PrimitiveType.CREATE_REPLACE_EMPHASIS);
        _emphasis = emphasis;
    }

    public CreateEmphasisPrimitive(final boolean columnSeparator,
                                   final boolean strikeThrough,
                                   final boolean underscore) {
        super(PrimitiveType.CREATE_REPLACE_EMPHASIS);
        _emphasis = new Emphasis(columnSeparator, strikeThrough, underscore);
    }

    public boolean allFlagsClear() { return _emphasis.allFlagsClear(); }
    public boolean getColumnSeparator() { return _emphasis.isColumnSeparator(); }
    public boolean getStrikeThrough() { return _emphasis.isStrikeThrough(); }
    public boolean getUnderscore() { return _emphasis.isUnderscore(); }

    public static CreateEmphasisPrimitive deserializePrimitive(final UTSByteBuffer source) {
        var pointer = source.getIndex();
        try {
            if (source.atEnd() || (source.getNext() != ASCII_ESC)) {
                source.setIndex(pointer);
                return null;
            }

            var ch = source.getNext();
            if ((ch < 0x20) || (ch > 0x2F)) {
                source.setIndex(pointer);
                return null;
            }

            return new CreateEmphasisPrimitive(new Emphasis(ch));
        } catch (UTSBufferOverflowException ex) {
            source.setIndex(pointer);
            return null;
        }
    }

    @Override
    public void serialize(final UTSByteBuffer destination) throws UTSCoordinateException {
        destination.put(ASCII_ESC).put(_emphasis.getCode());
    }

    @Override
    public String toString() {
        return getType().getToken() + _emphasis.toString();
    }
}
