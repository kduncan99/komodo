/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

class HdgHandler extends Handler {

    @Override
    public boolean allowCSF() { return false; }

    @Override
    public boolean allowCSI() { return false; }

    @Override
    public boolean allowTIP() { return false; }

    @Override
    public String getCommand() { return "HDG"; }

    @Override
    public void handle(final HandlerPacket hp) {
        // TODO
    }
}
