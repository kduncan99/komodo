/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.baselib.FileSpecification;
import com.bearsnake.komodo.kexec.Granularity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CatalogDiskFileRequest {

    final FileSpecification _fileSpecification;
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
    String _projectId = null;
    boolean _saveOnCheckpoint = false;

    public CatalogDiskFileRequest(FileSpecification fileSpecification) {
        _fileSpecification = fileSpecification;
    }

    public CatalogDiskFileRequest appendPackId(final String packId) {
        _packIds.add(packId);
        return this;
    }

    public CatalogDiskFileRequest setAccountId(final String value) {
        _accountId = value;
        return this;
    }

    public CatalogDiskFileRequest setGranularity(final Granularity value) {
        _granularity = value;
        return this;
    }

    public CatalogDiskFileRequest setInitialGranules(final long value) {
        _initialGranules = value;
        return this;
    }

    public CatalogDiskFileRequest setIsGuarded() {
        _isGuarded = true;
        return this;
    }

    public CatalogDiskFileRequest setIsGuarded(final boolean value) {
        _isGuarded = value;
        return this;
    }

    public CatalogDiskFileRequest setIsPrivate() {
        _isPrivate = true;
        return this;
    }

    public CatalogDiskFileRequest setIsPrivate(final boolean value) {
        _isPrivate = value;
        return this;
    }

    public CatalogDiskFileRequest setIsUnloadInhibited() {
        _isUnloadInhibited = true;
        return this;
    }

    public CatalogDiskFileRequest setIsUnloadInhibited(final boolean value) {
        _isUnloadInhibited = value;
        return this;
    }

    public CatalogDiskFileRequest setIsReadOnly() {
        _isReadOnly = true;
        return this;
    }

    public CatalogDiskFileRequest setIsReadOnly(final boolean value) {
        _isReadOnly = value;
        return this;
    }

    public CatalogDiskFileRequest setIsWriteOnly() {
        _isWriteOnly = true;
        return this;
    }

    public CatalogDiskFileRequest setIsWriteOnly(final boolean value) {
        _isWriteOnly = value;
        return this;
    }

    public CatalogDiskFileRequest setMaximumGranules(final long value) {
        _maximumGranules = value;
        return this;
    }

    public CatalogDiskFileRequest setMnemonic(final String value) {
        _mnemonic = value;
        return this;
    }

    public CatalogDiskFileRequest setPackIds(final Collection<String> list) {
        _packIds = new ArrayList<>(list);
        return this;
    }

    public CatalogDiskFileRequest setProjectId(final String value) {
        _projectId = value;
        return this;
    }

    public CatalogDiskFileRequest setSaveOnCheckpoint() {
        _saveOnCheckpoint = true;
        return this;
    }

    public CatalogDiskFileRequest setSaveOnCheckpoint(final boolean value) {
        _saveOnCheckpoint = value;
        return this;
    }
}
