/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.PathNames;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Simple utility class to create a temporary deployment for testing purposes
 */
public class Test_Deployer {

    //  No static initialization - the results of PathNames.xxx may be different when invoked,
    //  as compared to class load time.
    private Path _binariesDestPath = null;
    private Path _configDestPath = null;
    private Path _disksDestPath = null;
    private Path _logsDestPath = null;
    private Path _symbiontsDestPath = null;
    private Path _tapesDestPath = null;
    private Path _webDestPath = null;

    /**
     * If source is a file, copy the file to the destination (which should be a directory)
     * If source is a directory, copy all the entities of source to destination (which should be a directory)
     */
    private static void copy(
        final File sourceFile,
        final File destinationFile,
        final String indent
    ) throws IOException {
        String sourceFileName = sourceFile.getName();
        if (!sourceFileName.equals(".") && !sourceFileName.equals("..")) {
            System.out.println(String.format("%sCopying %s to %s",
                                             indent,
                                             sourceFile.toString(),
                                             destinationFile.toString()));
            if (!destinationFile.exists()) {
                System.out.println(String.format("%s  Creating %s", indent, destinationFile.toString()));
                Files.createDirectories(destinationFile.toPath());
            } else {
                if (!destinationFile.isDirectory()) {
                    throw new RuntimeException("! " + destinationFile.toString() + " is not a directory");
                }
            }

            if (sourceFile.isDirectory()) {
                File[] subFiles = sourceFile.listFiles();
                if (subFiles != null) {
                    for (File subFile : subFiles) {
                        Path destSubPath = Paths.get(destinationFile.toString(), subFile.getName());
                        File dspf = destSubPath.toFile();
                        if (dspf.exists()) {
                            delete(dspf, indent + "  ");
                        }
                        copy(subFile, dspf, indent + "  ");
                    }
                }
            } else if (sourceFile.isFile()) {
                Files.deleteIfExists(destinationFile.toPath());
                Files.copy(sourceFile.toPath(), destinationFile.toPath());
            }
        }
    }

    /**
     * recursive path delete
     */
    private static void delete(
        final File destination,
        final String indent
    ) throws IOException {
        if (destination != null) {
            System.out.println(String.format("%sDeleting %s...", indent, destination.toString()));
            if (destination.exists()) {
                if (destination.isDirectory()) {
                    File[] subFiles = destination.listFiles();
                    if (subFiles != null) {
                        for (File subFile : subFiles) {
                            delete(subFile, indent + "  ");
                        }
                    }
                }
                Files.deleteIfExists(destination.toPath());
            }
        }
    }

    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Deploys directory content.
     * Can run standalone, or be invoked by other tests.
     * Generally, standalone won't work if for non-containerized situations, so...
     */
    @Test
    public void deploy(
    ) throws IOException {
        Path binariesSourcePath = Paths.get("../resources/media/binaries");
        Path configSourcePath = Paths.get("../resources/config");
        Path disksSourcePath = Paths.get("../resources/media/disks");
        Path tapesSourcePath = Paths.get("../resources/media/tapes");
        Path webSourcePath = Paths.get("../resources/web");

        _binariesDestPath = Paths.get(PathNames.BINARIES_ROOT_DIRECTORY);
        _configDestPath = Paths.get(PathNames.CONFIG_ROOT_DIRECTORY);
        _disksDestPath = Paths.get(PathNames.DISKS_ROOT_DIRECTORY);
        _logsDestPath = Paths.get(PathNames.LOGS_ROOT_DIRECTORY);
        _symbiontsDestPath = Paths.get(PathNames.SYMBIONTS_ROOT_DIRECTORY);
        _tapesDestPath = Paths.get(PathNames.TAPES_ROOT_DIRECTORY);
        _webDestPath = Paths.get(PathNames.WEB_ROOT_DIRECTORY);

        Files.createDirectories(_logsDestPath);

        copy(binariesSourcePath.toFile(), _binariesDestPath.toFile(), "");
        copy(configSourcePath.toFile(), _configDestPath.toFile(), "");
        copy(disksSourcePath.toFile(), _disksDestPath.toFile(), "");
        copy(disksSourcePath.toFile(), _symbiontsDestPath.toFile(), "");
        copy(tapesSourcePath.toFile(), _tapesDestPath.toFile(), "");
        copy(webSourcePath.toFile(), _webDestPath.toFile(), "");
    }

    /**
     * Removes the deployed directories
     * Can run standalone, or be invoked by other tests
     * Generally, standalone won't work if for non-containerized situations, so...
     */
    @Test
    public void remove(
    ) throws IOException {
        delete(_binariesDestPath.toFile(), "");
        delete(_configDestPath.toFile(), "");
        delete(_disksDestPath.toFile(), "");
        delete(_logsDestPath.toFile(), "");
        delete(_symbiontsDestPath.toFile(), "");
        delete(_tapesDestPath.toFile(), "");
        delete(_webDestPath.toFile(), "");

        _binariesDestPath = null;
        _configDestPath = null;
        _disksDestPath = null;
        _logsDestPath = null;
        _symbiontsDestPath = null;
        _tapesDestPath = null;
        _webDestPath = null;
    }
}
