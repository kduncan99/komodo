/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import java.util.HashMap;
import java.util.Map;
import com.kadware.em2200.minalib.exceptions.*;

/**
 * Represents the assembly of a particular set of lines of source code,
 * generally accepted as representing a symbolic element or file.
 */
public class RelocatableModule {

    private final String _name;
    private final Map<Integer, LocationCounter> _locationCounters = new HashMap<>();

    /**
     * Constructor
     * <p>
     * @param name
     */
    public RelocatableModule(
        final String name
    ) {
        _name = name;
    }

    /**
     * Retrieves the LocationCounter associated with the given index.
     * If one does not exist, it is created and returned.
     * <p>
     * @param index
     * <p>
     * @return
     * <p>
     * @throws InvalidParameterException
     */
    public LocationCounter getLocationCounter(
        final int index
    ) throws InvalidParameterException {
        if ((index < 0) || (index > 63)) {
            throw new InvalidParameterException(String.format("Location Counter Index %d is out of range", index));
        }

        LocationCounter lc = _locationCounters.get(index);
        if (lc == null) {
            lc = new LocationCounter();
            _locationCounters.put(index, lc);
        }

        return lc;
    }
}
