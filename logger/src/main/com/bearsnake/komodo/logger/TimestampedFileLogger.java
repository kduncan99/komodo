/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.logger;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimestampedFileLogger extends FileLogger {

    public TimestampedFileLogger(final String fileNamePrefix) throws FileNotFoundException {
        super(generateFileName(fileNamePrefix));
    }

    public TimestampedFileLogger(final String fileNamePrefix,
                                 final Level level) throws FileNotFoundException {
        super(generateFileName(fileNamePrefix), level);
    }

    public TimestampedFileLogger(final String fileNamePrefix,
                                 final boolean enabled,
                                 final Level level) throws FileNotFoundException {
        super(generateFileName(fileNamePrefix), enabled, level);
    }

    private static String generateFileName(final String fileNamePrefix) {
        var dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        String dtStr = dateTime.format(formatter);
        return String.format("%s-%s.log", fileNamePrefix, dtStr);
    }
}
