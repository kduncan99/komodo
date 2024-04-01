/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.logger;

public abstract class Logger {
    public abstract void close();
    public abstract void log(final Level level, final String message);
}
