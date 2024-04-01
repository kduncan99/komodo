/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.logger;

import java.io.PrintStream;

public class FileLogger implements Logger {

    private boolean _enabled;
    private Level _level;
    private PrintStream _printer;

    public FileLogger(final String fileName) {
        _enabled = true;
        _level = Level.Error;
    }

    public FileLogger(final String fileName,
                      final Level level) {
        _enabled = true;
        _level = Level.Error;
    }

    public FileLogger(final String fileName,
                      final boolean enabled,
                      final Level level) {
        _enabled = enabled;
        _level = level;
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
