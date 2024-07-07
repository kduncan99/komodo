/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration;

import java.util.LinkedList;

public class ChannelNode implements ConfigNode {

    private final String _name;
    private final LinkedList<DeviceNode> _devices = new LinkedList<>();

    ChannelNode(final String name) {
        _name = name;
    }

    public final void addDevice(final DeviceNode device) {
        _devices.add(device);
    }

    public final String getName() {
        return _name;
    }
}
