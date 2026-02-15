/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

class SymcnHandler extends CSIHandler {

    @Override
    public boolean allowCSF() { return true; }

    @Override
    public boolean allowCSI() { return false; }

    @Override
    public boolean allowTIP() { return true; }

    @Override
    public String getCommand() { return "SYMCN"; }

    @Override
    public void handle(final HandlerPacket hp) {
        // TODO
    }
}
