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
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;

import java.util.HashSet;
import java.util.Set;

public class TerminalStack extends TabPane {

    private final Set<KeyPad> _keyPads = new HashSet<>();
    private final ChangeListener<Boolean> _keyboardLockedListener = (observable, oldValue, newValue) -> {
        _keyPads.forEach(KeyPad::refreshButtons);
    };
    private final ChangeListener<Boolean> _connectedListener = (observable, oldValue, newValue) -> {
        _keyPads.forEach(KeyPad::refreshButtons);
    };
    private final ChangeListener<Boolean> _traceStateListener = (observable, oldValue, newValue) -> {
        _keyPads.forEach(KeyPad::refreshButtons);
    };

    public TerminalStack() {
        // TODO temporary hard-coded terminals
        var fontInfo = new FontInfo(16);

        var settings1 = new TerminalSettings();
        var terminal1 = new Terminal("DEMAND", settings1, fontInfo);
        var tab1 = new Tab("DEMAND", terminal1);//new StackPane(terminal1));

        var settings2 = new TerminalSettings().setDisplayGeometry(new DisplayGeometry(36, 132));
        var terminal2 = new Terminal("TIP", settings2, fontInfo);
        var tab2 = new Tab("TIP", terminal2);

        var settings3 = new TerminalSettings().setDisplayGeometry(new DisplayGeometry(16, 64));
        var terminal3 = new Terminal("DORK", settings3, fontInfo);
        var tab3 = new Tab("DORK", terminal3);

        getTabs().addAll(tab1, tab2, tab3);
        getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
                if (oldValue != null) {
                    oldValue.setStyle("");
                    var oldTerminal = getTerminal(oldValue);
                    if (oldTerminal != null) {
                        var lockProp = oldTerminal.keyboardLockedProperty();
                        if (lockProp != null) {
                            lockProp.removeListener(_keyboardLockedListener);
                        }
                        var connProp = oldTerminal.connectedProperty();
                        if (connProp != null) {
                            connProp.removeListener(_connectedListener);
                        }
                        var traceProp = oldTerminal.traceStateProperty();
                        if (traceProp != null) {
                            traceProp.removeListener(_traceStateListener);
                        }
                    }
                }
                if (newValue != null) {
                    newValue.setStyle("-fx-background-color: lightyellow;");
                    var newActiveTerminal = getTerminal(newValue);
                    newActiveTerminal.adjustLayout();
                    newActiveTerminal.keyboardLockedProperty().addListener(_keyboardLockedListener);
                    newActiveTerminal.connectedProperty().addListener(_connectedListener);
                    newActiveTerminal.traceStateProperty().addListener(_traceStateListener);
                    _keyPads.forEach(keyPad -> keyPad.setActiveTerminal(newActiveTerminal));
                }
            }
        });

        getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Platform.runLater(() -> {
                    newValue.getContent().requestFocus();
                });
            }
        });

        // Initialize first tab style
        if (!getTabs().isEmpty()) {
            getTabs().getFirst().setStyle("-fx-background-color: lightyellow;");
            var term = getTerminal(getTabs().getFirst());
            term.keyboardLockedProperty().addListener(_keyboardLockedListener);
            term.connectedProperty().addListener(_connectedListener);
            term.traceStateProperty().addListener(_traceStateListener);
            Platform.runLater(() -> {
                term.adjustLayout();
                term.requestFocus();
            });
        }

        addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);
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

    /**
     * Handles certain command (meta) keystrokes
     * @param event key typed event
     */
    private void handleKeyTyped(final KeyEvent event) {
        if (event.isMetaDown()) {
            switch (event.getCharacter().charAt(0)) {
                case 'b' -> /* meta b */ {
                    getActiveTerminal().cycleBackgroundColor();
                    event.consume();
                }
                case 'c' -> /* meta c */ {
                    getActiveTerminal().cycleTextColor();
                    event.consume();
                }
                case 't' -> /* meta t */ {
                    cycleTabs();
                    event.consume();
                }
            }
        }
    }

    public void registerKeyPad(final KeyPad keyPad) {
        _keyPads.add(keyPad);
        keyPad.setActiveTerminal(getActiveTerminal());
    }
}
