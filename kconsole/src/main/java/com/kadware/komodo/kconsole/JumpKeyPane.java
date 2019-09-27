/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kconsole;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadware.komodo.commlib.SecureClient;
import com.kadware.komodo.commlib.SystemProcessorPoll;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

@SuppressWarnings("Duplicates")
class JumpKeyPane extends GridPane {

    private final ConsoleInfo _consoleInfo;
    private final CheckBox[] _checkBoxes = new CheckBox[36];

    private JumpKeyPane(ConsoleInfo consoleInfo) { _consoleInfo = consoleInfo; }

    static JumpKeyPane create(
        final ConsoleInfo consoleInfo
    ) {
        //TODO mouse-overs to describe the usage of the jump key
        JumpKeyPane pane = new JumpKeyPane(consoleInfo);
        for (int row = 0; row < 9; ++row) {
            for (int col = 0; col < 4; ++col) {
                int jx = row + (col * 9);
                pane._checkBoxes[jx] = new CheckBox(String.format("JK%d", jx + 1));
                pane.add(pane._checkBoxes[jx], col * 2, row);
            }
        }

        return pane;
    }

    /**
     * jump keys have been updated - update the pane
     */
    void update(
        final long value
    ) {
        long mask = 0_400000_000000L;
        for (int jx = 0; jx < 36; ++jx) {
            _checkBoxes[jx].setSelected((value & mask) != 0);
            mask >>= 1;
        }
    }
}
