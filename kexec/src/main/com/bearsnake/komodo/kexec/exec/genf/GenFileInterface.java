/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.FacStatusResult;
import com.bearsnake.komodo.logger.LogManager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

/**
 * Manages the GENF$ file and manages the backlog and the output queues.
 * GENF$ is read at boot time, and all information is kept in memory, with GENF$ being rewritten as necessary.
 * ---
 * Information is stored on sector boundaries. Sector types identify the type of sector, and are stored
 * in Word+0, S1 of each sector. The values include:
 * Type Description
 *  00   unused items
 *  01   input queue items (backlog)
 *  02   output queue items (for SMOQUE)
 *  03   system information items (persist information across system boots)
 * ---
 * Input queue item format
 *   +000,S1    Type (01)
 *   +000,S2    Scheduling Priority fieldata 'A' through 'Z'
 *   +000,S3    Processing Priority fieldata 'A' through 'Z'
 *   +001       @RUN statement options
 *   +002       Actual Run-id FD LJSF
 *   +003       Original Run-id FD LJSF
 *   +004:005   Account-id FD LJSF
 *   +006:007   Project-id FD LJSF
 *   +010:011   User-id FD LJSF
 *   +012       Start-time ModSW
 *   +013       Deadline-time ModSW
 *   +014       Submission-time ModSW
 *   +015,H1    Max Pages
 *   +015,H2    Max Cards
 *   +016,H1    Max Time
 *   +017       Source Symbiont FD LJSF
 *   +017:033   reserved
 * ---
 * Output queue item format
 * // TODO, see SMOQUE entry format (but we have to do it at least a *little* differently
 */

public class GenFileInterface {

    public static final String LOG_SOURCE = "GENF";

    // Item inventory - keyed by sector id
    private final TreeMap<Long, Item> _inventory = new TreeMap<>();

    // System item - always in core, for convenience
    private SystemItem _systemItem;

    // Input queue - keyed by scheduling priority, and each list there-in is in order by submission date/time
    private final HashMap<Character, LinkedList<InputQueueItem>> _inputQueue = new HashMap<>();
    // TODO need input queue management

    // TODO need output queue

    public GenFileInterface() {
        // TODO
    }

    /**
     * Initializes the GENF$ file - used during JK13 and JK9 boots
     */
    public void initialize() throws ExecStoppedException {
        var exec = Exec.getInstance();
        var cfg = exec.getConfiguration();
        var fm = exec.getFacilitiesManager();

        exec.sendExecReadOnlyMessage("Creating spool file...");
        if (!exec.catalogDiskFileForExec("SYS$", "GENF$", cfg.getGENFAssignMnemonic(), cfg.getGENFInitialReserve(), 9999)) {
            LogManager.logFatal(LOG_SOURCE, "Cannot catalog GENF$");
            exec.stop(StopCode.FileAssignErrorOccurredDuringSystemInitialization);
            throw new ExecStoppedException();
        }

        var fileSpec = new FileSpecification("SYS$", "GENF$");
        var facResult = new FacStatusResult();
        if (!fm.assignCatalogedDiskFileToExec(fileSpec, false, facResult)
            || (facResult.hasErrorMessages())) {
            LogManager.logFatal(LOG_SOURCE, "Cannot assign GENF$");
            exec.stop(StopCode.FileAssignErrorOccurredDuringSystemInitialization);
            throw new ExecStoppedException();
        }

        // Create one track worth of items - one system item and 63 free items
        _systemItem = new SystemItem(0);
        // TODO
    }

    /**
     * Recovers the GENF$ file - used during regular recovery boots
     */
    public void recover() {
        // TODO
    }

    private static Item deserializeItem(
        final long sectorAddress,
        final ArraySlice source
    ) {
        return switch (ItemType.getItemType((int)source.getS1(0))) {
            case FreeItem -> FreeItem.deserialize(sectorAddress);
            case InputQueueItem -> InputQueueItem.deserialize(sectorAddress, source);
            case OutputQueueItem -> OutputQueueItem.deserialize(sectorAddress, source);
            case SystemItem -> SystemItem.deserialize(sectorAddress, source);
        };
    }
}
