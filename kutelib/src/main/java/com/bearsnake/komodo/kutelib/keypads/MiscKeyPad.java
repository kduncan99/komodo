/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.keypads;

import com.bearsnake.komodo.kutelib.Terminal;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import static com.bearsnake.komodo.baselib.Constants.*;

/**
 *  SOE   SetTab
 *  CR      LF
 *  ESC     FF
 * ChErs  ColSep
 * StkThr UndScr
 */
public class MiscKeyPad extends GridPane implements KeyPad, KeyListener {

    private static final float MIN_WIDTH = 50.0f;
    private static final float MIN_HEIGHT = 44.0f;

    private static final Color BASE_COLOR_TOP = Color.web("#4a90e2");
    private static final Color BASE_COLOR_BOTTOM = Color.web("#1e5799");
    private static final Color TEXT_COLOR = Color.WHITE;

    private static final int ID_SOE = 1;
    private static final int ID_SET_TAB = 2;
    private static final int ID_CR = 3;
    private static final int ID_LF = 4;
    private static final int ID_ESC = 5;
    private static final int ID_FF = 6;
    private static final int ID_CHAR_ERASE = 7;
    private static final int ID_COL_SEP = 8;
    private static final int ID_STRIKE_THROUGH = 9;
    private static final int ID_UNDERSCORE = 10;

    private Terminal _activeTerminal;
    private final Key[] _buttons = new Key[11];

    public MiscKeyPad() {
        setFocusTraversable(false);

        _buttons[ID_SOE] = new Key("▷", this, ID_SOE, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        _buttons[ID_SOE].setEnableCycle(true);
        add(_buttons[ID_SOE], 0, 0);

        _buttons[ID_SET_TAB] = new Key("Tab\nSet", this, ID_SET_TAB, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        _buttons[ID_SET_TAB].setEnableCycle(true);
        add(_buttons[ID_SET_TAB], 1, 0);

        _buttons[ID_CR] = new Key("CR\nSet", this, ID_CR, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        add(_buttons[ID_CR], 0, 1);

        _buttons[ID_LF] = new Key("LF\nSet", this, ID_LF, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        add(_buttons[ID_LF], 1, 1);

        _buttons[ID_ESC] = new Key("ESC\nSet", this, ID_ESC, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        add(_buttons[ID_ESC], 0, 2);

        _buttons[ID_FF] = new Key("FF\nSet", this, ID_FF, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        add(_buttons[ID_FF], 1, 2);

        _buttons[ID_CHAR_ERASE] = new Key("Chr\nErs", this, ID_CHAR_ERASE, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        _buttons[ID_CHAR_ERASE].setEnableCycle(true);
        add(_buttons[ID_CHAR_ERASE], 0, 3);

        _buttons[ID_COL_SEP] = new Key("Col\nSep", this, ID_COL_SEP, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        add(_buttons[ID_COL_SEP], 1, 3);

        _buttons[ID_STRIKE_THROUGH] = new Key("Stk\nThr", this, ID_STRIKE_THROUGH, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        add(_buttons[ID_STRIKE_THROUGH], 0, 4);

        _buttons[ID_UNDERSCORE] = new Key("Und\nScr", this, ID_UNDERSCORE, BASE_COLOR_TOP, BASE_COLOR_BOTTOM, TEXT_COLOR, this);
        add(_buttons[ID_UNDERSCORE], 1, 4);

        for (var button : _buttons) {
            if (button != null) {
                button.setMinWidth(MIN_WIDTH);
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
            case ID_SOE -> _activeTerminal.kbSOE();
            case ID_SET_TAB -> _activeTerminal.kbSetTab();
            case ID_CR -> _activeTerminal.kbPutCharacter(ASCII_CR);
            case ID_LF -> _activeTerminal.kbPutCharacter(ASCII_LF);
            case ID_ESC -> _activeTerminal.kbPutCharacter(ASCII_ESC);
            case ID_FF -> _activeTerminal.kbPutCharacter(ASCII_FF);
            case ID_CHAR_ERASE -> _activeTerminal.kbEraseCharacter();
            case ID_COL_SEP -> _activeTerminal.kbToggleColumnSeparator();
            case ID_STRIKE_THROUGH -> _activeTerminal.kbToggleStrikeThrough();
            case ID_UNDERSCORE -> _activeTerminal.kbToggleUnderScore();
        }
    }

    @Override
    public void notifyReleased(final Pane source, final int id) {
    }

    @Override
    public void refreshButtons() {
        boolean locked = (_activeTerminal != null) && _activeTerminal.isKeyboardLocked();
        for (var button : _buttons) {
            if (button != null) {
                button.setDisable(locked);
                button.updateStyle();
            }
        }
    }

    @Override
    public void setActiveTerminal(Terminal terminal) {
        _activeTerminal = terminal;
        refreshButtons();
    }
}
