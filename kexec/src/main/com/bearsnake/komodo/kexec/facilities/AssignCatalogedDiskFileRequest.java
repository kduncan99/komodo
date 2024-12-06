/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.baselib.FileSpecification;
import com.bearsnake.komodo.kexec.Granularity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AssignCatalogedDiskFileRequest {

    final FileSpecification _fileSpecification;
    Granularity _granularity = null;    // if specified, must match existing file
    Long _initialReserve = null;        // if specified, represents an attempt to change existing value
    Long _maxGranules = null;           // used in attempt to change existing value (null otherwise)
    String _mnemonic = null;            // equip type / assign mnemonic
    Long _optionsWord = null;           // only used in populating a new facItem - does not drive actual behavior
    List<String> _packIds = new ArrayList<>(); // empty for fixed, optional for removable
    String _placement = null;           // only for fixed (can be null - must be null for removable)
    DeleteBehavior _deleteBehavior = DeleteBehavior.None;                       // as indicated by D/K options
    DirectoryOnlyBehavior _directoryOnlyBehavior = DirectoryOnlyBehavior.None;  // as indicated by E/Y options
    boolean _saveOnCheckpoint = false;  // represents M option (TODO CHKPT)
    boolean _assignIfDisabled = false;  // represents Q option
    boolean _readOnly = false;          // represents R option
    boolean _exclusiveUse = false;      // represents X option
    boolean _releaseOnTaskEnd = false;  // represents I option
    boolean _doNotHoldRun = false;      // represents Z option

    public AssignCatalogedDiskFileRequest(FileSpecification fileSpecification) {
        _fileSpecification = fileSpecification;
    }

    public AssignCatalogedDiskFileRequest appendPackId(final String packId) {
        _packIds.add(packId);
        return this;
    }

    public AssignCatalogedDiskFileRequest setOptionsWord(final Long value) {
        _optionsWord = value;
        return this;
    }

    public AssignCatalogedDiskFileRequest setMnemonic(final String value) {
        _mnemonic = value;
        return this;
    }

    public AssignCatalogedDiskFileRequest setInitialReserve(final long value) {
        _initialReserve = value;
        return this;
    }

    public AssignCatalogedDiskFileRequest setGranularity(final Granularity value) {
        _granularity = value;
        return this;
    }

    public AssignCatalogedDiskFileRequest setMaxGranules(final long value) {
        _maxGranules = value;
        return this;
    }

    public AssignCatalogedDiskFileRequest setPlacement(final String value) {
        _placement = value;
        return this;
    }

    public AssignCatalogedDiskFileRequest setPackIds(final Collection<String> list) {
        _packIds = new ArrayList<>(list);
        return this;
    }

    public AssignCatalogedDiskFileRequest setDeleteBehavior(final DeleteBehavior value) {
        _deleteBehavior = value;
        return this;
    }

    public AssignCatalogedDiskFileRequest setDirectoryOnlyBehavior(final DirectoryOnlyBehavior value) {
        _directoryOnlyBehavior = value;
        return this;
    }

    public AssignCatalogedDiskFileRequest setSaveOnCheckpoint() {
        _saveOnCheckpoint = true;
        return this;
    }

    public AssignCatalogedDiskFileRequest setSaveOnCheckpoint(final boolean value) {
        _saveOnCheckpoint = value;
        return this;
    }

    public AssignCatalogedDiskFileRequest setAssignIfDisabled() {
        _assignIfDisabled = true;
        return this;
    }

    public AssignCatalogedDiskFileRequest setAssignIfDisabled(final boolean value) {
        _assignIfDisabled = value;
        return this;
    }

    public AssignCatalogedDiskFileRequest setReadOnly() {
        _readOnly = true;
        return this;
    }

    public AssignCatalogedDiskFileRequest setReadOnly(final boolean value) {
        _readOnly = value;
        return this;
    }

    public AssignCatalogedDiskFileRequest setExclusiveUse() {
        _exclusiveUse = true;
        return this;
    }

    public AssignCatalogedDiskFileRequest setExclusiveUse(final boolean value) {
        _exclusiveUse = value;
        return this;
    }

    public AssignCatalogedDiskFileRequest setReleaseOnTaskEnd() {
        _releaseOnTaskEnd = true;
        return this;
    }

    public AssignCatalogedDiskFileRequest setReleaseOnTaskEnd(final boolean value) {
        _releaseOnTaskEnd = value;
        return this;
    }

    public AssignCatalogedDiskFileRequest setDoNotHoldRun() {
        _doNotHoldRun = true;
        return this;
    }

    public AssignCatalogedDiskFileRequest setDoNotHoldRun(final boolean value) {
        _doNotHoldRun = value;
        return this;
    }
}
