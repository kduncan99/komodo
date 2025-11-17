/*
 * Copyright (c) 2025 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Two rows across the top of the display
 *
 *  Erase  Erase  Delete  Insert  Insert  Line
 * Display  EOD   InDisp  InDisp   Line   Dup                                XFER   PRINT   XMIT
 *
 *  Erase  Erase  Delete  Insert  Delete  FCC   FCC     FCC    FCC   Clear   Ctl            Msg
 * To EOF   EOL   InLine  InLine   Line   Gen  Enable  Clear  Locate Change  Page   Unlock  Wait
 */
public class ControlKeyPad extends StackPane {

    public static final float BUTTON_HEIGHT = 45.0f;
    public static final float BUTTON_WIDTH = 70.0f;

    public ControlKeyPad() {
        setAlignment(Pos.CENTER);

        Button[][] _buttons = new Button[2][13];
        _buttons[0][0] = new Button("Erase\nDisplay");
        _buttons[0][0].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbEraseDisplay());

        _buttons[0][1] = new Button("Erase\nEOD");
        _buttons[0][1].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbEraseToEndOfDisplay());

        _buttons[0][2] = new Button("Delete\nIn Disp");
        _buttons[0][2].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbDeleteInDisplay());

        _buttons[0][3] = new Button("Insert\nIn Disp");
        _buttons[0][3].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbInsertInDisplay());

        _buttons[0][4] = new Button("Insert\nLine");
        _buttons[0][4].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbInsertLine());

        _buttons[0][5] = new Button("Line\nDup");
        _buttons[0][5].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbDuplicateLine());

        _buttons[0][10] = new Button("XFER");
        _buttons[0][10].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbTransfer());

        _buttons[0][11] = new Button("PRINT");
        _buttons[0][11].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbPrint());

        _buttons[0][12] = new Button("XMIT");
        _buttons[0][12].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbTransmit());

        _buttons[1][0] = new Button("Erase\nEOF");
        _buttons[1][0].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbEraseToEndOfField());

        _buttons[1][1] = new Button("Erase\nEOL");
        _buttons[1][1].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbEraseToEndOfLine());

        _buttons[1][2] = new Button("Delete\nIn Line");
        _buttons[1][2].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbDeleteInLine());

        _buttons[1][3] = new Button("Insert\nIn Line");
        _buttons[1][3].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbInsertInLine());

        _buttons[1][4] = new Button("Delete\nLine");
        _buttons[1][4].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbDeleteLine());

        _buttons[1][5] = new Button("FCC\nGen");
        _buttons[1][5].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbFCCGenerate());

        _buttons[1][6] = new Button("FCC\nEnable");
        _buttons[1][6].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbFCCEnable());

        _buttons[1][7] = new Button("FCC\nClear");
        _buttons[1][7].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbFCCClear());

        _buttons[1][8] = new Button("FCC\nLocate");
        _buttons[1][8].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbFCCLocate());

        _buttons[1][9] = new Button("Clear\nChange");
        _buttons[1][9].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbClearChanged());

        _buttons[1][10] = new Button("Control\nPage");
        _buttons[1][10].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbToggleControlPage());

        _buttons[1][11] = new Button("KB\nUnlock");
        _buttons[1][11].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbUnlock());

        _buttons[1][12] = new Button("MSG\nWait");
        _buttons[1][12].setOnAction(e -> Kute.getInstance().getActiveTerminal().kbMessageWait());

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
}
