/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib;

import com.bearsnake.komodo.kutelib.keypads.KeyPad;
import com.bearsnake.komodo.kutelib.panes.FontInfo;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.util.HashSet;
import java.util.Set;

public class TerminalStack extends TabPane {

    private final Set<KeyPad> _keyPads = new HashSet<>();

    public TerminalStack() {
        // TODO temporary hard-coded terminals
        var settings = new TerminalSettings();
        var fontInfo = new FontInfo(16);

        var terminal1 = new Terminal("DEMAND", settings, fontInfo);
        var tab1 = new Tab("DEMAND", terminal1);

        var terminal2 = new Terminal("TIP", settings, fontInfo);
        var tab2 = new Tab("TIP", terminal2);

        getTabs().addAll(tab1, tab2);
        getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
                _keyPads.forEach(keyPad -> {
                    if (newValue != null) {
                        keyPad.setActiveTerminal((Terminal) newValue.getContent());
                    }
                });
            }
        });
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

    /**
     * Retrieves the currently-active Terminal
     * @return reference to Terminal, or null if there are no Terminal objects
     */
    public Terminal getActiveTerminal() {
        for (var tab  : getTabs()) {
            var content = tab.getContent();
            if ((content instanceof Terminal terminal) && tab.isSelected()) {
                return terminal;
            }
        }

        return null;
    }

    public void registerKeyPad(final KeyPad keyPad) {
        _keyPads.add(keyPad);
        keyPad.setActiveTerminal(getActiveTerminal());
    }
}
