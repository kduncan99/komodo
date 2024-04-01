/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.logger;

public interface Logger {
    void close();
    void log(final Level level, final String message);
    void setEnabled(final boolean enabled);
    void setLevel(final Level level);
}
