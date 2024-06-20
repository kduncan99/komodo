/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.kexec.exceptions.EndOfFileException;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;

/**
 * Represents all classes which assist the exec in handling PRINT$ or PUNCH$ IO for a run.
 */
public interface SymbiontWriter {

    public abstract void setCurrentCharacterSet(
        final int characterSet
    ) throws ExecStoppedException;

    public abstract void writeControlImage(
        final String image
    ) throws ExecStoppedException, EndOfFileException;

    public abstract void writeImage(
        final String image
    ) throws ExecStoppedException, EndOfFileException;

    public abstract void writeImage(
        final int spacing,
        final String image
    ) throws ExecStoppedException, EndOfFileException;
}
