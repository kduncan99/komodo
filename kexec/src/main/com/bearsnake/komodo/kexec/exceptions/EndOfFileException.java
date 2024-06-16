/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exceptions;

/**
 * Thrown when we expect to read something (perhaps from a disk file?) but there is nothing left to read
 */
public class EndOfFileException extends KExecException {

    public EndOfFileException() {}
}
