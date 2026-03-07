/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.keypads;

import com.bearsnake.komodo.kutelib.Terminal;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 *  Home   CR
 *      Up
 *  Left  Right
 *     Down
 *  BTab  FTab
 */
public class CursorKeyPad extends GridPane implements KeyPad, KeyListener {

    private static final float MIN_WIDTH = 50.0f;
    private static final float MIN_HEIGHT = 20.0f;

    private static final Color BASE_COLOR_TOP = Color.web("#888888");
    private static final Color BASE_COLOR_BOTTOM = Color.web("#444444");
    private static final Color TEXT_COLOR = Color.WHITE;

    private static final int ID_HOME = 1;
    private static final int ID_RETURN = 2;
    private static final int ID_UP = 3;
    private static final int ID_LEFT = 4;
    private static final int ID_RIGHT = 5;
    private static final int ID_DOWN = 6;
    private static final int ID_BTAB = 7;
    private static final int ID_FTAB = 8;

    private Terminal _activeTerminal;
    private final Key[] _buttons = new Key[9];

    public CursorKeyPad() {
        setFocusTraversable(false);

        _buttons[ID_HOME] = new Key("↖", this, ID_HOME, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        add(_buttons[ID_HOME], 0, 0);

        _buttons[ID_RETURN] = new Key("↲", this, ID_RETURN, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        add(_buttons[ID_RETURN], 1, 0);

        _buttons[ID_UP] = new Key("↑", this, ID_UP, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        add(_buttons[ID_UP], 0, 1, 2, 1);

        _buttons[ID_LEFT] = new Key("←", this, ID_LEFT, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        add(_buttons[ID_LEFT], 0, 2);

        _buttons[ID_RIGHT] = new Key("→", this, ID_RIGHT, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        add(_buttons[ID_RIGHT], 1, 2);

        _buttons[ID_DOWN] = new Key("↓", this, ID_DOWN, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        add(_buttons[ID_DOWN], 0, 3, 2, 1);

        _buttons[ID_BTAB] = new Key("⇤", this, ID_BTAB, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        add(_buttons[ID_BTAB], 0, 4);

        _buttons[ID_FTAB] = new Key("⇥", this, ID_FTAB, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        add(_buttons[ID_FTAB], 1, 4);

        for (var button : _buttons) {
            if (button != null) {
                if (GridPane.getColumnSpan(button) != null && GridPane.getColumnSpan(button) > 1) {
                    button.setMinWidth(MIN_WIDTH * 2.0);
                } else {
                    button.setMinWidth(MIN_WIDTH);
                }
                button.setMinHeight(MIN_HEIGHT);
            }
        }
    }

    public void enableKeys(final boolean enabled) {
        for (var button : _buttons) {
            if (button != null) {
                button.setDisable(!enabled);
            }
        }
    }

    @Override
    public void notify(final Pane source, final int id) {
        switch (id) {
            case ID_HOME -> _activeTerminal.kbCursorToHome();
            case ID_RETURN -> _activeTerminal.kbCursorReturn();
            case ID_UP -> _activeTerminal.kbScanUp();
            case ID_LEFT -> _activeTerminal.kbScanLeft();
            case ID_RIGHT -> _activeTerminal.kbScanRight();
            case ID_DOWN -> _activeTerminal.kbScanDown();
            case ID_BTAB -> _activeTerminal.kbTabBackward();
            case ID_FTAB -> _activeTerminal.kbTabForward();
        }
    }

    @Override
    public void notifyReleased(final Pane source, final int id) {
        switch (id) {
            case ID_HOME -> _activeTerminal.handleKeyReleased(javafx.scene.input.KeyCode.HOME);
            case ID_UP -> _activeTerminal.handleKeyReleased(javafx.scene.input.KeyCode.UP);
            case ID_LEFT -> _activeTerminal.handleKeyReleased(javafx.scene.input.KeyCode.LEFT);
            case ID_RIGHT -> _activeTerminal.handleKeyReleased(javafx.scene.input.KeyCode.RIGHT);
            case ID_DOWN -> _activeTerminal.handleKeyReleased(javafx.scene.input.KeyCode.DOWN);
        }
    }

    @Override
    public void refreshButtons() {
        // Re-evaluate enablement of certain buttons
        for (var button : _buttons) {
            if (button != null) {
                button.updateStyle();
            }
        }
    }

    @Override
    public void setActiveTerminal(Terminal terminal) {
        _activeTerminal = terminal;
    }
}
