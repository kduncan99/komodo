/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exceptions;

import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;

public class ExecStoppedException extends KExecException {

    public ExecStoppedException(final StopCode stopCode) {
        super(String.format("Exec stopped:%s", stopCode));
    }

    public ExecStoppedException() {
        super(String.format("Exec stopped:%s", Exec.getInstance().getStopCode()));
    }
}
