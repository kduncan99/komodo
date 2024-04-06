/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.logger;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class FileLogger extends Logger {

    private PrintStream _printer;

    public FileLogger(final String fileName) throws FileNotFoundException {
        super();
        _printer = new PrintStream(fileName);
    }

    public FileLogger(final String fileName,
                      final Level level) throws FileNotFoundException {
        super(level);
        _printer = new PrintStream(fileName);
    }

    public FileLogger(final String fileName,
                      final boolean enabled,
                      final Level level) throws FileNotFoundException {
        super(enabled, level);
        _printer = new PrintStream(fileName);
    }

    @Override
    public final void close() {
        _printer.close();
        _printer = null;
    }

    @Override
    public final void log(Level level, String message) {
        if (_printer != null && level.ordinal() <= _level.ordinal() && _enabled) {
            _printer.println(message);
        }
    }
}
