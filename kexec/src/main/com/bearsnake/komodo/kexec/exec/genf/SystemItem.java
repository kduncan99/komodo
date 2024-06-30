/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf;

import com.bearsnake.komodo.baselib.ArraySlice;

class SystemItem extends Item {

    public SystemItem(
        final long sectorAddress
    ) {
        super(ItemType.SystemItem, sectorAddress);
        // TODO
    }

    public static SystemItem deserialize(
        final long sectorAddress,
        final ArraySlice source
    ) {
        var item = new SystemItem(sectorAddress);
        // TODO
        return item;
    }

    @Override
    public void serialize(final ArraySlice destination) {
        super.serialize(destination);
        // TODO
    }
}
