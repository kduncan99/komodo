/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf;

import com.bearsnake.komodo.baselib.ArraySlice;

public class OutputQueueItem extends Item {

    public OutputQueueItem(
        final long sectorAddress
    ) {
        super(ItemType.OutputQueueItem, sectorAddress);
        // TODO
    }

    public static OutputQueueItem deserialize(
        final long sectorAddress,
        final ArraySlice source
    ) {
        var item = new OutputQueueItem(sectorAddress);
        // TODO
        return item;
    }

    @Override
    public void serialize(final ArraySlice destination) {
        super.serialize(destination);
        // TODO
    }
}
