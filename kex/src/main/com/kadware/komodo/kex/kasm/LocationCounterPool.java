/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.kex.RelocatableModule;
import java.util.LinkedList;
import java.util.List;

public class LocationCounterPool {

    private final int _index ;
    private boolean _requiresExtendedMode = false;
    private final List<RelocatableModule.RelocatableWord> _relocatableWords = new LinkedList<>();

    LocationCounterPool(
        final int locationCounterIndex
    ) {
        _index = locationCounterIndex;
    }

    public void appendRelocatableWord(
        final RelocatableModule.RelocatableWord rw
    ) {
        _relocatableWords.add(rw);
    }

    public RelocatableModule.RelocatableWord[] getContent() {
        return _relocatableWords.toArray(new RelocatableModule.RelocatableWord[0]);
    }

    public boolean requiresExtendedMode() {
        return _requiresExtendedMode;
    }
}
