/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;

class BKeyinHandler extends KeyinHandler {

    private static final String[] HELP_TEXT = {
        "B [ max_runs ]",
        "  Displays or sets the maximum number of batch runs allowed, 0 to 99999.",
    };

    public static final String COMMAND = "B";

    private Integer _maxRuns;

    public BKeyinHandler(final ConsoleId source,
                         final String options,
                         final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        if (_options != null) {
            return false;
        }

        if (_arguments != null) {
            try {
                if (_arguments.startsWith("-")) {
                    return false;
                }
                _maxRuns = Integer.parseInt(_arguments);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    String getCommand() { return COMMAND; }

    @Override
    String[] getHelp() { return HELP_TEXT; }

    @Override
    boolean isAllowed() {
        var genf = Exec.getInstance().getGenFileInterface();
        return genf != null && genf.isReady();
    }

    @Override
    void process() {
        var exec = Exec.getInstance();
        var sch = exec.getScheduleManager();
        if (_maxRuns != null) {
            if (_maxRuns > 99999) {
                var msg = "B KEYIN - VALUE MUST BE <= 99999";
                exec.sendExecReadOnlyMessage(msg, _source);
                return;
            }

            sch.setMaxBatchJobs(_maxRuns);
        }

        var msg = String.format("MAX BATCH RUNS = %d", sch.getMaxBatchJobs());
        exec.sendExecReadOnlyMessage(msg, _source);
    }
}
