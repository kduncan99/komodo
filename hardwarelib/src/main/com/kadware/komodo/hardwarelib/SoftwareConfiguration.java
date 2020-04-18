/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 *
 * Represents the entirety of the hardware configuration
 */

package com.kadware.komodo.hardwarelib;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadware.komodo.baselib.Credentials;
import com.kadware.komodo.baselib.PathNames;
import java.io.File;
import java.io.IOException;

public class SoftwareConfiguration {

    private static final String FILE_NAME = PathNames.CONFIG_ROOT_DIRECTORY + "software.json";

    public static class Version {
        @JsonProperty("major")      public final int _major;
        @JsonProperty("minor")      public final int _minor;
        @JsonProperty("patch")      public final int _patch;
        @JsonProperty("build")      public final int _build;

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

    @JsonProperty("copyright")          public final String _copyright;
    @JsonProperty("credentials")        public final Credentials _adminCredentials;
    @JsonProperty("httpPort")           public final Integer _httpPort;
    @JsonProperty("systemIdentifier")   public final String _systemIdentifier;
    @JsonProperty("version")            public final Version _version;

    public final String _versionString;

    @JsonCreator
    public SoftwareConfiguration(
        @JsonProperty("copyright")          final String copyright,
        @JsonProperty("version")            final Version version,
        @JsonProperty("systemIdentifier")   final String systemIdentifier,
        @JsonProperty("credentials")        final Credentials adminCredentials,
        @JsonProperty("httpPort")           final Integer httpPort
    ) {
        _copyright = copyright;
        _version = version;
        _versionString = String.format("%d.%d.%d.%d",
                                       _version._major,
                                       _version._minor,
                                       _version._patch,
                                       _version._build);
        _systemIdentifier = systemIdentifier;
        _httpPort = httpPort;
        _adminCredentials = adminCredentials;
    }


    public static SoftwareConfiguration read(
    ) throws IOException {
        return read(FILE_NAME);
    }

    public static SoftwareConfiguration read(
        final String fileName
    ) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(new File(fileName), SoftwareConfiguration.class);
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
