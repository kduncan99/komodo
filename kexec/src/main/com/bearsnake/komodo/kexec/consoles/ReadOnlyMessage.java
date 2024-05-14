/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

import com.bearsnake.komodo.kexec.exec.RunControlEntry;

public class ReadOnlyMessage {
    private final RunControlEntry _source;
    private final ConsoleId _routing; // may be null
    private final ConsoleType _consoleType; // may be null
    private final String _runId;   // for logging purposes - may not match RCE of instigator, and may be null
    private final String _text;
    private final boolean _doNotEmitRunId;

    public ReadOnlyMessage(final RunControlEntry source,
                           final ConsoleId routing,
                           final ConsoleType consoleType,
                           final String runId,
                           final String text,
                           final boolean doNotEmitRunId) {
        _source = source;
        _routing = routing;
        _consoleType = consoleType;
        _runId = runId;
        _text = text;
        _doNotEmitRunId = doNotEmitRunId;
    }

    public final boolean doNotEmitRunId() { return _doNotEmitRunId; }
    public final ConsoleType getConsoleType() { return _consoleType; }
    public final ConsoleId getRouting() { return _routing; }
    public final String getRunId() { return _runId; }
    public final RunControlEntry getSource() { return _source; }
    public final String getText() { return _text; }
}
