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
 *  Erase    Erase    Delete   Insert   Insert   Reset   Connect     Drop     Trace    Trace    Trace
 * Display    EOD     InDisp   InDisp    Line            Session   Session    Stop     Pause    Start     XFER     PRINT    XMIT
 *
 *  Erase    Erase    Delete   Insert   Delete    Line     FCC       FCC       FCC      FCC     Clear    Control     KB      MSG
 * To EOF     EOL     InLine   InLine    Line     Dup      Gen     Enable     Clear    Locate   Change    Page    Unlock    Wait
 */
public class ControlKeyPad extends StackPane implements KeyPad {

    public static final float BUTTON_HEIGHT = 45.0f;
    public static final float BUTTON_WIDTH = 70.0f;

    private Terminal _activeTerminal;

    private static final String FCC_DEFAULT_STYLE = "-fx-background-color: linear-gradient(to bottom, #d7ffd7, lightgreen); -fx-text-fill: black; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";
    private static final String FCC_PRESSED_STYLE = "-fx-background-color: linear-gradient(to bottom, #7acc7a, #5cad5c); -fx-text-fill: black; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";

    private static final String BLUE_DEFAULT_STYLE = "-fx-background-color: linear-gradient(to bottom, #d7f0ff, lightblue); -fx-text-fill: black; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";
    private static final String BLUE_PRESSED_STYLE = "-fx-background-color: linear-gradient(to bottom, #7abacc, #5c9aad); -fx-text-fill: black; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";

    private static final String RED_DEFAULT_STYLE = "-fx-background-color: linear-gradient(to bottom, #ffd7d7, #ff9999); -fx-text-fill: black; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";
    private static final String RED_PRESSED_STYLE = "-fx-background-color: linear-gradient(to bottom, #cc7a7a, #ad5c5c); -fx-text-fill: black; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";

    private static final String YELLOW_DEFAULT_STYLE = "-fx-background-color: linear-gradient(to bottom, #ffffd7, #ffff00); -fx-text-fill: black; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";
    private static final String YELLOW_PRESSED_STYLE = "-fx-background-color: linear-gradient(to bottom, #cccc7a, #adad5c); -fx-text-fill: black; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";

    private static final String ORANGE_DEFAULT_STYLE = "-fx-background-color: linear-gradient(to bottom, #ffd7b5, #ff8c00); -fx-text-fill: black; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";
    private static final String ORANGE_PRESSED_STYLE = "-fx-background-color: linear-gradient(to bottom, #ccac90, #ad5f00); -fx-text-fill: black; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";

    private static final String LIGHT_ORANGE_DEFAULT_STYLE = "-fx-background-color: linear-gradient(to bottom, #fff0e0, #ffb366); -fx-text-fill: black; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";
    private static final String LIGHT_ORANGE_PRESSED_STYLE = "-fx-background-color: linear-gradient(to bottom, #ffcc99, #ff8000); -fx-text-fill: black; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";

    private static final String DARK_RED_DEFAULT_STYLE = "-fx-background-color: linear-gradient(to bottom, #cc7a7a, #990000); -fx-text-fill: white; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";
    private static final String DARK_RED_PRESSED_STYLE = "-fx-background-color: linear-gradient(to bottom, #990000, #660000); -fx-text-fill: white; -fx-text-alignment: center; -fx-border-color: black; -fx-border-width: 1px;";

    private void setGreenButtonStyle(Button button) {
        button.setStyle("-fx-background-color: lightgreen; -fx-text-fill: black; -fx-text-alignment: center;");
        button.setOnMousePressed(e -> button.setStyle("-fx-background-color: derive(lightgreen, -20%); -fx-text-fill: black; -fx-text-alignment: center;"));
        button.setOnMouseReleased(e -> button.setStyle("-fx-background-color: lightgreen; -fx-text-fill: black; -fx-text-alignment: center;"));
    }

    private void setFCCButtonStyle(Button button) {
        button.setStyle(FCC_DEFAULT_STYLE);
        button.setOnMousePressed(e -> button.setStyle(FCC_PRESSED_STYLE));
        button.setOnMouseReleased(e -> button.setStyle(FCC_DEFAULT_STYLE));
    }

