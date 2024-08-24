/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.ERIO$Status;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.exec.genf.queues.PrintQueue;
import com.bearsnake.komodo.kexec.exec.genf.queues.PunchQueue;
import com.bearsnake.komodo.kexec.exec.genf.queues.ReaderQueue;
import com.bearsnake.komodo.kexec.facilities.FacStatusResult;
import com.bearsnake.komodo.kexec.facilities.IOResult;
import com.bearsnake.komodo.logger.LogManager;

import java.util.Collection;
import java.util.TreeMap;

/**
 * Manages the GENF$ file and manages the backlog and the output queues.
 * GENF$ is read at boot time, and all information is kept in memory, with GENF$ being rewritten as necessary.
 * ---
 * Information is stored on sector boundaries.
 * Sector types identify the type of sector, and are stored in Word+0, S1 of each sector.
 * See the various Item subclasses for the serialized sector format for the various sector types.
 */

public class GenFileInterface {

    private static final String LOG_SOURCE = "GENF";
    private static final String QUALIFIER = "SYS$";
    private static final String FILE_NAME = "GENF$";
    private static final FileSpecification FILE_SPECIFICATION = new FileSpecification(QUALIFIER, FILE_NAME);

    // Item inventory - keyed by sector id
    private final TreeMap<Long, Item> _inventory = new TreeMap<>();

    // System item - always in core, for convenience
    private SystemItem _systemItem;

    // Reader, Print, and Punch queues - each is keyed by queue name
    private final TreeMap<String, ReaderQueue> _readerQueues = new TreeMap<>();
    private final TreeMap<String, PrintQueue> _printQueues = new TreeMap<>();
    private final TreeMap<String, PunchQueue> _punchQueues = new TreeMap<>();

    public Collection<PrintQueue> getPrintQueues() { return _printQueues.values(); }
    public Collection<PunchQueue> getPunchQueues() { return _punchQueues.values(); }
    public Collection<ReaderQueue> getReaderQueues() { return _readerQueues.values(); }

    /**
     * Initializes the GENF$ file - used during JK13 and JK9 boots
     */
    public void initialize() throws ExecStoppedException {
        var exec = Exec.getInstance();
        var cfg = exec.getConfiguration();
        var fm = exec.getFacilitiesManager();

        buildQueues();

        exec.sendExecReadOnlyMessage("Creating GENF$ file...");
        if (!exec.catalogDiskFileForExec(QUALIFIER,
                                         FILE_NAME,
                                         cfg.getStringValue(Tag.GENFASGMNE),
                                         cfg.getIntegerValue(Tag.GENFINTRES),
                                         9999)) {
            LogManager.logFatal(LOG_SOURCE, "Cannot catalog GENF$");
            exec.stop(StopCode.FileAssignErrorOccurredDuringSystemInitialization);
            throw new ExecStoppedException();
        }

        var facResult = new FacStatusResult();
        if (!fm.assignCatalogedDiskFileToExec(FILE_SPECIFICATION, false, facResult)
            || (facResult.hasErrorMessages())) {
            LogManager.logFatal(LOG_SOURCE, "Cannot assign GENF$");
            exec.stop(StopCode.FileAssignErrorOccurredDuringSystemInitialization);
            throw new ExecStoppedException();
        }

        fm.establishUseItem(exec, FILE_NAME, FILE_SPECIFICATION, false);

        // Create one track worth of items - one system item and 63 free items
        _systemItem = new SystemItem(0, 0, 64);
        _systemItem.setIsDirty(true);
        _inventory.put(_systemItem.getSectorAddress(), _systemItem);
        for (long addr = 01; addr <= 64; addr++) {
            var fi = new FreeItem(addr);
            fi.setIsDirty(true);
            _inventory.put(fi.getSectorAddress(), fi);
        }

        writeDirtyItems();
        exec.sendExecReadOnlyMessage("GENF$ initialized");
    }

