/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities.facItems;

import java.io.PrintStream;

/**
 * A NameItem is a special FacilitiesItem - it exists for the case where an @USE was done on a qual*file
 * but the file is not assigned. Since the internal name must be resolvable but there is no concrete fac item
 * for it to resolve to, we have NameItem entities to which it resolves instead.
 */
public class NameItem extends FacilitiesItem {

    @Override
    public void dump(final PrintStream out,
                     final String indent) {
        super.dump(out, indent);
        out.printf("%s  NameItem\n", indent);
    }
}
