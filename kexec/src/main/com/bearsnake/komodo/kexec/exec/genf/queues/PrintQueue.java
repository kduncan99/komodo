/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf.queues;

import com.bearsnake.komodo.kexec.configuration.Node;

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
}
