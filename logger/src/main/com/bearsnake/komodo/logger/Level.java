/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.logger;

public enum Level {
    Silent(""),
    Fatal("FATAL"),
    Error("ERROR"),
    Warning("WARN"),
    Info("INFO"),
    Debug("DEBUG"),
    Trace("TRACE"),
    All("ALL");

    public final String _text;

    Level(final String text) {
        _text = text;
    }
}
