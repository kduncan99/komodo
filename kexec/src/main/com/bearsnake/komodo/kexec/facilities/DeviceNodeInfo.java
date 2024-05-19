/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.hardwarelib.Channel;
import com.bearsnake.komodo.hardwarelib.Node;

import java.util.LinkedList;

/**
 * Contains temporal information for fac manager regarding a particular node
 */
public class DeviceNodeInfo extends NodeInfo {

    MediaInfo  _mediaInfo;
    final LinkedList<Channel> _routes = new LinkedList<>();

    public DeviceNodeInfo(final Node node) {
        super(node);
    }

    public void addChannel(final Channel channel) { _routes.add(channel); }
}
