/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exceptions;

public class ResourceException extends KExecException {

    public ResourceException(final String message) {
        super(String.format("Resource Exception:%s", message));
    }
}
