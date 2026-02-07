/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.netlib;

import java.util.Arrays;
import java.util.LinkedList;

public class SocketTrace {

    public enum Source {
        LOCAL,
        REMOTE
    }

    public static class Entry {

        private final long _timestamp;
        private final Source _source;
        private final byte[] _data;

        private Entry(final Source source, final byte[] data) {
            _timestamp = System.currentTimeMillis();
            _source = source;
            _data = data;
        }

        public long getTimestamp() { return _timestamp; }
        public Source getSource() { return _source; }
        public byte[] getData() { return _data; }
    }

    public static final int DEFAULT_MAX_ENTRIES = 1000;
    public static final int MIN_MAX_ENTRIES = 100;
    public static final int MAX_MAX_ENTRIES = 10000;

    private final LinkedList<Entry> _entries = new LinkedList<>();
    private int _maxEntries = DEFAULT_MAX_ENTRIES;

    public void addEntry(final Source source,
                         final byte[] data) {
        synchronized (_entries) {
            _entries.add(new Entry(source, data.clone()));
            while (_entries.size() > _maxEntries) {
                _entries.removeFirst();
            }
        }
    }

    public void addEntry(final Source source,
                         final byte[] data,
                         final int offset,
                         final int length) {
        synchronized (_entries) {
            _entries.add(new Entry(source, Arrays.copyOfRange(data, offset, length)));
            while (_entries.size() > _maxEntries) {
                _entries.removeFirst();
            }
        }
    }

    public void setMaxEntries(int maxEntries) {
        if (maxEntries < MIN_MAX_ENTRIES) {
            maxEntries = MIN_MAX_ENTRIES;
        } else if (maxEntries > MAX_MAX_ENTRIES) {
            maxEntries = MAX_MAX_ENTRIES;
        }

        synchronized (_entries) {
            _maxEntries = maxEntries;
            while (_entries.size() > _maxEntries) {
                _entries.removeFirst();
            }
        }
    }

    public LinkedList<Entry> getEntries() {
        synchronized (_entries) {
            return new LinkedList<>(_entries);
        }
    }
}
