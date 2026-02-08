/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.network;

import com.bearsnake.komodo.kutelib.exceptions.BufferOverflowException;
import com.bearsnake.komodo.netlib.SocketTrace;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import static com.bearsnake.komodo.kutelib.Constants.*;

public class TraceViewer extends Stage {

    private final SocketTrace _trace;
    private final TableView<SocketTrace.Entry> _tableView;
    private final CheckBox _hex;
    private final CheckBox _primitives;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final Font MONOSPACED_FONT = Font.font("Courier New", FontWeight.BOLD, 13);
    private static final Color HEX_COLOR = Color.BLACK;
    private static final Color PRIMITIVE_COLOR = Color.RED;
    private static final Color TEXT_COLOR = Color.BLUE;
    private static final Color TOKEN_COLOR = Color.GREEN;

    private static final HashMap<Byte, Character> CHARACTER_LOOKUP = new HashMap<>();
    static {
        CHARACTER_LOOKUP.put(ASCII_SOE, '▷');
        CHARACTER_LOOKUP.put(ASCII_HT, '⇥');
        CHARACTER_LOOKUP.put(ASCII_LF, '↓');
        CHARACTER_LOOKUP.put(ASCII_FF, '↖');
        CHARACTER_LOOKUP.put(ASCII_CR, '↲');
        CHARACTER_LOOKUP.put(ASCII_DEL, '░');
        CHARACTER_LOOKUP.put(ASCII_SP, '∙');
    }

    private static final HashMap<Byte, String> CONTROL_TOKEN_LOOKUP = new HashMap<>();
    static {
        CONTROL_TOKEN_LOOKUP.put(ASCII_NUL, "NUL");
        CONTROL_TOKEN_LOOKUP.put(ASCII_SOH, "SOH");
        CONTROL_TOKEN_LOOKUP.put(ASCII_STX, "STX");
        CONTROL_TOKEN_LOOKUP.put(ASCII_ETX, "ETX");
        CONTROL_TOKEN_LOOKUP.put(ASCII_EOT, "EOT");
        CONTROL_TOKEN_LOOKUP.put(ASCII_ENQ, "ENQ");
        CONTROL_TOKEN_LOOKUP.put(ASCII_ACK, "ACK");
        CONTROL_TOKEN_LOOKUP.put(ASCII_BEL, "BEL");
        CONTROL_TOKEN_LOOKUP.put(ASCII_BS, "BS");
        CONTROL_TOKEN_LOOKUP.put(ASCII_HT, "HT");
        CONTROL_TOKEN_LOOKUP.put(ASCII_LF, "LF");
        CONTROL_TOKEN_LOOKUP.put(ASCII_VT, "VT");
        CONTROL_TOKEN_LOOKUP.put(ASCII_FF, "FF");
        CONTROL_TOKEN_LOOKUP.put(ASCII_CR, "CR");
        CONTROL_TOKEN_LOOKUP.put(ASCII_SO, "SO");
        CONTROL_TOKEN_LOOKUP.put(ASCII_SI, "SI");
        CONTROL_TOKEN_LOOKUP.put(ASCII_DLE, "DLE");
        CONTROL_TOKEN_LOOKUP.put(ASCII_DC1, "DC1");
        CONTROL_TOKEN_LOOKUP.put(ASCII_DC2, "DC2");
        CONTROL_TOKEN_LOOKUP.put(ASCII_DC3, "DC3");
        CONTROL_TOKEN_LOOKUP.put(ASCII_DC4, "DC4");
        CONTROL_TOKEN_LOOKUP.put(ASCII_NAK, "NAK");
        CONTROL_TOKEN_LOOKUP.put(ASCII_SYN, "SYN");
        CONTROL_TOKEN_LOOKUP.put(ASCII_ETB, "ETB");
        CONTROL_TOKEN_LOOKUP.put(ASCII_CAN, "CAN");
        CONTROL_TOKEN_LOOKUP.put(ASCII_EM, "EM");
        CONTROL_TOKEN_LOOKUP.put(ASCII_SUB, "SUB");
        CONTROL_TOKEN_LOOKUP.put(ASCII_ESC, "ESC");
        CONTROL_TOKEN_LOOKUP.put(ASCII_FS, "FS");
        CONTROL_TOKEN_LOOKUP.put(ASCII_GS, "GS");
        CONTROL_TOKEN_LOOKUP.put(ASCII_RS, "RS");
        CONTROL_TOKEN_LOOKUP.put(ASCII_US, "US");
        CONTROL_TOKEN_LOOKUP.put(ASCII_DEL, "DEL");
    }

