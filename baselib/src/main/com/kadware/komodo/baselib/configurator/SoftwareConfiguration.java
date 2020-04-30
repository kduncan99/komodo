/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 *
 * Represents the entirety of the hardware configuration
 */

package com.kadware.komodo.baselib.configurator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

public class SoftwareConfiguration extends Configuration {

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

    @JsonProperty("format")             public final int _format;
    @JsonProperty("copyright")          public final String _copyright;
    @JsonProperty("notes")              public final String[] _notes;
    @JsonProperty("systemIdentifier")   public String _systemIdentifier;
    @JsonProperty("version")            public Version _version;

    public final String _versionString;

    @JsonCreator
    public SoftwareConfiguration(
        @JsonProperty("format")             final int format,
        @JsonProperty("copyright")          final String copyright,
        @JsonProperty("notes")              final String[] notes,
        @JsonProperty("version")            final Version version,
        @JsonProperty("systemIdentifier")   final String systemIdentifier
    ) {
        _format = format;
        _copyright = copyright;
        _notes = Arrays.copyOf(notes, notes.length);
        _version = version;
        _versionString = String.format("%d.%d.%d.%d",
                                       _version._major,
                                       _version._minor,
                                       _version._patch,
                                       _version._build);
        _systemIdentifier = systemIdentifier;
    }
}
