/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.exceptions.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the assembly of a particular set of lines of source code,
 * generally accepted as representing a symbolic element or file.
 */
class RelocatableModule {

    private final String _name;
    private final Map<Integer, LocationCounterPool> _locationCounters = new HashMap<>();

    /**
     * Constructor
     * @param name name of the module
     */
    RelocatableModule(
        final String name
    ) {
        _name = name;
    }

    /**
     * Retrieves the LocationCounterPool associated with the given index.
     * If one does not exist, it is created and returned.
     * @param index lc index
     * @return reference to LocationCounterPool
     * @throws InvalidParameterException if the index is out of range
     */
    LocationCounterPool getLocationCounterPool(
        final int index
    ) throws InvalidParameterException {
        if ((index < 0) || (index > 63)) {
            throw new InvalidParameterException(String.format("Location Counter Index %d is out of range", index));
        }

        LocationCounterPool lc = _locationCounters.get(index);
        if (lc == null) {
            lc = new LocationCounterPool();
            _locationCounters.put(index, lc);
        }

        return lc;
    }
}
