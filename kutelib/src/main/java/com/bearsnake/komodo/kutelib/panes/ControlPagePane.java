/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.panes;

import com.bearsnake.komodo.kutelib.PrintMode;
import com.bearsnake.komodo.kutelib.TransferMode;
import com.bearsnake.komodo.kutelib.TransmitMode;
import com.bearsnake.komodo.utslib.Coordinates;
import com.bearsnake.komodo.utslib.Emphasis;
import com.bearsnake.komodo.utslib.fields.ExplicitField;
import com.bearsnake.komodo.utslib.fields.Field;
import com.bearsnake.komodo.utslib.fields.FieldAttributes;
import com.bearsnake.komodo.utslib.fields.UTSColor;

import java.nio.charset.StandardCharsets;

/**
 * This is a special overlay to which Terminal directs host and keyboard traffic when it exists.
 */
public class ControlPagePane extends TerminalDisplayPane {

    //TODO check protocol book 10.3 page 10-5 and verify # of tabs to get to the various fields

    // (**PRNT**)STA-dd nnn(**XFER**)PRNT(....)XFER(....)XMIT(....)MM..(PARAM)
    // (../../..)ADR-nnnn  (../../..)SEARCH(.......................)   (../..)
    //
    // **PRNT** from/to/function
    // **XFER** from/to/function
    //  from, to:   "P1","P2",... - printer selection (we implement multiple printer paths)
    //              "D1","D2",... - disk file selection (we implement multiples)
    //  function:   "  " - writes to the destination when print or xfer is pressed
    //
    // STA-dd nnn reports status
    //  Pn 000: Device Ready
    //     001: Device Error
    //     004: Printer Error
    //     010: Mechanical Error
    //     100: Device Not Ready
    //     200: Out Of Paper
    //  Dn 000: Successful
    //     001: Data Error
    //     002: End Of Diskette
    //     010: Disk Address Error
    //
    // ADR-nnnn screen block number (not implemented)
    //
    // PRNT:    "PRNT", "FORM", "XPAR"
    // XFER:    "ALL ", "VAR ", "CHAN"
    // XMIT:    "ALL ", "VAR ", "CHAN"
    // MM:      not implemented
    // SEARCH:  not implemented
    //
    // PARAM:
    //  CL/nn - set columns to nn (minimum is 64, max is 99)
    //  LN/nn - set rows to nn (minimum is 16, max is 99)
    //  SP/NS - non-destructive space, SP/DS - destructive space
    //  TF/EF - enables transmission of expanded mode FCCs
    //  TF/DF - disables transmission of expanded mode FCCs
    //  TF/CF - enables transmission of color FCCs
    //  US/YS - uppercase only, US/NO - upper/lower case

    public record Settings(PrintMode printMode, TransferMode transferMode, TransmitMode transmitMode) {}

    private static final byte[] TOP_TEXT =
        "(**PRNT**)STA-NA 000(**XFER**)PRNT(    )XFER(    )XMIT(    )MM  (PARAM)".getBytes(StandardCharsets.UTF_8);

    private static final byte[] BOTTOM_TEXT =
        "(../../..)ADR-0000  (../../..)SEARCH(                       )   (  /  )".getBytes(StandardCharsets.UTF_8);

    private static final Coordinates HOME_COORDS = Coordinates.HOME_POSITION.copy();
    private static final Coordinates STATUS_COORDS = new Coordinates(1, 15);
    private static final Coordinates STATUS_LIMIT = new Coordinates(1, 21);
    private static final Coordinates PRINT_COORDS = new Coordinates(1, 36);
    private static final Coordinates PRINT_LIMIT = new Coordinates(1, 40);
    private static final Coordinates XFER_COORDS = new Coordinates(1, 46);
    private static final Coordinates XFER_LIMIT = new Coordinates(1, 50);
    private static final Coordinates XMIT_COORDS = new Coordinates(1, 56);
    private static final Coordinates XMIT_LIMIT = new Coordinates(1, 60);
    private static final Coordinates MM_COORDS = new Coordinates(1, 63);
    private static final Coordinates MM_LIMIT = new Coordinates(1, 65);

