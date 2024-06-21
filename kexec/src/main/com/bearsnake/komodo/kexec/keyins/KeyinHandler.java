/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.logger.LogManager;

import java.time.LocalDateTime;

public abstract class KeyinHandler implements Runnable {

    protected final String _arguments;
    protected final String _options;
    protected final ConsoleId _source;
    protected LocalDateTime _timeFinished;
    protected LocalDateTime _timeToPrune;

    KeyinHandler(final ConsoleId source,
                 final String options,
                 final String arguments) {
        _source = source;
        _options = options;
        _arguments = arguments;
    }

    abstract boolean checkSyntax();
    String getArguments() { return _arguments; }
    abstract String getCommand();
    String getOptions() { return _options; }
    abstract String[] getHelp();
    LocalDateTime getTimeFinished() { return _timeFinished; }
    abstract boolean isAllowed();
    boolean isDone() { return _timeFinished != null; }

    abstract void process() throws ExecStoppedException;

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

    @Override
    public final void run() {
        LogManager.logTrace(getCommand(), "starting");
        try {
            process();
            _timeFinished = LocalDateTime.now();
            _timeToPrune = _timeFinished.plusMinutes(5);
            LogManager.logTrace(getCommand(), "stopped");
        } catch (Throwable t) {
            LogManager.logCatching(getCommand(), t);
            Exec.getInstance().stop(StopCode.ExecContingencyHandler);
            _timeFinished = LocalDateTime.now();
            LogManager.logTrace(getCommand(), "aborted");
        }
    }
}
