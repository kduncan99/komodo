/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.AccessPermissions;

/**
 * Resulting bank descriptor information - should be similar to, although possibly not exactly,
 * the information provided in the corresponding BankDeclaration object. This information is not
 * specific to either absolute or object module banking schemes - it is a superset of both of those.
 */
public class BankDescriptor {

    public final AccessInfo _accessInfo;
    public final int _bankDescriptorIndex;
    public final int _bankLevel;
    public final String _bankName;
    public final BankType _bankType;
    public long[] _content;
    public final AccessPermissions _generalPermissions;
    public int _lowerLimit;                                 //  address of first word in the bank
    public final AccessPermissions _specialPermissions;
    public int _upperLimit;                                 //  address of last word in the bank

    BankDescriptor(
        final String bankName,
        final int bankLevel,
        final int bankDescriptorIndex,
        final AccessInfo accessInfo,
        final BankType bankType,
        final AccessPermissions generalPermissions,
        final AccessPermissions specialPermissions,
        final int lowerLimit,
        final int upperLimit,
        final long[] content
    ) {
        _accessInfo = accessInfo;
        _bankDescriptorIndex = bankDescriptorIndex;
        _bankLevel = bankLevel;
        _bankName = bankName;
        _bankType = bankType;
        _content = content;
        _generalPermissions = generalPermissions;
        _lowerLimit = lowerLimit;
        _specialPermissions = specialPermissions;
        _upperLimit = upperLimit;
    }
}
