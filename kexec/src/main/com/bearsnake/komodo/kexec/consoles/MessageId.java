/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

import java.util.concurrent.atomic.AtomicInteger;

public class MessageId {

    private static AtomicInteger _nextMessageId = new AtomicInteger(1);

    private final int _identifier;

    public MessageId() {
        _identifier = _nextMessageId.getAndIncrement();
    }

    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof MessageId mid) && (mid._identifier == _identifier);
    }

    @Override
    public int hashCode() {
        return _identifier;
    }

    @Override
    public String toString() {
        return String.valueOf(_identifier);
    }
}
