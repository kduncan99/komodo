/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.keypads;

import com.bearsnake.komodo.kutelib.Terminal;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * Two rows across the top of the display
 *
 *  Erase    Erase    Delete   Insert   Insert   Reset   Connect     Drop     Trace    Trace    Trace
 * Display    EOD     InDisp   InDisp    Line            Session   Session    Stop     Pause    Start     XFER     PRINT    XMIT
 *
 *  Erase    Erase    Delete   Insert   Delete    Line     FCC       FCC       FCC      FCC     Clear    Control     KB      MSG
 * To EOF     EOL     InLine   InLine    Line     Dup      Gen     Enable     Clear    Locate   Change    Page    Unlock    Wait
 */
public class ControlKeyPad extends StackPane implements KeyPad, KeyListener {

    public static final float BUTTON_HEIGHT = 45.0f;
    public static final float BUTTON_WIDTH = 70.0f;

    private static final Color FCC_TOP = Color.web("#d7ffd7");
    private static final Color FCC_BOTTOM = Color.web("lightgreen");
    private static final Color BLUE_TOP = Color.web("#d7f0ff");
    private static final Color BLUE_BOTTOM = Color.web("lightblue");
    private static final Color RED_TOP = Color.web("#ffd7d7");
    private static final Color RED_BOTTOM = Color.web("#ff9999");
    private static final Color YELLOW_TOP = Color.web("#ffffd7");
    private static final Color YELLOW_BOTTOM = Color.web("#ffff00");
    private static final Color ORANGE_TOP = Color.web("#ffd7b5");
    private static final Color ORANGE_BOTTOM = Color.web("#ff8c00");
    private static final Color LIGHT_ORANGE_TOP = Color.web("#fff0e0");
    private static final Color LIGHT_ORANGE_BOTTOM = Color.web("#ffb366");
    private static final Color DARK_RED_TOP = Color.web("#cc7a7a");
    private static final Color DARK_RED_BOTTOM = Color.web("#990000");

    private static final Color TEXT_BLACK = Color.BLACK;
    private static final Color TEXT_WHITE = Color.WHITE;

    private static final int ID_ERASE_DISPLAY = 1;
    private static final int ID_ERASE_EOD = 2;
    private static final int ID_DELETE_IN_DISP = 3;
    private static final int ID_INSERT_IN_DISP = 4;
    private static final int ID_INSERT_LINE = 5;
    private static final int ID_RESET = 6;
    private static final int ID_CONNECT_SESSION = 7;
    private static final int ID_DROP_SESSION = 8;
    private static final int ID_TRACE_STOP = 9;
    private static final int ID_TRACE_PAUSE = 10;
    private static final int ID_TRACE_START = 11;
    private static final int ID_XFER = 12;
    private static final int ID_PRINT = 13;
    private static final int ID_XMIT = 14;

    private static final int ID_ERASE_EOF = 15;
    private static final int ID_ERASE_EOL = 16;
    private static final int ID_DELETE_IN_LINE = 17;
    private static final int ID_INSERT_IN_LINE = 18;
    private static final int ID_DELETE_LINE = 19;
    private static final int ID_LINE_DUP = 20;
    private static final int ID_FCC_GEN = 21;
    private static final int ID_FCC_ENABLE = 22;
    private static final int ID_FCC_CLEAR = 23;
    private static final int ID_FCC_LOCATE = 24;
    private static final int ID_CLEAR_CHANGE = 25;
    private static final int ID_CONTROL_PAGE = 26;
    private static final int ID_KB_UNLOCK = 27;
    private static final int ID_MSG_WAIT = 28;

    private Terminal _activeTerminal;
    private final Key[][] _buttons = new Key[2][14];

