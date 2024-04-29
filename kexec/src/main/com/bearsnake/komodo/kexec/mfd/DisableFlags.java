/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

public class DisableFlags {

    private boolean _directoryError;
    private boolean _assignedAndWrittenAtExecStop;
    private boolean _inaccessibleBackup;
    private boolean _cacheDrainFailure;

    public int compose() {
        int value = _directoryError ? 060 : 0;
        value |= _assignedAndWrittenAtExecStop ? 050 : 0;
        value |= _inaccessibleBackup ? 044 : 0;
        value |= _cacheDrainFailure ? 042 : 0;
        return value;
    }

    public DisableFlags extract(final long value) {
        _directoryError = (value & 020) != 0;
        _assignedAndWrittenAtExecStop = (value & 010) != 0;
        _inaccessibleBackup = (value & 004) != 0;
        _cacheDrainFailure = (value & 002) != 0;

        return this;
    }

    public static DisableFlags extractFrom(final int value) {
        var inf = new DisableFlags();
        inf.extract(value);
        return inf;
    }

    public DisableFlags setDirectoryError(final boolean value) { _directoryError = value; return this; }
    public DisableFlags setAssignedAndWrittenAtExecStop(final boolean value) { _assignedAndWrittenAtExecStop = value; return this; }
    public DisableFlags setInaccessibleBackup(final boolean value) { _inaccessibleBackup = value; return this; }
    public DisableFlags setCacheDrainFailure(final boolean value) { _cacheDrainFailure = value; return this; }
}
