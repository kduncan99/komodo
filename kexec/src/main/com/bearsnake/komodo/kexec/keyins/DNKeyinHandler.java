/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.logger.LogManager;

public class DNKeyinHandler extends FacHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "DN node_name[,...]",
        "DN,ALL channel_name",
        "Makes channel and nodes unavailable for use.",
        "Devices attached to a specified channel are inaccessible via that channel."
    };

    public static final String COMMAND = "DN";

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
