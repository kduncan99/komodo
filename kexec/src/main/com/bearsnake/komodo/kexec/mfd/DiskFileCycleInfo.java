/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.DateConverter;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import java.time.Instant;
import java.util.LinkedList;

public abstract class DiskFileCycleInfo extends FileCycleInfo {

    protected DiskFileCycleInfo(FileSetInfo fileSetInfo) {
        super(fileSetInfo);
    }

    protected Instant _timeOfFirstWriteOrUnload = null;
    protected FileFlags _fileFlags = new FileFlags();
    protected PCHARFlags _pcharFlags = new PCHARFlags();
    protected long _initialSMOQUELink = 0;
    protected long _initialGranulesReserved = 0;
    protected long _maxGranules = 0;
    protected long _highestGranuleAssigned = 0;
    protected long _highestTrackWritten = 0;
    protected long[] _quotaGroupGranules = new long[8];
    protected BackupInfo _backupInfo = new BackupInfo();
    protected LinkedList<DiskPackEntry> _diskPackEntries = new LinkedList<>();
    protected FileAllocationSet _fileAllocations = new FileAllocationSet();

    public final DiskFileCycleInfo addDiskPackEntry(final DiskPackEntry value) { _diskPackEntries.add(value); return this; }

    public final Instant getTimeOfFirstWriteOrUnload() { return _timeOfFirstWriteOrUnload; }
    public final FileFlags getFileFlags() { return _fileFlags; }
    public final PCHARFlags getPCHARFlags() { return _pcharFlags; }
    public final long getInitialSMOQUELink() { return _initialSMOQUELink; }
    public final long getInitialGranulesReserved() { return _initialGranulesReserved; }
    public final long getMaxGranules() { return _maxGranules; }
    public final long getHighestGranuleAssigned() { return _highestGranuleAssigned; }
    public final long getHighestTrackWritten() { return _highestTrackWritten; }
    public final long[] getQuotaGroupGranules() { return _quotaGroupGranules; }
    public final BackupInfo getBackupInfo() { return _backupInfo; }
    public final LinkedList<DiskPackEntry> getDiskPackEntries() { return _diskPackEntries; }
    public final FileAllocationSet getFileAllocations() { return _fileAllocations; }

    public final DiskFileCycleInfo setTimeOfFirstWriteOrUnload(final Instant value) { _timeOfFirstWriteOrUnload = value; return this; }
    public final DiskFileCycleInfo setFileFlags(final FileFlags value) { _fileFlags = value; return this; }
    public final DiskFileCycleInfo setPCHARFlags(final PCHARFlags value) { _pcharFlags = value; return this; }
    public final DiskFileCycleInfo setInitialSMOQUELink(final long value) { _initialSMOQUELink = value; return this; }
    public final DiskFileCycleInfo setInitialGranulesReserved(final long value) { _initialGranulesReserved = value; return this; }
    public final DiskFileCycleInfo setMaxGranules(final long value) { _maxGranules = value; return this; }
    public final DiskFileCycleInfo setHighestGranuleAssigned(final long value) { _highestGranuleAssigned = value; return this; }
    public final DiskFileCycleInfo setHighestTrackWritten(final long value) { _highestTrackWritten = value; return this; }
    public final DiskFileCycleInfo setQuotaGroupGranules(final long[] array) { _quotaGroupGranules = array ; return this; }
    public final DiskFileCycleInfo setBackupInfo(final BackupInfo value) { _backupInfo = value; return this; }
    public final DiskFileCycleInfo setDiskPackEntries(final LinkedList<DiskPackEntry> list) { _diskPackEntries = list; return this; }
    public final DiskFileCycleInfo setFileAllocations(final FileAllocationSet value) { _fileAllocations = value; return this; }

    @Override
    public int getRequiredNumberOfMainItems() {
        int count = super.getRequiredNumberOfMainItems();
        int mod = (_diskPackEntries.size() - 5) / 10;
        count += ((_diskPackEntries.size() - 5) / 10) + (mod > 0 ? 1 : 0);
        mod = (_backupInfo.getBackupReelNumbers().size() - 2) / 20;
        count += ((_backupInfo.getBackupReelNumbers().size() - 2) / 20) + (mod > 0 ? 1 : 0);
        return count;
    }

    /**
     * Populates cataloged file main item sectors 0 and 1 - completely overwriting anything previous.
     * Invokes super class to do the most common things, then fills in anything related to mass storage
     */
    @Override
    public void populateMainItems(
        final LinkedList<ArraySlice> mainItemSectors
    ) throws ExecStoppedException {
        super.populateMainItems(mainItemSectors);
        var sector0 = mainItemSectors.get(0);
        var sector1 = mainItemSectors.get(1);

        sector0.set(10, DateConverter.getModifiedSingleWordTime(_timeOfFirstWriteOrUnload));
        sector0.setS3(12, _fileFlags.compose());
        sector0.setS1(13, _pcharFlags.compose());
        sector0.setH1(15, _initialSMOQUELink);
        sector0.setH1(20, _initialGranulesReserved);
        sector0.setH1(21, _maxGranules);
        sector0.setH1(22, _highestGranuleAssigned);
        sector0.setH1(23, _highestTrackWritten);
        for (int x = 0; x < 8; x++) {
            sector0.setH2(20 + x, _quotaGroupGranules[x]);
        }

        // disk pack entries
        sector1.setT3(021, _diskPackEntries.size());
        int wx = 022;
        int sx = 1;
        var dpeSector = sector1;
        for (DiskPackEntry dpe : _diskPackEntries) {
            dpeSector.set(wx, Word36.stringToWordFieldata(dpe.getPackName()));
            dpeSector.set(wx + 1, dpe.getMainItem0Address().getValue());
            wx++;
            if (wx == 28) {
                sx++;
                dpeSector = mainItemSectors.get(sx);
                dpeSector.setS1(7, 0); // entry type
                wx = 8;
            }
        }

        // backup info
        if (_backupInfo == null) {
            // no backup info
            sector1.setT1(7, 0); // number of backup words
        } else {
            var reelNums = _backupInfo.getBackupReelNumbers();
            sector1.setT1(7, _backupInfo.getNumberOfBackupWords());
            sector1.setS1(8, 1);// max backup levels
            sector1.setS2(8, _backupInfo.getFASBits());
            sector1.setS3(8, 1);// current backup levels
            sector1.setH2(8, _backupInfo.getNumberOfBackupWords());
            sector1.set(9, Word36.stringToWordFieldata(reelNums.getFirst()));
            if (reelNums.size() > 1) {
                sector1.set(10, Word36.stringToWordFieldata(reelNums.get(1)));
                sx++;
                var rnSector = mainItemSectors.get(sx);
                rnSector.setS1(7, 1); // entry type
                wx = 8;
                for (int rnx = 2; rnx < reelNums.size(); rnx++) {
                    rnSector.set(wx, Word36.stringToWordFieldata(reelNums.get(rnx)));
                    wx++;
                    if (wx == 28) {
                        sx++;
                        rnSector = mainItemSectors.get(sx);
                        rnSector.setS1(7, 1); // entry type
                        wx = 8;
                    }
                }
            }
        }
    }
}
