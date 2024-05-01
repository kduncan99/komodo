/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.DateConverter;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import java.time.Instant;
import java.util.LinkedList;

public abstract class DiskFileCycleInfo extends FileCycleInfo {

    protected DiskFileCycleInfo(
        final MFDSector leadItem0
    ) {
        super(leadItem0);
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
     * Loads this object from the content in the given main item MFD sector chain.
     * Should be overridden by sub-classes.
     * @param mfdSectors main item chain
     */
    public void loadFromMainItemChain(
        final LinkedList<MFDSector> mfdSectors
    ) {
        super.loadFromMainItemChain(mfdSectors);
        var sector0 = mfdSectors.get(0).getSector();
        var sector1 = mfdSectors.get(1).getSector();

        _timeOfFirstWriteOrUnload = DateConverter.fromModifiedSingleWordTime(sector0.get(012));
        _fileFlags = new FileFlags().extract(sector0.getS3(014));
        _pcharFlags = new PCHARFlags().extract(sector0.getS1(015));
        _initialSMOQUELink = sector0.getH1(017);
        _initialGranulesReserved = sector0.getH1(024);
        _maxGranules = sector0.getH1(025);
        _highestGranuleAssigned = sector0.getH1(026);
        _highestTrackWritten = sector0.getH1(027);
        for (int x = 0; x < 8; x++) {
            _quotaGroupGranules[x] = sector0.getH2(x + 024);
        }

        // backup info
        _backupInfo = null;
        long numberOfBackupWords = sector1.getT1(07);
        var currentNumberOfBackupLevels = sector1.getS3(011);
        if (currentNumberOfBackupLevels > 0) {
            _backupInfo = new BackupInfo().setTimeBackupCreated(DateConverter.fromModifiedSingleWordTime(sector1.get(010)))
                                          .setFASBits(sector1.getS2(011))
                                          .setStartingFilePosition(sector1.get(012))
                                          .setNumberOfTextBlocks(sector1.getH2(011));
            if (numberOfBackupWords > 0) {
                _backupInfo.addBackupReelNumber(Word36.toStringFromFieldata(sector1.get(013)));
                if (numberOfBackupWords > 1) {
                    _backupInfo.addBackupReelNumber(Word36.toStringFromFieldata(sector1.get(014)));
                }
            }
        }

        // disk pack entries
        _diskPackEntries.clear();
        long numberOfDPEs = sector1.getT3(021);
        for (int dpx = 0; (dpx < 5) && (dpx < numberOfDPEs); dpx++) {
            var wx = 022 + (2 * dpx);
            var packName = Word36.toStringFromFieldata(sector1.get(wx));
            var mainItemAddr = sector1.get(wx + 1);
            _diskPackEntries.add(new DiskPackEntry(packName, new MFDRelativeAddress(mainItemAddr)));
        }

        for (int sx = 2; sx < mfdSectors.size(); sx++) {
            var sector = mfdSectors.get(sx).getSector();
            var entryType = sector.getS1(07);
            if (entryType == 0) {
                // Up to 10 disk pack entries (of 2 words each).
                // Total number of disk pack entries is in T3 of sector1[021]
                for (int ex = 0; (ex < 10) && (_diskPackEntries.size() < numberOfDPEs); ex++) {
                    var wx = 010 + (2 * ex);
                    var packName = Word36.toStringFromFieldata(sector1.get(wx));
                    var mainItemAddr = sector1.get(wx + 1);
                    _diskPackEntries.add(new DiskPackEntry(packName, new MFDRelativeAddress(mainItemAddr)));
                }
            } else if ((_backupInfo != null)
                && (entryType == 1)
                && (_backupInfo.getBackupReelNumbers().size() < numberOfBackupWords)) {
                // Up to 20 backup reel entries (of 1 word each) -
                // Total number of backup reel entries can be derived from number-of-backup-words.
                // If we support only one level of backups, then this will be the number of reels.
                for (int ex = 0; (ex < 20) && (_backupInfo.getBackupReelNumbers().size() < numberOfBackupWords); ex++) {
                    _backupInfo.addBackupReelNumber(Word36.toStringFromFieldata(sector.get(8 + ex)));
                }
            }
        }
    }

    /**
     * Populates cataloged file main item sectors 0 and 1 - completely overwriting anything previous.
     * Invokes super class to do the most common things, then fills in anything related to mass storage
     * @param mfdSectors enough MFDSectors to store all the information required for this file cycle.
     */
    @Override
    public void populateMainItems(
        final LinkedList<MFDSector> mfdSectors
    ) throws ExecStoppedException {
        super.populateMainItems(mfdSectors);
        var sector0 = mfdSectors.get(0).getSector();
        var sector1 = mfdSectors.get(1).getSector();

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
                dpeSector = mfdSectors.get(sx).getSector();
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
                var rnSector = mfdSectors.get(sx).getSector();
                rnSector.setS1(7, 1); // entry type
                wx = 8;
                for (int rnx = 2; rnx < reelNums.size(); rnx++) {
                    rnSector.set(wx, Word36.stringToWordFieldata(reelNums.get(rnx)));
                    wx++;
                    if (wx == 28) {
                        sx++;
                        rnSector = mfdSectors.get(sx).getSector();
                        rnSector.setS1(7, 1); // entry type
                        wx = 8;
                    }
                }
            }
        }
    }
}
