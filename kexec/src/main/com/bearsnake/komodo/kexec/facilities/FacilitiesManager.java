/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.hardwarelib.DiskChannel;
import com.bearsnake.komodo.hardwarelib.FileSystemDiskDevice;
import com.bearsnake.komodo.hardwarelib.Node;
import com.bearsnake.komodo.hardwarelib.TapeChannel;
import com.bearsnake.komodo.kexec.HardwareTrackId;
import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.mfd.FileAllocationSet;
import com.bearsnake.komodo.kexec.mfd.MFDRelativeAddress;
import com.bearsnake.komodo.logger.LogManager;

import java.io.PrintStream;
import java.util.HashMap;

public class FacilitiesManager implements Manager {

    private static final String LOG_SOURCE = "FacMgr";

    // All assigned disk files are recorded here so that we can easily access and manage the file allocations.
    private final HashMap<MFDRelativeAddress, FileAllocationSet> _acceleratedFileAllocations = new HashMap<>();

    // Inventory of all the hardware nodes, keyed by node identifier.
    // It is loaded at initialization(), and will remain unchanged during the application existence.
    private final HashMap<Integer, Node> _nodeGraph = new HashMap<>();

    public FacilitiesManager() {
        Exec.getInstance().managerRegister(this);
    }

    // -------------------------------------------------------------------------
    // Manager interface
    // -------------------------------------------------------------------------

    @Override
    public void boot() {
        LogManager.logTrace(LOG_SOURCE, "boot()");
        // TODO
    }

    @Override
    public synchronized void dump(final PrintStream out,
                                  final String indent,
                                  final boolean verbose) {
        out.printf("%sFacilitiesManager ********************************\n", indent);

        // TODO

        out.printf("%s  Node Graph:\n", indent);
        var subIndent = indent + "    ";
        for (var node : _nodeGraph.values()) {
            node.dump(out, subIndent);
        }

        if (verbose) {
            // Accelerated file information
            out.printf("%s  Accelerated files:\n", indent);
            for (var e : _acceleratedFileAllocations.entrySet()) {
                var prefix = e.getKey().toString();
                for (var fa : e.getValue().getFileAllocations()) {
                    out.printf("%s    %s: %s\n", indent, prefix, fa.toString());
                    prefix = "              ";
                }
            }
        }
    }

    @Override
    public void initialize() {
        LogManager.logTrace(LOG_SOURCE, "initialize()");

        // Load node graph based on the configuration TODO
        // The following is temporary
        var disk0 = new FileSystemDiskDevice("DISK0", "media/disk0.pack", false);
        var disk1 = new FileSystemDiskDevice("DISK1", "media/disk1.pack", false);
        var disk2 = new FileSystemDiskDevice("DISK2", "media/disk2.pack", false);
        var disk3 = new FileSystemDiskDevice("DISK3", "media/disk3.pack", false);

        var dch0 = new DiskChannel("CHDSK0");
        dch0.attach(disk0);
        dch0.attach(disk1);
        dch0.attach(disk2);
        dch0.attach(disk3);

        var dch1 = new DiskChannel("CHDSK1");
        dch1.attach(disk0);
        dch1.attach(disk1);
        dch1.attach(disk2);
        dch1.attach(disk3);

        var tape0 = new FileSystemDiskDevice("TAPE0");
        var tape1 = new FileSystemDiskDevice("TAPE1");

        var tch = new TapeChannel("CHTAPE");
        tch.attach(tape0);
        tch.attach(tape1);
    }

    @Override
    public synchronized void stop() {
        LogManager.logTrace(LOG_SOURCE, "stop()");
    }

    // -------------------------------------------------------------------------
    // Service API
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // Core methods
    // -------------------------------------------------------------------------

    /**
     * Given the MFD-relative address of a main item sector 0 for a file cycle,
     * we translate the given file-relative track id to an LDAT and physical device track.
     * @param mainItem0Address main item address of file cycle
     * @param fileTrackId file-relative track id
     * @return HardwareTrackId object if the given track is allocated, else null
     * @throws ExecStoppedException if the main item is not accelerated (generally meaning it is not assigned)
     */
    private synchronized HardwareTrackId convertFileRelativeTrackId(
        final MFDRelativeAddress mainItem0Address,
        final long fileTrackId
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "convertFileRelativeTrackId(%s, %d)", mainItem0Address.toString(), fileTrackId);
        var fa = _acceleratedFileAllocations.get(mainItem0Address);
        if (fa == null) {
            LogManager.logFatal(LOG_SOURCE, "mainItem0 is not accelerated");
            Exec.getInstance().stop(StopCode.DirectoryErrors);
            throw new ExecStoppedException();
        }

        var hwTid = fa.resolveFileRelativeTrackId(fileTrackId);
        LogManager.logTrace(LOG_SOURCE, "returning %s", hwTid);
        return hwTid;
    }
}
