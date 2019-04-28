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
    public final int _startingAddress;

    /**
     * Constructor
     * @param name name of the module
     */
    public AbsoluteModule(
        final String name,
        final LoadableBank[] loadableBanks,
        final int startingAddress
    ) throws InvalidParameterException {
        _name = name;
        for (LoadableBank lb : loadableBanks) {
            if (_loadableBanks.containsKey(lb._bankDescriptorIndex)) {
                throw new InvalidParameterException(String.format("BDI %07o specified more than once", lb._bankDescriptorIndex));
            }
            _loadableBanks.put(lb._bankDescriptorIndex, lb);
        }
        _startingAddress = startingAddress;
    }

    /**
     * Mainly for development and debugging
     */
    public void display(
    ) {
        System.out.println("Absolute Module name=" + _name);
        for (LoadableBank bank : _loadableBanks.values()) {
            System.out.println(String.format("  Bank=%s BDI=%06o", bank._bankName, bank._bankDescriptorIndex));
            int address = bank._startingAddress;
            int arraySize = bank._content.getArraySize();
            for (int ax = 0; ax < arraySize; ax += 8) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("    %06o:", address));
                for (int bx = 0; bx < 8; ++bx) {
                    sb.append(String.format("%012o ", bank._content.getValue(ax + bx)));
                    if (ax + bx + 1 == arraySize) {
                        break;
                    }
                }

                System.out.println(sb.toString());
            }
        }
        System.out.println(String.format("  Starting Address:%06o", _startingAddress));
    }
}
