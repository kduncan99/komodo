/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.hardwarelib.Node;

/**
 * Contains temporal information for fac manager regarding a particular node
 */
public class ChannelNodeInfo extends NodeInfo {

    public ChannelNodeInfo(final Node node) {
        super(node);
    }

    @Override
    public String toString() {
        return String.format("%s %s %s",
                             getNode().toString(),
                             getNodeStatus(),
                             getMediaInfo() == null ? "" : getMediaInfo().toString());
    }
}