    private static final Coordinates PRINT_FROM_COORDS = new Coordinates(2, 2);
    private static final Coordinates PRINT_FROM_LIMIT = new Coordinates(2, 4);
    private static final Coordinates PRINT_TO_COORDS = new Coordinates(2, 5);
    private static final Coordinates PRINT_TO_LIMIT = new Coordinates(2, 7);
    private static final Coordinates PRINT_FUNC_COORDS = new Coordinates(2, 8);
    private static final Coordinates PRINT_FUNC_LIMIT = new Coordinates(2, 10);
    private static final Coordinates ADDRESS_COORDS = new Coordinates(2, 15);
    private static final Coordinates ADDRESS_LIMIT = new Coordinates(2, 19);
    private static final Coordinates XFER_FROM_COORDS = new Coordinates(2, 22);
    private static final Coordinates XFER_FROM_LIMIT = new Coordinates(2, 24);
    private static final Coordinates XFER_TO_COORDS = new Coordinates(2, 25);
    private static final Coordinates XFER_TO_LIMIT = new Coordinates(2, 27);
    private static final Coordinates XFER_FUNC_COORDS = new Coordinates(2, 28);
    private static final Coordinates XFER_FUNC_LIMIT = new Coordinates(2, 30);
    private static final Coordinates SEARCH_COORDS = new Coordinates(2, 38);
    private static final Coordinates SEARCH_LIMIT = new Coordinates(2, 61);
    private static final Coordinates PARAM_KEY_COORDS = new Coordinates(2, 66);
    private static final Coordinates PARAM_KEY_LIMIT = new Coordinates(2, 68);
    private static final Coordinates PARAM_VALUE_COORDS = new Coordinates(2, 69);
    private static final Coordinates PARAM_VALUE_LIMIT = new Coordinates(2, 71);

    private static final Coordinates[] NON_DATA_FIELD_COORDINATES = {
        HOME_COORDS,
        STATUS_LIMIT,
        PRINT_LIMIT,
        XFER_LIMIT,
        XMIT_LIMIT,
        MM_LIMIT,
        PRINT_FROM_LIMIT,
        PRINT_TO_LIMIT,
        PRINT_FUNC_LIMIT,
        ADDRESS_LIMIT,
        XFER_FROM_LIMIT,
        XFER_TO_LIMIT,
        XFER_FUNC_LIMIT,
        SEARCH_LIMIT,
        PARAM_KEY_LIMIT,
        PARAM_VALUE_LIMIT
    };

    private static final Coordinates[] PROTECTED_DATA_FIELD_COORDINATES = {
        STATUS_COORDS,
        ADDRESS_COORDS,
    };

    private static final Coordinates[] UNPROTECTED_DATA_FIELD_COORDINATES = {
        PRINT_COORDS,
        XFER_COORDS,
        XMIT_COORDS,
        MM_COORDS,
        PRINT_FROM_COORDS,
        PRINT_TO_COORDS,
        PRINT_FUNC_COORDS,
        XFER_FROM_COORDS,
        XFER_TO_COORDS,
        XFER_FUNC_COORDS,
        SEARCH_COORDS,
        PARAM_KEY_COORDS,
        PARAM_VALUE_COORDS,
    };

    // colors for non-data fields
    private static final UTSColor ND_BG = UTSColor.BLACK;
    private static final UTSColor ND_FG = UTSColor.GREEN;

    // colors for protected data fields
    private static final UTSColor PD_BG = UTSColor.BLACK;
    private static final UTSColor PD_FG = UTSColor.CYAN;

    // colors for unprotected data fields
    private static final UTSColor UD_BG = UTSColor.BLACK;
    private static final UTSColor UD_FG = UTSColor.YELLOW;

