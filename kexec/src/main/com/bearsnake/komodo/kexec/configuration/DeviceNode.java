/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration;

public class DeviceNode implements ConfigNode {

    private final String _name;

    DeviceNode(final String name) {
        _name = name;
    }

    public final String getName() {
        return _name;
    }
}
