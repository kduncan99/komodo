/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 *
 * Represents the entirety of the hardware configuration
 */

package com.kadware.komodo.hardwarelib;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kadware.komodo.baselib.Credentials;
import java.io.File;
import java.io.IOException;

public class Configurator {

    public static class Version {
        public final int _major;
        public final int _minor;
        public final int _patch;
        public final int _build;

        @JsonCreator
        public Version(
            @JsonProperty("major") final int major,
            @JsonProperty("minor") final int minor,
            @JsonProperty("patch") final int patch,
            @JsonProperty("build") final int build
        ) {
            _major = major;
            _minor = minor;
            _patch = patch;
            _build = build;
        }
    }

    public final String _copyright;
    public final Credentials _adminCredentials;
    public final String _systemIdentifier;
    public final Version _version;
    public final String _versionString;

    @JsonCreator
    public Configurator(
        @JsonProperty("copyright") final String copyright,
        @JsonProperty("version") final Version version,
        @JsonProperty("systemIdentifier") final String systemIdentifier,
        @JsonProperty("credentials") final Credentials adminCredentials
    ) {
        _copyright = copyright;
        _version = version;
        _versionString = String.format("%d.%d.%d.%d",
                                       _version._major,
                                       _version._minor,
                                       _version._patch,
                                       _version._build);
        if (systemIdentifier == null) {
            _systemIdentifier = "EM2200";
        } else {
            _systemIdentifier = systemIdentifier;
        }

        _adminCredentials = adminCredentials;
    }

    /**
     * Creates a Configurator object from a JSON-formatted file
     */
    public static Configurator read(
        final String fileName
    ) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(new File(fileName), Configurator.class);
    }

    /**
     * Writes a Configurator object to a JSON-formatted file
     */
    public void write(
        final String fileName
    ) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(fileName), this);
    }
}
