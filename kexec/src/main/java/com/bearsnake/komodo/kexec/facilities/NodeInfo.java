/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.hardwarelib.Node;
import com.bearsnake.komodo.kexec.scheduleManager.Run;

/**
 * Contains temporal information for fac manager regarding a particular node
 */
public abstract class NodeInfo {

    private final Node _node;
    private NodeStatus _nodeStatus;
    private MediaInfo  _mediaInfo;
    private Run _assignedTo;

    public NodeInfo(final Node node) {
        _node = node;
        _nodeStatus = NodeStatus.Up;
    }

    public Node getNode() { return _node; }
    public NodeStatus getNodeStatus() { return _nodeStatus; }
    public MediaInfo getMediaInfo() { return _mediaInfo; }
    public Run getAssignedTo() { return _assignedTo; }

    public void setAssignedTo(final Run rce) { _assignedTo = rce; }
    public void setMediaInfo(final MediaInfo mi) { _mediaInfo = mi; }
    public void setNodeStatus(final NodeStatus status) { _nodeStatus = status; }

    @Override
    public String toString() {
        return String.format("%s %s", getNode().toString(), getNodeStatus());
    }
}
