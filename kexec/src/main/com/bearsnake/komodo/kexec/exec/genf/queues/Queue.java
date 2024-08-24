/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf.queues;

import com.bearsnake.komodo.kexec.configuration.Node;

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

    public final Node getAssociatedDevice() { return _associatedDevice; }
    public final boolean hasAssociatedDevice() { return _associatedDevice != null; }
    public final String getQueueName() { return _queueName; }
}
