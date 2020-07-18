/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.AbsoluteAddress;

/**
 * Describes a breakpoint register.
 */
public class BreakpointRegister {

    final boolean _haltFlag;
    final boolean _fetchFlag;
    final boolean _readFlag;
    final boolean _writeFlag;
    final AbsoluteAddress _absoluteAddress;

    private BreakpointRegister(
        final boolean haltFlag,
        final boolean fetchFlag,
        final boolean readFlag,
        final boolean writeFlag,
        final AbsoluteAddress absoluteAddress
    ) {
        _haltFlag = haltFlag;
        _fetchFlag = fetchFlag;
        _readFlag = readFlag;
        _writeFlag = writeFlag;
        _absoluteAddress = absoluteAddress;
    }

    public static class Builder {

        private boolean _haltFlag = false;
        private boolean _fetchFlag = false;
        private boolean _readFlag = false;
        private boolean _writeFlag = false;
        private AbsoluteAddress _absoluteAddress = null;

        public Builder setHaltFlag(boolean value)                   { _haltFlag = value; return this; }
        public Builder setFetchFlag(boolean value)                  { _fetchFlag = value; return this; }
        public Builder setReadFlag(boolean value)                   { _readFlag = value; return this; }
        public Builder setWriteFlag(boolean value)                  { _writeFlag = value; return this; }
        public Builder setAbsoluteAddress(AbsoluteAddress value)    { _absoluteAddress = value; return this; }

        public BreakpointRegister build() {
            return new BreakpointRegister(_haltFlag, _fetchFlag, _readFlag, _writeFlag, _absoluteAddress);
        }
    }
}
