/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Describes a file set
 */
public class FileSetInfo {

    private MFDRelativeAddress _leadItem0Address;
    private String _qualifier;
    private String _filename;
    private String _projectId;
    private String _readKey;
    private String _writeKey;
    private FileType _fileType;
    private boolean _isGuarded;
    private boolean _plusOneExists;
    private int _cycleCount; // number of f-cycles that exist, not counting to-be-cataloged / to-be-dropped
    private int _maxCycleRange; //
    private int _currentCycleRange; // highest cycle - lowest cycle + 1
    private int _highestAbsoluteCycle;
    private Collection<FileSetCycleInfo> _cycleInfo = new LinkedList<>();
    private int _numberOfSecurityWords;

    public MFDRelativeAddress getLeadItem0Address() { return _leadItem0Address; }
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
    public Collection<FileSetCycleInfo> getCycleInfo() { return _cycleInfo; }
    public int getNumberOfSecurityWords() { return _numberOfSecurityWords; }

    public FileSetInfo addCycleInfo(final FileSetCycleInfo value) { _cycleInfo.add(value); return this; }
    public FileSetInfo setLeadItem0Address(final MFDRelativeAddress value) { _leadItem0Address = value; return this; }
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
    public FileSetInfo setCycleInfo(final Collection<FileSetCycleInfo> list) { _cycleInfo = list; return this; }
    public FileSetInfo setNumberOfSecurityWords(final int value) { _numberOfSecurityWords = value; return this; }
}
