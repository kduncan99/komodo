/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.netlib;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

public class SocketTrace {

    public static class HexSerializer extends JsonSerializer<byte[]> {
        @Override
        public void serialize(byte[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            StringBuilder sb = new StringBuilder();
            for (byte b : value) {
                sb.append(String.format("%02X", b));
            }
            gen.writeString(sb.toString());
        }
    }

    public static class HexDeserializer extends JsonDeserializer<byte[]> {
        @Override
        public byte[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String s = p.getValueAsString();
            if (s == null || s.isEmpty()) {
                return new byte[0];
            }
            byte[] bytes = new byte[s.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
            }
            return bytes;
        }
    }

    public enum Source {
        LOCAL,
        REMOTE
    }

    public static class Entry {

        private final long _timestamp;
        private final Source _source;
        private final byte[] _data;

        @JsonCreator
        public Entry(@JsonProperty("source") final Source source,
                     @JsonProperty("data") @JsonDeserialize(using = HexDeserializer.class) final byte[] data,
                     @JsonProperty("timestamp") final long timestamp) {
            _source = source;
            _data = data;
            _timestamp = timestamp;
        }

        private Entry(final Source source, final byte[] data) {
            _timestamp = System.currentTimeMillis();
            _source = source;
            _data = data;
        }

        @JsonProperty("timestamp")
        public long getTimestamp() { return _timestamp; }

        @JsonProperty("source")
        public Source getSource() { return _source; }

        @JsonProperty("data")
        @JsonSerialize(using = HexSerializer.class)
        public byte[] getData() { return _data; }
    }

    private final LinkedList<Entry> _entries = new LinkedList<>();
    private int _maxEntries = DEFAULT_MAX_ENTRIES;

    @JsonCreator
    public SocketTrace(@JsonProperty("maxEntries") int maxEntries,
                       @JsonProperty("entries") LinkedList<Entry> entries) {
        _maxEntries = maxEntries;
        if (entries != null) {
            _entries.addAll(entries);
        }
    }

    public SocketTrace() {
    }

    @JsonProperty("maxEntries")
    public int getMaxEntries() {
        return _maxEntries;
    }

    @JsonProperty("entries")
    public LinkedList<Entry> getEntriesForJson() {
        return getEntries();
    }

    public static final int DEFAULT_MAX_ENTRIES = 1000;
    public static final int MIN_MAX_ENTRIES = 100;
    public static final int MAX_MAX_ENTRIES = 10000;

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

    public static SocketTrace loadFromFile(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, SocketTrace.class);
    }

    public void saveToFile(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(file, this);
    }
}
