/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import java.io.PrintStream;

/*
 * Free item sector format
 *   +000,S1    Type (00)
 */
public class FreeItem extends Item {

    public FreeItem(
        final int sectorAddress
    ) {
        super(ItemType.FreeItem, sectorAddress);
    }

    public static FreeItem deserialize(
        final int sectorAddress
    ) {
        return new FreeItem(sectorAddress);
    }

    @Override
    public void dump(PrintStream out, String indent) {
        super.dump(out, indent);
    }

    @Override
    public void serialize(final ArraySlice destination) throws ExecStoppedException {
        super.serialize(destination);
    }
}
