/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import java.util.LinkedList;
import java.util.List;

import static com.bearsnake.komodo.kexec.facilities.FacStatusMessage.FacStatusMessages;

public class FacStatusResult {

    private final List<FacStatusMessageInstance> _errors = new LinkedList<>();
    private final List<FacStatusMessageInstance> _infos = new LinkedList<>();
    private final List<FacStatusMessageInstance> _warnings = new LinkedList<>();
    private long _statusWord = 0;

    public long getStatusWord() { return _statusWord; }

    public boolean hasErrorMessages() { return !_errors.isEmpty(); }
    public boolean hasInfoMessages() { return !_infos.isEmpty(); }
    public boolean hasWarningMessages() { return !_warnings.isEmpty(); }

    public void mergeStatusBits(final long statusWord) {
        _statusWord |= statusWord;
    }

    public void postMessage(
        final FacStatusCode code
    ) {
        var template = FacStatusMessages.get(code);
        var inst = new FacStatusMessageInstance(code);
        switch (template.getCategory()) {
            case Error -> _errors.add(inst);
            case Info -> _infos.add(inst);
            case Warning -> _warnings.add(inst);
        }
    }

    public void postMessage(
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
    }

    public void dump() {
        _infos.stream().map(FacStatusMessageInstance::toString).forEach(System.out::println);
        _warnings.stream().map(FacStatusMessageInstance::toString).forEach(System.out::println);
        _errors.stream().map(FacStatusMessageInstance::toString).forEach(System.out::println);
    }
}
