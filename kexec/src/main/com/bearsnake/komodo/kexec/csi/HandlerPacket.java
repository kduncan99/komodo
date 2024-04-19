/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.kexec.exec.RunControlEntry;
import com.bearsnake.komodo.kexec.exec.TIPRunControlEntry;

class HandlerPacket {

    final ParsedStatement _statement;
    final RunControlEntry _runControlEntry;
    final boolean _isTIP;
    final StatementSource _source;
    final boolean _sourceIsExecRequest;
    long _optionWord = 0;

    HandlerPacket(
        final RunControlEntry runControlEntry,
        final StatementSource source,
        final ParsedStatement statement
    ) {
        _runControlEntry = runControlEntry;
        _source = source;
        _statement = statement;
        _isTIP = runControlEntry instanceof TIPRunControlEntry;
        _sourceIsExecRequest = (source == StatementSource.ER_CSF) || (source == StatementSource.ER_CSI);
    }
}
