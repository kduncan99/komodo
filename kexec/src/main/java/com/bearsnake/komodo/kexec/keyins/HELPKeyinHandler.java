/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

class HELPKeyinHandler extends KeyinHandler {

    private static final String[] HELP_TEXT = {
        "HELP [ keyin ]",
        "Display a list of all available keyins, or details help regarding the indicated keyin."
    };

    private static final String[] SYNTAX_TEXT = {
        "HELP [ keyin ]",
    };

    public static final String COMMAND = "HELP";

    public String _command;

    public HELPKeyinHandler(final ConsoleId source,
                            final String options,
                            final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        if (_options == null) {
            _command = _arguments;
        }
        return true;
    }

    @Override String getCommand() { return COMMAND; }
    @Override String[] getHelp() { return HELP_TEXT; }
    @Override String[] getSyntax() { return SYNTAX_TEXT; }

    @Override
    boolean isAllowed() {
        return true;
    }

    @Override
    void process() {
        var exec = Exec.getInstance();
        if (_command == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("COMMANDS:");
            var preLength = sb.length();
            var clean = true;
            for (var command : KeyinManager.getHandlerCommands()) {
                sb.append(" ").append(command);
                clean = false;
                if (sb.length() > 65) {
                    exec.sendExecReadOnlyMessage(sb.toString(), _source);
                    sb.setLength(preLength);
                    clean = true;
                }
            }
            if (!clean) {
                exec.sendExecReadOnlyMessage(sb.toString(), _source);
            }
        } else {
            var clazz = KeyinManager.getHandlerClass(_command.toUpperCase());
            if (clazz == null) {
                exec.sendExecReadOnlyMessage(String.format("HELP - KEYIN %s NOT FOUND", _command), _source);
            } else {
                try {
                    Constructor<?> ctor = clazz.getConstructor(ConsoleId.class, String.class, String.class);
                    var kh = (KeyinHandler)ctor.newInstance(_source, null, null);
                    for (var msg : kh.getHelp()) {
                        exec.sendExecReadOnlyMessage(msg, _source);
                    }
                } catch (IllegalAccessException |
                         InvocationTargetException |
                         InstantiationException |
                         NoSuchMethodException ex) {
                    exec.sendExecReadOnlyMessage("HELP - INTERNAL ERROR", _source);
                }
            }
        }
    }
}
