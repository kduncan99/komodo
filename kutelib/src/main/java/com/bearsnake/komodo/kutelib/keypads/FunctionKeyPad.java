/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.keypads;

import com.bearsnake.komodo.kutelib.Terminal;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

/**
 * Two columns to the left of the display F1..F22
 */
public class FunctionKeyPad extends GridPane implements KeyPad {

    private static final float MIN_WIDTH = 40.0f;
    private static final float MIN_HEIGHT = 20.0f;

    private static final String DEFAULT_STYLE = "-fx-background-color: linear-gradient(to bottom, #ffd7d7, #ff9999); -fx-text-fill: black; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";
    private static final String PRESSED_STYLE = "-fx-background-color: linear-gradient(to bottom, #cc7a7a, #ad5c5c); -fx-text-fill: black; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";

    private Terminal _activeTerminal;

    private void setButtonStyle(Button button) {
        button.setStyle(DEFAULT_STYLE);
        button.setOnMousePressed(e -> button.setStyle(PRESSED_STYLE));
        button.setOnMouseReleased(e -> button.setStyle(DEFAULT_STYLE));
    }

    public FunctionKeyPad() {
        setFocusTraversable(false);

        for (int fx = 0; fx < 22; fx++) {
            int functionNumber = fx + 1;
            var button = new Button(String.format("F%d", functionNumber));
            button.setMinWidth(MIN_WIDTH);
            button.setMinHeight(MIN_HEIGHT);
            button.setOnAction(event -> handleFunctionKey(functionNumber));
            setButtonStyle(button);

            int column = fx & 0x01;
            int row = fx / 2;
            add(button, column, row);
        }
    }

    private void handleFunctionKey(final int functionNumber) {
        _activeTerminal.kbSendFunctionKey(functionNumber);
    }

    @Override
    public void setActiveTerminal(Terminal terminal) {
        _activeTerminal = terminal;
    }
}
