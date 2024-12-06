/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.baselib.FileSpecification;
import com.bearsnake.komodo.kexec.Granularity;
import com.bearsnake.komodo.kexec.mfd.FileSetInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CatalogDiskFileCycleRequest {

    final FileSpecification _fileSpecification;
    final FileSetInfo _fileSetInfo;
    String _accountId = null;
    Granularity _granularity = null;
    boolean _isGuarded = false;
    boolean _isPrivate = false;
    boolean _isUnloadInhibited = false;
    boolean _isReadOnly = false;
    boolean _isWriteOnly = false;
    long _initialGranules = 0;
    Long _maximumGranules = null;
    String _mnemonic = null;
    List<String> _packIds = new ArrayList<>();
    boolean _saveOnCheckpoint = false;

    public CatalogDiskFileCycleRequest(
        final FileSpecification fileSpecification,
        final FileSetInfo fileSetInfo
    ) {
        _fileSpecification = fileSpecification;
        _fileSetInfo = fileSetInfo;
    }

    public CatalogDiskFileCycleRequest appendPackId(final String packId) {
        _packIds.add(packId);
        return this;
    }

    public CatalogDiskFileCycleRequest setAccountId(final String value) {
        _accountId = value;
        return this;
    }

    public CatalogDiskFileCycleRequest setGranularity(final Granularity value) {
        _granularity = value;
        return this;
    }

    public CatalogDiskFileCycleRequest setInitialGranules(final long value) {
        _initialGranules = value;
        return this;
    }

    public CatalogDiskFileCycleRequest setIsGuarded() {
        _isGuarded = true;
        return this;
    }

    public CatalogDiskFileCycleRequest setIsGuarded(final boolean value) {
        _isGuarded = value;
        return this;
    }

    public CatalogDiskFileCycleRequest setIsPrivate() {
        _isPrivate = true;
        return this;
    }

    public CatalogDiskFileCycleRequest setIsPrivate(final boolean value) {
        _isPrivate = value;
        return this;
    }

    public CatalogDiskFileCycleRequest setIsUnloadInhibited() {
        _isUnloadInhibited = true;
        return this;
    }

    public CatalogDiskFileCycleRequest setIsUnloadInhibited(final boolean value) {
        _isUnloadInhibited = value;
        return this;
    }

    public CatalogDiskFileCycleRequest setIsReadOnly() {
        _isReadOnly = true;
        return this;
    }

    public CatalogDiskFileCycleRequest setIsReadOnly(final boolean value) {
        _isReadOnly = value;
        return this;
    }

    public CatalogDiskFileCycleRequest setIsWriteOnly() {
        _isWriteOnly = true;
        return this;
    }

    public CatalogDiskFileCycleRequest setIsWriteOnly(final boolean value) {
        _isWriteOnly = value;
        return this;
    }

    public CatalogDiskFileCycleRequest setMaximumGranules(final long value) {
        _maximumGranules = value;
        return this;
    }

    public CatalogDiskFileCycleRequest setMnemonic(final String value) {
        _mnemonic = value;
        return this;
    }

    public CatalogDiskFileCycleRequest setPackIds(final Collection<String> list) {
        _packIds = new ArrayList<>(list);
        return this;
    }

    public CatalogDiskFileCycleRequest setSaveOnCheckpoint() {
        _saveOnCheckpoint = true;
        return this;
    }

    public CatalogDiskFileCycleRequest setSaveOnCheckpoint(final boolean value) {
        _saveOnCheckpoint = value;
        return this;
    }
}
