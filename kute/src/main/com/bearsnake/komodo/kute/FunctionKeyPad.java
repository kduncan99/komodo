/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

/**
 * Two columns to the left of the display F1..F22
 */
public class FunctionKeyPad extends GridPane {

    private static final float MIN_WIDTH = 40.0f;
    private static final float MIN_HEIGHT = 20.0f;

    public FunctionKeyPad() {
        setFocusTraversable(false);

        for (int fx = 0; fx < 22; fx++) {
            int functionNumber = fx + 1;
            var button = new Button(String.format("F%d", functionNumber));
            button.setMinWidth(MIN_WIDTH);
            button.setMinHeight(MIN_HEIGHT);
            button.setOnAction(event -> handleFunctionKey(functionNumber));

            int column = fx & 0x01;
            int row = fx / 2;
            add(button, column, row);
        }
    }

    private void handleFunctionKey(final int functionNumber) {
        Kute.getInstance().getActiveTerminal().kbSendFunctionKey(functionNumber);
    }
}
