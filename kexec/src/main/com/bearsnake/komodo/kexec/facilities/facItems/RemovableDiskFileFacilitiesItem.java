/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities.facItems;

import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class RemovableDiskFileFacilitiesItem extends DiskFileFacilitiesItem {

    // Names of any packs we are waiting for.
    // Note that a multi-activity program may have activities waiting on different portions of the file,
    // which would entail requesting pack load for more than one pack at a time.
    // Not sure how this works in practice, but we'll implement it this way for now.
    private ConcurrentHashMap<String, Object> _packNames;

    @Override
    public void dump(final PrintStream out,
                     final String indent) {
        super.dump(out, indent);
        if (!_packNames.isEmpty()) {
            out.printf("%s    Waiting for packs:%s\n", indent, String.join(" ", _packNames.keySet()));
        }
    }

    public void addPackName(final String packName) { _packNames.put(packName, this); }
    public Collection<String> getPackNames() { return new LinkedList<>(_packNames.keySet()); }
    public boolean removePackName(final String packName) { return _packNames.remove(packName, this); }

    @Override
    public boolean isWaiting() {
        return super.isWaiting() | !_packNames.isEmpty();
    }
}
