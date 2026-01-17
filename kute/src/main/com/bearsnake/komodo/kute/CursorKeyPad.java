/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

/**
 *  Home   CR
 *      Up
 *  Left  Right
 *     Down
 *  BTab  FTab
 */
public class CursorKeyPad extends GridPane {

    private static final float MIN_WIDTH = 40.0f;
    private static final float MIN_HEIGHT = 20.0f;

    public CursorKeyPad() {
        setFocusTraversable(false);

        var homeButton = new Button("↖");
        homeButton.setOnAction(_ -> Kute.getInstance().getActiveTerminal().kbCursorToHome());
        homeButton.setMinWidth(MIN_WIDTH);
        homeButton.setMinHeight(MIN_HEIGHT);
        add(homeButton, 0, 0);

        var returnButton = new Button("↲");
        returnButton.setMinWidth(MIN_WIDTH);
        returnButton.setMinHeight(MIN_HEIGHT);
        returnButton.setOnAction(_ -> Kute.getInstance().getActiveTerminal().kbSOE());
        add(returnButton, 1, 0);

        var upButton = new Button("↑");
        upButton.setOnAction(_ -> Kute.getInstance().getActiveTerminal().kbScanUp());
        upButton.setMinWidth(MIN_WIDTH * 2.0);
        upButton.setMinHeight(MIN_HEIGHT);
        add(upButton, 0, 1, 2, 1);

        var leftButton = new Button("←");
        leftButton.setOnAction(_ -> Kute.getInstance().getActiveTerminal().kbScanLeft());
        leftButton.setMinWidth(MIN_WIDTH);
        leftButton.setMinHeight(MIN_HEIGHT);
        add(leftButton, 0, 2);

        var rightButton = new Button("→");
        rightButton.setOnAction(_ -> Kute.getInstance().getActiveTerminal().kbScanRight());
        rightButton.setMinWidth(MIN_WIDTH);
        rightButton.setMinHeight(MIN_HEIGHT);
        add(rightButton, 1, 2);

        var downButton = new Button("↓");
        downButton.setOnAction(_ -> Kute.getInstance().getActiveTerminal().kbScanDown());
        downButton.setMinWidth(MIN_WIDTH * 2.0);
        downButton.setMinHeight(MIN_HEIGHT);
        add(downButton, 0, 3, 2, 1);

        var backwardTabButton = new Button("⇤");
        backwardTabButton.setMinWidth(MIN_WIDTH);
        backwardTabButton.setMinHeight(MIN_HEIGHT);
        backwardTabButton.setOnAction(_ -> Kute.getInstance().getActiveTerminal().kbTabBackward());
        add(backwardTabButton, 0, 4);

        var forwardTabButton = new Button("⇥");
        forwardTabButton.setMinWidth(MIN_WIDTH);
        forwardTabButton.setMinHeight(MIN_HEIGHT);
        forwardTabButton.setOnAction(_ -> Kute.getInstance().getActiveTerminal().kbTabForward());
        add(forwardTabButton, 1, 4);
    }
}
