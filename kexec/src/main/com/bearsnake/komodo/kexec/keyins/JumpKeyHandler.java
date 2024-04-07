/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;

import java.util.LinkedList;

public abstract class JumpKeyHandler extends KeyinHandler {

    public JumpKeyHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    public final void abort(){}

    @Override
    public final boolean checkSyntax() {
        if (this instanceof CJKeyinHandler || this instanceof SJKeyinHandler) {
            if (_options != null && _arguments == null) {
                return _options.equalsIgnoreCase("ALL");
            } else if (_options == null && _arguments != null) {
                var split = _arguments.split(",");
                for (var arg : split) {
                    try {
                        var i = Integer.parseInt(arg);
                        if (i < 0) {
                            return false;
                        }
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        } else {
            return _options == null && _arguments == null;
        }
    }

    protected final void displayJumpKeys(final ConsoleId source) {
        var sb = new StringBuilder();
        var anySet = false;
        for (int jk = 1; jk <= 36; ++jk) {
            if (Exec.getInstance().isJumpKeySet(jk)) {
                anySet = true;
                if (sb.isEmpty()) {
                    sb.append("Jump Keys Set: ").append(jk);
                } else {
                    sb.append(",").append(jk);
                    if (sb.length() > 60) {
                        Exec.getInstance().sendExecReadOnlyMessage(sb.toString(), source);
                        sb.setLength(0);
                    }
                }
            }
        }

        if (!anySet) {
            Exec.getInstance().sendExecReadOnlyMessage("Jump Keys Set: <none>", source);
        } else if (!sb.isEmpty()) {
            Exec.getInstance().sendExecReadOnlyMessage(sb.toString(), source);
        }
    }

    @Override
    public final boolean isAllowed() {
        return true;
    }

    protected final void process(final boolean jkValue) {
        if (_options != null) {
            processAll(jkValue);
        } else {
            processArgs(jkValue);
        }

        if (Exec.getInstance().isRunning()) {
            displayJumpKeys(_source);
        }
    }

    protected final void processAll(final boolean jkValue) {
        for (int jk = 1; jk <= 36 && Exec.getInstance().isRunning(); jk++) {
            Exec.getInstance().setJumpKeyValue(jk, jkValue);
        }
    }

    protected final void processArgs(final boolean jkValue) {
        var jumpKeys = new LinkedList<Integer>();
        var split = _arguments.split(",");
        for (var arg : split) {
            if (!Exec.getInstance().isRunning()) {
                return;
            }

            var jk = Integer.parseInt(arg);
            if (jk < 1 || jk > 36) {
                var msg = "Invalid jump key specified";
                Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
                return;
            }

            jumpKeys.add(jk);
        }

        for (var jk : jumpKeys) {
            if (!Exec.getInstance().isRunning()) {
                return;
            }

            Exec.getInstance().setJumpKeyValue(jk, jkValue);
        }
    }
}
