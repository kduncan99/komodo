/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities.facItems;

import com.bearsnake.komodo.hardwarelib.Node;

import java.io.PrintStream;

public abstract class DiskFileFacilitiesItem extends FacilitiesItem {

    protected boolean _deleteOnAnyRunTermination;
    protected boolean _deleteOnNormalRunTermination;
    protected boolean _isExclusive;
    protected boolean _isReadable;
    protected boolean _isWriteable;
    protected boolean _waitingForExclusiveRelease; // some other run has x-use on the file.
    protected boolean _waitingForExclusiveUse;     // some other run(s) has/have assigned the file.
    protected boolean _waitingForRollback;         // file is rolled out

    public DiskFileFacilitiesItem(
        final Node node,
        final String requestedPackName
    ) {
    }

    @Override
    public void dump(final PrintStream out,
                     final String indent) {
        super.dump(out, indent);
//        out.printf("%s  node:%s reqPack:%s asg:%s\n", indent, _node.getNodeName(), _requestedPackName, _isAssigned);
    }

    public final boolean deleteOnAnyRunTermination() { return _deleteOnAnyRunTermination; }
    public final boolean deleteOnNormalRunTermination() { return _deleteOnNormalRunTermination; }
    public final boolean isExclusive(){ return _isExclusive; }
    public final boolean isReadable() { return _isReadable; }
    public final boolean isWaitingForExclusiveRelease() { return _waitingForExclusiveRelease; }
    public final boolean isWaitingForExclusiveUse() { return _waitingForExclusiveRelease; }
    public final boolean isWaitingForRollback() { return _waitingForRollback; }
    public final boolean isWriteable() { return _isWriteable; }

    public final DiskFileFacilitiesItem setDeleteOnAnyRunTermination(final boolean value) { _deleteOnAnyRunTermination = value; return this; }
    public final DiskFileFacilitiesItem setDeleteOnNormalRunTermination(final boolean value) { _deleteOnNormalRunTermination = value; return this; }
    public final DiskFileFacilitiesItem setIsExclusive(final boolean value) { _isExclusive = value; return this; }
    public final DiskFileFacilitiesItem setIsReadable(final boolean value) { _isReadable = value; return this; }
    public final DiskFileFacilitiesItem setIsWaitingForExclusiveRelease(final boolean value) { _waitingForExclusiveRelease = value; return this; }
    public final DiskFileFacilitiesItem setIsWaitingForExclusiveUse(final boolean value) { _waitingForExclusiveUse = value; return this; }
    public final DiskFileFacilitiesItem setIsWaitingForRollback(final boolean value) { _waitingForRollback = value; return this; }
    public final DiskFileFacilitiesItem setIsWriteable(final boolean value) { _isWriteable = value; return this; }
}
