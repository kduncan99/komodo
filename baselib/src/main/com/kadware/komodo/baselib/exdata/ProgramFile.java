/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib.exdata;

import com.kadware.komodo.baselib.Word36;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

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
