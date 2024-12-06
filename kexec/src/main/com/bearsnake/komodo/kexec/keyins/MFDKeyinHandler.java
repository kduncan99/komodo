/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.baselib.FileSpecification;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.ERIO$Status;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.FacStatusResult;
import com.bearsnake.komodo.kexec.facilities.IOResult;
import com.bearsnake.komodo.kexec.facilities.ReleaseBehavior;
import com.bearsnake.komodo.kexec.facilities.facItems.DiskFileFacilitiesItem;
import com.bearsnake.komodo.logger.LogManager;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class MFDKeyinHandler extends KeyinHandler implements Runnable {

    private enum Command {
        List,
        Dump,
    }

    private Command _command;
    private FileSpecification _fileSpecification;

    private static final String[] HELP_TEXT = {
        "MFD [ LIST | DUMP qual*file(cycle) ]",
        "Lists all files in the MFD or dumps the content of the given file"
    };

    private static final String[] SYNTAX_TEXT = {
        "MFD [ LIST | DUMP qual*file(cycle) ]",
    };

    public static final String COMMAND = "MFD";

    public MFDKeyinHandler(final ConsoleId source,
                           final String options,
                           final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        if (_options != null || _arguments == null) {
            return false;
        }

        var split = _arguments.split(" ");
        if (split[0].equalsIgnoreCase("LIST")) {
            if (split.length > 1) {
                return false;
            }
            _command = Command.List;
            return true;
        }

        if (split[0].equalsIgnoreCase("DUMP")) {
            if (split.length != 2) {
                return false;
            }

            try {
                var p = new Parser(split[1]);
                _fileSpecification = FileSpecification.parse(p, "");
                _command = Command.Dump;
                return true;
            } catch (FileSpecification.Exception ex) {
                return false;
            }
        }

        return false;
    }

    @Override String getCommand() { return COMMAND; }
    @Override String[] getHelp() { return HELP_TEXT; }
    @Override String[] getSyntax() { return SYNTAX_TEXT; }

    @Override
    boolean isAllowed() {
        return true;
    }

    @Override
    void process() {
        if (_command == Command.Dump) {
            processDump();
        } else {
            processList();
        }
    }

    private PrintStream createFile() {
        var dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        String dtStr = dateTime.format(formatter);
        var dumpFileName = String.format("kexec-%s.dump", dtStr);
        try {
            var stream = new PrintStream(dumpFileName);
            Exec.getInstance().sendExecReadOnlyMessage("Created file " + dumpFileName, _source);
            return stream;
        } catch (FileNotFoundException ex) {
            LogManager.logFatal("MFDKeyin", "Failed to create panic dump file");
            return null;
        }
    }

    void processDump() {
        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();
        PrintStream stream = null;

        try {
            // assign file to exec (noting whether it was already assigned)
            var fsResult = new FacStatusResult();
            if (!fm.assignCatalogedDiskFileToExec(_fileSpecification, false, fsResult)) {
                exec.sendExecReadOnlyMessage("CANNOT ASSIGN FILE TO EXEC", _source);
                return;
            }
            var alreadyAssigned = (fsResult.getStatusWord() & 0_100000_000000L) != 0;

            stream = createFile();
            if (stream != null) {

                // apply @use name to file
                fm.establishUseItem(exec, "MFDKEYIN", _fileSpecification, false);

                // get aci's from MFD via FacMgr so we know the allocated file-relative tracks
                var facItem = exec.getFacilitiesItemTable().getFacilitiesItemByInternalName("MFDKEYIN");
                var acInfo = ((DiskFileFacilitiesItem) facItem).getAcceleratedCycleInfo();
                var faSet = acInfo.getFileAllocationSet();

                // iterate over file-relative tracks, reading each one and dumping it in octal, fdata, and ascii
                var allocations = faSet.getFileAllocations();
                var buffer = new ArraySlice(new long[1792]);
                var ioResult = new IOResult();
                for (var allocation : allocations) {
                    var region = allocation.getFileRegion();
                    var hwTid = allocation.getHardwareTrackId();
                    stream.printf("Region: %s DeviceAddr: %s\n", region.toString(), hwTid.toString());
                    for (var trackId = region.getTrackId(); trackId <= region.getHighestTrack(); trackId++) {
                        var sectorAddr = trackId * 64;
                        fm.ioReadFromDiskFile(exec, "MFDKEYIN", sectorAddr, buffer, false, ioResult);
                        if (ioResult.getStatus() == ERIO$Status.Success) {
                            for (int sx = 0, wz = 0; sx < 64; sx++) {
                                var sectorId = sectorAddr + sx;
                                var preamble = String.format("  %010o:", sectorId);
                                for (int wx = 0; wx < 7; wx++) {
                                    var octalStr = new StringBuilder();
                                    var fdStr = new StringBuilder();
                                    var ascStr = new StringBuilder();
                                    for (int wy = 0; wy < 4; wy++) {
                                        var word = buffer.get(wz++);
                                        octalStr.append(" ").append(String.format("%012o", word));
                                        fdStr.append(" ").append(Word36.toStringFromFieldata(word));
                                        ascStr.append(" ").append(Word36.toStringFromASCII(word));
                                    }
                                    stream.println(preamble + "  " + octalStr + "  " + fdStr + "  " + ascStr);
                                    preamble = "             ";
                                }
                            }
                        } else {
                            stream.printf("IOStatus:%s Words:%d\n", ioResult.getStatus(), ioResult.getWordsTransferred());
                        }
                    }
                }

                // Free file and/or use name
                var useSpec = new FileSpecification(null, "MFDKEYIN");
                var behavior = alreadyAssigned
                    ? ReleaseBehavior.ReleaseUseItemOnly
                    : ReleaseBehavior.Normal;
                if (!alreadyAssigned) {
                    fm.releaseFile(exec, useSpec, behavior, false, false, false, false, fsResult);
                }
            }
        } catch (ExecStoppedException e) {
            // nothing to do here
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    void processList() {
        var stream = createFile();
        if (stream != null) {
            var mfd = Exec.getInstance().getMFDManager();
            var fsInfos = mfd.getFileSetInfos();
            for (var fsInfo : fsInfos) {
                var sb = new StringBuilder();
                sb.append(fsInfo.getQualifier()).append("*").append(fsInfo.getFilename());
                while (sb.length() < 26) sb.append(" ");

                for (var fscInfo : fsInfo.getCycleInfo()) {
                    sb.append(" ");
                    if (fscInfo.isToBeCataloged()) {
                        sb.append("+");
                    } else if (fscInfo.isToBeDropped()) {
                        sb.append("*");
                    }
                    sb.append(fscInfo.getAbsoluteCycle());
                }

                stream.println(sb);
            }

            stream.close();
        }
    }
}
