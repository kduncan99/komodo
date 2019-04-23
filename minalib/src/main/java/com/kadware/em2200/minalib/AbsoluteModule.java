/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.exceptions.*;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a fully-linked module which is ready to be loaded and executed.
 */
public class AbsoluteModule {

    public final String _name;
    public final Map<Integer, LoadableBank> _loadableBanks = new TreeMap<>();

    /**
     * Constructor
     * @param name name of the module
     */
    public AbsoluteModule(
        final String name,
        final LoadableBank[] loadableBanks
    ) throws InvalidParameterException {
        _name = name;
        for (LoadableBank lb : loadableBanks) {
            if (_loadableBanks.containsKey(lb._bankDescriptorIndex)) {
                throw new InvalidParameterException(String.format("BDI %07o specified more than once", lb._bankDescriptorIndex));
            }
            _loadableBanks.put(lb._bankDescriptorIndex, lb);
        }
    }
}
