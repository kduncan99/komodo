/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.minalib.exceptions.InvalidParameterException;

/**
 * Represents a loadable bank, part of an AbsoluteModule object.
 */
@SuppressWarnings("Duplicates")
public class LoadableBank {

    /**
     * Ring and domain for the bank
     */
    public final AccessInfo _accessInfo;

    /**
     * RWX privileges for less-privileged client
     */
    public final AccessPermissions _generalPermissions;

    /**
     * RWX privileges for equal ore more-privileged client
     */
    public final AccessPermissions _specialPermissions;

    /**
     * BDI for this bank
     */
    public final int _bankDescriptorIndex;

    /**
     * Bank level for this bank
     */
    public final int _bankLevel;

    /**
     * Bank name for the bank
     */
    public final String _bankName;

    /**
     * BR upon which the bank should be based at load time
     */
    public final Integer _initialBaseRegister;

    /**
     * true if this bank requires extended mode
     */
    public final boolean _isExtendedMode;

    /**
     * wth is this?
     */
    public final Integer _startingAddress;  //  null if none specified

    /**
     * content of this bank
     */
    public final Word36Array _content;

    /**
     * Constructor
     * @param bankDescriptorIndex bank descriptor index
     * @param bankLevel bank level for loader (might be ignored)
     * @param bankName bank name
     * @param startingAddress starting address for the bank
     * @param content content of the bank
     * @param initialBaseRegister initial base register for the bank, null if not to be initially based
     * @param isExtendedMode true if bank is marked as extended moe
     * @param accessInfo access info - domain and ring values for the bank
     * @param generalPermissions GAP permissions
     * @param specialPermissions GAP permissions
     */
    private LoadableBank(
        final int bankDescriptorIndex,
        final int bankLevel,
        final String bankName,
        final int startingAddress,
        final Word36Array content,
        final Integer initialBaseRegister,
        final boolean isExtendedMode,
        final AccessInfo accessInfo,
        final AccessPermissions generalPermissions,
        final AccessPermissions specialPermissions
    ) {
        _bankDescriptorIndex = bankDescriptorIndex;
        _bankLevel = bankLevel;
        _bankName = bankName;
        _startingAddress = startingAddress;
        _content = content;
        _initialBaseRegister = initialBaseRegister;
        _isExtendedMode = isExtendedMode;
        _accessInfo = accessInfo;
        _generalPermissions = generalPermissions;
        _specialPermissions = specialPermissions;
    }

    static class Builder{
        private AccessInfo _accessInfo = new AccessInfo((byte)0, (short)0);
        private AccessPermissions _generalPermissions = new AccessPermissions();
        private AccessPermissions _specialPermissions = new AccessPermissions();
        private int _bankDescriptorIndex;
        private int _bankLevel;
        private String _bankName = null;
        private Integer _initialBaseRegister = null;
        private boolean _isExtendedMode = false;
        private int _startingAddress;
        private Word36Array _content = null;

        Builder setAccessInfo(final AccessInfo value) { _accessInfo = value; return this; }
        Builder setGeneralPermissions(final AccessPermissions value) { _generalPermissions = value; return this; }
        Builder setSpecialPermissions(final AccessPermissions value) { _specialPermissions = value; return this; }
        Builder setBankDescriptorIndex(final int value) { _bankDescriptorIndex = value; return this; }
        Builder setBankLevel(final int value) { _bankLevel = value; return this; }
        Builder setBankName(final String value) { _bankName = value; return this; }
        Builder setInitialBaseRegister(final Integer value) { _initialBaseRegister = value; return this; }
        Builder setIsExtendedMode(final boolean value) { _isExtendedMode = value; return this; }
        Builder setStartingAddress(final Integer value) { _startingAddress = value; return this; }
        Builder setContent(final Word36Array value) { _content = value; return this; }

        LoadableBank build(
        ) throws InvalidParameterException {
            if (_bankName == null) {
                throw new InvalidParameterException("bank name not specified in LoadableBank builder");
            }

            return new LoadableBank(_bankDescriptorIndex,
                                    _bankLevel,
                                    _bankName,
                                    _startingAddress,
                                    _content,
                                    _initialBaseRegister,
                                    _isExtendedMode,
                                    _accessInfo,
                                    _generalPermissions,
                                    _specialPermissions);
        }
    }
}
