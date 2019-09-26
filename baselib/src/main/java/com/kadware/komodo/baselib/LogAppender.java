/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import org.apache.logging.log4j.core.*;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LogAppender implements Appender {

    public static class LogEntry {
        final Long _identifier;
        final String _message;
        final String _source;
        final long _timeMillis;

        private LogEntry(
            final LogEvent event,
            final long identifier
        ) {
            _identifier = identifier;
            _source = event.getLoggerName();
            _message = event.getMessage().getFormattedMessage();
            _timeMillis = event.getTimeMillis();
        }

        public boolean equals(final Object obj) {
            if (obj instanceof LogEntry) {
                return ((LogEntry) obj)._identifier.equals(_identifier);
            }
            return false;
        }

        public int hashCode() { return _identifier.hashCode(); }
    }

    private long _nextIdentifier = 1;
    private LifeCycle.State _state = State.INITIALIZED;
    private static final int MAX_LOG_ENTRIES = 1000;
    private final TreeMap<Long, LogEntry> _logEntries = new TreeMap<>();

    @Override
    public void append(
        final LogEvent event
    ) {
        synchronized (this) {
            if (_state == State.STARTED) {
                LogEntry entry = new LogEntry(event, _nextIdentifier++);
                _logEntries.put(entry._identifier, entry);
                if (_logEntries.size() > MAX_LOG_ENTRIES) {
                    _logEntries.remove(_logEntries.firstKey());
                }
                System.out.println("======>" + entry._message);//????TODO
            }
        }
    }

    @Override public ErrorHandler getHandler() { return null; }
    @Override public Layout<? extends Serializable> getLayout() { return null; }
    @Override public String getName() { return "KomodoAppender"; }
    @Override public boolean ignoreExceptions() { return true; }
    @Override public void setHandler(ErrorHandler handler) {}

    @Override public LifeCycle.State getState() { return _state; }

    @Override public void initialize() {
        synchronized (this) {
            _state = State.INITIALIZED;
            _nextIdentifier = 1;
        }
    }

    @Override public boolean isStarted() { return _state == State.STARTED; }
    @Override public boolean isStopped() { return _state == State.STOPPED; }
    @Override public void start() { _state = State.STARTED; }
    @Override public void stop() { _state = State.STOPPED; }

    /**
     * Retrieves all the entries with an identifier equal to or greater than the given identifier
     */
    public LogEntry[] retrieveFrom(
        final long lowestIdentifier
    ) {
        List<LogEntry> resultList = new LinkedList<>();
        synchronized (this) {
            Map.Entry<Long, LogEntry> entry = _logEntries.ceilingEntry(lowestIdentifier);
            while (entry != null) {
                resultList.add(entry.getValue());
                entry = _logEntries.higherEntry(entry.getKey());
            }
        }
        return resultList.toArray(new LogEntry[0]);
    }
}
