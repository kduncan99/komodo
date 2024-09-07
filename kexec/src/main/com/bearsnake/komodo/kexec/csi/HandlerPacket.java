/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.kexec.scheduleManager.Run;
import com.bearsnake.komodo.kexec.scheduleManager.TIPRun;

class HandlerPacket {

    final ParsedStatement _statement;
    final Run _run;
    final boolean _isTIP;
    final StatementSource _source;
    final boolean _sourceIsExecRequest;
    long _optionWord = 0;

    HandlerPacket(
        final Run run,
        final StatementSource source,
        final ParsedStatement statement
    ) {
        _run = run;
        _source = source;
        _statement = statement;
        _isTIP = run instanceof TIPRun;
        _sourceIsExecRequest = (source == StatementSource.ER_CSF) || (source == StatementSource.ER_CSI);
    }
}
