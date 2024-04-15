/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.hardwarelib.Node;

/**
 * Contains temporal information for fac manager regarding a particular node
 */
public abstract class NodeInfo {

    Node _node;
    NodeStatus _nodeStatus;
    MediaInfo  _mediaInfo;

    public NodeInfo(final Node node) {
        _node = node;
        _nodeStatus = NodeStatus.Up;
    }

    public Node getNode() { return _node; }
    public NodeStatus getNodeStatus() { return _nodeStatus; }
    public MediaInfo getMediaInfo() { return _mediaInfo; }

    public abstract String toString();
}
