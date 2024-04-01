/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import java.io.PrintStream;

public interface Manager {
    // Invoked for all managers when the exec boots
    void boot() throws Exception;

    void dump(PrintStream out, String indent);

    // Invoked for all managers when the exec is instantiated (presumably when the application starts)
    void initialize() throws Exception;

    // Invoked for all managers when the exec stops
    void stop() throws Exception;
}
