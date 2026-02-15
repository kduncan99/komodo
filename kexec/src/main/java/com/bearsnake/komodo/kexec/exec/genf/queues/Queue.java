/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf.queues;

import com.bearsnake.komodo.kexec.configuration.Node;

import java.io.PrintStream;

public abstract class Queue {

    private final String _queueName;
    public final Node _associatedDevice; // null if this is an unassociated queue

    protected Queue(final String queueName) {
        this(queueName, null);
    }

    protected Queue(final String queueName,
                    final Node associatedDevice) {
        _queueName = queueName;
        _associatedDevice = associatedDevice;
    }

    public abstract void dump(final PrintStream out, final String indent, final boolean verbose);

    public final Node getAssociatedDevice() { return _associatedDevice; }
    public final boolean hasAssociatedDevice() { return _associatedDevice != null; }
    public final String getQueueName() { return _queueName; }
}
