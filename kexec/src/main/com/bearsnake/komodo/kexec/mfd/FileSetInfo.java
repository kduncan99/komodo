/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.IntStream;

/**
 * Describes a file set
 */
public class FileSetInfo {

    MFDRelativeAddress _leadItem0Address; // populated by mfd on retrieval

    private String _qualifier = "";
    private String _filename = "";
    private String _projectId = "";
    private String _readKey = ""; // empty string means no read key
    private String _writeKey = ""; // empty string means no write key
    private FileType _fileType;
    private boolean _isGuarded;
    private boolean _plusOneExists;
    private int _cycleCount; // number of f-cycles that exist, not counting to-be-cataloged / to-be-dropped
    private int _maxCycleRange;
    private int _currentCycleRange; // highest cycle - lowest cycle + 1
    private int _highestAbsoluteCycle;
    private LinkedList<FileSetCycleInfo> _cycleInfo = new LinkedList<>();
    private int _numberOfSecurityWords;
    private int _accessType;

    public String getQualifier() { return _qualifier; }
    public String getFilename() { return _filename; }
    public String getProjectId() { return _projectId; }
    public String getReadKey() { return _readKey; }
    public String getWriteKey() { return _writeKey; }
    public FileType getFileType() { return _fileType; }
    public boolean isGuarded() { return _isGuarded; }
    public boolean plusOneExists() { return _plusOneExists; }
    public int getCycleCount() { return _cycleCount; }
    public int getMaxCycleRange() { return _maxCycleRange; }
    public int getCurrentCycleRange() { return _currentCycleRange; }
    public int getHighestAbsoluteCycle() { return _highestAbsoluteCycle; }
    public LinkedList<FileSetCycleInfo> getCycleInfo() { return _cycleInfo; }
    public int getNumberOfSecurityWords() { return _numberOfSecurityWords; }
    public int getAccessType() { return _accessType; }

    public FileSetInfo addCycleInfo(final FileSetCycleInfo value) { _cycleInfo.add(value); return this; }
    public FileSetInfo setQualifier(final String value) { _qualifier = value; return this; }
    public FileSetInfo setFilename(final String value) { _filename = value; return this; }
    public FileSetInfo setProjectId(final String value) { _projectId = value; return this; }
    public FileSetInfo setReadKey(final String value) { _readKey = value; return this; }
    public FileSetInfo setWriteKey(final String value) { _writeKey = value; return this; }
    public FileSetInfo setFileType(final FileType value) { _fileType = value; return this; }
    public FileSetInfo setIsGuarded(final boolean value) { _isGuarded = value; return this; }
    public FileSetInfo setPlusOneExists(final boolean value) { _plusOneExists = value; return this; }
    public FileSetInfo setCycleCount(final int value) { _cycleCount = value; return this; }
    public FileSetInfo setMaxCycleRange(final int value) { _maxCycleRange = value; return this; }
    public FileSetInfo setCurrentCycleRange(final int value) { _currentCycleRange = value; return this; }
    public FileSetInfo setHighestAbsoluteCycle(final int value) { _highestAbsoluteCycle = value; return this; }
    public FileSetInfo setCycleInfo(final Collection<FileSetCycleInfo> list) { _cycleInfo = new LinkedList<>(list); return this; }
    public FileSetInfo setNumberOfSecurityWords(final int value) { _numberOfSecurityWords = value; return this; }
    public FileSetInfo setAccessType(final int value) { _accessType = value; return this; }

    /**
     * Indicates whether sector 1 is required based on the content of this object.
     */
    public boolean isSector1Required() {
        var cycleCapacity = 28 - 11 - _numberOfSecurityWords;
        return _cycleCount > cycleCapacity;
    }

