/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Ths StatusPane is a text strip below the text display which indicates the current
 * cursor row and column, a poll indicator (which is momentary) and several states.
 * The geometry, color, and values displayed are set by external entities.
 * There is one of these per Terminal, aligned directly below the Display for the Terminal.
 */
public class StatusPane
    extends Canvas
    implements CursorPositionListener {

    // amount of time between executions of poll indicator de-illumination check
    private static final int PULSE_POLL_INDICATOR_CYCLE_MSEC = 10;

    // amount of time to leave POLL indicator illuminated after pulse
    private static final int PULSE_POLL_INDICATOR_PERSIST_MSEC = 500;

    private final Timer _timer;

    private FontInfo _fontInfo;
    private int _columns;
    private UTSColor _bgColor;
    private UTSColor _textColor;

    private final Coordinates _cursorPosition;
    private boolean _errorIndicator;
    private boolean _isConnected;
    private boolean _keyboardLocked;
    private boolean _messageWaiting;
    private final AtomicInteger _pollCountDown;
    private boolean _pollIndicator;

    public StatusPane(final DisplayGeometry initialGeometry,
                      final FontInfo initialFontInfo,
                      final UTSColorSet initialColors) {
        _pollCountDown = new AtomicInteger(PULSE_POLL_INDICATOR_CYCLE_MSEC);
        _timer = new Timer(true);
        _timer.schedule(new PollIndicatorTask(), PULSE_POLL_INDICATOR_CYCLE_MSEC, PULSE_POLL_INDICATOR_CYCLE_MSEC);
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

        // construct cursor position and draw it left-justified
        gfContext.setFill(jfxTextColor);
        gfContext.fillText(String.format("ROW=%03d COL=%03d", _cursorPosition.getRow(), _cursorPosition.getColumn()),
                           0,
                           _fontInfo.getCharacterHeight() - 3);

        // draw indicators - we have to do these separately since some may be dimmed
        gfContext.setFill(_errorIndicator ? jfxTextColor : jfxTextDimColor);
        gfContext.fillText("ERR ", (_columns - 24) * _fontInfo.getCharacterWidth(), _fontInfo.getCharacterHeight() - 3);

        gfContext.setFill(_isConnected ? jfxTextColor : jfxTextDimColor);
        gfContext.fillText("CONN", (_columns - 19) * _fontInfo.getCharacterWidth(), _fontInfo.getCharacterHeight() - 3);

        gfContext.setFill(_keyboardLocked ? jfxTextColor : jfxTextDimColor);
        gfContext.fillText("WAIT", (_columns - 14) * _fontInfo.getCharacterWidth(), _fontInfo.getCharacterHeight() - 3);

        gfContext.setFill(_messageWaiting ? jfxTextColor : jfxTextDimColor);
        gfContext.fillText("MSGW", (_columns - 9) * _fontInfo.getCharacterWidth(), _fontInfo.getCharacterHeight() - 3);

        gfContext.setFill(_pollIndicator ? jfxTextColor : jfxTextDimColor);
        gfContext.fillText("POLL", (_columns - 4) * _fontInfo.getCharacterWidth(), _fontInfo.getCharacterHeight() - 3);
    }

    /**
     * @return true if keyboard lock state is set
     */
    public boolean isKeyboardLocked() {
        return _keyboardLocked;
    }

    /**
     * Notifies us that the terminal's cursor position has changed
     */
    @Override
    public void notifyCursorPositionChange(int newRow, int newColumn) {
        setCursorPosition(newRow, newColumn);
    }

    /**
     * Causes the poll indicator to illuminate for a period of time
     */
    public void pulsePollIndicator() {
        _pollCountDown.set(PULSE_POLL_INDICATOR_PERSIST_MSEC / PULSE_POLL_INDICATOR_CYCLE_MSEC);
        _pollIndicator = true;
        scheduleDrawStatus();
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
     * Sets the display geometry
     * @param geometry the new display geometry
     */
    public void setDisplayGeometry(final DisplayGeometry geometry) {
        // TODO this is a bigger thing than just setting FontInfo
        _columns = geometry.getColumns();
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

    public class PollIndicatorTask
        extends TimerTask {

        public void run() {
            var newValue = _pollCountDown.decrementAndGet();
            if (newValue == 0) {
                _pollIndicator = false;
                scheduleDrawStatus();
            } else if (newValue < 0) {
                _pollCountDown.set(0);
            }
        }
    }
}
