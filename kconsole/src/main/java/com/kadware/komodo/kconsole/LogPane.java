/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kconsole;

import com.kadware.komodo.commlib.SystemProcessorPoll.HardwareLogEntry;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;

@SuppressWarnings("Duplicates")
class LogPane extends ScrollPane {

    private static final int MAX_LINES = 1000;
    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("YYYYMMDD-HHmmss.SSS");
    static {
        DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final List<String> _logCache = new LinkedList<>();
    private final Label _label;

    private LogPane() {
        _label = new Label();
        _label.setStyle("-fx-font-family: Courier");
        this.setContent(_label);
    }

    static LogPane create(
        final ConsoleInfo consoleInfo
    ) {
        LogPane pane = new LogPane();
        return pane;
    }

    /**
     * more log info is available - update the display
     */
    void update(
        final HardwareLogEntry[] logEntries
    ) {
        for (HardwareLogEntry entry : logEntries) {
            Date date = new Date(entry._timestamp);
            String[] entitySplit = entry._entity.split("\\.");
            String s = String.format("%s [%s] %s",
                                     DATE_FORMATTER.format(date),
                                     entitySplit[entitySplit.length - 1],
                                     entry._message);
            _logCache.add(s);
        }

        while (_logCache.size() > MAX_LINES) {
            _logCache.remove(0);
        }

        StringBuilder content = new StringBuilder();
        for (String logString : _logCache) {
            content.append(logString);
            content.append("\n");
        }

        _label.setText(content.toString());
        setVvalue(getVmax());
    }
}
