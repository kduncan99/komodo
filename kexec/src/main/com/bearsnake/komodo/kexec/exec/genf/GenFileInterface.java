/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.kexec.FileSpecification;
import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.FileCycleDoesNotExistException;
import com.bearsnake.komodo.kexec.exceptions.FileSetDoesNotExistException;
import com.bearsnake.komodo.kexec.exec.ERIO$Status;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.Run;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.exec.genf.queues.OutputQueue;
import com.bearsnake.komodo.kexec.exec.genf.queues.PrintQueue;
import com.bearsnake.komodo.kexec.exec.genf.queues.PunchQueue;
import com.bearsnake.komodo.kexec.exec.genf.queues.Queue;
import com.bearsnake.komodo.kexec.exec.genf.queues.ReaderQueue;
import com.bearsnake.komodo.kexec.facilities.FacStatusResult;
import com.bearsnake.komodo.kexec.facilities.IOResult;
import com.bearsnake.komodo.kexec.mfd.DiskFileCycleInfo;
import com.bearsnake.komodo.logger.LogManager;

import java.io.PrintStream;
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

    private boolean _isReady = false;
    private int _recoveryCycle;

    // Item inventory - keyed by sector id
    private final TreeMap<Long, Item> _inventory = new TreeMap<>();

    // Reader, Print, and Punch queues - each is keyed by queue name
    private final TreeMap<String, ReaderQueue> _readerQueues = new TreeMap<>();
    private final TreeMap<String, PrintQueue> _printQueues = new TreeMap<>();
    private final TreeMap<String, PunchQueue> _punchQueues = new TreeMap<>();

    public Collection<PrintQueue> getPrintQueues() { return _printQueues.values(); }
    public Collection<PunchQueue> getPunchQueues() { return _punchQueues.values(); }
    public Collection<ReaderQueue> getReaderQueues() { return _readerQueues.values(); }

    public void dump(final PrintStream out, final String indent, final boolean verbose) {
        out.printf("%sGeneral File Interface ********************************\n", indent);

        var subIndent = indent + "  ";
        _readerQueues.values().forEach(q -> q.dump(out, subIndent, verbose));
        _punchQueues.values().forEach(q -> q.dump(out, subIndent, verbose));
        _printQueues.values().forEach(q -> q.dump(out, subIndent, verbose));

        if (verbose) {
            _inventory.values().forEach(item -> item.dump(out, subIndent));
        }
    }

    /**
     * Enqueues an SDF file to a print or punch queue.
     * Caller must ensure the file is in SDF format.
     * @param queue queue to which the file should be added
     * @param run Run object describing the run which is asking for this
     * @param priorityIndex indicates the priority for this print file
     * @param fileSpecification describes the file to be enqueued
     * @param breakpointPartNumber for PRINT$ files, the breakpoint part number (else zero)
     * @param banner banner page string
     * @param estimatedPages estimated number of pages in the print file
     * @param flags queue item flags
     * @param partNames only for tape files, this is an (optional) list of part-names to be printed from the tape file
     * @throws ExecStoppedException If the exec died in the process
     */
    public synchronized void enqueue(
        final OutputQueue queue,
        final Run run,
        final int priorityIndex,
        final FileSpecification fileSpecification,
        final int breakpointPartNumber,
        final String banner,
        final long estimatedPages,
        final long flags,
        final String[] partNames
    ) throws ExecStoppedException {
        // Is there already an entry for this qual*file(cycle) ?
        OutputQueueItem existingInitialItem = null;
        for (var qi : _inventory.values()) {
            if (qi instanceof OutputQueueItem oqi) {
                if (oqi.getQualifier().equals(fileSpecification.getQualifier())
                    && oqi.getFilename().equals(fileSpecification.getFilename())
                    && (oqi.getAbsoluteCycle() == fileSpecification.getFileCycleSpecification().getCycle())) {
                    existingInitialItem = oqi;
                    break;
                }
            }
        }

        // Create a new output queue item
        var newItem = new OutputQueueItem(allocateFreeQueueItem().getSectorAddress(), _recoveryCycle);
        newItem.setFileSpecificationInfo(fileSpecification, breakpointPartNumber)
          .setRunInfo(run)
          .setBanner(banner)
          .setEstimatedCards(estimatedPages)
          .setFlags(flags)
          .setPriorityIndex(priorityIndex)
          .setQueueId(queue.getQueueName());

        if (existingInitialItem != null) {
            newItem.setInitialEntrySectorAddress(existingInitialItem.getInitialEntrySectorAddress());
        } else {
            newItem.setInitialEntrySectorAddress((int) newItem.getSectorAddress());
            var mfd = Exec.getInstance().getMFDManager();
            try {
                var fcInfo = mfd.getFileCycleInfo(fileSpecification.getQualifier(),
                                                  fileSpecification.getFilename(),
                                                  fileSpecification.getFileCycleSpecification().getCycle());
                if (fcInfo instanceof DiskFileCycleInfo dfci) {
                    dfci.setInitialSMOQUELink(newItem.getSectorAddress());
                    // TODO tell MFD to update the file cycle info
                }
            } catch (FileSetDoesNotExistException | FileCycleDoesNotExistException ex) {
                // TODO this is fatal
            }
        }

        // Create GENF entry(ies) for tape file part-names
        //  TODO - this means we need to create a part-name queue item type, probably containing a forward link to the next
        //    such item (if any), and up to 13 FD LJSF partname entries.

        queue.enqueue(newItem);
        newItem.setIsDirty(true);
        _inventory.put(newItem.getSectorAddress(), newItem);
        writeDirtyItems();
    }

    /**
     * Retrieves a queue by name
     */
    public Queue getQueue(final String name) {
        Queue queue = _printQueues.get(name);
        if (queue == null) {
            queue = _punchQueues.get(name);
        }
        if (queue == null) {
            queue = _readerQueues.get(name);
        }
        return queue;
    }

    /**
     * Initializes the GENF$ file - used during JK13 and JK9 boots
     */
    public void initialize() throws ExecStoppedException {
        var exec = Exec.getInstance();
        var cfg = exec.getConfiguration();
        var fm = exec.getFacilitiesManager();

        _recoveryCycle = 1;
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
        var systemItem = new SystemItem(0, 0, 64);
        systemItem.setIsDirty(true);
        _inventory.put(systemItem.getSectorAddress(), systemItem);
        for (long addr = 1; addr < 64; addr++) {
            var fi = new FreeItem(addr);
            fi.setIsDirty(true);
            _inventory.put(fi.getSectorAddress(), fi);
        }

        writeDirtyItems();
        exec.sendExecReadOnlyMessage("GENF$ initialized");
        _isReady = true;
    }

    public boolean isReady() { return _isReady; }

    /**
     * Recovers the GENF$ file - used during regular recovery boots
     */
    public void recover() throws ExecStoppedException {
        // TODO
        //  SymbiontNameNotFound(024),
        //  IllegalNumberOfEntriesOnQueue(054),

        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();

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

        var systemItem = getSystemItem();
        _recoveryCycle = systemItem.getRecoveryCycle() + 1;
        systemItem.setRecoveryCycle(_recoveryCycle);
        systemItem.setIsDirty(true);

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
            _inventory.put(item.getSectorAddress(), item);
            if (item instanceof InputQueueItem iqi) {
                var queue = _readerQueues.get(iqi.getSourceSymbiontName());
                if (queue == null) {
                    exec.stop(StopCode.SymbiontNameNotFound);
                    throw new ExecStoppedException();
                }
                queue.enqueue(iqi);
            } else if (item instanceof OutputQueueItem oqi) {
                OutputQueue queue = _printQueues.get(oqi.getQueueId());
                if (queue == null) {
                    queue = _punchQueues.get(oqi.getQueueId());
                }
                if (queue == null) {
                    exec.stop(StopCode.SymbiontNameNotFound);
                    throw new ExecStoppedException();
                }
                queue.enqueue(oqi);
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

        _isReady = true;
    }

    /**
     * Finds the first available free queue item to be eventually replaced by an input or output queue item.
     * If there isn't one, a new track of items is created, and the first of these is then selected.
     */
    private synchronized Item allocateFreeQueueItem() throws ExecStoppedException {
        for (var item : _inventory.values()) {
            if (item.getItemType() == ItemType.FreeItem) {
                return item;
            }
        }

        long newSectorAddress = _inventory.size();
        for (long addr = newSectorAddress; addr < newSectorAddress + 64; addr++) {
            var fi = new FreeItem(addr);
            fi.setIsDirty(true);
            _inventory.put(fi.getSectorAddress(), fi);
        }
        writeDirtyItems();

        return _inventory.get(newSectorAddress);
    }

    /**
     * Builds the various queues according to the given configuration.
     * This happens *before* initialization or recovery.
     */
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
    }

    /**
     * Deserializes the on-disk sector representation of a queue item into an actual Item of the appropriate type.
     * @param sectorAddress Sector address of the queue item on disk (as GENF$)
     * @param source Slice of an IO buffer containing exactly the sector containing the serialized queue item
     * @return allocated subclass of Item, representing the queue item.
     */
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

    /**
     * Purely for convenience - system item is always at sector address 0
     */
    private SystemItem getSystemItem() {
        return (SystemItem) _inventory.get(0L);
    }

    /**
     * Iterates over the inventory, serializing the various updated (and not yet persisted) objects to disk
     */
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
