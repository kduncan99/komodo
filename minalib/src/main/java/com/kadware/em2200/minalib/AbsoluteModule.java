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

    //  Arithmetic fault compatibility mode
    public final boolean _afcmClear;
    public final boolean _afcmSet;

    //  Third/Quarter word mode
    public final boolean _setQuarter;
    public final boolean _setThird;

    //  Entry point
    public final Integer _entryPointAddress;
    public final LoadableBank _entryPointBank;

    /**
     * Constructor
     * @param name name of the module
     */
    private AbsoluteModule(
        final String name,
        final LoadableBank[] loadableBanks,
        final Integer entryPointAddress,
        final LoadableBank entryPointBank,
        final boolean afcmClear,
        final boolean afcmSet,
        final boolean setQuarter,
        final boolean setThird
    ) throws InvalidParameterException {
        _name = name;

        for (LoadableBank lb : loadableBanks) {
            if (_loadableBanks.containsKey(lb._bankDescriptorIndex)) {
                throw new InvalidParameterException(String.format("BDI %07o specified more than once", lb._bankDescriptorIndex));
            }
            _loadableBanks.put(lb._bankDescriptorIndex, lb);
        }

        _entryPointAddress = entryPointAddress;
        _entryPointBank = entryPointBank;
        _afcmClear = afcmClear;
        _afcmSet = afcmSet;
        _setQuarter = setQuarter;
        _setThird = setThird;
    }

    /**
     * Mainly for development and debugging
     */
    public void display(
    ) {
        System.out.println(String.format("Absolute Module Name:%s   AFCM:%s Q/TWord:%s   Entry:%d",
                                         _name,
                                         _afcmClear ? "CLEAR" : (_afcmSet ? "SET" : "nonSpec"),
                                         _setQuarter ? "QW" : (_setThird ? "TW" : "nonSpec"),
                                         _entryPointAddress));

        for (LoadableBank bank : _loadableBanks.values()) {
            System.out.println(String.format("  Bank=%s BDI=%06o Addr=%08o Init:%s ExtMode:%s",
                                             bank._bankName,
                                             bank._bankDescriptorIndex,
                                             bank._startingAddress,
                                             bank._initialBaseRegister == null ? "n/a" : String.format("B%d", bank._initialBaseRegister),
                                             String.valueOf(bank._isExtendedMode)));

            int address = bank._startingAddress;
            int arraySize = bank._content.getSize();
            for (int ax = 0; ax < arraySize; ax += 8) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("    %06o:", address));
                for (int bx = 0; bx < 8; ++bx) {
                    sb.append(String.format("%012o ", bank._content.get(ax + bx)));
                    if (ax + bx + 1 == arraySize) {
                        break;
                    }
                }

                System.out.println(sb.toString());
            }
        }
    }

    static class Builder {

        private Integer _entryPointAddress = null;
        private LoadableBank _entryPointBank = null;
        private String _name = null;
        private LoadableBank[] _loadableBanks = null;
        private boolean _afcmClear = false;
        private boolean _afcmSet = false;
        private boolean _setQuarter = false;
        private boolean _setThird = false;
        private boolean _allowNoEntryPoint = false;

        Builder setEntryPointAddress(final Integer value) { _entryPointAddress = value; return this; }
        Builder setEntryPointBank(final LoadableBank value) { _entryPointBank = value; return this; }
        Builder setLoadableBanks(final LoadableBank[] value) { _loadableBanks = value; return this; }
        Builder setName(final String value) { _name = value; return this; }
        Builder setAFCMClear(final boolean value) { _afcmClear = value; return this; }
        Builder setAFCMSet(final boolean value) { _afcmSet = value; return this; }
        Builder setAllowNoEntryPoint(final boolean value) { _allowNoEntryPoint = value; return this; }
        Builder setQuarter(final boolean value) { _setQuarter = value; return this; }
        Builder setThird(final boolean value) { _setThird = value; return this; }

        AbsoluteModule build(
        ) throws InvalidParameterException {
            if (_name == null) {
                throw new InvalidParameterException("Name not specified");
            }

            if ((_loadableBanks == null) || (_loadableBanks.length == 0)) {
                throw new InvalidParameterException("No banks specified");
            }

            if (_afcmSet && _afcmClear) {
                throw new InvalidParameterException("AFCM SET and CLEAR are both true");
            }

            if (_setQuarter && _setThird) {
                throw new InvalidParameterException("THIRD and QUARTER WORD modes are both set");
            }

            if (!_allowNoEntryPoint) {
                if (_entryPointAddress == null) {
                    throw new InvalidParameterException("No entry point address specified");
                }

                if (_entryPointBank == null) {
                    throw new InvalidParameterException("No entry point bank specified");
                }
            }

            //  TODO check entry point address against entry point bank if specified

            return new AbsoluteModule(_name,
                                      _loadableBanks,
                                      _entryPointAddress,
                                      _entryPointBank,
                                      _afcmClear,
                                      _afcmSet,
                                      _setQuarter,
                                      _setThird);
        }
    }
}
