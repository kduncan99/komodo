/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 *
 * Represents the entirety of the hardware configuration
 */

package com.kadware.komodo.configlib;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

public class HardwareConfiguration extends Configuration {

    @JsonProperty("format")                     public final int _format;
    @JsonProperty("copyright")                  public final String _copyright;
    @JsonProperty("notes")                      public final String[] _notes;
    @JsonProperty("processorDefinitions")       public final ProcessorDefinition[] _processorDefinitions;

    @JsonCreator
    public HardwareConfiguration(
        @JsonProperty("format")                 final int format,
        @JsonProperty("copyright")              final String copyright,
        @JsonProperty("notes")                  final String[] notes,
        @JsonProperty("processorDefinitions")   final ProcessorDefinition[] processorDefinitions
    ) {
        _format = format;
        _copyright = copyright;
        _notes = Arrays.copyOf(notes, notes.length);
        _processorDefinitions = processorDefinitions;
    }
}
