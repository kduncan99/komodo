/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.misc;

import com.kadware.em2200.baselib.AccessInfo;
import com.kadware.em2200.baselib.AccessPermissions;

/**
 * Represents a particular gate within a gate bank
 */
class Gate {

    //TODO package private?
    public final AccessPermissions _generalPermissions;     //  GAP.E
    public final AccessPermissions _specialPermissions;     //  SAP.E
    public final boolean _libraryGate;                      //  LIB - we don't support these currently
    public final boolean _gotoInhibit;                      //  GI
    public final boolean _designatorBitInhibit;             //  DBI
    public final boolean _accessKeyInhibit;                 //  AKI
    public final boolean _latentParameter0Inhibit;          //  LP0I
    public final boolean _latentParameter1Inhibit;          //  LP1I
    public final AccessInfo _accessLock;                    //  Lock which caller must satisfy to enter the gate
    public final int _targetBankLevel;
    public final int _targetBankDescriptorIndex;
    public final int _targetBankOffset;
    public final int _basicModeBaseRegister;                //  range is 0:3 meaning B12:B15
    public final DesignatorRegister _designatorBits12_17;   //  bits 12-17 significant (okay, maybe not 16)
    public final AccessInfo _accessKey;                     //  key to be loaded into IKR
    public final long _latentParameter0;
    public final long _latentParameter1;

    /**
     * Builds a Gate object from the 8-word storage entry representing the gate
     * @param gateDefinition represents the content of the containing gate
     */
    Gate(
        final long[] gateDefinition
    ) {
        _generalPermissions = new AccessPermissions((int) (gateDefinition[0] >> 33));
        _specialPermissions = new AccessPermissions((int) (gateDefinition[0] >> 30));
        _libraryGate = (gateDefinition[0] & 0_000040_000000L) != 0;
        _gotoInhibit = (gateDefinition[0] & 0_000020_000000L) != 0;
        _designatorBitInhibit = (gateDefinition[0] & 0_000010_000000L) != 0;
        _accessKeyInhibit = (gateDefinition[0] & 0_000004_000000L) != 0;
        _latentParameter0Inhibit = (gateDefinition[0] & 0_000002_000000L) != 0;
        _latentParameter1Inhibit = (gateDefinition[0] & 0_000001_000000L) != 0;
        _accessLock = new AccessInfo(gateDefinition[0] & 0_777777);

        _targetBankLevel = (int) (gateDefinition[1] >> 33) & 03;
        _targetBankDescriptorIndex = (int) (gateDefinition[1] >> 18) & 077777;
        _targetBankOffset = (int) (gateDefinition[1] & 0777777);

        _basicModeBaseRegister = (int) ((gateDefinition[2] >> 24) & 03);
        _designatorBits12_17 = new DesignatorRegister(gateDefinition[2] & 0_000077_000000L);
        _accessKey = new AccessInfo(gateDefinition[2] & 0_777777);

        _latentParameter0 = gateDefinition[3];
        _latentParameter1 = gateDefinition[4];
    }
}
