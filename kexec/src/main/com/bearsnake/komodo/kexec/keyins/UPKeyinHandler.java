/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;
import com.bearsnake.komodo.logger.LogManager;
import java.util.Collection;
import java.util.LinkedList;

public class UPKeyinHandler extends FacHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "UP node_name[,...]",
        "UP,ALL channel_name",
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
        if (_options.equalsIgnoreCase("ALL")) {
            processAll();
        } else {
            processList();
        }
    }

    private void processAll() {
        var nodeInfos = getNodeInfoListForChannel();

        // TODO

        displayStatusForNodes(nodeInfos);
    }

    private void processList() {
        var nodeInfos = getNodeInfoList();
        if (nodeInfos == null) {
            return;
        }

        // TODO

        displayStatusForNodes(nodeInfos);
    }
}
