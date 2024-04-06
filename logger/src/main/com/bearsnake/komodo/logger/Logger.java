/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.logger;

public abstract class Logger {

    protected boolean _enabled = true;
    protected Level _level = Level.Error;

    protected Logger(
        final boolean enabled,
        final Level level
    ) {
        _enabled = enabled;
        _level = level;
    }

    protected Logger(
        final Level level
    ) {
        _level = level;
    }

    protected Logger() {}

    abstract void close();
    abstract void log(final Level level, final String message);

    public final void setEnabled(final boolean enabled) {
        _enabled = enabled;
    }

    public final void setLevel(final Level level) {
        _level = level;
    }
}
