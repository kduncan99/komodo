/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class LogHandler extends CSIHandler {

    private static final Logger LOGGER = LogManager.getLogger(LogHandler.class);

    @Override
    public boolean allowCSF() { return true; }

    @Override
    public boolean allowCSI() { return false; }

    @Override
    public boolean allowTIP() { return true; }

    @Override
    public String getCommand() { return "LOG"; }

    @Override
    public void handle(final HandlerPacket hp) {
        if (!cleanOptions(hp) || (hp._optionWord != 0)) {
            postSyntaxError(hp);
            return;
        }

        if (hp._statement._operandFields.size() != 1) {
            postSyntaxError(hp);
            return;
        }

        var opField = hp._statement._operandFields.get(new SubfieldSpecifier(0, 0));
        LOGGER.info("[{}] {}", hp._run.getActualRunId(), opField);
    }
}
