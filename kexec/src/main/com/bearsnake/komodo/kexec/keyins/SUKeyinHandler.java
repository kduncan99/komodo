/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.hardwarelib.DiskDevice;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.logger.LogManager;

public class SUKeyinHandler extends FacHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "SU node_name[,...]",
        "Makes devices available for use, however space cannot be allocated.",
    };

    public static final String COMMAND = "SU";

    public SUKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    public void abort(){}

    @Override
    public boolean checkSyntax() {
        return _options == null && _arguments != null;
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
        var nodeInfos = getNodeInfoList();
        if (nodeInfos == null) {
            return;
        }

        var error = false;
        for (var ni : nodeInfos) {
            if (!(ni.getNode() instanceof DiskDevice)) {
                var msg = String.format("SU not allowed for %s", ni.getNode().getNodeName());
                Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
                error = true;
            }
        }
        if (error) {
            return;
        }

        // TODO

        displayStatusForNodes(nodeInfos);
    }
}
