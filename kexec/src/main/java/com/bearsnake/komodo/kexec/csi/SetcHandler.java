/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

class SetcHandler extends CSIHandler {

    @Override
    public boolean allowCSF() { return true; }

    @Override
    public boolean allowCSI() { return true; }

    @Override
    public boolean allowTIP() { return true; }

    @Override
    public String getCommand() { return "SETC"; }

    @Override
    public void handle(final HandlerPacket hp) {
        // TODO
    }
}
