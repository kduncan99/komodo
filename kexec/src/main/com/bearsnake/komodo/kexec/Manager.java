/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import com.bearsnake.komodo.kexec.exceptions.KExecException;
import java.io.PrintStream;

// A manager is any entity which needs to know about initialization, exec boot/stop,
// or which needs to be dump()ed whenever the exec is dump()ed.
public interface Manager {
    // Invoked for all managers when the exec boots
    void boot() throws KExecException;

    void dump(PrintStream out, String indent, boolean verbose);

    // Invoked for all managers when the exec is instantiated (presumably when the application starts)
    void initialize() throws KExecException;

    // Invoked for all managers when the exec stops
    void stop();
}
