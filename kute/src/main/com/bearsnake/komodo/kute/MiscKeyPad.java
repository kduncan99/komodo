/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

import static com.bearsnake.komodo.kute.Constants.*;

/**
 *  SOE   SetTab
 *  CR      LF
 *  ESC     FF
 * ChErs  ColSep
 * StkThr UndScr
 */
public class MiscKeyPad extends GridPane {

    private static final float MIN_WIDTH = 40.0f;
    private static final float MIN_HEIGHT = 44.0f;

    public MiscKeyPad() {
        setFocusTraversable(false);

        var soeSet = new Button("▷");
        soeSet.setOnAction(_ -> Kute.getInstance().getActiveTerminal().kbSOE());
        add(soeSet, 0, 0);

        var tabSet = new Button("Tab\nSet");
        tabSet.setOnAction(_ -> Kute.getInstance().getActiveTerminal().kbSetTab());
        add(tabSet, 1, 0);

        var crSet = new Button("CR\nSet");
        crSet.setOnAction(e -> Kute.getInstance().getActiveTerminal().kbPutCharacter(ASCII_CR));
        add(crSet, 0, 1);

        var lfSet = new Button("LF\nSet");
        lfSet.setOnAction(e -> Kute.getInstance().getActiveTerminal().kbPutCharacter(ASCII_LF));
        add(lfSet, 1, 1);

        var escSet = new Button("ESC\nSet");
        escSet.setOnAction(e -> Kute.getInstance().getActiveTerminal().kbPutCharacter(ASCII_ESC));
        add(escSet, 0, 2);

        var ffSet = new Button("FF\nSet");
        ffSet.setOnAction(e -> Kute.getInstance().getActiveTerminal().kbPutCharacter(ASCII_FF));
        add(ffSet, 1, 2);

        var charErase = new Button("Chr\nErs");
        charErase.setOnAction(e -> Kute.getInstance().getActiveTerminal().kbEraseCharacter());
        add(charErase, 0, 3);

        var columnSeparator = new Button("Col\nSep");
        columnSeparator.setOnAction(e -> Kute.getInstance().getActiveTerminal().kbToggleColumnSeparator());
        add(columnSeparator, 1, 3);

        var strikeThrough = new Button("Stk\nThr");
        strikeThrough.setOnAction(e -> Kute.getInstance().getActiveTerminal().kbToggleStrikeThrough());
        add(strikeThrough, 0, 4);

        var underScore = new Button("Und\nScr");
        underScore.setOnAction(e -> Kute.getInstance().getActiveTerminal().kbToggleUnderScore());
        add(underScore, 1, 4);

        for (var button : getChildren()) {
            ((Button)button).setMinWidth(MIN_WIDTH);
            ((Button)button).setMinHeight(MIN_HEIGHT);
        }
    }
}
