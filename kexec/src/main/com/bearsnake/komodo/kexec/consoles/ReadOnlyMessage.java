/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

import com.bearsnake.komodo.kexec.scheduleManager.Run;
import java.util.Collection;
import java.util.LinkedList;

public class ReadOnlyMessage {
    private final Run _source;
    private final ConsoleId _routing; // may be null
    private final LinkedList<ConsoleType> _consoleTypes = new LinkedList<>(); // may be empty
    private final String _runId; // for logging purposes - may not match RCE of instigator, and may be null
    private final String _text;
    private final boolean _doNotEmitRunId;

    public ReadOnlyMessage(final Run source,
                           final String runId,
                           final String text,
                           final boolean doNotEmitRunId) {
        _source = source;
        _routing = null;
        _runId = runId;
        _text = text;
        _doNotEmitRunId = doNotEmitRunId;
    }

    public ReadOnlyMessage(final Run source,
                           final ConsoleId routing,
                           final String runId,
                           final String text,
                           final boolean doNotEmitRunId) {
        _source = source;
        _routing = routing;
        _runId = runId;
        _text = text;
        _doNotEmitRunId = doNotEmitRunId;
    }

    public ReadOnlyMessage(final Run source,
                           final ConsoleType consoleType,
                           final String runId,
                           final String text,
                           final boolean doNotEmitRunId) {
        _source = source;
        _routing = null;
        _consoleTypes.add(consoleType);
        _runId = runId;
        _text = text;
        _doNotEmitRunId = doNotEmitRunId;
    }

    public ReadOnlyMessage(final Run source,
                           final Collection<ConsoleType> consoleTypes,
                           final String runId,
                           final String text,
                           final boolean doNotEmitRunId) {
        _source = source;
        _routing = null;
        _consoleTypes.addAll(consoleTypes);
        _runId = runId;
        _text = text;
        _doNotEmitRunId = doNotEmitRunId;
    }

    public final boolean doNotEmitRunId() { return _doNotEmitRunId; }
    public final Collection<ConsoleType> getConsoleTypes() { return _consoleTypes; }
    public final ConsoleId getRouting() { return _routing; }
    public final String getRunId() { return _runId; }
    public final Run getSource() { return _source; }
    public final String getText() { return _text; }
}
