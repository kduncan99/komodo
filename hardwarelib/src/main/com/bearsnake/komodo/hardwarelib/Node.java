/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Node {

    private static final AtomicInteger _nextInteger = new AtomicInteger(1);

    protected boolean _logIos = false;
    protected final int _nodeIdentifier;
    protected final String _nodeName;

    public Node(final String nodeName) {
        _nodeIdentifier = _nextInteger.getAndIncrement();
        _nodeName = nodeName;
    }

    public final int getNodeIdentifier() { return _nodeIdentifier; }
    public final String getNodeName() { return _nodeName; }
    public abstract NodeCategory getNodeCategory();

    public final void setLogIos(final boolean flag) { _logIos = flag; }
}
