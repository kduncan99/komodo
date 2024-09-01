/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;

import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/*
 * Tape part name item
 *   +000,S1    Type (03)
 *   +001,H1    Sector address of previous part name sector or of OutputQueueItem if this is the first
 *   +001,H2    Sector address of next part name sector or 0s if this is the last
 *   +002:003   Part name 1 (FD LJSF)
 *   +004:005   Part name 2 (FD LJSF) or 0s
 *   ...
 *   +026:027   Part name 13 (FD LJSF) or 0s
 */
public class PartNameItem extends Item {

    private int _previousSectorAddress;
    private int _nextSectorAddress;
    private final List<String> _partNames = new LinkedList<String>();

    public PartNameItem(
        final int sectorAddress
    ) {
        super(ItemType.FreeItem, sectorAddress);
    }

    public static PartNameItem deserialize(
        final int sectorAddress,
        final ArraySlice source
    ) {
        var pni = new PartNameItem(sectorAddress);

        pni._previousSectorAddress = (int) source.getH1(01);
        pni._nextSectorAddress = (int) source.getH2(01);

        for (int wx = 2; wx < 28; wx += 2) {
            if ((source.get(wx) == 0) && (source.get(wx + 1) == 0)) {
                break;
            }
            var partName = Word36.toStringFromFieldata(source.get(wx)) + Word36.toStringFromFieldata(source.get(wx + 1));
            pni._partNames.add(partName.trim());
        }

        return pni;
    }

    @Override
    public void dump(PrintStream out, String indent) {
        super.dump(out, indent);
        var subIndent = indent + "  ";
        out.printf(subIndent, "prev sector:%06o next sector:%06o\n", _previousSectorAddress, _nextSectorAddress);
        out.printf(subIndent, "part names: %s", String.join(" ", _partNames));
    }

    public int getPreviousSectorAddress() { return _previousSectorAddress; }
    public int getNextSectorAddress() { return _nextSectorAddress; }
    public Collection<String> getPartNames() { return new LinkedList<>(_partNames); }

    @Override
    public void serialize(final ArraySlice destination) throws ExecStoppedException {
        super.serialize(destination);

        destination.setH1(01, _previousSectorAddress);
        destination.setH2(01, _nextSectorAddress);
        for (int nx = 0, wx = 2; wx < 28; wx += 2, nx++) {
            destination.set(wx, Word36.stringToWordFieldata(_partNames.get(nx).substring(0, 6)));
            destination.set(wx, Word36.stringToWordFieldata(_partNames.get(nx).substring(7)));
        }
    }

    public PartNameItem addPartName(final String partName) { _partNames.add(partName); return this; }
    public PartNameItem setPreviousSectorAddress(final int previousSectorAddress) { _previousSectorAddress = previousSectorAddress; return this; }
    public PartNameItem setNextSectorAddress(final int nextSectorAddress) { _nextSectorAddress = nextSectorAddress; return this; }
}
