/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities.facItems;

import java.io.PrintStream;

public class NameItem extends FacilitiesItem {

    @Override
    public void dump(final PrintStream out,
                     final String indent) {
        super.dump(out, indent);
        out.printf("%s  NameItem\n", indent);
    }
}
