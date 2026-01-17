/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class TerminalStack extends TabPane {

    public TerminalStack() {
        var terminal1 = new Terminal(new DisplayGeometry(24, 80),
                                     new FontInfo(16),
                                     new UTSColorSet(UTSColor.GREEN, UTSColor.BLACK),
                                     false,
                                     true,
                                     true);
        var tab1 = new Tab("DEMAND", terminal1);

        var terminal2 = new Terminal(new DisplayGeometry(24, 80),
                                     new FontInfo(16),
                                     new UTSColorSet(UTSColor.YELLOW, UTSColor.BLACK),
                                     false,
                                     true,
                                     true);
        var tab2 = new Tab("TIP", terminal2);

        getTabs().addAll(tab1, tab2);
    }

    public void closeAll() {
        for (var tab : getTabs()) {
            var content = tab.getContent();
            if (content instanceof Terminal terminal) {
                terminal.close();
            }
        }
        getTabs().clear();
    }

    public void cycleTabs() {
        SingleSelectionModel<Tab> selectionModel = getSelectionModel();
        int ix = selectionModel.getSelectedIndex() + 1;
        if (ix >= getTabs().size()) {
            ix = 0;
        }
        selectionModel.select(ix);
    }

    public Terminal getActiveTerminal() {
        for (var tab  : getTabs()) {
            var content = tab.getContent();
            if ((content instanceof Terminal terminal) && tab.isSelected()) {
                return terminal;
            }
        }

        // TODO what to do if there is no terminal at all?
        throw new RuntimeException("No Terminal selected");
    }
}