    private static final Field[] FIELDS =
        new Field[NON_DATA_FIELD_COORDINATES.length
                  + PROTECTED_DATA_FIELD_COORDINATES.length
                  + UNPROTECTED_DATA_FIELD_COORDINATES.length];
    static {
        int fx = 0;
        for (Coordinates coord : NON_DATA_FIELD_COORDINATES) {
            var attr = new FieldAttributes().setProtected(true).setTextColor(ND_FG).setBackgroundColor(ND_BG);
            FIELDS[fx++] = new ExplicitField(coord, attr);
        }
        for (Coordinates coord : PROTECTED_DATA_FIELD_COORDINATES) {
            var attr = new FieldAttributes().setProtected(true).setTextColor(PD_FG).setBackgroundColor(PD_BG);
            FIELDS[fx++] = new ExplicitField(coord, attr);
        }
        for (Coordinates coord : UNPROTECTED_DATA_FIELD_COORDINATES) {
            var attr = new FieldAttributes().setProtected(false).setTabStop(true).setTextColor(UD_FG).setBackgroundColor(UD_BG);
            FIELDS[fx++] = new ExplicitField(coord, attr);
        }
    }

    private PrintMode _printMode;
    private TransferMode _transferMode;
    private TransmitMode _transmitMode;

    public ControlPagePane(
        final DisplayGeometry geometry,
        final FontInfo fontInfo,
        final UTSColorSet colorSet,
        final StatusPane listener,
        final Settings settings) {
        super(new DisplayGeometry(2, geometry.getColumns()), fontInfo, colorSet, listener);

        // Set fields and text
        var sx = 0;
        var cx = 0;
        while (sx < TOP_TEXT.length) {
            _characterCells[cx++].setCharacter(TOP_TEXT[sx++]);
        }
        sx = 0;
        cx = _geometry.getColumns();
        while (sx < TOP_TEXT.length) {
            _characterCells[cx++].setCharacter(BOTTOM_TEXT[sx++]);
        }

        for (var f : FIELDS) {
            _fields.put(f.getCoordinates(), f);
        }
        repairFieldReferences();

        _printMode = settings.printMode;
        _transferMode = settings.transferMode;
        _transmitMode = settings.transmitMode;
        putPrintMode();
        putTransferMode();
        putTransmitMode();

        _cursorPosition.setRow(PRINT_COORDS.getRow());
        _cursorPosition.setColumn(PRINT_COORDS.getColumn());
        _statusPane.notifyCursorPositionChange(PRINT_COORDS.getRow(), PRINT_COORDS.getColumn());
    }

    // Things we don't allow in the control page
    @Override public boolean clearChangedBits() { return false; }
    @Override public boolean cursorToHome() { return false; }
    @Override public boolean deleteLine() { return false; }
    @Override public boolean duplicateLine() { return false; }
    @Override public boolean eraseDisplay() { return false; }
    @Override public boolean eraseUnprotectedData() { return false; }
    @Override public boolean fccClear() { return false; }
    @Override public boolean fccEnable() { return false; }
    @Override public boolean fccLocate() { return false; }
    @Override public boolean insertLine() { return false; }
    @Override public boolean putFCC(final FieldAttributes attr) { return false; }
    @Override public boolean setCursorPosition(final Coordinates coordinates) { return false; }
    @Override public boolean toggleEmphasis(final Emphasis emphasis) { return false; }

    public synchronized Settings evaluate() {
        var printStr = getString(PRINT_COORDS, 4).trim();
        if (!printStr.isEmpty()) {
            _printMode = PrintMode.getPrintMode(printStr);
            if (_printMode == null) {
                return null;
            }
        }

        var xferStr = getString(XFER_COORDS, 4).trim();
        if (!xferStr.isEmpty()) {
            _transferMode = TransferMode.getTransferMode(xferStr);
            if (_transferMode == null) {
                return null;
            }
        }

        var xmitStr = getString(XMIT_COORDS, 4).trim();
        if (!xmitStr.isEmpty()) {
            _transmitMode = TransmitMode.getTransmitMode(xmitStr);
            if (_transmitMode == null) {
                return null;
            }
        }

        return new Settings(_printMode, _transferMode, _transmitMode);
    }

