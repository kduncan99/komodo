/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf.queues;

import com.bearsnake.komodo.kexec.configuration.Node;

import java.io.PrintStream;

/**
 * An in-memory struct describing the content of a printer queue
 */
public class PrintQueue extends OutputQueue {

    public PrintQueue(
        final String queueName
    ) {
        super(queueName);
    }

    public PrintQueue(
        final String queueName,
        final Node associatedDevice
    ) {
        super(queueName, associatedDevice);
    }

    @Override
    public void dump(final PrintStream out, final String indent, final boolean verbose) {
        out.printf("%sPrint Queue %s:\n", indent, getQueueName());
        // TODO
    }
}
