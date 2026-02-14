/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.primitives;

import static com.bearsnake.komodo.baselib.Constants.*;

public enum UTSPrimitiveType {
    ADD_EMPHASIS(new byte[]{ASCII_ESC, 'Y'}, "AddEmp"),
    BACKWARD_TAB(new byte[]{ASCII_ESC, 'z'}, "BkTab"),
    CLEAR_CHANGED_BITS(new byte[]{ASCII_ESC, 'u'}, "ClrChan"),
    CLEAR_FCC(new byte[]{ASCII_ESC, 'w'}, "ClrFcc"),
    CONTROL_PAGE_ACCESS(new byte[]{ASCII_ESC, 'o'}, "CtlPg"),
    CREATE_REPLACE_EMPHASIS(null, "CreEmp"),
    CURSOR_POSITION(null, "CurPos"),
    CURSOR_TO_HOME(new byte[]{ASCII_ESC, 'e'}, "CrsHome"),
    DELETE_EMPHASIS(new byte[]{ASCII_ESC, 'Z'}, "DelEmp"),
    DELETE_IN_DISPLAY(new byte[]{ASCII_ESC, 'C'}, "DelInDsp"),
    DELETE_IN_LINE(new byte[]{ASCII_ESC, 'c'}, "DelInLin"),
    DELETE_LINE(new byte[]{ASCII_ESC, 'k'}, "DelLine"),
    ERASE_DISPLAY(new byte[]{ASCII_ESC, 'M'}, "ErsDsp"),
    ERASE_TO_END_OF_FIELD(new byte[]{ASCII_ESC, 'K'}, "ErsEof"),
    ERASE_TO_END_OF_LINE(new byte[]{ASCII_ESC, 'b'}, "ErsEol"),
    ERASE_UNPROTECTED_DATA(new byte[]{ASCII_ESC, 'a'}, "ErsUnpr"),
    FCC_SEQUENCE(null, "FccSeq"),
    IMMEDIATE_FCC_SEQUENCE(null, "ImmFcc"),
    INSERT_IN_DISPLAY(new byte[]{ASCII_ESC, 'D'}, "InsInDsp"),
    INSERT_IN_LINE(new byte[]{ASCII_ESC, 'd'}, "InsInLin"),
    INSERT_LINE(new byte[]{ASCII_ESC, 'j'}, "InsLine"),
    LINE_DUPLICATION(new byte[]{ASCII_ESC, 'y'}, "LineDup"),
    LOCK_KEYBOARD(new byte[]{ASCII_ESC, ASCII_DC4}, "LockKb"),
    PRINT_ALL(new byte[]{ASCII_DC2}, "PrtAll"),
    PRINT_FORM(new byte[]{ASCII_ESC, 'H'}, "PrtForm"),
    PRINT_TRANSPARENT(new byte[]{ASCII_ESC, ASCII_DC2}, "PrtTran"),
    PUT_DECIMAL(null, "PutDec"),// ESC { nnn }
    PUT_ESCAPE(new byte[]{ASCII_ESC, '['}, "PutEsc"),
    PUT_HEXADECIMAL(null, "PutHex"), // ESC X xx
    SCAN_DOWN(new byte[]{ASCII_ESC, 'i'}, "ScDown"),
    SCAN_LEFT(new byte[]{ASCII_ESC, 'g'}, "ScLeft"),
    SCAN_RIGHT(new byte[]{ASCII_ESC, 'h'}, "ScRight"),
    SCAN_UP(new byte[]{ASCII_ESC, 'f'}, "ScUp"),
    SEND_CURSOR_ADDRESS(new byte[]{ASCII_ESC, 'T'}, "SendCur"),
    TAB_SET(new byte[]{ASCII_ESC, ASCII_HT}, "TabSet"),
    TRANSFER_ALL_FIELDS(new byte[]{ASCII_ESC, 'G'}, "XferAll"),
    TRANSFER_CHANGED_FIELDS(new byte[]{ASCII_ESC, 'E'}, "XferChan"),
    TRANSFER_VARIABLE_FIELDS(new byte[]{ASCII_ESC, 'F'}, "XferVar"),
    TRANSMIT_ALL_FIELDS(new byte[]{ASCII_ESC, ASCII_DC1}, "XmitAll"),
    TRANSMIT_CHANGED_FIELDS(new byte[]{ASCII_ESC, 't'}, "XmitChan"),
    TRANSMIT_VARIABLE_FIELDS(new byte[]{ASCII_DC1}, "XmitVar"),
    UNLOCK_KEYBOARD(new byte[]{ASCII_ESC, 'L'}, "UnlkKb");

    // A null pattern indicates a primitive type that does not have a completely-fixed byte sequence,
    // and which must be handled specially during parsing and processing.
    private final byte[] _pattern;

    // The token indicates what is displayed in traces and debug text.
    private final String _token;

    UTSPrimitiveType(final byte[] pattern, final String token) {
        _pattern = pattern;
        _token = token;
    }

    public byte[] getPattern() { return _pattern; }
    public String getToken() { return _token; }
}
