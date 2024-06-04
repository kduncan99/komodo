/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import java.util.LinkedList;

public class RemovableDiskFileCycleInfo extends DiskFileCycleInfo {

    private String _readKey = "";
    private String _writeKey = "";
    protected LinkedList<DiskPackEntry> _diskPackEntries = new LinkedList<>();

    public final DiskFileCycleInfo addDiskPackEntry(final DiskPackEntry value) { _diskPackEntries.add(value); return this; }

    public final String getReadKey() { return _readKey; }
    public final String getWriteKey() { return _writeKey; }
    public final LinkedList<DiskPackEntry> getDiskPackEntries() { return _diskPackEntries; }

    public RemovableDiskFileCycleInfo setReadKey(final String readKey) { _readKey = readKey; return this; }
    public RemovableDiskFileCycleInfo setWriteKey(final String writeKey) { _writeKey = writeKey; return this; }
    public final DiskFileCycleInfo setDiskPackEntries(final LinkedList<DiskPackEntry> list) { _diskPackEntries = list; return this; }

    @Override
    public int getRequiredNumberOfMainItems() {
        int count = super.getRequiredNumberOfMainItems();
        int mod = (_diskPackEntries.size() - 5) / 10;
        count += ((_diskPackEntries.size() - 5) / 10) + (mod > 0 ? 1 : 0);
        return count;
    }

    /**
     * Loads this object from the content in the given main item MFD sector chain.
     * @param mfdSectors main item chain
     */
    public void loadFromMainItemChain(
        final LinkedList<MFDSector> mfdSectors
    ) {
        super.loadFromMainItemChain(mfdSectors);
        var sector0 = mfdSectors.getFirst().getSector();
        var sector1 = mfdSectors.get(1).getSector();

        var wRead = (sector0.getH1(030) << 18) | sector0.getH1(031);
        _readKey = Word36.toStringFromFieldata(wRead);
        var wWrite = (sector0.getH1(032) << 18) | sector0.getH1(033);
        _writeKey = Word36.toStringFromFieldata(wWrite);

        // disk pack entries
        _diskPackEntries.clear();
        long numberOfDPEs = sector1.getT3(021);
        for (int dpx = 0; (dpx < 5) && (dpx < numberOfDPEs); dpx++) {
            var wx = 022 + (2 * dpx);
            var packName = Word36.toStringFromFieldata(sector1.get(wx));
            var mainItemAddr = sector1.get(wx + 1);
            _diskPackEntries.add(new DiskPackEntry(packName, new MFDRelativeAddress(mainItemAddr)));
        }

        long numberOfBackupWords = sector1.getT1(07);
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
     * Populates cataloged file main item sectors 0 and 1
     * Invokes super class to do the most common things, then fills in anything related to mass storage
     * @param mfdSectors enough MFDSectors to store all of the information required for this file cycle.
     * @return number of sectors we populated
     */
    @Override
    public int populateMainItems(
        final LinkedList<MFDSector> mfdSectors
    ) throws ExecStoppedException {
        var sectorsUsed = super.populateMainItems(mfdSectors);
        var sector0 = mfdSectors.get(0).getSector();
        var sector1 = mfdSectors.get(1).getSector();

        var wReadKey = Word36.stringToWordFieldata(_readKey);
        var wWriteKey = Word36.stringToWordFieldata(_writeKey);
        sector0.setH1(24, wReadKey >> 18);
        sector0.setH1(25, wReadKey & 0_777777);
        sector0.setH1(26, wWriteKey >> 18);
        sector0.setH1(27, wWriteKey & 0_777777);

        // disk pack entries - write the first 5 into sector 1
        sector1.setT3(021, _diskPackEntries.size());
        int dpx = 0;
        int wx = 022;
        while (dpx < _diskPackEntries.size() && dpx < 5) {
            var dpe = _diskPackEntries.get(dpx++);
            sector1.set(wx, Word36.stringToWordFieldata(dpe.getPackName()));
            sector1.set(wx + 1, dpe.getMainItem0Address().getValue());
            wx += 2;
        }

        // If there are more, they go into separate sectors
        var entryCount = 10;
        ArraySlice dpeSector = null;
        while (dpx < _diskPackEntries.size()) {
            if (entryCount == 10) {
                dpeSector = mfdSectors.get(sectorsUsed++).getSector();
                dpeSector.setS1(7, 0); // entry type
                wx = 8;
                entryCount = 0;
            }

            var dpe = _diskPackEntries.get(dpx++);
            dpeSector.set(wx, Word36.stringToWordFieldata(dpe.getPackName()));
            dpeSector.set(wx + 1, dpe.getMainItem0Address().getValue());
            wx += 2;
            entryCount++;
        }

        return sectorsUsed;
    }
}
