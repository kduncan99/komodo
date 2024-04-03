/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import java.time.LocalDateTime;
import java.time.chrono.MinguoChronology;
import java.time.temporal.ChronoUnit;

public abstract class KeyinHandler {

    protected final String _arguments;
    protected final String _options;
    protected final ConsoleId _source;
    protected LocalDateTime _timeFinished;
    protected LocalDateTime _timeToPrune;

    public KeyinHandler(final ConsoleId source,
                        final String options,
                        final String arguments) {
        _source = source;
        _options = options;
        _arguments = arguments;
    }

    public abstract void abort();
    public abstract boolean checkSyntax();
    public String getArguments() { return _arguments; }
    public abstract String getCommand();
    public String getOptions() { return _options; }
    public abstract String[] getHelp();
    public LocalDateTime getTimeFinished() { return _timeFinished; }
    public abstract void invoke();
    public abstract boolean isAllowed();
    public boolean isDone() { return _timeFinished != null; }

    protected void setFinished() {
        _timeFinished = LocalDateTime.now();
        _timeToPrune = _timeFinished.plusMinutes(5);
    }
}
