/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.baselib.ArraySlice;
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

    protected final FileSetInfo _fileSetInfo;
    protected LinkedList<MFDRelativeAddress> _mainItemAddresses;
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

    protected FileCycleInfo(
        final FileSetInfo fileSetInfo
    ) {
        _fileSetInfo = fileSetInfo;
        _qualifier = fileSetInfo.getQualifier();
        _filename = fileSetInfo.getFilename();
        _projectId = fileSetInfo.getProjectId();
    }

    public final FileSetInfo getFileSetInfo() { return _fileSetInfo; }
    public final MFDRelativeAddress getMainItem0Address() { return _mainItemAddresses.getFirst(); }
    public final MFDRelativeAddress getMainItem1Address() { return _mainItemAddresses.get(1); }
    public final LinkedList<MFDRelativeAddress> getMainItemAddresses() { return _mainItemAddresses; }
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

    public final FileCycleInfo addMainItemAddress(final MFDRelativeAddress value) { _mainItemAddresses.add(value); return this; }
    public final FileCycleInfo setMainItemAddresses(final LinkedList<MFDRelativeAddress> list) {
        _mainItemAddresses = new LinkedList<>(list);
        return this;
    }
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

    public int getRequiredNumberOfMainItems() { return 2; }

    /**
     * Handles the common aspects of populating cataloged file main item sectors 0 and 1
     * Should be overridden by sub-classes.
     * The _mainItemAddresses entity must be loaded with the address of all the sectors
     * presented in mainItemSectors.
     */
    public void populateMainItems(
        final LinkedList<ArraySlice> mainItemSectors
    ) throws ExecStoppedException {
        if (mainItemSectors.size() < getRequiredNumberOfMainItems()) {
            Exec.getInstance().stop(StopCode.ExecActivityTakenToEMode);
            throw new ExecStoppedException();
        }

        for (var sector : mainItemSectors) {
            IntStream.range(0, 28).forEach(vx -> sector._array[vx] = 0);
        }

        var sector0 = mainItemSectors.get(0);
        var sector1 = mainItemSectors.get(1);

        sector0.set(0, 0_600000_000000L);

        String paddedQualifier = String.format("%-12s", _qualifier);
        sector0.set(1, Word36.stringToWordFieldata(paddedQualifier.substring(0, 6)));
        sector0.set(2, Word36.stringToWordFieldata(paddedQualifier.substring(6)));

        String paddedFilename = String.format("%-12s", _filename);
        sector0.set(3, Word36.stringToWordFieldata(paddedFilename.substring(0, 6)));
        sector0.set(4, Word36.stringToWordFieldata(paddedFilename.substring(6)));

        String paddedProjectId = String.format("%-12s", _projectId);
        sector0.set(5, Word36.stringToWordFieldata(paddedProjectId.substring(0, 6)));
        sector0.set(6, Word36.stringToWordFieldata(paddedProjectId.substring(6)));

        String paddedAccountId = String.format("%-12s", _accountId);
        sector0.set(7, Word36.stringToWordFieldata(paddedAccountId.substring(0, 6)));
        sector0.set(8, Word36.stringToWordFieldata(paddedAccountId.substring(6)));

        sector0.set(11, _fileSetInfo.getLeadItem0Address().getValue());
        sector0.setS1(11, _disableFlags.compose());
        sector0.setT1(12, _descriptorFlags.compose());
        sector0.set(13, _mainItemAddresses.get(1).getValue());
        sector0.set(14, Word36.stringToWordFieldata(_assignMnemonic));
        sector0.set(15, _cumulativeAssignCount); // this is T2|T3 for tape, H2 for disk
        sector0.setS2(17, _inhibitFlags.compose());
        sector0.setT2(17, _currentAssignCount);
        sector0.setT3(17, _absoluteCycle);
        sector0.set(18, DateConverter.getModifiedSingleWordTime(_timeOfLastReference));
        sector0.set(19, DateConverter.getModifiedSingleWordTime(_timeCataloged));

        sector1.set(0, 0_400000_000000L);
        sector1.set(6, _mainItemAddresses.getFirst().getValue());
        sector1.setT3(7, _absoluteCycle);

        // link all the sectors and set up the qual/file and identifiers
        var prev = sector1;
        var prevAddr = _mainItemAddresses.get(1);
        for (int sx = 2; sx < _mainItemAddresses.size(); sx++) {
            prev.set(0, _mainItemAddresses.get(sx).getValue());
            var sector = mainItemSectors.get(sx);
            sector.set(1, Word36.stringToWordFieldata(paddedQualifier.substring(0, 6)));
            sector.set(2, Word36.stringToWordFieldata(paddedQualifier.substring(6)));
            sector.set(3, Word36.stringToWordFieldata(paddedFilename.substring(0, 6)));
            sector.set(4, Word36.stringToWordFieldata(paddedFilename.substring(6)));
            var ident = String.format("*NO.%1d*", sx);//TODO what if sx is > 9?
            sector.set(5, Word36.stringToWordFieldata(ident));
            sector.set(6, prevAddr.getValue());
            sector.setT3(7, _absoluteCycle);
            prev = sector;
            prevAddr = _mainItemAddresses.get(sx);
        }
        prev.set(0, 0_400000_000000L);
    }
}
