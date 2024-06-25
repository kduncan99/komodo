/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.kexec.exceptions.EndOfFileException;
import com.bearsnake.komodo.kexec.exceptions.ExecIOException;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;

/**
 * Represents all classes which assist the exec in handling READ$ IO for a run.
 */
public interface SymbiontReader {

    public abstract String readImage() throws ExecStoppedException, EndOfFileException, ExecIOException;
}
