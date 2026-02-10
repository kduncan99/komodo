/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.uts;

import com.bearsnake.komodo.kutelib.exceptions.BufferOverflowException;
import com.bearsnake.komodo.kutelib.exceptions.CoordinateException;
import com.bearsnake.komodo.kutelib.exceptions.IncompleteFCCSequenceException;
import com.bearsnake.komodo.kutelib.exceptions.InvalidFCCSequenceException;
import com.bearsnake.komodo.kutelib.FieldAttributes;

import static com.bearsnake.komodo.kutelib.Constants.*;

/**
 * For Immediate FCC sequences
 */
public class UTSImmediateFCCSequencePrimitive extends UTSPrimitive {

    private final FieldAttributes _attributes;
    private boolean _sendExpandedColor;
    private boolean _sendExpanded;

    public UTSImmediateFCCSequencePrimitive(final FieldAttributes attributes) {
        super(UTSPrimitiveType.IMMEDIATE_FCC_SEQUENCE);
        _attributes = attributes;
        _sendExpanded = true;
        _sendExpandedColor = true;
    }

    public FieldAttributes getAttributes() {
        return _attributes.copy();
    }

    /**
     * Sets whether the FCC sequence should be sent in expanded form.
     * This is true by default, but can be overridden (via this method) if basic FCCs are desired.
     * Note that color will not be sent if there is no color to send.
     * Note also that setting this true also sets sendExpanded to true.
     * @param flag true if the FCC sequence should be sent in expanded form with color, false otherwise
     */
    public void setSendExpandedColor(final boolean flag) {
        if (flag) {
            _sendExpanded = true;
        }
        _sendExpandedColor = flag;
    }

    /**
     * Sets whether the FCC sequence should be sent in expanded form.
     * This is true by default, but can be overridden (via this method) if basic FCCs are desired.
     * Note that setting this false also sets sendExpandedColor to false.
     * @param flag true if the FCC sequence should be sent in expanded form, false otherwise
     */
    public void setSendExpanded(final boolean flag) {
        if (!flag) {
            _sendExpandedColor = false;
        }
        _sendExpanded = flag;
    }

    public static UTSImmediateFCCSequencePrimitive deserializePrimitive(final UTSByteBuffer source)
        throws CoordinateException, IncompleteFCCSequenceException, InvalidFCCSequenceException {
        try {
            var pointer = source.getPointer();
            if (source.atEnd() || (source.getNext() != ASCII_EM)) {
                source.setPointer(pointer);
                return null;
            }

            var attributes = FieldAttributes.deserialize(source);
            return new UTSImmediateFCCSequencePrimitive(attributes);
        } catch (BufferOverflowException ex) {
            throw new IncompleteFCCSequenceException();
        }
    }

    @Override
    public void serialize(final UTSByteBuffer destination) throws CoordinateException {
        destination.put(ASCII_EM);
        if (_sendExpanded) {
            _attributes.serializeExpanded(destination, _sendExpandedColor);
        } else {
            _attributes.serializeBasic(destination);
        }
    }

    @Override
    public String toString() {
        return String.format("%s{%s}", getType().getToken(), _attributes.toString());
    }
}
