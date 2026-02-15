/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.DateConverter;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;

import java.time.Instant;
import java.util.LinkedList;
import java.util.stream.IntStream;

/**
 * Describes a particular cycle of a fileset
 */
public abstract class FileCycleInfo {

    protected MFDRelativeAddress _leadItem0Address;
    protected MFDRelativeAddress _mainItem0Address;

    protected String _qualifier = "";
    protected String _filename = "";
    protected String _projectId = "";
    protected String _accountId = "";

    protected DisableFlags _disableFlags = new DisableFlags();
    protected DescriptorFlags _descriptorFlags = new DescriptorFlags();
    protected String _assignMnemonic = "";
    protected int _cumulativeAssignCount = 0;
    protected InhibitFlags _inhibitFlags = new InhibitFlags();
    protected int _currentAssignCount = 0;
    protected int _absoluteCycle;
    protected Instant _timeOfLastReference = null;
    protected Instant _timeCataloged = null;

    public final String getQualifier() { return _qualifier; }
    public final String getFilename() { return _filename; }
    public final String getProjectId() { return _projectId; }
    public final String getAccountId() { return _accountId; }
    public final DisableFlags getDisableFlags() { return _disableFlags; }
    public final DescriptorFlags getDescriptorFlags() { return _descriptorFlags; }
    public final String getAssignMnemonic() { return _assignMnemonic; }
    public final int getCumulativeAssignCount() { return _cumulativeAssignCount; }
    public final InhibitFlags getInhibitFlags() { return _inhibitFlags; }
    public final int getCurrentAssignCount() { return _currentAssignCount; }
    public final int getAbsoluteCycle() { return _absoluteCycle; }
    public final Instant getTimeOfLastReference() { return _timeOfLastReference; }
    public final Instant getTimeCataloged() { return _timeCataloged; }

    public final FileCycleInfo setQualifier(final String value) { _qualifier = value; return this; }
    public final FileCycleInfo setFilename(final String value) { _filename = value; return this; }
    public final FileCycleInfo setProjectId(final String value) { _projectId = value; return this; }
    public final FileCycleInfo setAccountId(final String value) { _accountId = value; return this; }
    public final FileCycleInfo setDisableFlags(final DisableFlags value) { _disableFlags = value; return this; }
    public final FileCycleInfo setDescriptorFlags(final DescriptorFlags value) { _descriptorFlags = value; return this; }
    public final FileCycleInfo setAssignMnemonic(final String value) { _assignMnemonic = value; return this; }
    public final FileCycleInfo setCumulativeAssignCount(final int value) { _cumulativeAssignCount = value; return this; }
    public final FileCycleInfo setInhibitFlags(final InhibitFlags value) { _inhibitFlags = value; return this; }
    public final FileCycleInfo setCurrentAssignCount(final int value) { _currentAssignCount = value; return this; }
    public final FileCycleInfo setAbsoluteCycle(final int value) { _absoluteCycle = value; return this; }
    public final FileCycleInfo setTimeOfLastReference(final Instant value) { _timeOfLastReference = value; return this; }
    public final FileCycleInfo setTimeCataloged(final Instant value) { _timeCataloged = value; return this; }

    /**
     * Should be overridden by sub-class, to account for number of additional backup and disk pack entries (if any).
     * Tape file cycle items do not have these, so the TapeFileCycle class can dispense with this.
     * @return required number of main items (always at least 2).
     */
    public int getRequiredNumberOfMainItems() {
        return 2;
    }

    /**
     * Loads this object from the content in the given main item MFD sector chain.
     * Should be overridden by sub-classes.
     * Ths main item chain does not include DAD table entries, nor tape reel entries (for cataloged tape files).
     * @param mfdSectors main item chain
     */
    public void loadFromMainItemChain(
        final LinkedList<MFDSector> mfdSectors
    ) {
        var sector0 = mfdSectors.getFirst().getSector();
        _leadItem0Address = new MFDRelativeAddress(sector0.get(11));
        _mainItem0Address = mfdSectors.getFirst().getAddress();

        _qualifier = (Word36.toStringFromFieldata(sector0.get(1)) + Word36.toStringFromFieldata(sector0.get(2))).trim();
        _filename = (Word36.toStringFromFieldata(sector0.get(3)) + Word36.toStringFromFieldata(sector0.get(4))).trim();
        _projectId = (Word36.toStringFromFieldata(sector0.get(5)) + Word36.toStringFromFieldata(sector0.get(6))).trim();
        _accountId = (Word36.toStringFromFieldata(sector0.get(7)) + Word36.toStringFromFieldata(sector0.get(8))).trim();
        _disableFlags = new DisableFlags().extract(sector0.getS1(013));
        _descriptorFlags = new DescriptorFlags().extract(sector0.getT1(014));
        _assignMnemonic = Word36.toStringFromFieldata(sector0.get(016)).trim();
        _cumulativeAssignCount = (int)sector0.getH2(017);
        _inhibitFlags = new InhibitFlags().extract(sector0.getS2(021));
        _currentAssignCount = (int)sector0.getT2(021);
        _absoluteCycle = (int)sector0.getT3(021);
        _timeOfLastReference = DateConverter.fromModifiedSingleWordTime(sector0.get(022));
        _timeCataloged = DateConverter.fromModifiedSingleWordTime(sector0.get(023));
    }

