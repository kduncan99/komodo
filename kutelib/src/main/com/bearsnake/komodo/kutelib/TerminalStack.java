/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib;

import com.bearsnake.komodo.kutelib.keypads.KeyPad;
import com.bearsnake.komodo.kutelib.panes.DisplayGeometry;
import com.bearsnake.komodo.kutelib.panes.FontInfo;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import javafx.scene.layout.StackPane;
import java.util.HashSet;
import java.util.Set;

public class TerminalStack extends TabPane {

    private final Set<KeyPad> _keyPads = new HashSet<>();

    public TerminalStack() {
        // TODO temporary hard-coded terminals
        var fontInfo = new FontInfo(16);

        var settings1 = new TerminalSettings();
        var terminal1 = new Terminal("DEMAND", settings1, fontInfo);
        var tab1 = new Tab("DEMAND", new StackPane(terminal1));

        var settings2 = new TerminalSettings().setDisplayGeometry(new DisplayGeometry(36, 132));
        var terminal2 = new Terminal("TIP", settings2, fontInfo);
        var tab2 = new Tab("TIP", new StackPane(terminal2));

        var settings3 = new TerminalSettings().setDisplayGeometry(new DisplayGeometry(16, 64));
        var terminal3 = new Terminal("DORK", settings3, fontInfo);
        var tab3 = new Tab("DORK", new StackPane(terminal3));

        getTabs().addAll(tab1, tab2, tab3);
        getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
                if (oldValue != null) {
                    oldValue.setStyle("");
                }
                if (newValue != null) {
                    newValue.setStyle("-fx-background-color: lightyellow;");
                    var newActiveTerminal = getTerminal(newValue);
                    newActiveTerminal.adjustLayout();
                    _keyPads.forEach(keyPad -> keyPad.setActiveTerminal(newActiveTerminal));
                }
            }
        });

        // Initialize first tab style
        if (!getTabs().isEmpty()) {
            getTabs().getFirst().setStyle("-fx-background-color: lightyellow;");
            var term = getTerminal(getTabs().getFirst());
            Platform.runLater(term::adjustLayout);
        }
    }

    private Terminal getTerminal(Tab tab) {
        if (tab == null) {
            return null;
        }
        var content = tab.getContent();
        if (content instanceof StackPane stackPane) {
            for (var child : stackPane.getChildren()) {
                if (child instanceof Terminal terminal) {
                    return terminal;
                }
            }
        } else if (content instanceof Terminal terminal) {
            return terminal;
        }
        return null;
    }

    public void closeAll() {
        for (var tab : getTabs()) {
            var terminal = getTerminal(tab);
            if (terminal != null) {
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
            var terminal = getTerminal(tab);
            if (terminal != null && tab.isSelected()) {
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
