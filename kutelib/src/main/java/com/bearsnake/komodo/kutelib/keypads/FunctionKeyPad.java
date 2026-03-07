/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.keypads;

import com.bearsnake.komodo.kutelib.Terminal;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * Two columns to the left of the display F1..F22
 */
public class FunctionKeyPad extends GridPane implements KeyPad, KeyListener {

    private static final float MIN_WIDTH = 40.0f;
    private static final float MIN_HEIGHT = 20.0f;

    private static final Color BASE_COLOR_TOP = Color.web("#ffd7d7");
    private static final Color BASE_COLOR_BOTTOM = Color.web("#ff9999");
    private static final Color TEXT_COLOR = Color.BLACK;

    private Terminal _activeTerminal;
    private final Key[] _buttons = new Key[22];

    public FunctionKeyPad() {
        setFocusTraversable(false);

        for (int fx = 0; fx < 22; fx++) {
            int functionNumber = fx + 1;
            var button = new Key(String.format("F%d", functionNumber),
                                 this,
                                 functionNumber,
                                 BASE_COLOR_TOP,
                                 BASE_COLOR_BOTTOM,
                                 TEXT_COLOR,
                                 this);
            button.setMinWidth(MIN_WIDTH);
            button.setMinHeight(MIN_HEIGHT);
            _buttons[fx] = button;

            int column = fx & 0x01;
            int row = fx / 2;
            add(button, column, row);
        }
    }

    public void enableKeys(final boolean enabled) {
        for (var button : _buttons) {
            button.setDisable(!enabled);
        }
    }

    @Override
    public void notify(final Pane source, final int id) {
        _activeTerminal.kbSendFunctionKey(id);
    }

    @Override
    public void refreshButtons() {
        // Re-evaluate enablement of certain buttons
        for (var button : _buttons) {
            button.updateStyle();
        }
    }

    @Override
    public void setActiveTerminal(Terminal terminal) {
        _activeTerminal = terminal;
    }
}
