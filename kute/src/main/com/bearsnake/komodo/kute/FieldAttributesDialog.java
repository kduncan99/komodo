/*
 * Copyright (c) 2025 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

import static com.bearsnake.komodo.kute.Intensity.NORMAL;

public class FieldAttributesDialog {

    private final Stage _stage;

    private FieldAttributes _attributes = null;

    private final ChoiceBox<Intensity> _intensity;
    private final ChoiceBox<UTSColor> _textColor;
    private final ChoiceBox<UTSColor> _backgroundColor;
    private final CheckBox _tabStop;
    private final CheckBox _blinking;
    private final CheckBox _reverseVideo;
    private final CheckBox _protected;
    private final CheckBox _protectedEmphasis;
    private final CheckBox _alphaOnly;
    private final CheckBox _numericOnly;
    private final CheckBox _rightJustified;

    public FieldAttributesDialog(final String caption,
                                 final Window dialog) {
        _stage = new Stage();
        _stage.initOwner(dialog);
        _stage.initModality(Modality.APPLICATION_MODAL);
        _stage.setTitle(caption);

        _intensity = new ChoiceBox<>(FXCollections.observableArrayList(Intensity.values()));
        _textColor = new ChoiceBox<>(FXCollections.observableArrayList(UTSColor.values()));
        _backgroundColor = new ChoiceBox<>(FXCollections.observableArrayList(UTSColor.values()));
        _tabStop = new CheckBox("Tab Stop");
        _blinking = new CheckBox("Blinking");
        _reverseVideo = new CheckBox("Reverse Video");
        _protected = new CheckBox("Protected");
        _protectedEmphasis = new CheckBox("Protected Emphasis");
        _alphaOnly = new CheckBox("Alpha Only");
        _numericOnly = new CheckBox("Numeric Only");
        _rightJustified = new CheckBox("Right Justified");

        _intensity.setValue(NORMAL);
        _textColor.setValue(UTSColor.GREEN);
        _backgroundColor.setValue(UTSColor.BLACK);

        _protected.setOnAction(event -> {
            if (_protected.isSelected()) {
                _protectedEmphasis.setSelected(true);
            }
        });

        _alphaOnly.setOnAction(event -> {
            _numericOnly.setSelected(false);
        });

        _numericOnly.setOnAction(event -> {
            _alphaOnly.setSelected(false);
        });

        // Main pane is a vbox with the selector pane above and the button pane below.
        var selectors = new GridPane();
        selectors.setHgap(20.0);
        selectors.setVgap(3.0);

        selectors.add(new Label("Intensity:"), 0, 0);
        selectors.add(_intensity, 0, 1);
        selectors.add(new Label("Text Color:"), 0, 2);
        selectors.add(_textColor, 0, 3);
        selectors.add(new Label("Background Color:"), 0, 4);
        selectors.add(_backgroundColor, 0, 5);
        selectors.add(_tabStop, 1, 0);
        selectors.add(_blinking, 1, 1);
        selectors.add(_reverseVideo, 1, 2);

        selectors.add(_protected, 2, 0);
        selectors.add(_protectedEmphasis, 2, 1);
        selectors.add(_alphaOnly, 2, 2);
        selectors.add(_numericOnly, 2, 3);
        selectors.add(_rightJustified, 2, 4);

        var cancelButton = new Button("Cancel");
        cancelButton.setOnAction(event -> _stage.close());

        var okButton = new Button("Ok");
        okButton.setOnAction(event -> {
            _attributes = createFieldAttributes();
            _stage.close();
        });

        var buttons = new HBox();
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setSpacing(10.0);
        buttons.getChildren().addAll(cancelButton, okButton);

        var vBox = new VBox();
        vBox.setSpacing(10.0);
        vBox.getChildren().addAll(selectors, buttons);
        vBox.setPadding(new Insets(10));
        var scene = new Scene(vBox);

        _stage.setScene(scene);
    }

    private FieldAttributes createFieldAttributes() {
        var attr = new FieldAttributes();
        attr.setIntensity(_intensity.getValue());
        attr.setTextColor(_textColor.getValue());
        attr.setBackgroundColor(_backgroundColor.getValue());
        attr.setTabStop(_tabStop.isSelected());
        attr.setBlinking(_blinking.isSelected());
        attr.setReverseVideo(_reverseVideo.isSelected());
        attr.setProtected(_protected.isSelected());
        attr.setProtectedEmphasis(_protectedEmphasis.isSelected());
        attr.setAlphabeticOnly(_alphaOnly.isSelected());
        attr.setNumericOnly(_numericOnly.isSelected());
        attr.setRightJustified(_rightJustified.isSelected());
        return attr;
    }

    public Optional<FieldAttributes> showDialog() {
        _stage.showAndWait();
        return Optional.ofNullable(_attributes);
    }
}
