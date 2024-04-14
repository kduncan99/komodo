/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.logger.LogManager;

public class PREPKeyinHandler extends KeyinHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "PREP,[F|R] {device},{packname}",
        "Writes a label and an initial directory track to the pack currently mounted",
        "on the indicated device. The prep factor is determined from the geometry",
        "reported by the device for the mounted pack. The device must be reserved (RV)."
    };

    public static final String COMMAND = "D";

    private String _deviceName;
    private String _packName;

    public PREPKeyinHandler(final ConsoleId source,
                            final String options,
                            final String arguments) {
        super(source, options, arguments);
    }

    @Override
    public void abort(){}

    @Override
    public boolean checkSyntax() {
        if (_options == null || _arguments == null) {
            return false;
        }

        if (_options.length() != 1) {
            return false;
        }

        var split = _arguments.split(",");
        if (split.length != 2) {
            return false;
        }

        _deviceName = split[0];
        _packName = split[1];
        return true;
    }

    @Override
    public String getCommand() { return COMMAND; }

    @Override
    public String[] getHelp() { return HELP_TEXT; }

    @Override
    public boolean isAllowed() {
        return true;
    }

    private void process() {
        var fixed = _options.equalsIgnoreCase("F");
        var rem = _options.equalsIgnoreCase("R");
        if (!fixed && !rem) {
            var msg = "Invalid option on PREP keyin";
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        if (!Exec.isValidDeviceName(_deviceName)) {
            var msg = "Invalid device name on PREP keyin";
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        if (!Exec.isValidPackName(_packName)) {
            var msg = "Invalid device name on PREP keyin";
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }


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
}