    public ControlKeyPad() {
        setAlignment(Pos.CENTER);

        _buttons[0][0] = new Key("Erase\nDisplay", this, ID_ERASE_DISPLAY, BLUE_TOP, BLUE_BOTTOM, TEXT_BLACK, this);
        _buttons[0][1] = new Key("Erase\nEOD", this, ID_ERASE_EOD, BLUE_TOP, BLUE_BOTTOM, TEXT_BLACK, this);
        _buttons[0][2] = new Key("Delete\nIn Disp", this, ID_DELETE_IN_DISP, BLUE_TOP, BLUE_BOTTOM, TEXT_BLACK, this);
        _buttons[0][2].setEnableCycle(true);
        _buttons[0][3] = new Key("Insert\nIn Disp", this, ID_INSERT_IN_DISP, BLUE_TOP, BLUE_BOTTOM, TEXT_BLACK, this);
        _buttons[0][3].setEnableCycle(true);
        _buttons[0][4] = new Key("Insert\nLine", this, ID_INSERT_LINE, BLUE_TOP, BLUE_BOTTOM, TEXT_BLACK, this);
        _buttons[0][4].setEnableCycle(true);
        _buttons[0][5] = new Key("Reset", this, ID_RESET, DARK_RED_TOP, DARK_RED_BOTTOM, TEXT_WHITE, this);
        _buttons[0][6] = new Key("Connect\nSession", this, ID_CONNECT_SESSION, ORANGE_TOP, ORANGE_BOTTOM, TEXT_BLACK, this);
        _buttons[0][7] = new Key("Drop\nSession", this, ID_DROP_SESSION, ORANGE_TOP, ORANGE_BOTTOM, TEXT_BLACK, this);
        _buttons[0][8] = new Key("Trace\nStop", this, ID_TRACE_STOP, LIGHT_ORANGE_TOP, LIGHT_ORANGE_BOTTOM, TEXT_BLACK, this);
        _buttons[0][9] = new Key("Trace\nPause", this, ID_TRACE_PAUSE, LIGHT_ORANGE_TOP, LIGHT_ORANGE_BOTTOM, TEXT_BLACK, this);
        _buttons[0][10] = new Key("Trace\nStart", this, ID_TRACE_START, LIGHT_ORANGE_TOP, LIGHT_ORANGE_BOTTOM, TEXT_BLACK, this);
        _buttons[0][11] = new Key("XFER", this, ID_XFER, YELLOW_TOP, YELLOW_BOTTOM, TEXT_BLACK, this);
        _buttons[0][12] = new Key("PRINT", this, ID_PRINT, YELLOW_TOP, YELLOW_BOTTOM, TEXT_BLACK, this);
        _buttons[0][13] = new Key("XMIT", this, ID_XMIT, YELLOW_TOP, YELLOW_BOTTOM, TEXT_BLACK, this);

        _buttons[1][0] = new Key("Erase\nEOF", this, ID_ERASE_EOF, BLUE_TOP, BLUE_BOTTOM, TEXT_BLACK, this);
        _buttons[1][1] = new Key("Erase\nEOL", this, ID_ERASE_EOL, BLUE_TOP, BLUE_BOTTOM, TEXT_BLACK, this);
        _buttons[1][2] = new Key("Delete\nIn Line", this, ID_DELETE_IN_LINE, BLUE_TOP, BLUE_BOTTOM, TEXT_BLACK, this);
        _buttons[1][2].setEnableCycle(true);
        _buttons[1][3] = new Key("Insert\nIn Line", this, ID_INSERT_IN_LINE, BLUE_TOP, BLUE_BOTTOM, TEXT_BLACK, this);
        _buttons[1][3].setEnableCycle(true);
        _buttons[1][4] = new Key("Delete\nLine", this, ID_DELETE_LINE, BLUE_TOP, BLUE_BOTTOM, TEXT_BLACK, this);
        _buttons[1][4].setEnableCycle(true);
        _buttons[1][5] = new Key("Line\nDup", this, ID_LINE_DUP, BLUE_TOP, BLUE_BOTTOM, TEXT_BLACK, this);
        _buttons[1][5].setEnableCycle(true);
        _buttons[1][6] = new Key("FCC\nGen", this, ID_FCC_GEN, FCC_TOP, FCC_BOTTOM, TEXT_BLACK, this);
        _buttons[1][7] = new Key("FCC\nLocate", this, ID_FCC_LOCATE, FCC_TOP, FCC_BOTTOM, TEXT_BLACK, this);
        _buttons[1][8] = new Key("FCC\nClear", this, ID_FCC_CLEAR, FCC_TOP, FCC_BOTTOM, TEXT_BLACK, this);
        _buttons[1][9] = new Key("FCC\nEnable", this, ID_FCC_ENABLE, FCC_TOP, FCC_BOTTOM, TEXT_BLACK, this);
        _buttons[1][10] = new Key("Clear\nChanged", this, ID_CLEAR_CHANGE, FCC_TOP, FCC_BOTTOM, TEXT_BLACK, this);
        _buttons[1][11] = new Key("Control\nPage", this, ID_CONTROL_PAGE, YELLOW_TOP, YELLOW_BOTTOM, TEXT_BLACK, this);
        _buttons[1][12] = new Key("KB\nUnlock", this, ID_KB_UNLOCK, YELLOW_TOP, YELLOW_BOTTOM, TEXT_BLACK, this);
        _buttons[1][13] = new Key("MSG\nWait", this, ID_MSG_WAIT, RED_TOP, RED_BOTTOM, TEXT_BLACK, this);

        var grid = new GridPane();
        grid.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        for (int rx = 0; rx < 2; rx++) {
            for (int cx = 0; cx < 14; cx++) {
                if (_buttons[rx][cx] != null) {
                    _buttons[rx][cx].setMinWidth(BUTTON_WIDTH);
                    _buttons[rx][cx].setMinHeight(BUTTON_HEIGHT);
                    grid.add(_buttons[rx][cx], cx, rx);
                }
            }
        }

        getChildren().add(grid);
        setFocusTraversable(false);
    }

