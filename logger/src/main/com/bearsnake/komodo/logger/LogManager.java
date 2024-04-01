/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.logger;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.HashMap;

public class LogManager {

    private static boolean _enabled = true;
    private static Level _level = Level.Error;
    private static final HashMap<Logger, String> _loggers = new HashMap<>();
    private static final SimpleDateFormat _dateTimeFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");

    public static void close() {
        synchronized (_loggers) {
            for (var logger : _loggers.keySet()) {
                logger.close();
            }
        }
    }

    public static void clear() {
        synchronized (_loggers) {
            for (var logger : _loggers.keySet()) {
                logger.close();
            }
            _loggers.clear();
        }
    }

    public static void register(final Logger logger) {
        synchronized (_loggers) {
            _loggers.put(logger, null);
        }
    }

    public static void register(final Logger logger,
                                final String source) {
        synchronized (_loggers) {
            _loggers.put(logger, source);
        }
    }

    public static void setGlobalEnabled(final boolean enabled) {
        _enabled = enabled;
    }

    public static void setGlobalLevel(final Level level) {
        _level = level;
    }

    public static void log(final Level level,
                           final String source,
                           final String format,
                           final Object[]... parameters) {
        if (level.ordinal() <= _level.ordinal() && _enabled) {
            var tmStamp = LocalDateTime.now();
            var dtStr = _dateTimeFormat.format(tmStamp);
            var msg = String.format("%s:%s:%s:", dtStr, level._text, source);
            msg += String.format(format, parameters); // TODO is this a problem?

            synchronized (_loggers) {
                for (java.util.Map.Entry<Logger, String> entry : _loggers.entrySet()) {
                    var logger = entry.getKey();
                    var loggerSource = entry.getValue();
                    if (loggerSource == null || loggerSource == source) {
                        logger.log(level, msg);
                    }
                }
            }
        }
    }

    public static void logFatal(final String source,
                                final String format,
                                final Object[]... parameters) {
        log(Level.Fatal, source, format, parameters);
    }

    public static void logError(final String source,
                                final String format,
                                final Object[]... parameters) {
        log(Level.Error, source, format, parameters);
    }

    public static void logWarning(final String source,
                                  final String format,
                                  final Object[]... parameters) {
        log(Level.Warning, source, format, parameters);
    }
}
