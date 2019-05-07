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
    public LoadableBank _entryPointBank = null;
    public int _entryPointAddress = 0;

    /**
     * Constructor
     * @param name name of the module
     */
    private AbsoluteModule(
        final String name,
        final LoadableBank[] loadableBanks,
        final int startingAddress,
        final LoadableBank entryPointBank,  //  must refer to one of the banks in loadableBanks
        final int entryPointAddress,        //  must refer to an address in the above entryPointBank
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
    }

    static class Builder {

        private int _entryPointAddress = 0;
        private LoadableBank _entryPointBank = null;
        private String _name = null;
        private LoadableBank[] _loadableBanks = null;
        private Integer _startingAddress = null;
        private Boolean _afcmClear = false;
        private Boolean _afcmSet = false;
        private Boolean _setQuarter = false;
        private Boolean _setThird = false;

        Builder setEntryPoint(
            final LoadableBank entryPointBank,
            final int entryPointAddress
        ) {
            _entryPointBank = entryPointBank;
            _entryPointAddress = entryPointAddress;
            return this;
        }

        Builder setLoadableBanks(final LoadableBank[] value) { _loadableBanks = value; return this; }
        Builder setName(final String value) { _name = value; return this; }
        Builder setStartingAddress(final int value) { _startingAddress = value; return this; }
        Builder setAFCMClear(final boolean value) { _afcmClear = value; return this; }
        Builder setAFCMSet(final boolean value) { _afcmSet = value; return this; }
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

            if (_entryPointBank == null) {
                throw new InvalidParameterException("Entry point not specified");
            }

            boolean found = false;
            for (int bx = 0; !found && (bx < _loadableBanks.length); ++bx) {
                if (_loadableBanks[bx] == _entryPointBank) {
                    found = true;
                }
            }
            if (!found) {
                throw new InvalidParameterException("Entry point bank is not in the array of loadable banks");
            }

            if ((_entryPointAddress < _entryPointBank._startingAddress)
                || (_entryPointAddress >= (_entryPointBank._startingAddress + _entryPointBank._content.getArraySize()))) {
                throw new InvalidParameterException("Entry point address is outside of the limits of the entry point bank");
            }

            return new AbsoluteModule(_name,
                                      _loadableBanks,
                                      _startingAddress,
                                      _entryPointBank,
                                      _entryPointAddress,
                                      _afcmClear,
                                      _afcmSet,
                                      _setQuarter,
                                      _setThird);
        }
    }
}
