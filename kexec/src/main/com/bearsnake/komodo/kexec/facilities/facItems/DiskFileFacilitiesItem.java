/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities.facItems;

import com.bearsnake.komodo.kexec.mfd.AcceleratedCycleInfo;

import java.io.PrintStream;

public abstract class DiskFileFacilitiesItem extends FacilitiesItem {

    protected AcceleratedCycleInfo _acceleratedCycleInfo; // only for cataloged disk files
    protected boolean _deleteOnAnyRunTermination;
    protected boolean _deleteOnNormalRunTermination;
    protected boolean _isExclusive;
    protected boolean _isReadable;
    protected boolean _isWriteable;
    protected boolean _waitingForExclusiveRelease; // some other run has x-use on the file.
    protected boolean _waitingForExclusiveUse;     // some other run(s) has/have assigned the file.
    protected boolean _waitingForRollback;         // file is rolled out

    @Override
    public void dump(final PrintStream out,
                     final String indent) {
        super.dump(out, indent);
        out.printf("%s  delAnyTerm:%s delNormTerm:%s excl:%s rd:%s wr:%s\n",
                   indent, _deleteOnAnyRunTermination, _deleteOnNormalRunTermination,
                   _isExclusive, _isReadable, _isWriteable);
        if (_waitingForExclusiveRelease || _waitingForExclusiveUse || _waitingForRollback) {
            out.printf("%s  HOLDS:%s%s%s\n", indent,
                       _waitingForExclusiveRelease ? " X-USE-REL" : "",
                       _waitingForExclusiveUse ? " X-USE" : "",
                       _waitingForRollback ? " ROLBAK" : "");
        }
    }

    public final boolean deleteOnAnyRunTermination() { return _deleteOnAnyRunTermination; }
    public final boolean deleteOnNormalRunTermination() { return _deleteOnNormalRunTermination; }
    public final AcceleratedCycleInfo getAcceleratedCycleInfo() { return _acceleratedCycleInfo; }
    public final boolean isExclusive(){ return _isExclusive; }
    public final boolean isReadable() { return _isReadable; }
    public final boolean isWaitingForExclusiveRelease() { return _waitingForExclusiveRelease; }
    public final boolean isWaitingForExclusiveUse() { return _waitingForExclusiveRelease; }
    public final boolean isWaitingForRollback() { return _waitingForRollback; }
    public final boolean isWriteable() { return _isWriteable; }

    public boolean isWaiting() {
        return _waitingForExclusiveRelease || _waitingForExclusiveUse || _waitingForRollback;
    }

    public final DiskFileFacilitiesItem setAcceleratedCycleInfo(final AcceleratedCycleInfo value) { _acceleratedCycleInfo = value; return this; }
    public final DiskFileFacilitiesItem setDeleteOnAnyRunTermination(final boolean value) { _deleteOnAnyRunTermination = value; return this; }
    public final DiskFileFacilitiesItem setDeleteOnNormalRunTermination(final boolean value) { _deleteOnNormalRunTermination = value; return this; }
    public final DiskFileFacilitiesItem setIsExclusive(final boolean value) { _isExclusive = value; return this; }
    public final DiskFileFacilitiesItem setIsReadable(final boolean value) { _isReadable = value; return this; }
    public final DiskFileFacilitiesItem setIsWaitingForExclusiveRelease(final boolean value) { _waitingForExclusiveRelease = value; return this; }
    public final DiskFileFacilitiesItem setIsWaitingForExclusiveUse(final boolean value) { _waitingForExclusiveUse = value; return this; }
    public final DiskFileFacilitiesItem setIsWaitingForRollback(final boolean value) { _waitingForRollback = value; return this; }
    public final DiskFileFacilitiesItem setIsWriteable(final boolean value) { _isWriteable = value; return this; }
}
