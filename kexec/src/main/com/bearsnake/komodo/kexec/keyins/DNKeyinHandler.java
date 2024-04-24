/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.hardwarelib.Device;
import com.bearsnake.komodo.hardwarelib.DiskDevice;
import com.bearsnake.komodo.hardwarelib.TapeDevice;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.RunControlEntry;
import com.bearsnake.komodo.kexec.exec.RunType;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.NodeStatus;
import com.bearsnake.komodo.logger.LogManager;
import java.util.HashSet;

public class DNKeyinHandler extends FacHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "DN node_name[,...]",
        "DN,ALL channel_name",
        "Makes channel and nodes unavailable for use.",
        "Devices attached to a specified channel are inaccessible via that channel."
    };

    public static final String COMMAND = "DN";

    /*
    Console Messages TODO
    DN device FATAL, CONTINUE PROCESSING KEYIN? Y OR N
    DN Keyin already performed for component
    DN KEYIN - component DOES NOT EXIST, INPUT IGNORED

     */
    public DNKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    public void abort(){}

    @Override
    public boolean checkSyntax() {
        if (_options != null && !_options.equalsIgnoreCase("ALL")) {
            return false;
        }

        return (_arguments != null);
    }

    @Override
    public String getCommand() { return COMMAND; }

    @Override
    public String[] getHelp() { return HELP_TEXT; }

    @Override
    public boolean isAllowed() {
        // TODO depends on @@CONS privilege
        return true;
    }

    @Override
    public void run() {
        try {
            process();
        } catch (Throwable t) {
            LogManager.logCatching(COMMAND, t);
            Exec.getInstance().stop(StopCode.ExecContingencyHandler);
        }
        setFinished();
    }

    private void process() {
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
        var runsToAbort = new HashSet<RunControlEntry>();
        for (var ni : nodeInfos) {
            var node = (Device) ni.getNode();
            var rce = ni.getAssignedTo();
            if (rce != null) {
                if (node instanceof DiskDevice) {
                    if (rce.getRunType() == RunType.Exec) {
                        if (!verifyOperation(node)) {
                            return;
                        }
                        requiresStop = true;
                    } else {
                        runsToAbort.add(rce);
                    }
                } else if (node instanceof TapeDevice) {
                    if (rce.getRunType() == RunType.Exec) {
                        // TODO something special here, it's probably an audit tape or something like that
                    } else {
                        runsToAbort.add(rce);
                    }
                }
            }
        }

        if (!requiresStop) {
            for (var rce : runsToAbort) {
                // abort the run
            }
        }

        nodeInfos.forEach(ni -> ni.setNodeStatus(NodeStatus.Down));
        displayStatusForNodes(nodeInfos);
        if (requiresStop) {
            Exec.getInstance().stop(StopCode.ConsoleResponseRequiresReboot);
        }
    }

    private void processNode() {
        var nodeName = _arguments.toUpperCase();
        var ni = _facMgr.getNodeInfo(nodeName);
        if (ni == null) {
            var msg = String.format("%s is not a configured node", nodeName);
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        if (ni.getNodeStatus() == NodeStatus.Down) {
            var msg = String.format("%s is already down", ni.getNode().getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        // TODO need to also check - are we a channel? and are we the last path to any devices?
        var requiresStop = false;
        var node = (Device) ni.getNode();
        var rce = ni.getAssignedTo();
        if (rce != null) {
            if (node instanceof DiskDevice) {
                if (rce.getRunType() == RunType.Exec) {
                    if (!verifyOperation(node)) {
                        return;
                    }
                    requiresStop = true;
                } else {
                    // TODO abort the run
                }
            } else if (node instanceof TapeDevice) {
                if (rce.getRunType() == RunType.Exec) {
                    // TODO something special here, it's probably an audit tape or something like that
                } else {
                    // TODO abort the run
                }
            }
        }

        ni.setNodeStatus(NodeStatus.Down);
        displayStatusForNode(ni);
        if (requiresStop) {
            Exec.getInstance().stop(StopCode.ConsoleResponseRequiresReboot);
        }
    }

    private boolean verifyOperation(final Device device) {
        var msg = String.format("DN of %s will cause system recovery - continue? Y/N", device.getNodeName());
        var allowed = new String[]{"Y", "N"};
        try {
            var response = Exec.getInstance().sendExecRestrictedReadReplyMessage(msg, allowed, _source);
            return response.equalsIgnoreCase("Y");
        } catch (ExecStoppedException ex) {
            return false;
        }
    }
}
