/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf.queues;

import com.bearsnake.komodo.kexec.configuration.Node;

/**
 * An in-memory struct describing the content of a card punch queue
 */
public class PunchQueue extends OutputQueue {

    public PunchQueue(
        final String queueName
    ) {
        super(queueName);
    }

    public PunchQueue(
        final String queueName,
        final Node associatedDevice
    ) {
        super(queueName, associatedDevice);
    }
}