    /*
     * Retrieves the next field following the given location,
     * or the first unprotected field on the screen if there are none following the location.
     */
    private Field fieldAfter(final Coordinates location) {
        var result = _fields.higherEntry(location);
        return result == null ? _fields.firstEntry().getValue() : result.getValue();
    }

    /*
     * Retrieves the previous field before the given location,
     * or the last unprotected field on the screen if there are none before the location.
     */
    private Field fieldBefore(final Coordinates location) {
        var result = _fields.lowerEntry(location);
        return result == null ? _fields.lastEntry().getValue() : result.getValue();
    }

    private String getString(final Coordinates coordinates,
                             final int length) {
        final int cx = getIndex(coordinates);
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char)_characterCells[cx + i].getCharacter());
        }
        return sb.toString();
    }

    // Things we do slightly differently than the regular display.
    // We allow only alphabetic and numeric characters, ignoring the rest.
    // Also, we do not advance beyond the field if we are in the last position of the field...
    // this allows consistent tab operation for both the host and the keyboard.
    // Also also, we do not worry about emphasis - there is none in the control page.
    @Override
    public synchronized boolean kbPutCharacter(final byte ch) {
        return putCharacter(ch, EmphasisAction.NONE, null);
    }

    @Override
    public synchronized boolean putCharacter(final byte ch,
                                             final EmphasisAction emphasisAction,
                                             final Emphasis emphasis) {
        byte ch2 = ch;
        if (Character.isLowerCase(ch2)) {
            ch2 = (byte)Character.toUpperCase(ch2);
        } else if (!Character.isUpperCase(ch2) && !Character.isDigit(ch2)) {
            return false;
        }

        var ix = getIndex(_cursorPosition);
        _characterCells[ix].setCharacter(ch2);
        if (!coordinatesAtEndOfField(_cursorPosition)) {
            advanceCoordinates(_cursorPosition);
        }
        return true;
    }

    /**
     * Simplified tab backward.
     * We have only protected and unprotected fields, no tab-set characters.
     */
    @Override
    public synchronized boolean tabBackward() {
        // Find the previous unprotected field coordinates
        var field = fieldBefore(_cursorPosition);
        while (field.isProtected()) {
            field = fieldBefore(field.getCoordinates());
        }
        _cursorPosition.set(field.getCoordinates());
        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        scheduleDrawDisplay();
        return true;
    }

    /**
     * Simplified tab forward.
     * We have only protected and unprotected fields, no tab-set characters.
     */
    @Override
    public synchronized boolean tabForward() {
        // Find the next unprotected field coordinates
        var field = fieldAfter(_cursorPosition);
        while (field.isProtected()) {
            field = fieldAfter(field.getCoordinates());
        }
        _cursorPosition.set(field.getCoordinates());
        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        scheduleDrawDisplay();
        return true;
    }

    private void putString(final Coordinates coordinates,
                           final String string) {
        int sx = 0;
        int cx = getIndex(coordinates);
        while (sx < string.length()) {
            _characterCells[cx++].setCharacter((byte)string.charAt(sx++));
        }
    }

    private void putPrintMode() {
        putString(PRINT_COORDS, _printMode.toString());
        scheduleDrawDisplay();
    }

    private void putTransferMode() {
        putString(XFER_COORDS, _transferMode.toString());
        scheduleDrawDisplay();
    }

    private void putTransmitMode() {
        putString(XMIT_COORDS, _transmitMode.toString());
        scheduleDrawDisplay();
    }
}
