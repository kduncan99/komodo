/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.hardwarelib.devices.Device;
import com.bearsnake.komodo.hardwarelib.devices.DiskDevice;
import com.bearsnake.komodo.hardwarelib.devices.TapeDevice;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.DeviceNodeInfo;
import com.bearsnake.komodo.kexec.facilities.NodeStatus;
import com.bearsnake.komodo.kexec.scheduleManager.Run;
import com.bearsnake.komodo.kexec.scheduleManager.RunType;

import java.util.HashSet;

class RVKeyinHandler extends FacHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "RV[,ALL] node_name",
        "Reserves a device for exclusive use.",
    };

    private static final String[] SYNTAX_TEXT = {
        "RV[,ALL] node_name",
    };

    public static final String COMMAND = "RV";

    public RVKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        if (_options != null && !_options.equalsIgnoreCase("ALL")) {
            return false;
        }

        return (_arguments != null) && Parser.isValidNodeName(_arguments.toUpperCase());
    }

    @Override String getCommand() { return COMMAND; }
    @Override String[] getHelp() { return HELP_TEXT; }
    @Override String[] getSyntax() { return SYNTAX_TEXT; }

    @Override
    boolean isAllowed() {
        // TODO depends on @@CONS privilege
        return true;
    }

    @Override
    void process() {
        if (_options != null) {
            processAll();
        } else {
            processNode();
        }
    }

    private void processAll() {
        var nodeInfos = getNodeInfoListForChannel();
        if (nodeInfos == null) {
            return;
        }

        var requiresStop = false;
        var runsToAbort = new HashSet<Run>();
        for (var ni : nodeInfos) {
            var node = (Device) ni.getNode();
            var runEntry = ni.getAssignedTo();
            if (runEntry != null) {
                if (node instanceof DiskDevice) {
                    if (runEntry.getRunType() == RunType.Exec) {
                        if (!verifyOperation(node)) {
                            return;
                        }
                        requiresStop = true;
                    } else {
                        runsToAbort.add(runEntry);
                    }
                } else if (node instanceof TapeDevice) {
                    if (runEntry.getRunType() == RunType.Exec) {
                        // TODO something special here, it's probably an audit tape or something like that
                    } else {
                        runsToAbort.add(runEntry);
                    }
                }
            }
        }

        if (!requiresStop) {
            for (var runEntry : runsToAbort) {
                // abort the run
            }
        }

        nodeInfos.forEach(ni -> ni.setNodeStatus(NodeStatus.Reserved));
        displayStatusForNodes(nodeInfos);
        if (requiresStop) {
            Exec.getInstance().stop(StopCode.ConsoleResponseRequiresReboot);
        }
    }

    private void processNode() {
        var devName = _arguments.toUpperCase();
        var ni = _facMgr.getNodeInfo(devName);
        if (!(ni instanceof DeviceNodeInfo)) {
            var msg = String.format("%s is not a configured device", devName);
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        if (ni.getNodeStatus() == NodeStatus.Reserved) {
            var msg = String.format("%s is already reserved", ni.getNode().getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        var requiresStop = false;
        if (ni.getNodeStatus() != NodeStatus.Down) {
            var node = (Device) ni.getNode();
            var runEntry = ni.getAssignedTo();
            if (runEntry != null) {
                if (node instanceof DiskDevice) {
                    if (runEntry.getRunType() == RunType.Exec) {
                        if (!verifyOperation(node)) {
                            return;
                        }
                        requiresStop = true;
                    } else {
                        // TODO abort the run
                    }
                } else if (node instanceof TapeDevice) {
                    if (runEntry.getRunType() == RunType.Exec) {
                        // TODO something special here, it's probably an audit tape or something like that
                    } else {
                        // TODO abort the run
                    }
                }
            }
        }

        ni.setNodeStatus(NodeStatus.Reserved);
        displayStatusForNode(ni);
        if (requiresStop) {
            Exec.getInstance().stop(StopCode.ConsoleResponseRequiresReboot);
        }
    }

    private boolean verifyOperation(final Device device) {
        var msg = String.format("RV of %s will cause system recovery - continue? Y/N", device.getNodeName());
        var allowed = new String[]{"Y", "N"};
        try {
            var response = Exec.getInstance().sendExecRestrictedReadReplyMessage(msg, allowed, _source);
            return response.equalsIgnoreCase("Y");
        } catch (ExecStoppedException ex) {
            return false;
        }
    }
}
