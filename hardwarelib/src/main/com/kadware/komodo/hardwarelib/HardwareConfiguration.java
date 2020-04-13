/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 *
 * Represents the entirety of the hardware configuration
 */

package com.kadware.komodo.hardwarelib;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadware.komodo.baselib.PathNames;
import java.io.File;
import java.io.IOException;

public class HardwareConfiguration {

    private static final String FILE_NAME = PathNames.CONFIG_ROOT_DIRECTORY + "hardware.json";

    @JsonCreator
    public HardwareConfiguration(
    ) {
    }

    public static HardwareConfiguration read(
    ) throws IOException {
        return read(FILE_NAME);
    }

    public static HardwareConfiguration read(
        final String fileName
    ) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(new File(fileName), HardwareConfiguration.class);
    }

    public void write(
    ) throws IOException {
        write(FILE_NAME);
    }

    public void write(
        final String fileName
    ) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(fileName), this);
    }
}
