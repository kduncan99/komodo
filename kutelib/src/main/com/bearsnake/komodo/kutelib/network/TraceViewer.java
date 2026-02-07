/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.network;

import com.bearsnake.komodo.netlib.SocketTrace;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TraceViewer extends Stage {

    private final SocketTrace _trace;
    private final TableView<SocketTrace.Entry> _tableView;
    private final CheckBox _interpretPrimitivesCheckBox;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public TraceViewer(final SocketTrace trace,
                       final String titleSuffix) {
        _trace = trace;
        setTitle("Trace Viewer - " + titleSuffix);

        _interpretPrimitivesCheckBox = new CheckBox("Interpret Primitives");
        Button saveButton = new Button("Save...");
        saveButton.setOnAction(e -> handleSave());

        ToolBar toolBar = new ToolBar(_interpretPrimitivesCheckBox, saveButton);

        _tableView = new TableView<>();
        _tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<SocketTrace.Entry, String> timestampCol = new TableColumn<>("Timestamp");
        timestampCol.setCellValueFactory(data -> new SimpleStringProperty(DATE_FORMAT.format(new Date(data.getValue().getTimestamp()))));
        timestampCol.setPrefWidth(150);
        timestampCol.setResizable(false);

        TableColumn<SocketTrace.Entry, String> sourceCol = new TableColumn<>("Source");
        sourceCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSource().toString()));
        sourceCol.setPrefWidth(80);
        sourceCol.setResizable(false);

        TableColumn<SocketTrace.Entry, String> dataCol = new TableColumn<>("Data");
        dataCol.setCellValueFactory(data -> {
            byte[] bytes = data.getValue().getData();
            if (_interpretPrimitivesCheckBox.isSelected()) {
                return new SimpleStringProperty(interpretData(bytes));
            } else {
                return new SimpleStringProperty(bytesToHex(bytes));
            }
        });

        // Use a custom cell factory to allow multi-line text
        dataCol.setCellFactory(tc -> {
            TableCell<SocketTrace.Entry, String> cell = new TableCell<>() {
                private final Label label = new Label();
                {
                    label.setWrapText(true);
                    label.setMaxWidth(Double.MAX_VALUE);
                    setGraphic(label);
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        label.setText(null);
                    } else {
                        label.setText(item);
                        label.prefWidthProperty().bind(tc.widthProperty().subtract(10));
                    }
                }
            };
            return cell;
        });

        _tableView.getColumns().addAll(timestampCol, sourceCol, dataCol);
        _tableView.getItems().addAll(_trace.getEntries());

        _interpretPrimitivesCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> _tableView.refresh());

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(_tableView);

        Scene scene = new Scene(root, 800, 600);
        setScene(scene);

        // Shutdown when parent program terminates is handled by being a Stage
        // but if we want it to close when the main application closes,
        // we usually depend on how it's launched.
        // The requirement says "shuts down when the parent program terminates".
        // In JavaFX, if we don't call Platform.setImplicitExit(false),
        // the app exits when the last stage is closed.
        // If the main app exits, all stages are closed by the JVM.
    }

    private void handleSave() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Trace");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showSaveDialog(this);
        if (file != null) {
            try {
                _trace.saveToFile(file);
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save trace: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private String interpretData(byte[] bytes) {
        // Simple interpretation for now, can be expanded
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if (b >= 32 && b < 127) {
                sb.append((char) b);
            } else {
                sb.append(String.format("<%02X>", b));
            }
        }
        return sb.toString();
    }
}
