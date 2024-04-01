/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exceptions;

import com.bearsnake.komodo.kexec.exec.Exec;

public class ExecStoppedException extends Exception {

    public ExecStoppedException() {
        super(String.format("Exec stopped:%s", Exec.getInstance().getStopCode()));
    }
}
