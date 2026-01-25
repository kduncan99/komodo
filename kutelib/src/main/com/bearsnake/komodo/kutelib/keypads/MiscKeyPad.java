/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.keypads;

import com.bearsnake.komodo.kutelib.Terminal;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

import static com.bearsnake.komodo.kutelib.Constants.*;

/**
 *  SOE   SetTab
 *  CR      LF
 *  ESC     FF
 * ChErs  ColSep
 * StkThr UndScr
 */
public class MiscKeyPad extends GridPane implements KeyPad {

    private static final float MIN_WIDTH = 40.0f;
    private static final float MIN_HEIGHT = 44.0f;

    private Terminal _activeTerminal;

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

        for (var button : getChildren()) {
            ((Button)button).setMinWidth(MIN_WIDTH);
            ((Button)button).setMinHeight(MIN_HEIGHT);
        }
    }

    @Override
    public void setActiveTerminal(Terminal terminal) {
        _activeTerminal = terminal;
    }
}
