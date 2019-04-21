/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.dictionary.*;
import com.kadware.em2200.minalib.exceptions.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the assembly of a particular set of lines of source code,
 * generally accepted as representing a symbolic element or file.
 */
public class RelocatableModule {

    public final String _name;
    public final Map<Integer, LocationCounterPool> _storage = new HashMap<>();
    public final Map<String, IntegerValue> _externalLabels = new HashMap<>();

    /**
     * Constructor
     * @param name name of the module
     */
    public RelocatableModule(
        final String name,
        final Map<Integer, LocationCounterPool> storage,
        final Map<String, IntegerValue> externalLabels
    ) {
        _name = name;
        _storage.putAll( storage );
        _externalLabels.putAll( externalLabels );
    }

    /**
     * Retrieves the LocationCounterPool associated with the given index.
     * If one does not exist, it is created and returned.
     * @param index lc index
     * @return reference to LocationCounterPool
     * @throws InvalidParameterException if the index is out of range
     */
    public LocationCounterPool getLocationCounterPool(
        final int index
    ) throws InvalidParameterException {
        if ( (index < 0) || (index > 63) ) {
            throw new InvalidParameterException(String.format("Location Counter Index %d is out of range", index));
        }

        LocationCounterPool lcp = _storage.get(index);
        if (lcp == null) {
            lcp = new LocationCounterPool( new RelocatableWord36[0] );
            _storage.put( index, lcp );
        }

        return lcp;
    }
}
