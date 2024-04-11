/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import java.util.HashMap;
import java.util.HashSet;

public abstract class Channel extends Node {

    // key is node identifier
    protected final HashMap<Integer, Device> _devices = new HashMap<>();

    public Channel(final String nodeName) {
        super(nodeName);
    }

    public final void attach(final Device device) {
        _devices.put(device.getNodeIdentifier(), device);
    }

    public final void detach(final Device device) {
        _devices.remove(device.getNodeIdentifier());
    }

    public abstract boolean canAttach(final Device device);
    public abstract ChannelType getChannelType();
    public HashSet<Device> getDevices() { return new HashSet<>(_devices.values()); }
    public abstract void routeIo(final int nodeIdentifier, final IoPacket ioPacket);

    @Override
    public NodeCategory getNodeCategory() { return NodeCategory.Channel; }
}
