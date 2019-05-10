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
     * starting address of the bank
     */
    public final int _startingAddress;

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
        private AccessInfo _accessInfo = null;
        private AccessPermissions _generalPermissions = null;
        private AccessPermissions _specialPermissions = null;
        private Integer _bankDescriptorIndex = null;
        private Integer _bankLevel = null;
        private String _bankName = null;
        private Integer _initialBaseRegister = null;
        private boolean _isExtendedMode = false;
        private Integer _startingAddress = null;
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
            if (_accessInfo == null) {
                throw new InvalidParameterException("ring/domain access info not specified");
            }

            if (_generalPermissions == null) {
                throw new InvalidParameterException("general permissions not specified");
            }

            if (_specialPermissions == null) {
                throw new InvalidParameterException("special permissions not specified");
            }

            if (_bankDescriptorIndex == null) {
                throw new InvalidParameterException("bank desriptor index not specified");
            }

            if (_bankLevel == null) {
                throw new InvalidParameterException("bank level not specified");
            }

            if (_bankName == null) {
                throw new InvalidParameterException("bank name not specified");
            }

            if (_startingAddress == null) {
                throw new InvalidParameterException("bank starting address not specified");
            }

            if (_content == null) {
                throw new InvalidParameterException("no content specified");
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
