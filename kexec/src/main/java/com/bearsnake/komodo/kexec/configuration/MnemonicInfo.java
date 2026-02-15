/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * For a given type/mnemonic string as specified on an ECL statement,
 * we define the MnemonicType (sector addressable disk, word addressable disk, tape) for the string
 * and the list of equipment types which can satisfy that type.
 */
public class MnemonicInfo {

    public final String _mnemonic;
    public final MnemonicType _type;
    public final List<EquipType> _equipmentTypes = new LinkedList<>();

    public MnemonicInfo(
        final String mnemonic,
        final MnemonicType type,
        final Collection<EquipType> equipmentTypes
    ) {
        _mnemonic = mnemonic;
        _type = type;
        _equipmentTypes.addAll(equipmentTypes);
    }
}
