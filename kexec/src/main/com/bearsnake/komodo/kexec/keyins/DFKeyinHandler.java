/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exceptions.FileCycleDoesNotExistException;
import com.bearsnake.komodo.kexec.exceptions.FileSetDoesNotExistException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.Phase;

class DFKeyinHandler extends KeyinHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "DF {qualifier}*{filename}",
        "Creates a dump file containing the interpreted content of the given file",
    };

    public static final String COMMAND = "DF";

    private String _qualifier;
    private String _filename;

    public DFKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        if (_options != null || _arguments == null) {
            return false;
        }

        var split = _arguments.split("\\*");
        if (split.length != 2) {
            return false;
        }

        _qualifier = split[0].toUpperCase();
        _filename = split[1].toUpperCase();
        return Parser.isValidQualifier(_qualifier) && Parser.isValidFilename(_filename);
    }

    @Override
    String getCommand() { return COMMAND; }

    @Override
    String[] getHelp() { return HELP_TEXT; }

    @Override
    boolean isAllowed() {
        return Exec.getInstance().getPhase() == Phase.Running;
    }

    @Override
    void process() {
        var exec = Exec.getInstance();
        try {
            var filename = exec.getMFDManager().dumpFileContent(_qualifier, _filename);
            String msg;
            if (filename == null) {
                msg = "Failed to create system dump";
            } else {
                msg = "Created dump file " + filename;
            }
            exec.sendExecReadOnlyMessage(msg, _source);
        } catch (FileCycleDoesNotExistException | FileSetDoesNotExistException ex) {
            var msg = "File does not exist";
            exec.sendExecReadOnlyMessage(msg, _source);
        }
    }
}
