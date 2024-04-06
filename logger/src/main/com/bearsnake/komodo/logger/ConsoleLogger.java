/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.logger;

public class ConsoleLogger extends Logger {

    private boolean _enabled;
    private Level _level;

    public ConsoleLogger() {
        _enabled = true;
        _level = Level.Error;
    }

    public ConsoleLogger(final Level level) {
        _enabled = true;
        _level = Level.Error;
    }

    public ConsoleLogger(final boolean enabled,
                         final Level level) {
        _enabled = enabled;
        _level = level;
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public void log(Level level, String message) {
        if (level.ordinal() <= _level.ordinal() && _enabled) {
            System.out.println(message);
        }
    }
}
