/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

public class UnitSelectionIndicators {

    private boolean _createdViaDevicePlacement;
    private boolean _createdViaControlUnitPlacement;
    private boolean _createdViaLogicalPlacement;
    private boolean _multipleDevices;
    private int _initialLDATIndex;

    public int compose() {
        int value = _createdViaDevicePlacement ? 0_400000 : 0;
        value |= _createdViaControlUnitPlacement ? 0_200000 : 0;
        value |= _createdViaLogicalPlacement ? 0_100000 : 0;
        value |= _multipleDevices ? 0_040000 : 0;
        value |= _initialLDATIndex & 07777;
        return value;
    }

    public void extract(final int value) {
        _createdViaDevicePlacement = (value & 0_400000) != 0;
        _createdViaControlUnitPlacement = (value & 0_200000) != 0;
        _createdViaLogicalPlacement = (value & 0_100000) != 0;
        _multipleDevices = (value & 0_040000) != 0;
        _initialLDATIndex = value & 07777;
    }

    public static UnitSelectionIndicators extractFrom(final int value) {
        var inf = new UnitSelectionIndicators();
        inf.extract(value);
        return inf;
    }
}
