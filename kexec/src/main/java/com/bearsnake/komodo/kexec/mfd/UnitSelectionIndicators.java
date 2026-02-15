/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
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

    public UnitSelectionIndicators extract(final long value) {
        _createdViaDevicePlacement = (value & 0_400000) != 0;
        _createdViaControlUnitPlacement = (value & 0_200000) != 0;
        _createdViaLogicalPlacement = (value & 0_100000) != 0;
        _multipleDevices = (value & 0_040000) != 0;
        _initialLDATIndex = (int)(value & 07777);

        return this;
    }

    public static UnitSelectionIndicators extractFrom(final int value) {
        var inf = new UnitSelectionIndicators();
        inf.extract(value);
        return inf;
    }

    public UnitSelectionIndicators setCreatedViaDevicePlacement(final boolean value) { _createdViaDevicePlacement = value; return this; }
    public UnitSelectionIndicators setCreatedViaControlUnitPlacement(final boolean value) { _createdViaControlUnitPlacement = value; return this; }
    public UnitSelectionIndicators setCreatedViaLogicalPlacement(final boolean value) { _createdViaLogicalPlacement = value; return this; }
    public UnitSelectionIndicators setMultipleDevices(final boolean value) { _multipleDevices = value; return this; }
    public UnitSelectionIndicators setInitialLDATIndex(final int value) { _initialLDATIndex = value & 07777; return this; }
}