    /**
     * Recovers the GENF$ file - used during regular recovery boots
     */
    public void recover() throws ExecStoppedException {
        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();

        _systemItem = null;
        _inventory.clear();
        _readerQueues.clear();
        _printQueues.clear();
        _punchQueues.clear();
        buildQueues();

        exec.sendExecReadOnlyMessage("Recovering GENF$ file...");
        var facResult = new FacStatusResult();
        if (!fm.assignCatalogedDiskFileToExec(FILE_SPECIFICATION, false, facResult)
            || (facResult.hasErrorMessages())) {
            LogManager.logFatal(LOG_SOURCE, "Cannot assign GENF$");
            exec.stop(StopCode.FileAssignErrorOccurredDuringSystemInitialization);
            throw new ExecStoppedException();
        }

        fm.establishUseItem(exec, FILE_NAME, FILE_SPECIFICATION, false);

        var buffer = new ArraySlice(new long[28]);
        var addr = 0;
        var ioResult = new IOResult();
        fm.ioReadFromDiskFile(exec, FILE_NAME, addr, buffer, 28, false, ioResult);
        if (ioResult.getStatus() != ERIO$Status.Success) {
            exec.stop(StopCode.InternalExecIOFailed);
            throw new ExecStoppedException();
        }

        var item = deserializeItem(0, buffer);
        if (!(item instanceof SystemItem si)) {
            LogManager.logFatal(LOG_SOURCE, "GENF$ sector 0 is not a system item");
            exec.stop(StopCode.UndefinedGENFType);
            throw new ExecStoppedException();
        }

        _systemItem = si;
        _inventory.put(_systemItem.getSectorAddress(), _systemItem);
        var msg = String.format("GENF$ recovery cycle = %d", si.getRecoveryCycle());
        exec.sendExecReadOnlyMessage(msg);

        for (addr = 1; addr < si.getSectorCount(); addr++) {
            ioResult.clear();
            fm.ioReadFromDiskFile(exec, FILE_NAME, addr, buffer, 28, false, ioResult);
            if (ioResult.getStatus() != ERIO$Status.Success) {
                exec.stop(StopCode.InternalExecIOFailed);
                throw new ExecStoppedException();
            }

            item = deserializeItem(addr, buffer);
            _inventory.put(_systemItem.getSectorAddress(), item);

            if (item instanceof InputQueueItem iqi) {
                var priority = iqi.getRunCardInfo().getSchedulingPriority();
                var queue = _readerQueues.get(priority);
                if (queue == null) {
                    LogManager.logFatal(LOG_SOURCE, "GENF$ sector %d has invalid priority: %d", addr, priority);
                    exec.stop(StopCode.BadGENFRecord);
                    throw new ExecStoppedException();
                }

                // TODO add iqi to the appropriate reader queue
            } else if (item instanceof OutputQueueItem oqi) {
                // TODO
            } else if (item instanceof FreeItem) {
                // nothing else to do here
            } else {
                LogManager.logFatal(LOG_SOURCE,
                                    "GENF$ sector %d is not an expected item type: %d",
                                    addr,
                                    item.getItemType().getCode());
                exec.stop(StopCode.UndefinedGENFType);
                throw new ExecStoppedException();
            }
        }
        // TODO
    }

    private void buildQueues() {
        var exec = Exec.getInstance();
        var cfg = exec.getConfiguration();

        // Create device-associated queues
        for (var node : cfg.getNodes()) {
            switch (node.getEquipType()) {
                case FILE_SYSTEM_CARD_READER -> _readerQueues.put(node.getName(), new ReaderQueue(node.getName(), node));
                case FILE_SYSTEM_CARD_PUNCH -> _punchQueues.put(node.getName(), new PunchQueue(node.getName(), node));
                case FILE_SYSTEM_PRINTER -> _printQueues.put(node.getName(), new PrintQueue(node.getName(), node));
            }
        }

        // Create non-associated queues as defined by SYMQUEUE configuration statements
        for (var queueName : cfg.getSymbiontPrintQueues()) {
            _printQueues.put(queueName, new PrintQueue(queueName));
        }

        for (var queueName : cfg.getSymbiontPunchQueues()) {
            _punchQueues.put(queueName, new PunchQueue(queueName));
        }

        // Create queues per configuration (replaces STATION LOCAL items from antiquity)
        //  TODO
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

    private synchronized void writeDirtyItems() throws ExecStoppedException {
        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();
        var buffer = new ArraySlice(new long[28]);
        var ioResult = new IOResult();

        for (var item : _inventory.values()) {
            if (item.isDirty()) {
                item.serialize(buffer);
                fm.ioWriteToDiskFile(exec, FILE_NAME, item.getSectorAddress(), buffer, 28, false, ioResult);
                if (ioResult.getStatus() != ERIO$Status.Success) {
                    exec.stop(StopCode.InternalExecIOFailed);
                    throw new ExecStoppedException();
                }
                item.setIsDirty(false);
            }
        }
    }
}
