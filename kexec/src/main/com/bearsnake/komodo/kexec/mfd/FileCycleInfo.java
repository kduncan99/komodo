/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

public abstract class FileCycleInfo {

    private MFDRelativeAddress _mainItem0Address;
    private String _accountId;
    private String _qualifier;
    private String _filename;
    private String _projectId;
    private InhibitFlags _inhibitFlags;
    private int _absoluteCycle;
    private String _assignMnemonic;
    private boolean _isAssigned;

    public final MFDRelativeAddress getMainItem0Address() { return _mainItem0Address; }
    public final String getQualifier() { return _qualifier; }
    public final String getFilename() { return _filename; }
    public final String getProjectId() { return _projectId; }
    public final String getAccountId() { return _accountId; }
    public final InhibitFlags getInhibitFlags() { return _inhibitFlags; }
    public final int getAbsoluteCycle() { return _absoluteCycle; }
    public final String getAssignMnemonic() { return _assignMnemonic; }
    public final boolean isAssigned() { return _isAssigned; }

    public final FileCycleInfo setMainItem0Address(final MFDRelativeAddress value) { _mainItem0Address = value; return this; }
    public final FileCycleInfo setQualifier(final String value) { _qualifier = value; return this; }
    public final FileCycleInfo setFilename(final String value) { _filename = value; return this; }
    public final FileCycleInfo setProjectId(final String value) { _projectId = value; return this; }
    public final FileCycleInfo setAccountId(final String value) { _accountId = value; return this; }
    public final FileCycleInfo setInhibitFlags(final InhibitFlags value) { _inhibitFlags = value; return this; }
    public final FileCycleInfo setAbsoluteCycle(final int value) { _absoluteCycle = value; return this; }
    public final FileCycleInfo setAssignMnemonic(final String value) { _assignMnemonic = value; return this; }
    public final FileCycleInfo setIsAssigned(final boolean value) { _isAssigned = value; return this; }

    public static class DiskPackEntry {
        // TODO what goes in here?
    }
}
