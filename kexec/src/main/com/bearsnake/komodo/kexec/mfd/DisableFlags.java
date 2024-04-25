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
        int value = _directoryError ? 020 : 0;
        value |= _assignedAndWrittenAtExecStop ? 010 : 0;
        value |= _inaccessibleBackup ? 004 : 0;
        value |= _cacheDrainFailure ? 002 : 0;
        return value;
    }

    public void extract(final int value) {
        _directoryError = (value & 020) != 0;
        _assignedAndWrittenAtExecStop = (value & 010) != 0;
        _inaccessibleBackup = (value & 004) != 0;
        _cacheDrainFailure = (value & 002) != 0;
    }

    public static DisableFlags extractFrom(final int value) {
        var inf = new DisableFlags();
        inf.extract(value);
        return inf;
    }
}
