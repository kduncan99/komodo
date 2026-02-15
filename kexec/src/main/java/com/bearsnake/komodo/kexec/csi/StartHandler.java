/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

class StartHandler extends CSIHandler {

    @Override
    public boolean allowCSF() { return true; }

    @Override
    public boolean allowCSI() { return false; }

    @Override
    public boolean allowTIP() { return true; }

    @Override
    public String getCommand() { return "START"; }

    @Override
    public void handle(final HandlerPacket hp) {
        // TODO
    }
}
