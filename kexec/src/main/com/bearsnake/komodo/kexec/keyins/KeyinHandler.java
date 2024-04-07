/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import java.time.LocalDateTime;

public abstract class KeyinHandler implements Runnable {

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
    public abstract boolean isAllowed();
    public boolean isDone() { return _timeFinished != null; }

    protected void setFinished() {
        _timeFinished = LocalDateTime.now();
        _timeToPrune = _timeFinished.plusMinutes(5);
    }

    @Override
    public synchronized String toString() {
        var sb = new StringBuilder();
        sb.append(getCommand());
        if (_options != null) {
            sb.append(",").append(_options);
        }
        if (_arguments != null) {
            sb.append(" ").append(_arguments);
        }
        if (_timeFinished != null) {
            sb.append(" (FINISHED)");
        }
        return sb.toString();
    }
}
