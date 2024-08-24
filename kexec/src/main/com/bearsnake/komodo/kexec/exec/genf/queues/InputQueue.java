/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf.queues;

import com.bearsnake.komodo.kexec.configuration.Node;
import com.bearsnake.komodo.kexec.exec.genf.InputQueueItem;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * An in-memory struct describing the content of an input queue - there is one queue for each card reader device.
 */
public abstract class InputQueue extends Queue {

    // keyed by priority, then each list is in order of submission
    private static final Map<Character, LinkedList<InputQueueItem>> _content = new HashMap<>();

    public InputQueue(
        final String queueName,
        final Node associatedDevice
    ) {
        super(queueName, associatedDevice);
    }
}
