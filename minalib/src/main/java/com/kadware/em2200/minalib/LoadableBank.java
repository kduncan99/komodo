/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.*;

/**
 * Represents a loadable bank, part of an AbsoluteModule object.
 */
@SuppressWarnings("Duplicates")
public class LoadableBank {

    public final AccessInfo _accessInfo;
    public final AccessPermissions _generalPermissions;
    public final AccessPermissions _specialPermissions;
    public final int _bankDescriptorIndex;
    public final String _bankName;
    public final Integer _initialBaseRegister;
    public final boolean _isExtendedMode;
    public final int _startingAddress;
    public final Word36Array _content;

    /**
     * Constructor
     * @param bdi bank descriptor index
     * @param name bank name
     * @param startingAddress starting address for the bank
     * @param content content of the bank
     * @param initialBaseRegister initial base register for the bank, null if not to be initially based
     * @param isExtendedMode true if bank is marked as extended moe
     * @param accessInfo access info - domain and ring values for the bank
     * @param generalPermissions GAP permissions
     * @param specialPermissions GAP permissions
     */
    public LoadableBank(
        final int bdi,
        final String name,
        final int startingAddress,
        final Word36Array content,
        final Integer initialBaseRegister,
        final boolean isExtendedMode,
        final AccessInfo accessInfo,
        final AccessPermissions generalPermissions,
        final AccessPermissions specialPermissions
    ) {
        _bankDescriptorIndex = bdi;
        _bankName = name;
        _startingAddress = startingAddress;
        _content = content;
        _initialBaseRegister = initialBaseRegister;
        _isExtendedMode = isExtendedMode;
        _accessInfo = accessInfo;
        _generalPermissions = generalPermissions;
        _specialPermissions = specialPermissions;
    }

    public static class Builder{
        private AccessInfo _accessInfo = new AccessInfo((byte)0, (short)0);
        private AccessPermissions _generalPermissions = new AccessPermissions();
        private AccessPermissions _specialPermissions = new AccessPermissions();
        private int _bankDescriptorIndex;
        private String _bankName = null;
        private Integer _initialBaseRegister = null;
        private boolean _isExtendedMode = false;
        private int _startingAddress;
        private Word36Array _content = null;

        public Builder setAccessInfo(final AccessInfo value) { _accessInfo = value; return this; }
        public Builder setGeneralPermissions(final AccessPermissions value) { _generalPermissions = value; return this; }
        public Builder setSpecialPermissions(final AccessPermissions value) { _specialPermissions = value; return this; }
        public Builder setBankDescriptorIndex(final int value) { _bankDescriptorIndex = value; return this; }
        public Builder setBankName(final String value) { _bankName = value; return this; }
        public Builder setInitialBaseRegister(final Integer value) { _initialBaseRegister = value; return this; }
        public Builder setIsExtendedMode(final boolean value) { _isExtendedMode = value; return this; }
        public Builder setStartingAddress(final int value) { _startingAddress = value; return this; }
        public Builder setContent(final Word36Array value) { _content = value; return this; }

        public LoadableBank build() {
            assert(_bankDescriptorIndex != 0);
            assert(_bankName != null);
            return new LoadableBank(_bankDescriptorIndex,
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
