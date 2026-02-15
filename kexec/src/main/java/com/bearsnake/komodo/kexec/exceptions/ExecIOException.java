/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exceptions;

import com.bearsnake.komodo.kexec.exec.ERIO$Status;

/**
 * Thrown in the case that an Exec IO fails, but we might not want to stop the exec over it.
 */
public class ExecIOException extends KExecException {

    private final ERIO$Status _ioStatus;

    public ExecIOException(final ERIO$Status ioStatus) {
        super(String.format("IO Error %03o", ioStatus.getCode()));
        _ioStatus = ioStatus;
    }

    public ERIO$Status getIoStatus() { return _ioStatus; }
}
