/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedList;

public class DiskFileCycleInfo extends FileCycleInfo {

    private Instant _timeOfFirstWriteOrUnload;
    private DisableFlags _disableFlags;
    private DescriptorFlags _descriptorFlags;
    private FileFlags _fileFlags;
    private PCHARFlags _pcharFlags;
    private long _initialSmoqueLink;
    private long _assignCount;
    private Instant _timeOfLastReference;
    private Instant _timeCataloged;
    private long _initialGranulesReserved;
    private long _maxGranules;
    private long _highestGranuleAssigned;
    private long _highestTrackWritten;
    private UnitSelectionIndicators _unitSelectionIndicators;
    private long[] _quotaGroupGranules;
    private BackupInfo _backupInfo;
    private Collection<DiskPackEntry> _diskPackEntries = new LinkedList<>();
    private FileAllocationSet _fileAllocations;

    public final Instant getTimeOfFirstWriteOrUnload() { return _timeOfFirstWriteOrUnload; }
    public final DisableFlags getDisableFlags() { return _disableFlags; }
    public final DescriptorFlags getDescriptorFlags() { return _descriptorFlags; }
    public final FileFlags getFileFlags() { return _fileFlags; }
    public final PCHARFlags getPCHARFlags() { return _pcharFlags; }
    public final long getInitialSmoqueLink() { return _initialSmoqueLink; }
    public final long getAssignCount() { return _assignCount; }
    public final Instant getTimeOfLastReference() { return _timeOfLastReference; }
    public final Instant getTimeCataloged() { return _timeCataloged; }
    public final long getInitialGranulesReserved() { return _initialGranulesReserved; }
    public final long getMaxGranules() { return _maxGranules; }
    public final long getHighestGranuleAssigned() { return _highestGranuleAssigned; }
    public final long getHighestTrackWritten() { return _highestTrackWritten; }
    public final UnitSelectionIndicators getUnitSelectionIndicators() { return _unitSelectionIndicators; }
    public final long[] getQuotaGroupGranules() { return _quotaGroupGranules; }
    public final BackupInfo getBackupInfo() { return _backupInfo; }
    public final Collection<DiskPackEntry> getDiskPackEntries() { return _diskPackEntries; }
    public final FileAllocationSet getFileAllocations() { return _fileAllocations; }

    public final DiskFileCycleInfo addDiskPackEntry(final DiskPackEntry value) { _diskPackEntries.add(value); return this; }
    public final DiskFileCycleInfo setTimeOfFirstWriteOrUnload(final Instant value) { _timeOfFirstWriteOrUnload = value; return this; }
    public final DiskFileCycleInfo setDisableFlags(final DisableFlags value) { _disableFlags = value; return this; }
    public final DiskFileCycleInfo setDescriptorFlags(final DescriptorFlags value) { _descriptorFlags = value; return this; }
    public final DiskFileCycleInfo setFileFlags(final FileFlags value) { _fileFlags = value; return this; }
    public final DiskFileCycleInfo setPCHARFlags(final PCHARFlags value) { _pcharFlags = value; return this; }
    public final DiskFileCycleInfo setInitialSmoqueLink(final long value) { _initialSmoqueLink = value; return this; }
    public final DiskFileCycleInfo setAssignCount(final long value) { _assignCount = value; return this; }
    public final DiskFileCycleInfo setTimeOfLastReference(final Instant value) { _timeOfLastReference = value; return this; }
    public final DiskFileCycleInfo setTimeCataloged(final Instant value) { _timeCataloged = value; return this; }
    public final DiskFileCycleInfo setInitialGranulesReserved(final long value) { _initialGranulesReserved = value; return this; }
    public final DiskFileCycleInfo setMaxGranules(final long value) { _maxGranules = value; return this; }
    public final DiskFileCycleInfo setHighestGranuleAssigned(final long value) { _highestGranuleAssigned = value; return this; }
    public final DiskFileCycleInfo setHighestTrackWritten(final long value) { _highestTrackWritten = value; return this; }
    public final DiskFileCycleInfo setUnitSelectionIndicators(final UnitSelectionIndicators value) { _unitSelectionIndicators = value; return this; }
    public final DiskFileCycleInfo setQuotaGroupGranules(final long[] array) { _quotaGroupGranules = array ; return this; }
    public final DiskFileCycleInfo setBackupInfo(final BackupInfo value) { _backupInfo = value; return this; }
    public final DiskFileCycleInfo setDiskPackEntries(final Collection<DiskPackEntry> list) { _diskPackEntries = list; return this; }
    public final DiskFileCycleInfo setFileAllocations(final FileAllocationSet value) { _fileAllocations = value; return this; }
}
