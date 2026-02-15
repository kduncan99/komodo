/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import java.io.PrintStream;

public interface MediaInfo {

    void dump(final PrintStream out, final String indent);
    String getMediaName();
    String toString();
}
