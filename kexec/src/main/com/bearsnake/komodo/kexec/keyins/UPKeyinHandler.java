/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.DeviceNodeInfo;
import com.bearsnake.komodo.kexec.facilities.NodeStatus;
import com.bearsnake.komodo.logger.LogManager;

public class UPKeyinHandler extends FacHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "UP[,ALL] node_name",
        "Makes channel and nodes available for use.",
        "Space can be allocated on disk drives.",
        "Devices attached to a specified channel are accessible via that channel."
    };

    public static final String COMMAND = "UP";

    public UPKeyinHandler(final ConsoleId source,
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

        return (_arguments != null) && Exec.isValidNodeName(_arguments.toUpperCase());
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

        nodeInfos.forEach(ni -> ni.setStatus(NodeStatus.Up));
        displayStatusForNodes(nodeInfos);
        // TODO how do we initiate scan of fixed disk(s) - send device ready?
    }

    private void processNode() {
        var devName = _arguments.toUpperCase();
        var ni = _facMgr.getNodeInfo(devName);
        if (!(ni instanceof DeviceNodeInfo)) {
            var msg = String.format("%s is not a configured device", devName);
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        ni.setStatus(NodeStatus.Up);
        displayStatusForNode(ni);
        // TODO how do we initiate scan of fixed disk - send device ready?
    }
}
