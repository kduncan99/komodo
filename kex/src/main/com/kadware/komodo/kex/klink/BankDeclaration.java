/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.AccessPermissions;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Class provided by clients for declaring how banks are to be created
 */
public class BankDeclaration {

    enum BankDeclarationOption {
        DYNAMIC,
        DBANK,
        WRITE_PROTECT,
        GUARANTEED_ENTRY_POINT,
        SHARED,
        TEST_AND_SET_QUEUING,
        DBANK_CONTINGENCIES,
        ALLOW_CONTINGENCY_PROCESSING,
        START_2048_WORD_BOUNDARY,
        EXTENDED_MODE
    }

    public final AccessInfo _accessInfo;
    public final int _bankDescriptorIndex;
    public final int _bankLevel;
    public final String _bankName;
    public final AccessPermissions _generalAccessPermissions;
    public final boolean _largeBank;
    public final LCPoolSpecification[] _poolSpecifications;
    public final AccessPermissions _specialAccessPermissions;
    public final int _startingAddress;
    public final Set<BankDeclarationOption> _options;

    /**
     * Constructor
     * @param bankName name of the bank
     * @param bankDescriptorIndex BDI for the bank
     * @param bankLevel level 0-7 for the bank
     * @param accessInfo ring and domain for the bank
     * @param generalAccessPermissions GAP permissions
     * @param largeBank true if this bank is a 'large' bank
     * @param specialAccessPermissions SAP permissions
     * @param startingAddress beginning address for the bank - i.e., 01000, 022000, etc
     * @param poolSpecifications set of LCPoolSpecification objects indicating the LCPools to be included in this bank
     * @param options bank declaration options
     */
    private BankDeclaration(
        final String bankName,
        final int bankDescriptorIndex,
        final int bankLevel,
        final AccessInfo accessInfo,
        final AccessPermissions generalAccessPermissions,
        final boolean largeBank,
        final AccessPermissions specialAccessPermissions,
        final Integer startingAddress,
        final LCPoolSpecification[] poolSpecifications,
        final Collection<BankDeclarationOption> options
    ) {
        _accessInfo = accessInfo;
        _bankDescriptorIndex = bankDescriptorIndex;
        _bankLevel = bankLevel;
        _bankName = bankName;
        _generalAccessPermissions = generalAccessPermissions;
        _largeBank = largeBank;
        _poolSpecifications = poolSpecifications;
        _specialAccessPermissions = specialAccessPermissions;
        _startingAddress = startingAddress;
        _options = new HashSet<>(options);
    }

    public static class Builder{
        private AccessInfo _accessInfo = new AccessInfo((byte) 0,(short) 0);
        private int _bankDescriptorIndex;
        private int _bankLevel;
        private String _bankName = null;
        private AccessPermissions _generalAccessPermissions = new AccessPermissions();
        private boolean _largeBank = false;
        private LCPoolSpecification[] _poolSpecifications = null;
        private AccessPermissions _specialAccessPermissions = new AccessPermissions();
        private int _startingAddress;
        private Set<BankDeclarationOption> _options = new HashSet<>();

        public Builder addOption(
            final BankDeclarationOption option
        ) {
            _options.add(option);
            return this;
        }

        public Builder setAccessInfo(
            final AccessInfo value
        ) {
            _accessInfo = value;
            return this;
        }

        public Builder setBankDescriptorIndex(
            final int value
        ) {
            _bankDescriptorIndex = value;
            return this;
        }

        public Builder setBankLevel(
            final int value
        ) {
            _bankLevel = value;
            return this;
        }

        public Builder setBankName(
            final String value
        ) {
            _bankName = value;
            return this;
        }

        public Builder setGeneralAccessPermissions(
            final AccessPermissions value
        ) {
            _generalAccessPermissions = value;
            return this;
        }

        public Builder setLargeBank(
            final boolean value
        ) {
            _largeBank = value;
            return this;
        }

        public Builder setOptions(
            final Set<BankDeclarationOption> options
        ) {
            _options = options;
            return this;
        }

        public Builder setPoolSpecifications(
            final LCPoolSpecification[] values
        ) {
            _poolSpecifications = values;
            return this;
        }

        public Builder setPoolSpecifications(
            final Collection<LCPoolSpecification> values
        ) {
            _poolSpecifications = values.toArray(new LCPoolSpecification[0]);
            return this;
        }

        public Builder setSpecialAccessPermissions(
            final AccessPermissions value
        ) {
            _specialAccessPermissions = value;
            return this;
        }

        public Builder setStartingAddress(
            final int value
        ) {
            _startingAddress = value;
            return this;
        }

        public BankDeclaration build() {
            return new BankDeclaration(_bankName,
                                       _bankDescriptorIndex,
                                       _bankLevel,
                                       _accessInfo,
                                       _generalAccessPermissions,
                                       _largeBank,
                                       _specialAccessPermissions,
                                       _startingAddress,
                                       _poolSpecifications,
                                       _options);
        }
    }
}
