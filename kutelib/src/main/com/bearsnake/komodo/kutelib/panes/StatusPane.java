/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.panes;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;

/*
 * Ths StatusPane is a text strip below the text display which indicates the current
 * cursor row and column and several states.
 * The geometry, color, and values displayed are set by external entities.
 * There is one of these per Terminal, aligned directly below the Display for the Terminal.
 */
public class StatusPane extends Canvas {

    private FontInfo _fontInfo;
    private int _columns;
    private UTSColor _bgColor;
    private UTSColor _textColor;

    private final Coordinates _cursorPosition;
    private boolean _errorIndicator;
    private boolean _isConnected;
    private boolean _keyboardLocked;
    private boolean _messageWaiting;

    public StatusPane(final DisplayGeometry initialGeometry,
                      final FontInfo initialFontInfo,
                      final UTSColorSet initialColors) {
        _bgColor = initialColors.getBGColor();
        _textColor = initialColors.getFGColor();
        _fontInfo = initialFontInfo;

        reconfigure(initialGeometry);
        _cursorPosition = new Coordinates(1, 1);
    }

    /**
     * Any necessary cleanup.
     */
    public void close() {
        // nothing to do currently
    }

    /*
     * Draw the status line - ROW=XXX COL=XXX  ...  ERR  CONN WAIT MSGW POLL
     * Do not invoke this directly - use scheduleDrawStatus() instead.
     */
    private void drawStatus() {
        var gfContext = getGraphicsContext2D();
        gfContext.setFont(_fontInfo.getFont());

        var jfxBgColor = _bgColor.getFxTextColor();
        var jfxTextColor = _textColor.getFxTextColor();
        var jfxTextDimColor = jfxTextColor.darker()
                                          .darker();

        // set background
        gfContext.setFill(jfxBgColor);
        gfContext.fillRect(0, 0, getWidth(), getHeight());

        // draw separator line
        gfContext.setFill(jfxTextColor);
        gfContext.fillRect(0, 0, getWidth(), 1);

        // construct cursor position and draw it left-justified
        gfContext.setFill(jfxTextColor);
        gfContext.fillText(String.format("ROW=%03d COL=%03d", _cursorPosition.getRow(), _cursorPosition.getColumn()),
                           0,
                           _fontInfo.getCharacterHeight() - 3);

        // draw indicators - we have to do these separately since some may be dimmed
        gfContext.setFill(_errorIndicator ? jfxTextColor : jfxTextDimColor);
        gfContext.fillText("ERR ", (_columns - 19) * _fontInfo.getCharacterWidth(), _fontInfo.getCharacterHeight() - 3);

        gfContext.setFill(_isConnected ? jfxTextColor : jfxTextDimColor);
        gfContext.fillText("CONN", (_columns - 14) * _fontInfo.getCharacterWidth(), _fontInfo.getCharacterHeight() - 3);

        gfContext.setFill(_keyboardLocked ? jfxTextColor : jfxTextDimColor);
        gfContext.fillText("WAIT", (_columns - 9) * _fontInfo.getCharacterWidth(), _fontInfo.getCharacterHeight() - 3);

        gfContext.setFill(_messageWaiting ? jfxTextColor : jfxTextDimColor);
        gfContext.fillText("MSGW", (_columns - 4) * _fontInfo.getCharacterWidth(), _fontInfo.getCharacterHeight() - 3);
    }

    /**
     * @return true if keyboard lock state is set
     */
    public boolean isKeyboardLocked() {
        return _keyboardLocked;
    }

    /**
     * Notifies us that the terminal's color scheme has changed.
     * @param colorSet new color scheme
     */
    public void notifyColorChange(final UTSColorSet colorSet) {
        _textColor = colorSet.getFGColor();
        _bgColor = colorSet.getBGColor();
        scheduleDrawStatus();
    }

    /**
     * Notifies us that the terminal's cursor position has changed
     */
    public void notifyCursorPositionChange(int newRow, int newColumn) {
        setCursorPosition(newRow, newColumn);
    }

    /**
     * Reconfigures our base class to the appropriate size
     */
    public void reconfigure(final DisplayGeometry geometry) {
        _columns = geometry.getColumns();
        setHeight(_fontInfo.getCharacterHeight());
        setWidth(_columns * _fontInfo.getCharacterWidth());
    }

    /*
     * Notifies the platform that it should schedule drawStatusAction() to run in the graphics thread
     */
    private void scheduleDrawStatus() {
        Platform.runLater(this::drawStatus);
    }

    /**
     * Sets the color scheme for the status pane
     * @param colorSet the color set to apply
     */
    public void setColors(final UTSColorSet colorSet) {
        _bgColor = colorSet.getBGColor();
        _textColor = colorSet.getFGColor();
        scheduleDrawStatus();
    }

    /**
     * Sets the connected state
     * @param flag the new connected state
     */
    public void setConnected(final boolean flag) {
        _isConnected = flag;
        scheduleDrawStatus();
    }

    /**
     * Updates the tracked cursor position
     */
    public void setCursorPosition(final int row, final int column) {
        _cursorPosition.setRow(row);
        _cursorPosition.setColumn(column);
        scheduleDrawStatus();
    }

    /**
     * Updates the tracked cursor position
     */
    public void setCursorPosition(final Coordinates coordinates) {
        _cursorPosition.set(coordinates);
        scheduleDrawStatus();
    }

    /**
     * Sets the error state
     * @param flag indicates the error state
     */
    public void setErrorIndicator(final boolean flag) {
        _errorIndicator = flag;
        scheduleDrawStatus();
    }

    /**
     * Sets the font information
     * @param fontInfo the new font information
     */
    public void setFontInfo(final FontInfo fontInfo) {
        // TODO this is a bigger thing than just setting FontInfo
        _fontInfo = fontInfo;
        scheduleDrawStatus();
    }

    /**
     * Sets the keyboard locked state
     * @param flag indicates the keyboard locked state
     */
    public void setKeyboardLocked(final boolean flag) {
        _keyboardLocked = flag;
        scheduleDrawStatus();
    }

    /**
     * Sets the message waiting state
     * @param flag indicates the message waiting state
     */
    public void setMessageWaiting(final boolean flag) {
        _messageWaiting = flag;
        scheduleDrawStatus();
    }
}
