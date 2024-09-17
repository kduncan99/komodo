/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;

import java.util.HashSet;
import java.util.Set;

public abstract class JumpKeyHandler extends KeyinHandler {

    protected Set<Integer> _jumpKeys = new HashSet<>();

    public JumpKeyHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    final boolean isAllowed() {
        return true;
    }

    /**
     * Checks the syntax of the arguments - should be 'ALL' or a comma-separated list of jump key numbers
     * (ranging from 1 to 36). - build up the set of jump keys referenced along the way.
     */
    protected final boolean checkJumpKeyArguments() {
        if (_options != null) {
            return false;
        }

        if (_arguments.equalsIgnoreCase("ALL")) {
            for (var jk = 1; jk <= 36; jk++) {
                _jumpKeys.add(jk);
            }
            return true;
        }

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
    }

    protected final void setOrClearJumpKeys(final boolean jumpKeyValue) {
        for (var jk : _jumpKeys) {
            if (!Exec.getInstance().isRunning()) {
                return;
            }

            Exec.getInstance().setJumpKeyValue(jk, jumpKeyValue);
        }
    }
}