    public void enableKeys(final boolean enabled) {
        for (int rx = 0; rx < 2; rx++) {
            for (int cx = 0; cx < 14; cx++) {
                if (_buttons[rx][cx] != null) {
                    _buttons[rx][cx].setDisable(!enabled);
                }
            }
        }
    }

    @Override
    public void notify(final Pane source, final int id) {
        switch (id) {
            case ID_ERASE_DISPLAY -> _activeTerminal.kbEraseDisplay();
            case ID_ERASE_EOD -> _activeTerminal.kbEraseToEndOfDisplay();
            case ID_DELETE_IN_DISP -> _activeTerminal.kbDeleteInDisplay();
            case ID_INSERT_IN_DISP -> _activeTerminal.kbInsertInDisplay();
            case ID_INSERT_LINE -> _activeTerminal.kbInsertLine();
            case ID_RESET -> _activeTerminal.reset(true);
            case ID_CONNECT_SESSION -> _activeTerminal.connect();
            case ID_DROP_SESSION -> _activeTerminal.disconnect(true);
            case ID_TRACE_STOP -> _activeTerminal.stopNetworkTrace();
            case ID_TRACE_PAUSE -> _activeTerminal.pauseNetworkTrace();
            case ID_TRACE_START -> _activeTerminal.startNetworkTrace();
            case ID_XFER -> _activeTerminal.kbTransfer();
            case ID_PRINT -> _activeTerminal.kbPrint();
            case ID_XMIT -> _activeTerminal.kbTransmit();
            case ID_ERASE_EOF -> _activeTerminal.kbEraseToEndOfField();
            case ID_ERASE_EOL -> _activeTerminal.kbEraseToEndOfLine();
            case ID_DELETE_IN_LINE -> _activeTerminal.kbDeleteInLine();
            case ID_INSERT_IN_LINE -> _activeTerminal.kbInsertInLine();
            case ID_DELETE_LINE -> _activeTerminal.kbDeleteLine();
            case ID_LINE_DUP -> _activeTerminal.kbDuplicateLine();
            case ID_FCC_GEN -> _activeTerminal.kbFCCGenerate();
            case ID_FCC_ENABLE -> _activeTerminal.kbFCCEnable();
            case ID_FCC_CLEAR -> _activeTerminal.kbFCCClear();
            case ID_FCC_LOCATE -> _activeTerminal.kbFCCLocate();
            case ID_CLEAR_CHANGE -> _activeTerminal.kbClearChanged();
            case ID_CONTROL_PAGE -> _activeTerminal.kbToggleControlPage();
            case ID_KB_UNLOCK -> _activeTerminal.kbUnlock();
            case ID_MSG_WAIT -> _activeTerminal.kbMessageWait();
        }
    }

    @Override
    public void notifyReleased(final Pane source, final int id) {
    }

    @Override
    public void refreshButtons() {
        boolean locked = (_activeTerminal != null) && _activeTerminal.isKeyboardLocked();
        boolean connected = (_activeTerminal != null) && _activeTerminal.isConnected();
        boolean traceActive = (_activeTerminal != null) && _activeTerminal.isTraceActive();
        boolean tracePaused = (_activeTerminal != null) && _activeTerminal.isTracePaused();
        boolean fccEnabled = (_activeTerminal != null) && _activeTerminal.isFCCEnabled();

        for (int rx = 0; rx < 2; rx++) {
            for (int cx = 0; cx < 14; cx++) {
                Key button = _buttons[rx][cx];
                if (button != null) {
                    int id = button.getIdValue();
                    if (locked) {
                        boolean allowed = switch (id) {
                            case ID_RESET,
                                 ID_CONNECT_SESSION,
                                 ID_DROP_SESSION,
                                 ID_TRACE_STOP,
                                 ID_TRACE_PAUSE,
                                 ID_TRACE_START,
                                 ID_KB_UNLOCK,
                                 ID_MSG_WAIT -> true;
                            default -> false;
                        };
                        button.setDisable(!allowed);
                    } else {
                        button.setDisable(false);
                    }

                    // Further individual overrides
                    if (!button.isDisabled()) {
                        if (id == ID_CONNECT_SESSION) {
                            button.setDisable(connected);
                        } else if (id == ID_DROP_SESSION) {
                            button.setDisable(!connected);
                        } else if (id == ID_TRACE_STOP) {
                            button.setDisable(!(connected && (traceActive || tracePaused)));
                        } else if (id == ID_TRACE_PAUSE) {
                            button.setDisable(!(connected && traceActive && !tracePaused));
                        } else if (id == ID_TRACE_START) {
                            button.setDisable(!(connected && (!traceActive || tracePaused)));
                        } else if (id == ID_FCC_ENABLE) {
                            button.setDisable(fccEnabled);
                        }
                    }

                    button.updateStyle();
                }
            }
        }
    }

    @Override
    public void setActiveTerminal(Terminal terminal) {
        _activeTerminal = terminal;
        refreshButtons();
    }
}