    public TraceViewer(final SocketTrace trace,
                       final String titleSuffix) {
        _trace = trace;
        setTitle("Trace Viewer - " + titleSuffix);

        _hex = new CheckBox("Hex");
        _primitives = new CheckBox("Primitives");
        Button saveButton = new Button("Save...");
        saveButton.setOnAction(e -> handleSave());

        ToolBar toolBar = new ToolBar(_hex, _primitives, saveButton);

        _tableView = new TableView<>();
        _tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Make column headers non-clickable by disabling sorting and ensuring sort order is empty
        _tableView.getSortOrder().clear();

        TableColumn<SocketTrace.Entry, String> timestampCol = new TableColumn<>("Timestamp");
        timestampCol.setCellValueFactory(data -> new SimpleStringProperty(DATE_FORMAT.format(new Date(data.getValue().getTimestamp()))));
        timestampCol.setPrefWidth(25 * 8);
        timestampCol.setMaxWidth(25 * 8);
        timestampCol.setResizable(true);
        timestampCol.setSortable(false);
        timestampCol.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setFont(MONOSPACED_FONT);
                }
            }
        });

        TableColumn<SocketTrace.Entry, String> sourceCol = new TableColumn<>("Source");
        sourceCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSource().toString()));
        sourceCol.setPrefWidth(10 * 8);
        sourceCol.setMaxWidth(10 * 8);
        sourceCol.setResizable(true);
        sourceCol.setSortable(false);
        sourceCol.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setFont(MONOSPACED_FONT);
                }
            }
        });

        TableColumn<SocketTrace.Entry, SocketTrace.Entry> dataCol = new TableColumn<>("Data");
        dataCol.setSortable(false);
        dataCol.setMinWidth(200);
        dataCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));

        // Use a custom cell factory to allow multi-line text and rich text via TextFlow
        dataCol.setCellFactory(tc -> {
            return new TableCell<SocketTrace.Entry, SocketTrace.Entry>() {
                @Override
                protected void updateItem(SocketTrace.Entry item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        TextFlow textFlow = new TextFlow();
                        textFlow.setTextAlignment(javafx.scene.text.TextAlignment.LEFT);
                        textFlow.prefWidthProperty().bind(getTableColumn().widthProperty().subtract(20));
                        populateTextFlow(textFlow, item.getData());
                        setGraphic(textFlow);
                        setPrefHeight(textFlow.prefHeight(dataCol.getWidth()) + 4);
                        dataCol.widthProperty().addListener((obs, oldVal, newVal) ->
                                                                setPrefHeight(textFlow.prefHeight(newVal.doubleValue()) + 4));
                    }
                }
            };
        });

        _hex.selectedProperty().addListener((obs, oldVal, newVal) -> _tableView.refresh());
        _primitives.selectedProperty().addListener((obs, oldVal, newVal) -> _tableView.refresh());

        _tableView.getColumns().add(timestampCol);
        _tableView.getColumns().add(sourceCol);
        _tableView.getColumns().add(dataCol);
        _tableView.getItems().addAll(_trace.getEntries());

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

    private void populateTextFlow(TextFlow textFlow, byte[] bytes) {
        var buffer = new UTSByteBuffer(bytes);
        var prevChar = false;
        while (!buffer.atEnd()) {
            try {
                if (_primitives.isSelected()) {
                    var prim = buffer.getPrimitive();
                    if (prim != null) {
                        var text = new Text((prevChar ? " " : "") + prim + " ");
                        text.setFont(MONOSPACED_FONT);
                        text.setFill(PRIMITIVE_COLOR);
                        textFlow.getChildren()
                                .add(text);
                        prevChar = false;
                        continue;
                    }
                }

                if (_hex.isSelected()) {
                    var text = new Text(String.format("%s%02X ", (prevChar ? " " : ""), buffer.getNext()));
                    text.setFont(MONOSPACED_FONT);
                    text.setFill(HEX_COLOR);
                    textFlow.getChildren()
                            .add(text);
                    prevChar = false;
                    continue;
                }

                var ch = buffer.getNext();
                if (CONTROL_TOKEN_LOOKUP.containsKey(ch)) {
                    var text = new Text((prevChar ? " " : "") + CONTROL_TOKEN_LOOKUP.get(ch) + " ");
                    text.setFont(MONOSPACED_FONT);
                    text.setFill(TOKEN_COLOR);
                    textFlow.getChildren()
                            .add(text);
                    prevChar = false;
                } else if (CHARACTER_LOOKUP.containsKey(ch)) {
                    var text = new Text(String.format("%c", CHARACTER_LOOKUP.get(ch)));
                    text.setFont(MONOSPACED_FONT);
                    text.setFill(TEXT_COLOR);
                    textFlow.getChildren()
                            .add(text);
                    prevChar = true;
                } else {
                    var text = new Text(String.format("%c", ch));
                    text.setFont(MONOSPACED_FONT);
                    text.setFill(TEXT_COLOR);
                    textFlow.getChildren()
                            .add(text);
                    prevChar = true;
                }
            } catch (BufferOverflowException ex) {
                // nothing to do
            }
        }
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

}
