/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

class XqtHandler extends CSIHandler {

    @Override
    public boolean allowCSF() { return false; }

    @Override
    public boolean allowCSI() { return false; }

    @Override
    public boolean allowTIP() { return false; }

    @Override
    public String getCommand() { return "XQT"; }

    @Override
    public void handle(final HandlerPacket hp) {
        // TODO
    }
}
