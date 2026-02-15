/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec.genf.queues;

import com.bearsnake.komodo.kexec.configuration.Node;
import com.bearsnake.komodo.kexec.exec.genf.OutputQueueItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An in-memory struct describing the content of an output queue
 */
public abstract class OutputQueue extends Queue {

    // keyed by priority index, then each list is in order of submission
    private static final Map<Integer, LinkedList<OutputQueueItem>> _content = new HashMap<>();

    public OutputQueue(
        final String queueName
    ) {
        super(queueName);
    }

    public OutputQueue(
        final String queueName,
        final Node associatedDevice
    ) {
        super(queueName, associatedDevice);
    }

    public synchronized void enqueue(final OutputQueueItem item) {
        if (!_content.containsKey(item.getPriorityIndex())) {
            _content.put(item.getPriorityIndex(), new LinkedList<>());
        }
        _content.get(item.getPriorityIndex()).add(item);
    }

    public void moveTo(final OutputQueue destination) {
        Collection<OutputQueueItem> list;
        synchronized (this) {
            list = _content.values()
                           .stream()
                           .flatMap(Collection::stream)
                           .collect(Collectors.toCollection(LinkedList::new));
            _content.clear();
        }

        list.forEach(this::enqueue);
    }
}