    private void setBlueButtonStyle(Button button) {
        button.setStyle(BLUE_DEFAULT_STYLE);
        button.setOnMousePressed(e -> button.setStyle(BLUE_PRESSED_STYLE));
        button.setOnMouseReleased(e -> button.setStyle(BLUE_DEFAULT_STYLE));
    }

    private void setRedButtonStyle(Button button) {
        button.setStyle(RED_DEFAULT_STYLE);
        button.setOnMousePressed(e -> button.setStyle(RED_PRESSED_STYLE));
        button.setOnMouseReleased(e -> button.setStyle(RED_DEFAULT_STYLE));
    }

    private void setYellowButtonStyle(Button button) {
        button.setStyle(YELLOW_DEFAULT_STYLE);
        button.setOnMousePressed(e -> button.setStyle(YELLOW_PRESSED_STYLE));
        button.setOnMouseReleased(e -> button.setStyle(YELLOW_DEFAULT_STYLE));
    }

    private void setOrangeButtonStyle(Button button) {
        button.setStyle(ORANGE_DEFAULT_STYLE);
        button.setOnMousePressed(e -> button.setStyle(ORANGE_PRESSED_STYLE));
        button.setOnMouseReleased(e -> button.setStyle(ORANGE_DEFAULT_STYLE));
    }

    private void setLightOrangeButtonStyle(Button button) {
        button.setStyle(LIGHT_ORANGE_DEFAULT_STYLE);
        button.setOnMousePressed(e -> button.setStyle(LIGHT_ORANGE_PRESSED_STYLE));
        button.setOnMouseReleased(e -> button.setStyle(LIGHT_ORANGE_DEFAULT_STYLE));
    }

    private void setDarkRedButtonStyle(Button button) {
        button.setStyle(DARK_RED_DEFAULT_STYLE);
        button.setOnMousePressed(e -> button.setStyle(DARK_RED_PRESSED_STYLE));
        button.setOnMouseReleased(e -> button.setStyle(DARK_RED_DEFAULT_STYLE));
    }

