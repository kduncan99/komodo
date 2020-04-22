/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

public class PathNames {

    //  If we are in testing mode, these directories will all be under a root directory.
    //  The environment value BASE_DIR shall be set to this directory name, with a path separator on the end.
    //  For production use, the environment variable shall not be set, or shall be set to the empty string.
    public static final String BASE_DIRECTORY = getTag("BASE_DIR", "/");

    //  System environment can override each individual directory name...
    //  Each so-designated directory name should end with a path delimiter.
    public static final String BINARIES_ROOT_DIRECTORY = getTag("BINARIES_DIR", BASE_DIRECTORY + "binaries/");
    public static final String CONFIG_ROOT_DIRECTORY = getTag("CONFIG_DIR", BASE_DIRECTORY + "config/");
    public static final String DISKS_ROOT_DIRECTORY = getTag("DISKS_DIR", BASE_DIRECTORY + "disks/");
    public static final String LOGS_ROOT_DIRECTORY = getTag("LOGS_DIR", BASE_DIRECTORY + "logs/");
    public static final String SYMBIONTS_ROOT_DIRECTORY = getTag("SYMBIONTS_DIR", BASE_DIRECTORY + "symbionts/");
    public static final String TAPES_ROOT_DIRECTORY = getTag("TAPES_DIR", BASE_DIRECTORY + "tapes/");
    public static final String WEB_ROOT_DIRECTORY = getTag("WEB_DIR", BASE_DIRECTORY + "web/");

    private static String getTag(
        final String key,
        final String defaultValue
    ) {
        String value = System.getProperty(key, null);
        if (value == null) {
            value = System.getenv().getOrDefault(key, defaultValue);
        }
        return value;
    }
}
