/*
 * Copyright (c) 2025 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;

/**
 * This is a special overlay to which Terminal directs host and keyboard traffic when it exists.
 */
public class ControlPage extends Canvas {

    // (**PRNT**)STA-dd nnn(**XFER**)PRNT(....)XFER(....)XMIT(....)MM..(PARAM)
    // (../../..)ADR-nnnn  (../../..)SEARCH(.......................)   (../..)

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

    private final Terminal _terminal; // parent terminal

    public ControlPage(Terminal terminal) {
        _terminal = terminal;
        var font = _terminal.getFont();
        var characterWidth = _terminal.getCharacterWidth();
        var characterHeight = _terminal.getCharacterHeight();
        var template = _terminal.getTemplate();

        var w = template.getColumns() * characterWidth;
        var h = 2 * characterHeight;
        setHeight(h);
        setWidth(w);
        draw();
    }

    private void draw() {
        Platform.runLater(this::drawDisplay);
    }

    private void drawDisplay() {
        var gcDisplay = getGraphicsContext2D();
        var template = _terminal.getTemplate();
        UTSColor utsBgColor = template.getBackgroundColor();
        UTSColor utsTextColor = template.getTextColor();

        var jfxBgColor = utsTextColor.getFxTextColor();
        var jfxTextColor = utsBgColor.getFxTextColor();

        gcDisplay.setFill(jfxBgColor);
        gcDisplay.fillRect(0, 0, getWidth(), getHeight());

        gcDisplay.setFill(jfxTextColor);// text color
        gcDisplay.fillText("(**PRNT**)STA-dd nnn(**XFER**)PRNT(....)XFER(....)XMIT(....)MM..(PARAM)",
                           0, _terminal.getCharacterHeight() - 1);
        gcDisplay.fillText("(../../..)ADR-nnnn  (../../..)SEARCH(.......................)   (../..)",
                           0, (2 * _terminal.getCharacterHeight()) - 1);
    }

    // ---------------------------------------------------------------------------------------------
    // API for Terminal to invoke
    // ---------------------------------------------------------------------------------------------

    void close() {
        // TODO
    }

    void cursorReturn() {
        // TODO
    }

    void cursorToHome() {
        // TODO
    }

    void deleteCharacter() {
        // TODO
    }

    void eraseToEndOfField() {
        // TODO
    }

    void eraseUnprotectedData() {
        // TODO
    }

    void insertCharacter() {
        // TODO
    }

    void putCharacter(final byte ch) {
        // TODO
    }

    void putSubCharacter() {
        // TODO
    }

    void scanDown() {
        // TODO
    }

    void scanLeft() {
        // TODO
    }

    void scanRight() {
        // TODO
    }

    void scanUp() {
        // TODO
    }

    void tabBackward() {
        // TODO
    }

    void tabForward() {
        // TODO
    }

    // TODO
}
