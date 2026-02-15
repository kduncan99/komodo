/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exceptions;

import com.bearsnake.komodo.kexec.exec.Exec;

public class ExecStoppedException extends KExecException {

    public ExecStoppedException() {
        super(String.format("Exec stopped:%s", Exec.getInstance().getStopCode()));
    }
}
