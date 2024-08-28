/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;

import java.io.PrintStream;
import java.util.stream.IntStream;

public abstract class Item {

    private final ItemType _itemType;
    private final long _sectorAddress;
    private boolean _dirty;

    protected Item(
        final ItemType itemType,
        final long sectorAddress
    ) {
        _itemType = itemType;
        _sectorAddress = sectorAddress;
        _dirty = false;
    }

    public void dump(final PrintStream out, final String indent) {
        out.printf("%s%06o:%s%s\n", indent, getSectorAddress(), getItemType(), isDirty() ? " (dirty)" : "");
    }

    public final ItemType getItemType() { return _itemType; }
    public final long getSectorAddress() { return _sectorAddress;}
    public final boolean isDirty() { return _dirty; }

    public final void setIsDirty(final boolean flag) { _dirty = flag; }

    protected void serialize(final ArraySlice destination) throws ExecStoppedException {
        IntStream.range(0, 033).forEach(wx -> destination.set(wx, 0));
        destination.setS1(0, _itemType.getCode());
    }
}
