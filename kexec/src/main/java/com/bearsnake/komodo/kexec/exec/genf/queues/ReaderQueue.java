/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf.queues;

import com.bearsnake.komodo.kexec.configuration.Node;

import java.io.PrintStream;

/**
 * An in-memory struct describing the content of a card reader queue
 */
public class ReaderQueue extends InputQueue {

    /*
     * Reader queues are always associated with a device
     */
    public ReaderQueue(
        final String queueName,
        final Node associatedDevice
    ) {
        super(queueName, associatedDevice);
    }

    @Override
    public void dump(final PrintStream out, final String indent, final boolean verbose) {
        out.printf("%sReader Queue %s:\n", indent, getQueueName());
        // TODO
    }
}
