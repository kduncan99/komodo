/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.uts;

import com.bearsnake.komodo.kutelib.exceptions.*;
import com.bearsnake.komodo.kutelib.FieldAttributes;

import static com.bearsnake.komodo.kutelib.Constants.ASCII_US;

/**
 * For FCC sequences containing coordinates
 */
public class UTSFCCSequencePrimitive extends UTSPrimitive {

    private final int _row;
    private final int _column;
    private final FieldAttributes _attributes;
    private boolean _sendExpandedColor;
    private boolean _sendExpanded;

    public UTSFCCSequencePrimitive(final int row,
                                   final int column,
                                   final FieldAttributes attributes) {
        super(UTSPrimitiveType.FCC_SEQUENCE);
        this._row = row;
        this._column = column;
        _attributes = attributes;
        _sendExpandedColor = true;
        _sendExpanded = true;
    }

    public int getRow() { return _row; }
    public int getColumn() { return _column; }
    public FieldAttributes getAttributes() { return _attributes.copy(); }

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

    public static UTSFCCSequencePrimitive deserializePrimitive(final UTSByteBuffer source)
        throws CoordinateException, IncompleteFCCSequenceException, InvalidFCCSequenceException {
        var pointer = source.getPointer();
        try {
            if (source.atEnd() || (source.getNext() != ASCII_US)) {
                source.setPointer(pointer);
                return null;
            }

            var row = source.getCoordinate();
            var column = source.getCoordinate();
            var attributes = FieldAttributes.deserialize(source);
            return new UTSFCCSequencePrimitive(row, column, attributes);
        } catch (BufferOverflowException ex) {
            throw new IncompleteFCCSequenceException();
        }
    }

    @Override
    public void serialize(final UTSByteBuffer destination) throws CoordinateException {
        destination.put(ASCII_US);
        destination.putCoordinate(_row);
        destination.putCoordinate(_column);
        if (_sendExpanded) {
            _attributes.serializeExpanded(destination, _sendExpandedColor);
        } else {
            _attributes.serializeBasic(destination);
        }
    }

    @Override
    public String toString() {
        return String.format("%s{%d,%d:%s}", getType().getToken(), _row, _column, _attributes.toString());
    }
}
