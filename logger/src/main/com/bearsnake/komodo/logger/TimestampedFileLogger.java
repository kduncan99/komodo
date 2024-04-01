/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.logger;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

public class TimestampedFileLogger extends FileLogger {

    private boolean _enabled;
    private Level _level;
    private PrintStream _printer;
    private static final SimpleDateFormat _dateTimeFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");

    public TimestampedFileLogger(final String fileNamePrefix) {
        super(generateFileName(fileNamePrefix));
    }

    public TimestampedFileLogger(final String fileNamePrefix,
                                 final Level level) {
        super(generateFileName(fileNamePrefix), level);
    }

    public TimestampedFileLogger(final String fileNamePrefix,
                                 final boolean enabled,
                                 final Level level) {
        super(generateFileName(fileNamePrefix), enabled, level);
    }

    private static String generateFileName(final String fileNamePrefix) {
        var tmStamp = LocalDateTime.now();
        var dtStr = _dateTimeFormat.format(tmStamp);
        return String.format("%s-%s.log", fileNamePrefix, dtStr);
    }

    @Override
    public void close() {
        _printer.close();
        _printer = null;
    }

    @Override
    public void log(Level level, String message) {
        if (_printer != null && level.ordinal() <= _level.ordinal() && _enabled) {
            _printer.println(message);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        _enabled = enabled;
    }

    @Override
    public void setLevel(Level level) {
        _level = level;
    }
}
