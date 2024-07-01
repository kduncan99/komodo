/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf;

import com.bearsnake.komodo.baselib.ArraySlice;

/*
 * Free item sector format
 *   +000,S1    Type (00)
 */
public class FreeItem extends Item {

    public FreeItem(
        final long sectorAddress
    ) {
        super(ItemType.FreeItem, sectorAddress);
    }

    public static FreeItem deserialize(
        final long sectorAddress
    ) {
        return new FreeItem(sectorAddress);
    }

    @Override
    public void serialize(final ArraySlice destination) {
        super.serialize(destination);
    }
}
