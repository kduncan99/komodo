/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.AccessInfo;

/**
 * Represents an RCS stack frame
 */
class ReturnControlStackFrame {

    final int _reentryPointBankLevel;
    final int _reentryPointBankDescriptorIndex;
    final int _reentryPointOffset;
    final boolean _trap;
    final int _basicModeBaseRegister;                       //  Range 0:3, added to 12 signifies a base register
    final DesignatorRegister _designatorRegisterDB12To17;
    final AccessInfo _accessKey;

    /**
     * Builds a frame object from the 2-word storage entry
     */
    ReturnControlStackFrame(
        final long[] frame
    ) {
        _reentryPointBankLevel = (int) (frame[0] >> 33) & 07;
        _reentryPointBankDescriptorIndex = (int) (frame[0] >> 18) & 077777;
        _reentryPointOffset = (int) (frame[0] & 0777777);
        _trap = (frame[1] & 0_400000_000000L) != 0;
        _basicModeBaseRegister = (int) (frame[1] >> 24) & 03;
        _designatorRegisterDB12To17 = new DesignatorRegister(frame[1] & 0_000077_000000L);
        _accessKey = new AccessInfo(frame[1] & 0777777);
    }

    /**
     * Builds a frame object from the discrete components
     */
    ReturnControlStackFrame(
        final int reentryPointBankLevel,
        final int reentryPointBankDescriptorIndex,
        final int reentryPointOffset,
        final boolean trap,
        final int basicModeBaseRegister,
        final DesignatorRegister designatorRegister,
        final AccessInfo accessKey
    ) {
        _reentryPointBankLevel = reentryPointBankLevel;
        _reentryPointBankDescriptorIndex = reentryPointBankDescriptorIndex;
        _reentryPointOffset = reentryPointOffset;
        _trap = trap;
        _basicModeBaseRegister = basicModeBaseRegister & 07;
        _designatorRegisterDB12To17 = new DesignatorRegister(designatorRegister.getW() & 0_000077_000000L);
        _accessKey = accessKey;
    }

    /**
     * Returns two-word representation of the RCS frame
     */
    long[] get() {
        long[] result = new long[2];
        result[0] = ((long) _reentryPointBankLevel << 33)
                    | ((long) _reentryPointBankDescriptorIndex << 18)
                    | (result[0] |= _reentryPointOffset);

        result[1] = (_trap ? 0_400000_000000L : 0)
                    | (_basicModeBaseRegister << 24)
                    | _designatorRegisterDB12To17.getW()
                    | _accessKey.get();

        return result;
    }
}
