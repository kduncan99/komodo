/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex;

import java.io.File;
import java.io.IOException;

/**
 * Represents a program file stored natively in the host system.
 */
public class ProgramFile extends SparseDataFile {

    public ProgramFile (
        final File file
    ) {
        super(file);
    }

    public ProgramFile (
        final String fileName
    ) {
        super(fileName);
    }

    //  ----------------------------------------------------------------------------------------------------------------------------

    public void open(
    ) throws IOException {
        super.open();
    }
}
