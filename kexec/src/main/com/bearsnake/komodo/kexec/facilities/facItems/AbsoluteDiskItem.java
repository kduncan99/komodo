/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities.facItems;

import com.bearsnake.komodo.hardwarelib.Node;

import java.io.PrintStream;

/**
 * Could refer to a disk device or a disk controller.
 */
public class AbsoluteDiskItem extends FacilitiesItem {

    public final Node _node;
    public final String _requestedPackName; // requested pack name (may not be the mounted pack name)
    public boolean _isAssigned; // false if we are waiting on the node

    public AbsoluteDiskItem(
        final Node node,
        final String requestedPackName
    ) {
        _node = node;
        _requestedPackName = requestedPackName;
        _isAssigned = false;
    }

    @Override
    public void dump(final PrintStream out,
                     final String indent) {
        super.dump(out, indent);
        out.printf("%s  node:%s reqPack:%s asg:%s\n", indent, _node.getNodeName(), _requestedPackName, _isAssigned);
    }

    public final Node getNode() { return _node; }
    public final String getRequestedPackName() { return _requestedPackName; }
    public final boolean isAssigned() { return _isAssigned; }
    public final AbsoluteDiskItem setIsAssigned(final boolean flag) { _isAssigned = flag; return this; }
}
