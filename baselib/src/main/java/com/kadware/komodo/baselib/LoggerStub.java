/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

/**
 * An empty stub class which implements the apache log4j 2 Logger, doing nothing.
 * For test cases, we might extend this so we can unit test our logging.
 * For working code, we might extend this somehow so that we can send stuff to various displays, or something like that.
 */

import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;

public class LoggerStub extends AbstractLogger {

    Level _level = Level.ALL;

    @Override
    public Level getLevel(
    ) {
        return _level;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final Message message,
        final Throwable t
    ) {
        return true;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final CharSequence s,
        final Throwable t
    ) {
        return true;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final String string,
        final Throwable t
    ) {
        return true;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final Object o,
        final Throwable t
    ) {
        return true;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final String string
    ) {
        return true;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final String string,
        final Object[] o
    ) {
        return true;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final String string,
        final Object o1
    ) {
        return true;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final String string,
        final Object o1,
        final Object o2
    ) {
        return true;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final String string,
        final Object o1,
        final Object o2,
        final Object o3
    ) {
        return true;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final String string,
        final Object o1,
        final Object o2,
        final Object o3,
        final Object o4
    ) {
        return true;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final String string,
        final Object o1,
        final Object o2,
        final Object o3,
        final Object o4,
        final Object o5
    ) {
        return true;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final String string,
        final Object o1,
        final Object o2,
        final Object o3,
        final Object o4,
        final Object o5,
        final Object o6
    ) {
        return true;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final String string,
        final Object o1,
        final Object o2,
        final Object o3,
        final Object o4,
        final Object o5,
        final Object o6,
        final Object o7
    ) {
        return true;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final String string,
        final Object o1,
        final Object o2,
        final Object o3,
        final Object o4,
        final Object o5,
        final Object o6,
        final Object o7,
        final Object o8
    ) {
        return true;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final String string,
        final Object o1,
        final Object o2,
        final Object o3,
        final Object o4,
        final Object o5,
        final Object o6,
        final Object o7,
        final Object o8,
        final Object o9
    ) {
        return true;
    }

    @Override
    public boolean isEnabled(
        final Level level,
        final Marker marker,
        final String string,
        final Object o1,
        final Object o2,
        final Object o3,
        final Object o4,
        final Object o5,
        final Object o6,
        final Object o7,
        final Object o8,
        final Object o9,
        final Object o10
    ) {
        return true;
    }

    @Override
    public void logMessage(
        final String string,
        final Level level,
        final Marker marker,
        final Message msg,
        final Throwable throwable
    ) {
    }
}
