/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

public class PathNames {

    //  If we are in testing mode, these directories will all be under a root directory.
    //  The environment value BASE_DIR shall be set to this directory name, with a path separator on the end.
    //  For production use, the environment variable shall not be set, or shall be set to the empty string.
    public static final String BASE_DIRECTORY = System.getenv().getOrDefault("BASE_DIR", "/");

    //  System environment can override each individual directory name...
    //  Each so-designated directory name should end with a path delimiter.
    public static final String CONFIG_ROOT_DIRECTORY = System.getenv().getOrDefault("CONFIG_DIR", BASE_DIRECTORY + "config/");
    public static final String DISKS_ROOT_DIRECTORY = System.getenv().getOrDefault("DISKS_DIR", BASE_DIRECTORY + "disks/");
    public static final String LOGS_ROOT_DIRECTORY = System.getenv().getOrDefault("LOGS_DIR", BASE_DIRECTORY + "logs/");
    public static final String RESOURCES_ROOT_DIRECTORY = System.getenv().getOrDefault("RESOURCES_DIR", BASE_DIRECTORY + "resources/");
    public static final String SYMBIONTS_ROOT_DIRECTORY = System.getenv().getOrDefault("SYMBIONTS_DIR", BASE_DIRECTORY + "symbionts/");
    public static final String TAPES_ROOT_DIRECTORY = System.getenv().getOrDefault("TAPES_DIR", BASE_DIRECTORY + "tapes/");
}
