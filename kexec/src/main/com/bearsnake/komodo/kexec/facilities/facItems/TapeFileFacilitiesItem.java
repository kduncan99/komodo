/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities.facItems;

import java.io.PrintStream;

public class TapeFileFacilitiesItem extends FacilitiesItem {

    protected boolean _deleteOnAnyRunTermination;
    protected boolean _deleteOnNormalRunTermination;
    protected boolean _waitingForFile;     // some other run(s) has/have assigned the file.
    protected boolean _waitingForTapeUnit; // no tape unit(s) available

    @Override
    public void dump(final PrintStream out,
                     final String indent) {
        super.dump(out, indent);
        out.printf("%s  delAnyTerm:%s delNormTerm:%s waitFile:%s waitUnit:%s\n",
                   indent, _deleteOnAnyRunTermination, _deleteOnNormalRunTermination);
        if (_waitingForFile || _waitingForTapeUnit) {
            out.printf("%s  HOLDS:%s%s%s\n", indent,
                       _waitingForFile ? " FILE" : "",
                       _waitingForTapeUnit ? " UNIT" : "");
        }
    }

    public final boolean deleteOnAnyRunTermination() { return _deleteOnAnyRunTermination; }
    public final boolean deleteOnNormalRunTermination() { return _deleteOnNormalRunTermination; }
    public final boolean isWaitingForFile() { return _waitingForFile; }
    public final boolean isWaitingForTapeUnit() { return _waitingForTapeUnit; }

    public boolean isWaiting() {
        return _waitingForFile || _waitingForTapeUnit;
    }

    public final TapeFileFacilitiesItem setDeleteOnAnyRunTermination(final boolean value) { _deleteOnAnyRunTermination = value; return this; }
    public final TapeFileFacilitiesItem setDeleteOnNormalRunTermination(final boolean value) { _deleteOnNormalRunTermination = value; return this; }
    public final TapeFileFacilitiesItem setIsWaitingForFile(final boolean value) { _waitingForFile = value; return this; }
    public final TapeFileFacilitiesItem setIsWaitingForTapeUnit(final boolean value) { _waitingForTapeUnit = value; return this; }
}
