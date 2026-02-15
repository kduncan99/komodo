/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;

import java.io.PrintStream;

/*
 * System item sector format
 *   +000,S1    Type (077)
 *   +001,H1    Most recent GENF$ recovery cycle (number of times this GENF$ used since last JK9/13 boot
 *   +001,H2    Number of sectors in the GENF$ file
 *   +002:033   reserved
 */
class SystemItem extends Item {

    private int _recoveryCycle; // number of boots since last JK9/13
    private int _sectorCount;   // number of sectors in the GENF$ file

    public SystemItem(
        final int sectorAddress,
        final int recoveryCycle,
        final int sectorCount
    ) {
        super(ItemType.SystemItem, sectorAddress);
        _recoveryCycle = recoveryCycle;
        _sectorCount = sectorCount;
    }

    public final int getRecoveryCycle() { return _recoveryCycle; }
    public final int getSectorCount() { return _sectorCount; }
    public void setRecoveryCycle(final int recoveryCycle) { _recoveryCycle = recoveryCycle; }
    public void setSectorCount(final int sectorCount) { _sectorCount = sectorCount; }

    public static SystemItem deserialize(
        final int sectorAddress,
        final ArraySlice source
    ) {
        return new SystemItem(sectorAddress,
                              (int)source.getH1(01),
                              (int)source.getH2(01));
    }

    @Override
    public void dump(PrintStream out, String indent) {
        super.dump(out, indent);
        out.printf("%s  cycle=%d sectors=%d\n", indent, _recoveryCycle, _sectorCount);
    }

    @Override
    public void serialize(final ArraySlice destination) throws ExecStoppedException {
        super.serialize(destination);
        destination.setH1(01, _recoveryCycle);
        destination.setH2(01, _sectorCount);
    }
}
