/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.hardwarelib.devices.DeviceType;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;

class FSKeyinHandler extends FacHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "FS,[ CM | DISK[S] | FDISK | MS | PACK[S] | RDISK | TAPE[S] ]",
        "FS node_name[,...]",
        "FS,ALL channel_name",
        "Displays facility status for various system components"
    };

    private static final String[] SYNTAX_TEXT = {
        "FS,[ CM | DISK[S] | FDISK | MS | PACK[S] | RDISK | TAPE[S] ]",
        "FS node_name[,...]",
        "FS,ALL channel_name",
    };

    public static final String COMMAND = "FS";

    /*
    Console messages TODO
    NO REMOVABLE DISKS PRESENT
     */

    public FSKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        // If option is ALL, we require a single argument which must be a node name
        if ((_options != null) && (_options.equalsIgnoreCase("ALL"))) {
            return _arguments != null && Parser.isValidNodeName(_arguments);
        }

        // If there is any other option, the argument list must be empty
        if (_options != null) {
            return _arguments == null;
        }

        // In the absence of any options, the argument list must be present
        return _arguments != null;
    }

    @Override String getCommand() { return COMMAND; }
    @Override String[] getHelp() { return HELP_TEXT; }
    @Override String[] getSyntax() { return SYNTAX_TEXT; }

    @Override
    boolean isAllowed() {
        return true;
    }

    @Override
    public void process() {
        if (_options == null) {
            var nodeName = _arguments.toUpperCase();
            var ni = _facMgr.getNodeInfo(nodeName);
            if (ni == null) {
                var msg = String.format("%s is not a configured node", nodeName);
                Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            } else {
                displayStatusForNode(ni);
            }
        } else {
            switch (_options.toUpperCase()) {
                case "ALL" -> processAll();
                case "DISK", "DISKS" -> processDisks();
                case "FDISK" -> processFixedDisks();
                case "MS" -> processMassStorage();
                case "PACK", "PACKS" -> processPacks();
                case "RDISK" -> processRemovableDisks();
                case "TAPE", "TAPES" -> processTapes();
                default -> {
                    var msg = String.format("Invalid option %s", _options.toUpperCase());
                    Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
                }
            }
        }
    }

    private void processAll() {
        // TODO
    }

    private void processDisks() {
        var diskInfos = _facMgr.getNodeInfos(DeviceType.DiskDevice);
        if (diskInfos.isEmpty()) {
            var msg = "No disk devices configured";
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
        } else {
            displayStatusForNodes(diskInfos);
        }
    }

    private void processFixedDisks() {
        // TODO
    }

    private void processMassStorage() {
        // TODO
    }

    private void processPacks() {
        // TODO
    }

    private void processRemovableDisks() {
        // TODO
    }

    private void processTapes() {
        var diskInfos = _facMgr.getNodeInfos(DeviceType.TapeDevice);
        if (diskInfos.isEmpty()) {
            var msg = "No tape devices configured";
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
        } else {
            displayStatusForNodes(diskInfos);
        }
    }
}
