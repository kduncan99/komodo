/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.NodeStatus;

class UPKeyinHandler extends FacHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "UP[,ALL] node_name",
        "Makes channel and nodes available for use.",
        "Space can be allocated on disk drives.",
        "Devices attached to a specified channel are accessible via that channel."
    };

    private static final String[] SYNTAX_TEXT = {
        "UP[,ALL] node_name",
    };

    public static final String COMMAND = "UP";

    public UPKeyinHandler(final ConsoleId source,
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

        nodeInfos.forEach(ni -> ni.setNodeStatus(NodeStatus.Up));
        displayStatusForNodes(nodeInfos);
        // TODO how do we initiate scan of fixed disk(s) - send device ready?
    }

    private void processNode() {
        var nodeName = _arguments.toUpperCase();
        var ni = _facMgr.getNodeInfo(nodeName);
        if (ni == null) {
            var msg = String.format("%s is not a configured node", nodeName);
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        if (ni.getNodeStatus() == NodeStatus.Up) {
            var msg = String.format("%s is already up", ni.getNode().getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        ni.setNodeStatus(NodeStatus.Up);
        displayStatusForNode(ni);
        // TODO how do we initiate scan of fixed disk - send device ready?
        // TODO also - did we just open up paths to any devices... if so, are they disks?
    }
}