    public ControlKeyPad() {
        setAlignment(Pos.CENTER);

        Button[][] _buttons = new Button[2][14];
        _buttons[0][0] = new Button("Erase\nDisplay");
        _buttons[0][0].setOnAction(e -> _activeTerminal.kbEraseDisplay());
        setBlueButtonStyle(_buttons[0][0]);

        _buttons[0][1] = new Button("Erase\nEOD");
        _buttons[0][1].setOnAction(e -> _activeTerminal.kbEraseToEndOfDisplay());
        setBlueButtonStyle(_buttons[0][1]);

        _buttons[0][2] = new Button("Delete\nIn Disp");
        _buttons[0][2].setOnAction(e -> _activeTerminal.kbDeleteInDisplay());
        setBlueButtonStyle(_buttons[0][2]);

        _buttons[0][3] = new Button("Insert\nIn Disp");
        _buttons[0][3].setOnAction(e -> _activeTerminal.kbInsertInDisplay());
        setBlueButtonStyle(_buttons[0][3]);

        _buttons[0][4] = new Button("Insert\nLine");
        _buttons[0][4].setOnAction(e -> _activeTerminal.kbInsertLine());
        setBlueButtonStyle(_buttons[0][4]);

        _buttons[0][5] = new Button("Reset");
        _buttons[0][5].setOnAction(e -> _activeTerminal.reset());
        setDarkRedButtonStyle(_buttons[0][5]);

        _buttons[0][6] = new Button("Connect\nSession");
        _buttons[0][6].setOnAction(e -> _activeTerminal.connect());
        setOrangeButtonStyle(_buttons[0][6]);

        _buttons[0][7] = new Button("Drop\nSession");
        _buttons[0][7].setOnAction(e -> _activeTerminal.disconnect());
        setOrangeButtonStyle(_buttons[0][7]);

        _buttons[0][8] = new Button("Trace\nStop");
        setLightOrangeButtonStyle(_buttons[0][8]);

        _buttons[0][9] = new Button("Trace\nPause");
        setLightOrangeButtonStyle(_buttons[0][9]);

        _buttons[0][10] = new Button("Trace\nStart");
        setLightOrangeButtonStyle(_buttons[0][10]);

        _buttons[0][11] = new Button("XFER");
        _buttons[0][11].setOnAction(e -> _activeTerminal.kbTransfer());
        setYellowButtonStyle(_buttons[0][11]);

        _buttons[0][12] = new Button("PRINT");
        _buttons[0][12].setOnAction(e -> _activeTerminal.kbPrint());
        setYellowButtonStyle(_buttons[0][12]);

        _buttons[0][13] = new Button("XMIT");
        _buttons[0][13].setOnAction(e -> _activeTerminal.kbTransmit());
        setYellowButtonStyle(_buttons[0][13]);

        _buttons[1][0] = new Button("Erase\nEOF");
        _buttons[1][0].setOnAction(e -> _activeTerminal.kbEraseToEndOfField());
        setBlueButtonStyle(_buttons[1][0]);

        _buttons[1][1] = new Button("Erase\nEOL");
        _buttons[1][1].setOnAction(e -> _activeTerminal.kbEraseToEndOfLine());
        setBlueButtonStyle(_buttons[1][1]);

        _buttons[1][2] = new Button("Delete\nIn Line");
        _buttons[1][2].setOnAction(e -> _activeTerminal.kbDeleteInLine());
        setBlueButtonStyle(_buttons[1][2]);

        _buttons[1][3] = new Button("Insert\nIn Line");
        _buttons[1][3].setOnAction(e -> _activeTerminal.kbInsertInLine());
        setBlueButtonStyle(_buttons[1][3]);

        _buttons[1][4] = new Button("Delete\nLine");
        _buttons[1][4].setOnAction(e -> _activeTerminal.kbDeleteLine());
        setBlueButtonStyle(_buttons[1][4]);

        _buttons[1][5] = new Button("Line\nDup");
        _buttons[1][5].setOnAction(e -> _activeTerminal.kbDuplicateLine());
        setBlueButtonStyle(_buttons[1][5]);

        _buttons[1][6] = new Button("FCC\nGen");
        _buttons[1][6].setOnAction(e -> _activeTerminal.kbFCCGenerate());
        setFCCButtonStyle(_buttons[1][6]);

        _buttons[1][7] = new Button("FCC\nEnable");
        _buttons[1][7].setOnAction(e -> _activeTerminal.kbFCCEnable());
        setFCCButtonStyle(_buttons[1][7]);

        _buttons[1][8] = new Button("FCC\nClear");
        _buttons[1][8].setOnAction(e -> _activeTerminal.kbFCCClear());
        setFCCButtonStyle(_buttons[1][8]);

        _buttons[1][9] = new Button("FCC\nLocate");
        _buttons[1][9].setOnAction(e -> _activeTerminal.kbFCCLocate());
        setFCCButtonStyle(_buttons[1][9]);

        _buttons[1][10] = new Button("Clear\nChange");
        _buttons[1][10].setOnAction(e -> _activeTerminal.kbClearChanged());
        setFCCButtonStyle(_buttons[1][10]);

        _buttons[1][11] = new Button("Control\nPage");
        _buttons[1][11].setOnAction(e -> _activeTerminal.kbToggleControlPage());
        setYellowButtonStyle(_buttons[1][11]);

        _buttons[1][12] = new Button("KB\nUnlock");
        _buttons[1][12].setOnAction(e -> _activeTerminal.kbUnlock());
        setYellowButtonStyle(_buttons[1][12]);

        _buttons[1][13] = new Button("MSG\nWait");
        _buttons[1][13].setOnAction(e -> _activeTerminal.kbMessageWait());
        setRedButtonStyle(_buttons[1][13]);

        var grid = new GridPane();
        grid.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        for (int rx = 0; rx < 2; rx++) {
            for (int cx = 0; cx < 14; cx++) {
                if (_buttons[rx][cx] != null) {
                    _buttons[rx][cx].setMinSize(BUTTON_WIDTH, BUTTON_HEIGHT);
                    if (_buttons[rx][cx].getStyle().isEmpty()) {
                        setGreenButtonStyle(_buttons[rx][cx]);
                    }
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
