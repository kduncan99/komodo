/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 *
 * Represents the entirety of the hardware configuration
 */

package com.kadware.komodo.baselib.configurator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kadware.komodo.baselib.Credentials;
import java.util.Arrays;

public class HardwareConfiguration extends Configuration {

    public static class ProcessorDefinition {
        @JsonProperty("nodeName")                   public final String _nodeName;
        @JsonProperty("systemProcessorDefinition")  public final SystemProcessorDefinition _systemProcessorDefinition;

        @JsonCreator
        public ProcessorDefinition(
            @JsonProperty("nodeName")                   final String nodeName,
            @JsonProperty("systemProcessorDefinition")  final SystemProcessorDefinition systemProcessorDefinition
        ) {
            _nodeName = nodeName;
            _systemProcessorDefinition = systemProcessorDefinition;
        }
    }

    public static class SystemProcessorDefinition {
        @JsonProperty("credentials")            public Credentials _adminCredentials;
        @JsonProperty("httpPort")               public final int _httpPort;
        @JsonProperty("httpsPort")              public final int _httpsPort;

        @JsonCreator
        public SystemProcessorDefinition(
            @JsonProperty("credentials")        final Credentials credentials,
            @JsonProperty("httpPort")           final int httpPort,
            @JsonProperty("httpsPort")          final int httpsPort
        ) {
            _adminCredentials = credentials;
            _httpPort = httpPort;
            _httpsPort = httpsPort;
        }
    }

    @JsonProperty("format")             public final int _format;
    @JsonProperty("copyright")          public final String _copyright;
    @JsonProperty("notes")              public final String[] _notes;
    @JsonProperty("processorDefinitions")       public final ProcessorDefinition[] _processorDefinitions;

    @JsonCreator
    public HardwareConfiguration(
        @JsonProperty("format")             final int format,
        @JsonProperty("copyright")          final String copyright,
        @JsonProperty("notes")              final String[] notes,
        @JsonProperty("processorDefinitions")   final ProcessorDefinition[] processorDefinitions
    ) {
        _format = format;
        _copyright = copyright;
        _notes = Arrays.copyOf(notes, notes.length);
        _processorDefinitions = processorDefinitions;
    }

    public ProcessorDefinition getProcessorDefinition(
        final String nodeName
    ) {
        for (ProcessorDefinition def : _processorDefinitions) {
            if (def._nodeName.equalsIgnoreCase(nodeName)) {
                return def;
            }
        }
        return null;
    }
}
