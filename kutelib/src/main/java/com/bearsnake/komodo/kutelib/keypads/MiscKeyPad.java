/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.keypads;

import com.bearsnake.komodo.kutelib.Terminal;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

import static com.bearsnake.komodo.baselib.Constants.*;

/**
 *  SOE   SetTab
 *  CR      LF
 *  ESC     FF
 * ChErs  ColSep
 * StkThr UndScr
 */
public class MiscKeyPad extends GridPane implements KeyPad {

    private static final float MIN_WIDTH = 50.0f;
    private static final float MIN_HEIGHT = 44.0f;

    private static final String DEFAULT_STYLE = "-fx-background-color: linear-gradient(to bottom, #4a90e2, #1e5799); -fx-text-fill: white; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";
    private static final String PRESSED_STYLE = "-fx-background-color: linear-gradient(to bottom, #1e5799, #103d6d); -fx-text-fill: white; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";

    private Terminal _activeTerminal;

    private void setButtonStyle(Button button) {
        button.setStyle(DEFAULT_STYLE);
        button.setOnMousePressed(e -> button.setStyle(PRESSED_STYLE));
        button.setOnMouseReleased(e -> button.setStyle(DEFAULT_STYLE));
    }

    public MiscKeyPad() {
        setFocusTraversable(false);

        var soeSet = new Button("▷");
        soeSet.setOnAction(_ -> _activeTerminal.kbSOE());
        add(soeSet, 0, 0);

        var tabSet = new Button("Tab\nSet");
        tabSet.setOnAction(_ -> _activeTerminal.kbSetTab());
        add(tabSet, 1, 0);

        var crSet = new Button("CR\nSet");
        crSet.setOnAction(e -> _activeTerminal.kbPutCharacter(ASCII_CR));
        add(crSet, 0, 1);

        var lfSet = new Button("LF\nSet");
        lfSet.setOnAction(e -> _activeTerminal.kbPutCharacter(ASCII_LF));
        add(lfSet, 1, 1);

        var escSet = new Button("ESC\nSet");
        escSet.setOnAction(e -> _activeTerminal.kbPutCharacter(ASCII_ESC));
        add(escSet, 0, 2);

        var ffSet = new Button("FF\nSet");
        ffSet.setOnAction(e -> _activeTerminal.kbPutCharacter(ASCII_FF));
        add(ffSet, 1, 2);

        var charErase = new Button("Chr\nErs");
        charErase.setOnAction(e -> _activeTerminal.kbEraseCharacter());
        add(charErase, 0, 3);

        var columnSeparator = new Button("Col\nSep");
        columnSeparator.setOnAction(e -> _activeTerminal.kbToggleColumnSeparator());
        add(columnSeparator, 1, 3);

        var strikeThrough = new Button("Stk\nThr");
        strikeThrough.setOnAction(e -> _activeTerminal.kbToggleStrikeThrough());
        add(strikeThrough, 0, 4);

        var underScore = new Button("Und\nScr");
        underScore.setOnAction(e -> _activeTerminal.kbToggleUnderScore());
        add(underScore, 1, 4);

        for (var node : getChildren()) {
            if (node instanceof Button button) {
                button.setMinWidth(MIN_WIDTH);
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
