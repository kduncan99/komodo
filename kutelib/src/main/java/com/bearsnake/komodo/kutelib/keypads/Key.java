/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.keypads;

import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * A JavaFX Button with a configurable base color used as a gradient.
 * It has enabled and disabled states with corresponding gradients.
 */
public class Key extends Button {

    private final Pane _source;
    private final int _id;
    private final KeyListener _listener;
    private Color _baseColorTop;
    private Color _baseColorBottom;
    private Color _textColor;
    private boolean _isKeyPressed = false;

    /**
     * Creates a new KuteButton
     * @param text the text to display on the button
     * @param id the identifier for this button
     * @param baseColorTop the base color for the gradient when enabled
     * @param baseColorBottom the base color for the gradient when disabled
     * @param textColor the color for the text when enabled
     * @param listener the listener to notify when clicked
     */
    public Key(final String text,
               final Pane source,
               final int id,
               final Color baseColorTop,
               final Color baseColorBottom,
               final Color textColor,
               final KeyListener listener) {
        super(text);
        _source = source;
        _id = id;
        setUserData(id);
        _baseColorTop = baseColorTop;
        _baseColorBottom = baseColorBottom;
        _textColor = textColor;
        _listener = listener;

        setFocusTraversable(false);

        setOnAction(event -> {
            if (!isDisabled() && _listener != null) {
                _listener.notify(_source, _id);
            }
        });

        // Listen for disabled property changes to update the style
        disabledProperty().addListener((observable, oldValue, newValue) -> updateStyle());
        disableProperty().addListener((observable, oldValue, newValue) -> updateStyle());

        // Listen for pressed property changes (mouse/SPACE)
        pressedProperty().addListener((observable, oldValue, newValue) -> {
            updateStyle();
            if (!newValue && _listener != null) {
                // The button was released.
                // We notify released regardless of disabled state, matching the key event behavior.
                _listener.notifyReleased(_source, _id);
            }
        });

        // Intercept key events to show pressed state
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            _isKeyPressed = true;
            updateStyle();
        });
        addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            _isKeyPressed = false;
            updateStyle();
            if (_listener != null) {
                _listener.notifyReleased(_source, _id);
            }
        });

        updateStyle();
    }

    /**
     * Sets a new base color and updates the style
     * @param baseColorTop the new base color for the top of the gradient
     * @param baseColorBottom the new base color for the bottom of the gradient
     */
    public void setBaseColor(final Color baseColorTop, final Color baseColorBottom) {
        _baseColorTop = baseColorTop;
        _baseColorBottom = baseColorBottom;
        updateStyle();
    }

    /**
     * Sets a new text color and updates the style
     * @param textColor the new text color
     */
    public void setTextColor(final Color textColor) {
        _textColor = textColor;
        updateStyle();
    }

    public int getIdValue() {
        return _id;
    }

    /**
     * Converts a JavaFX Color to a web hex string
     * @param color the color to convert
     * @return the hex string (e.g., #RRGGBB)
     */
    private String toWebColor(final Color color) {
        return String.format("#%02X%02X%02X",
                             (int) (color.getRed() * 255),
                             (int) (color.getGreen() * 255),
                             (int) (color.getBlue() * 255));
    }

    /**
     * Updates the button style based on enabled/disabled state
     */
    public void updateStyle() {
        if (isDisabled()) {
            // Light-to-dark gray gradient, but preserving a hint of the base color
            Color desaturatedColor = _baseColorTop.desaturate();
            String topColor = toWebColor(desaturatedColor.deriveColor(0, 0.4, 1.2, 1.0));
            String bottomColor = toWebColor(desaturatedColor.deriveColor(0, 0.4, 0.8, 1.0));
            String textColor = "#333333";
            setStyle(String.format("-fx-background-color: linear-gradient(to bottom, %s, %s); " +
                                   "-fx-text-fill: %s; " +
                                   "-fx-border-color: black; " +
                                   "-fx-border-width: 1px; " +
                                   "-fx-alignment: center; " +
                                   "-fx-text-alignment: center;",
                                   topColor, bottomColor, textColor));
        } else if (isPressed() || _isKeyPressed) {
            // Darker gradient for pressed state
            String topColor = toWebColor(_baseColorTop.deriveColor(0, 1.0, 0.7, 1.0));
            String bottomColor = toWebColor(_baseColorBottom.deriveColor(0, 1.0, 0.9, 1.0));
            String textColor = toWebColor(_textColor);
            setStyle(String.format("-fx-background-color: linear-gradient(to bottom, %s, %s); " +
                                   "-fx-text-fill: %s; " +
                                   "-fx-border-color: black; " +
                                   "-fx-border-width: 1px; " +
                                   "-fx-alignment: center; " +
                                   "-fx-text-alignment: center;",
                                   topColor, bottomColor, textColor));
        } else {
            // Configured color gradient
            // We'll create a simple gradient: slightly lighter than base to slightly darker than base
            String topColor = toWebColor(_baseColorTop.deriveColor(0, 1.0, 1.2, 1.0));
            String bottomColor = toWebColor(_baseColorBottom.deriveColor(0, 1.0, 0.8, 1.0));
            String textColor = toWebColor(_textColor);
            setStyle(String.format("-fx-background-color: linear-gradient(to bottom, %s, %s); " +
                                   "-fx-text-fill: %s; " +
                                   "-fx-border-color: black; " +
                                   "-fx-border-width: 1px; " +
                                   "-fx-alignment: center; " +
                                   "-fx-text-alignment: center;",
                                   topColor, bottomColor, textColor));
        }
    }
}
