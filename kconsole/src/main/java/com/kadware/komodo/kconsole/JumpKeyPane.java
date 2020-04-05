/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kconsole;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadware.komodo.commlib.SecureClient;
import com.kadware.komodo.commlib.JumpKeys;
import java.util.HashMap;
import java.util.Map;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;

@SuppressWarnings("Duplicates")
class JumpKeyPane extends GridPane {

    //  TODO Needs a good bit of layout enhancement. It works for now, but it isn't pretty
    private static class ButtonHandler implements EventHandler<ActionEvent> {

        private final ConsoleInfo _consoleInfo;
        private final int _jumpKeyId;                   //  1 to 36
        private final ToggleButton _toggleButton;

        private ButtonHandler(
            final ConsoleInfo consoleInfo,
            final ToggleButton toggleButton,
            final int jumpKeyId
        ) {
            _consoleInfo = consoleInfo;
            _jumpKeyId = jumpKeyId;
            _toggleButton = toggleButton;
        }

        @Override
        public void handle(ActionEvent event) {
            try {
                JumpKeys spjk = new JumpKeys();
                spjk._componentValues = new HashMap<>();
                spjk._componentValues.put(_jumpKeyId, _toggleButton.isSelected());
                _toggleButton.setStyle(_buttonStyles.get(_toggleButton.isSelected()));

                ObjectMapper mapper = new ObjectMapper();
                String content = mapper.writeValueAsString(spjk);
                SecureClient.ResultFromSend sendResult = _consoleInfo._secureClient.sendPut("/jumpkeys", content.getBytes());
                //TODO check sendResult for errors
            } catch (Exception ex) {
                //  do nothing
            }
        }
    }

    private final ToggleButton[] _buttons = new ToggleButton[36];
    private static final Map<Boolean, String> _buttonStyles = new HashMap<>();
    static {
//        _buttonStyles.put(false, "-fx-background-color: blue; -fx-text-fill: white");
//        _buttonStyles.put(true, "-fx-background-color: lightblue; -fx-text-fill: black");
        _buttonStyles.put(false, "-fx-base: blue");
        _buttonStyles.put(true, "-fx-base: lightblue");
    }

    static JumpKeyPane create(
        final ConsoleInfo consoleInfo
    ) {
        //TODO mouse-overs to describe the usage of the jump key?
        JumpKeyPane pane = new JumpKeyPane();
        for (int row = 0; row < 9; ++row) {
            for (int col = 0; col < 4; ++col) {
                int jx = row + (col * 9);
                int jumpKey = jx + 1;
                ToggleButton button = new ToggleButton(String.format("%d", jx + 1));
                button.setStyle(_buttonStyles.get(false));
                button.setOnAction(new ButtonHandler(consoleInfo, button, jumpKey));
                pane.add(button, col, row);
                pane._buttons[jx] = button;
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
            _buttons[jx].setSelected((value & mask) != 0);
            mask >>= 1;
        }
    }
}