    /**
     * Handles the common aspects of populating cataloged file main item sectors.
     * Should be overridden by subclasses.
     * Because we completely rewrite the entries, we *will* lose the links (if any) to DAD tables or reel tables.
     * Thus, caller must account for this, taking steps to (re-)establish those links after return from here.
     * @param mfdSectors enough MFDSectors to store all the information required for this file cycle.
     * @return number of sectors populated (for us, it is always 2)
     */
    public int populateMainItems(
        final LinkedList<MFDSector> mfdSectors
    ) throws ExecStoppedException {
        if (mfdSectors.size() < getRequiredNumberOfMainItems()) {
            Exec.getInstance().stop(StopCode.ExecActivityTakenToEMode);
            throw new ExecStoppedException();
        }

        // Zero out the main items, then link them in order. There will always be at least two.
        // For sector 0:
        //  Word 0 is the first DAD table link for disk, or the first reel table link for tape.
        //      The link (bits 4-35) is empty if Bit 0 (U) is set.
        //      Bit 1 is always set, and bits 2 and 3 are always clear.
        // Word 013 contains the link to lead item sector 0 in bits 6-35 (S2-S6).
        // Word 015 contains the link to sector 1 in bits 6-35 (S2-S6).
        // For sectors 1 - n:
        //  Word 0 is the forward link
        //      Bit 0 (U) set to 1 indicates that this sector is the last, and the link section (bits 4-35) are zero.
        //      Bit 0 (U) set to 0 indicates that bits 4-35 contain the address
        //  Word 5 of sector 1 for fixed disk entries and for all sector 2+ entries contains "*NO.n*" in fieldata,
        //      where n is the main item sector number. I'm not sure if we can have more than 9 main item sectors,
        //      and if we do, I have no idea what goes here in the case of sectors 10+.
        //  Word 5 of sector 1 for removable disk entries contains security information (compartment version number).
        //  Word 6 is the link to the previous main item sector
        // Note also that words 1-2 contain the qualifier in fieldata LJSF, while 3-4 contain the filename
        // (also fieldata LJSF) for all main item sectors *except* removable disk sector 1.
        mfdSectors.forEach(ms -> IntStream.range(0, 28).forEach(x -> ms.getSector()._array[x] = 0));

        var iter = mfdSectors.iterator();
        var msFirst = iter.next();
        var msSecond = iter.next();
        msFirst.getSector().set(0, 0_600000_000000L); // assume no DAD/REEL table - we'll fix that later.
        msFirst.getSector().set(013, _leadItem0Address.getValue());
        msFirst.getSector().set(015, msSecond.getAddress().getValue());

        msSecond.getSector().set(0, 0_400000_000000L);
        if (this instanceof FixedDiskFileCycleInfo) {
            msSecond.getSector().set(5, Word36.stringToWordFieldata("*NO.1*"));
        }
        msSecond.getSector().set(6, msFirst.getAddress().getValue());

        var msPrev = msSecond;
        var sectorNumber = 1;
        while (iter.hasNext()) {
            var msNext = iter.next();
            sectorNumber++;
            msPrev.getSector().set(0, msNext.getAddress().getValue());
            msNext.getSector().set(0, 0_400000_000000L);
            msNext.getSector().set(5, Word36.stringToWordFieldata(String.format("*NO.%d*", sectorNumber)));
            msNext.getSector().set(6, msPrev.getAddress().getValue());
            msPrev = msNext;
        }

        String paddedQualifier = String.format("%-12s", _qualifier);
        String paddedFilename = String.format("%-12s", _filename);

        for (int sx = 0; sx < mfdSectors.size(); sx++) {
            if ((sx != 1) || !(this instanceof RemovableDiskFileCycleInfo)) {
                var sector = mfdSectors.get(sx).getSector();
                sector.set(1, Word36.stringToWordFieldata(paddedQualifier.substring(0, 6)));
                sector.set(2, Word36.stringToWordFieldata(paddedQualifier.substring(6)));
                sector.set(3, Word36.stringToWordFieldata(paddedFilename.substring(0, 6)));
                sector.set(4, Word36.stringToWordFieldata(paddedFilename.substring(6)));
            }
        }

        var sector0 = mfdSectors.get(0).getSector();
        var sector1 = mfdSectors.get(1).getSector();

        // Fill in additional sector 0 information common to all formats of main item sector 0
        String paddedProjectId = String.format("%-12s", _projectId);
        sector0.set(5, Word36.stringToWordFieldata(paddedProjectId.substring(0, 6)));
        sector0.set(6, Word36.stringToWordFieldata(paddedProjectId.substring(6)));

        String paddedAccountId = String.format("%-12s", _accountId);
        sector0.set(7, Word36.stringToWordFieldata(paddedAccountId.substring(0, 6)));
        sector0.set(8, Word36.stringToWordFieldata(paddedAccountId.substring(6)));

        sector0.set(11, _leadItem0Address.getValue());
        sector0.setS1(11, _disableFlags.compose());
        sector0.setT1(12, _descriptorFlags.compose());
        sector0.set(14, Word36.stringToWordFieldata(_assignMnemonic));
        sector0.set(15, _cumulativeAssignCount); // this is T2|T3 for tape, H2 for disk
        sector0.setS2(17, _inhibitFlags.compose());
        sector0.setT2(17, _currentAssignCount);
        sector0.setT3(17, _absoluteCycle);
        if (_timeOfLastReference != null) {
            sector0.set(18, DateConverter.getModifiedSingleWordTime(_timeOfLastReference));
        }
        if (_timeCataloged != null) {
            sector0.set(19, DateConverter.getModifiedSingleWordTime(_timeCataloged));
        }

        // Fill in sector 1 information common to all formats of main item sector 1
        sector1.setT3(7, _absoluteCycle);
        return 2;
    }
}
