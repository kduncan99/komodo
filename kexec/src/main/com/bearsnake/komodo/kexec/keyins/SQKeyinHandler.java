/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;

class SQKeyinHandler extends KeyinHandler {

    private static final String[] HELP_TEXT = {
        "SQ",
        "  Displays brief information for each configured print or punch queue",
        // TODO many other formats to be implemented
    };

    public static final String COMMAND = "SQ";

    public SQKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        // TODO many formats to be implemented
        return true;
    }

    @Override
    String getCommand() { return COMMAND; }

    @Override
    String[] getHelp() { return HELP_TEXT; }

    @Override
    boolean isAllowed() {
        return true; // TODO change this so that it is only allowed after GENF$ is created or recovered
    }

    @Override
    void process() {
        // TODO many other formats to be implemented
        processStandard();
    }

    /**
     * SQ {with no arguments}
     */
    void processStandard() {
        var exec = Exec.getInstance();
        var gfi = exec.getGenFileInterface();

        boolean found = false;
        for (var q : gfi.getPunchQueues()) {
            var msg = String.format("%s: %02d FILES %03d CARDS %02d TAPES", q.getQueueName(), 0, 0, 0);
            exec.sendExecReadOnlyMessage(msg, _source);
            found = true;
        }
        if (!found) {
            exec.sendExecReadOnlyMessage("NO PUNCH QUEUES CONFIGURED", _source);
        }

        found = false;
        for (var q : gfi.getPrintQueues()) {
            var msg = String.format("%s: %02d FILES %03d PAGES %02d TAPES", q.getQueueName(), 0, 0, 0);
            exec.sendExecReadOnlyMessage(msg, _source);
            found = true;
        }
        if (!found) {
            exec.sendExecReadOnlyMessage("NO PRINT QUEUES CONFIGURED", _source);
        }
    }
}
