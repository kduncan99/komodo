/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.keypads;

import com.bearsnake.komodo.kutelib.Terminal;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

/**
 *  Home   CR
 *      Up
 *  Left  Right
 *     Down
 *  BTab  FTab
 */
public class CursorKeyPad extends GridPane implements KeyPad {

    private static final float MIN_WIDTH = 50.0f;
    private static final float MIN_HEIGHT = 20.0f;

    private static final String DEFAULT_STYLE = "-fx-background-color: linear-gradient(to bottom, #888888, #444444); -fx-text-fill: white; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";
    private static final String PRESSED_STYLE = "-fx-background-color: linear-gradient(to bottom, #444444, #222222); -fx-text-fill: white; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";

    private Terminal _activeTerminal;

    private void setButtonStyle(Button button) {
        button.setStyle(DEFAULT_STYLE);
        button.setOnMousePressed(e -> button.setStyle(PRESSED_STYLE));
        button.setOnMouseReleased(e -> button.setStyle(DEFAULT_STYLE));
    }

    public CursorKeyPad() {
        setFocusTraversable(false);

        var homeButton = new Button("↖");
        homeButton.setOnAction(_ -> _activeTerminal.kbCursorToHome());
        add(homeButton, 0, 0);

        var returnButton = new Button("↲");
        returnButton.setOnAction(_ -> _activeTerminal.kbSOE());
        add(returnButton, 1, 0);

        var upButton = new Button("↑");
        upButton.setOnAction(_ -> _activeTerminal.kbScanUp());
        add(upButton, 0, 1, 2, 1);

        var leftButton = new Button("←");
        leftButton.setOnAction(_ -> _activeTerminal.kbScanLeft());
        add(leftButton, 0, 2);

        var rightButton = new Button("→");
        rightButton.setOnAction(_ -> _activeTerminal.kbScanRight());
        add(rightButton, 1, 2);

        var downButton = new Button("↓");
        downButton.setOnAction(_ -> _activeTerminal.kbScanDown());
        add(downButton, 0, 3, 2, 1);

        var backwardTabButton = new Button("⇤");
        backwardTabButton.setOnAction(_ -> _activeTerminal.kbTabBackward());
        add(backwardTabButton, 0, 4);

        var forwardTabButton = new Button("⇥");
        forwardTabButton.setOnAction(_ -> _activeTerminal.kbTabForward());
        add(forwardTabButton, 1, 4);

        for (var node : getChildren()) {
            if (node instanceof Button button) {
                if (GridPane.getColumnSpan(button) != null && GridPane.getColumnSpan(button) > 1) {
                    button.setMinWidth(MIN_WIDTH * 2.0);
                } else {
                    button.setMinWidth(MIN_WIDTH);
                }
                button.setMinHeight(MIN_HEIGHT);
                setButtonStyle(button);
            }
        }
    }

    @Override
    public void setActiveTerminal(Terminal terminal) {
        _activeTerminal = terminal;
    }
}