    /**
     * Adds a FileSetCycleInfo object into our collection at the correct point
     * as defined by the absolute cycle value.
     */
    public void mergeFileSetCycleInfo(
        final FileSetCycleInfo fsci
    ) {
        if (_cycleInfo.isEmpty()) {
            _cycleInfo.add(fsci);
            _highestAbsoluteCycle = fsci.getAbsoluteCycle();
            _currentCycleRange = 1;
            _cycleCount = 1;
        } else {
            for (int cx = 0; cx < _cycleInfo.size(); ++cx) {
                if (_cycleInfo.get(cx).getAbsoluteCycle() < fsci.getAbsoluteCycle()) {
                    _cycleInfo.add(cx, fsci);
                    break;
                }
            }
            if (fsci.getAbsoluteCycle() > _highestAbsoluteCycle) {
                _highestAbsoluteCycle = fsci.getAbsoluteCycle();
            }
            _currentCycleRange = _highestAbsoluteCycle - _cycleInfo.getLast().getAbsoluteCycle() + 1;
            _cycleCount = _cycleInfo.size();
        }
    }

    /**
     * Populates the given lead item sector(s).
     * @param mfdSectors either 1 or 2 MFDSectors representing lead item 0 and optionally, lead item 1.
     */
    public void populateLeadItemSectors(
        LinkedList<MFDSector> mfdSectors
    ) throws ExecStoppedException {
        if (isSector1Required() && (mfdSectors.size() < 2)) {
            Exec.getInstance().stop(StopCode.ExecActivityTakenToEMode);
            throw new ExecStoppedException();
        }

        mfdSectors.forEach(ms -> IntStream.range(0, 28).forEach(x -> ms.getSector()._array[x] = 0));
        var sector0 = mfdSectors.getFirst().getSector();
        var sector1 = mfdSectors.size() > 1 ? mfdSectors.get(1).getSector() : null;

        sector0.set(0, 0_500000_000000L);

        String paddedQualifier = String.format("%-12s", _qualifier);
        sector0.set(1, Word36.stringToWordFieldata(paddedQualifier.substring(0, 6)));
        sector0.set(2, Word36.stringToWordFieldata(paddedQualifier.substring(6)));

        String paddedFilename = String.format("%-12s", _filename);
        sector0.set(3, Word36.stringToWordFieldata(paddedFilename.substring(0, 6)));
        sector0.set(4, Word36.stringToWordFieldata(paddedFilename.substring(6)));

        String paddedProjectId = String.format("%-12s", _projectId);
        sector0.set(5, Word36.stringToWordFieldata(paddedProjectId.substring(0, 6)));
        sector0.set(6, Word36.stringToWordFieldata(paddedProjectId.substring(6)));

        sector0.set(7, Word36.stringToWordFieldata(_readKey));
        sector0.set(8, Word36.stringToWordFieldata(_writeKey));

        sector0.setS1(9, _fileType.getValue());
        sector0.setS2(9, _cycleCount);
        sector0.setS3(9, _maxCycleRange);
        sector0.setS4(9, _currentCycleRange);
        sector0.setT3(9, _highestAbsoluteCycle);

        int statusBits = (_isGuarded ? 0_4000 : 0) | (_plusOneExists ? 0_2000 : 0);
        sector0.setT1(10, statusBits);
        sector0.setS4(10, _numberOfSecurityWords);
        sector0.setT3(10, _accessType);

        // point to first cycle word
        ArraySlice[] slices = new ArraySlice[]{ sector0, sector1 };
        int sx = 0; // item index
        int wx = 11 + _numberOfSecurityWords;
        for (var cycInfo : _cycleInfo) {
            var w = cycInfo.getMainItem0Address().getValue();
            if (cycInfo.isToBeCataloged()) {
                w |= 0_600000_000000L;
            }
            if (cycInfo.isToBeDropped()) {
                w |= 0_500000_000000L;
            }
            slices[sx].set(wx, w);
            wx++;
            if (wx == 28) {
                wx = 0;
                sx++;
            }
        }
    }
}
