/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.keypads;

import com.bearsnake.komodo.kutelib.Terminal;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Two rows across the top of the display
 *
 *  Erase  Erase  Delete  Insert  Insert  Line        Connect  Drop
 * Display  EOD   InDisp  InDisp   Line   Dup         Session Session          XFER   PRINT   XMIT
 *
 *  Erase  Erase  Delete  Insert  Delete  FCC   FCC     FCC     FCC    Clear   Ctl            Msg
 * To EOF   EOL   InLine  InLine   Line   Gen  Enable  Clear   Locate  Change  Page   Unlock  Wait
 */
public class ControlKeyPad extends StackPane implements KeyPad {

    public static final float BUTTON_HEIGHT = 45.0f;
    public static final float BUTTON_WIDTH = 70.0f;

    private Terminal _activeTerminal;

    public ControlKeyPad() {
        setAlignment(Pos.CENTER);

        Button[][] _buttons = new Button[2][13];
        _buttons[0][0] = new Button("Erase\nDisplay");
        _buttons[0][0].setOnAction(e -> _activeTerminal.kbEraseDisplay());

        _buttons[0][1] = new Button("Erase\nEOD");
        _buttons[0][1].setOnAction(e -> _activeTerminal.kbEraseToEndOfDisplay());

        _buttons[0][2] = new Button("Delete\nIn Disp");
        _buttons[0][2].setOnAction(e -> _activeTerminal.kbDeleteInDisplay());

        _buttons[0][3] = new Button("Insert\nIn Disp");
        _buttons[0][3].setOnAction(e -> _activeTerminal.kbInsertInDisplay());

        _buttons[0][4] = new Button("Insert\nLine");
        _buttons[0][4].setOnAction(e -> _activeTerminal.kbInsertLine());

        _buttons[0][5] = new Button("Line\nDup");
        _buttons[0][5].setOnAction(e -> _activeTerminal.kbDuplicateLine());

        _buttons[0][7] = new Button("Connect\nSession");
        _buttons[0][7].setOnAction(e -> _activeTerminal.connect());

        _buttons[0][8] = new Button("Drop\nSession");
        _buttons[0][8].setOnAction(e -> _activeTerminal.disconnect());

        _buttons[0][10] = new Button("XFER");
        _buttons[0][10].setOnAction(e -> _activeTerminal.kbTransfer());

        _buttons[0][11] = new Button("PRINT");
        _buttons[0][11].setOnAction(e -> _activeTerminal.kbPrint());

        _buttons[0][12] = new Button("XMIT");
        _buttons[0][12].setOnAction(e -> _activeTerminal.kbTransmit());

        _buttons[1][0] = new Button("Erase\nEOF");
        _buttons[1][0].setOnAction(e -> _activeTerminal.kbEraseToEndOfField());

        _buttons[1][1] = new Button("Erase\nEOL");
        _buttons[1][1].setOnAction(e -> _activeTerminal.kbEraseToEndOfLine());

        _buttons[1][2] = new Button("Delete\nIn Line");
        _buttons[1][2].setOnAction(e -> _activeTerminal.kbDeleteInLine());

        _buttons[1][3] = new Button("Insert\nIn Line");
        _buttons[1][3].setOnAction(e -> _activeTerminal.kbInsertInLine());

        _buttons[1][4] = new Button("Delete\nLine");
        _buttons[1][4].setOnAction(e -> _activeTerminal.kbDeleteLine());

        _buttons[1][5] = new Button("FCC\nGen");
        _buttons[1][5].setOnAction(e -> _activeTerminal.kbFCCGenerate());

        _buttons[1][6] = new Button("FCC\nEnable");
        _buttons[1][6].setOnAction(e -> _activeTerminal.kbFCCEnable());

        _buttons[1][7] = new Button("FCC\nClear");
        _buttons[1][7].setOnAction(e -> _activeTerminal.kbFCCClear());

        _buttons[1][8] = new Button("FCC\nLocate");
        _buttons[1][8].setOnAction(e -> _activeTerminal.kbFCCLocate());

        _buttons[1][9] = new Button("Clear\nChange");
        _buttons[1][9].setOnAction(e -> _activeTerminal.kbClearChanged());

        _buttons[1][10] = new Button("Control\nPage");
        _buttons[1][10].setOnAction(e -> _activeTerminal.kbToggleControlPage());

        _buttons[1][11] = new Button("KB\nUnlock");
        _buttons[1][11].setOnAction(e -> _activeTerminal.kbUnlock());

        _buttons[1][12] = new Button("MSG\nWait");
        _buttons[1][12].setOnAction(e -> _activeTerminal.kbMessageWait());

        var grid = new GridPane();
        grid.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        for (int rx = 0; rx < 2; rx++) {
            for (int cx = 0; cx < 13; cx++) {
                if (_buttons[rx][cx] != null) {
                    _buttons[rx][cx].setMinSize(BUTTON_WIDTH, BUTTON_HEIGHT);
                    _buttons[rx][cx].setStyle("-fx-text-alignment: center;");
                    grid.add(_buttons[rx][cx], cx, rx);
                }
            }
        }

        getChildren().add(grid);
        setFocusTraversable(false);
    }

    @Override
    public void setActiveTerminal(Terminal terminal) {
        _activeTerminal = terminal;
    }
}
