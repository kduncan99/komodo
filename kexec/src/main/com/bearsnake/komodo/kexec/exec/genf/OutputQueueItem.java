/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf;

import com.bearsnake.komodo.baselib.ArraySlice;

/*
 * Output queue item sector format
 *   +000,S1    Type (02)
 *   +000,S2    Priority Index
 *   +000,T2    GENF$ recovery cycle (at time of sym)
 *   +000,T3    Absolute F-Cycle
 *   +001       Actual Run-id FD LJSF
 *   +002:003   Account-id FD LJSF
 *   +004:005   Project-id FD LJSF
 *   +006:007   User-id FD LJSF
 *   +010       Queue-id FD LJSF
 *   +011       Output-id FD LJSF
 *   +012:013   Use-name FD LJSF
 *   +014:015   Qualifier FD LJSF
 *   +016:017   Filename FD LJSF
 *   +020:021   Banner FD LJSF
 *   +022       Time-sym'd ModSW
 *   +023       Fac status bits (of last fac operation on this entry
 *   +024,H1    Fac message code 1
 *   +024,H2    Fac message code 2
 *   +025,H1    Label block-id sector address (for tape partname data)
 *   +025,H2    Sector address of initial SMOQUE entry (could be this one)
 *   +026       Estimated pages or cards
 *   +027       Flags
 *   +030,H1    Breakpoint Part Number
 *   +030,H2    Number of times file is queue (if this is the initial entry)
 *   +031:033   reserved
 * Flags:
 *   000001  Tape File
 *   000002  Multi-tape file
 *   000004  All files on tape are to be printed/punched
 *   000010  Error occurred while processing this entry
 *   000200  File could not be assigned while processing this entry
 *   000400  Print file
 *   001000  Punch file
 *   002000  In-progress
 *   004000  SV is set for this entry
 *   010000  Queued to a user-id
 *   020000  Label-print-output set when sym'd
 *   040000  Every-page-labeling set when sym'd
 *   100000  Removable disk file
 * Priority Index Values:
 *     Index        File Priority   Run Priority
 *       1                0         ROLOUT and ROLBAK runs
 *       2                1         Critical deadline runs
 *       3                A         A, B, C
 *       4                D         D, E, F
 *       5                G         G, H, I
 *       6                J         J, K, L
 *       7                M         M, N, O
 *       8                P         P, Q, R
 *       9                S         S, T, U
 *      10                V         V, W, X
 *      11                Y         Y, Z
 */
public class OutputQueueItem extends Item {

    public OutputQueueItem(
        final long sectorAddress
    ) {
        super(ItemType.OutputQueueItem, sectorAddress);
        // TODO
    }

    public static OutputQueueItem deserialize(
        final long sectorAddress,
        final ArraySlice source
    ) {
        var item = new OutputQueueItem(sectorAddress);
        // TODO
        return item;
    }

    @Override
    public void serialize(final ArraySlice destination) {
        super.serialize(destination);
        // TODO
    }
}
