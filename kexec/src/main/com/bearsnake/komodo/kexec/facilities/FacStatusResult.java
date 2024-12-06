/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.logger.Level;
import com.bearsnake.komodo.logger.LogManager;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import static com.bearsnake.komodo.kexec.facilities.FacStatusMessage.FacStatusMessages;

public class FacStatusResult {

    private final List<FacStatusMessageInstance> _errors = new LinkedList<>();
    private final List<FacStatusMessageInstance> _infos = new LinkedList<>();
    private final List<FacStatusMessageInstance> _warnings = new LinkedList<>();
    private long _statusWord = 0;

    public long getStatusWord() { return _statusWord; }
    public boolean statusWordFatal() { return Word36.isNegative(_statusWord); }

    public boolean hasErrorMessages() { return !_errors.isEmpty(); }
    public boolean hasInfoMessages() { return !_infos.isEmpty(); }
    public boolean hasWarningMessages() { return !_warnings.isEmpty(); }

    public FacStatusResult mergeStatusBits(final long statusWord) {
        _statusWord |= statusWord;
        return this;
    }

    public FacStatusResult postMessage(
        final FacStatusCode code
    ) {
        var template = FacStatusMessages.get(code);
        var inst = new FacStatusMessageInstance(code);
        switch (template.getCategory()) {
            case Error -> _errors.add(inst);
            case Info -> _infos.add(inst);
            case Warning -> _warnings.add(inst);
        }
        return this;
    }

    public FacStatusResult postMessage(
        final FacStatusCode code,
        final String[] parameters
    ) {
        var template = FacStatusMessages.get(code);
        var inst = new FacStatusMessageInstance(code, parameters);
        switch (template.getCategory()) {
        case Error -> _errors.add(inst);
        case Info -> _infos.add(inst);
        case Warning -> _warnings.add(inst);
        }
        return this;
    }

    public void log(
        final Level level,
        final String source
    ) {
        _infos.forEach(s -> LogManager.log(level, source, "FacStatus Code=%s", s.toString()));
        _warnings.forEach(s -> LogManager.log(level, source, "FacStatus Code=%s", s.toString()));
        _errors.forEach(s -> LogManager.log(level, source, "FacStatus Code=%s", s.toString()));
        LogManager.log(level, source, "%012o", _statusWord);
    }

    public void dump(
        final PrintStream out,
        final String indent
    ) {
        _infos.forEach(s -> out.printf("%s%s\n", indent, s.toString()));
        _warnings.forEach(s -> out.printf("%s%s\n", indent, s.toString()));
        _errors.forEach(s -> out.printf("%s%s\n", indent, s.toString()));
        System.out.printf("%sCode:%012o\n", indent, _statusWord);
    }

    @Override
    public String toString() {
        return String.format("%012o", _statusWord);
    }
}
