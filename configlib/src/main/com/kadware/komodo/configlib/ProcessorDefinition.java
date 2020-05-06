/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 *
 * Represents the entirety of the hardware configuration
 */

package com.kadware.komodo.configlib;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kadware.komodo.baselib.Credentials;
import com.kadware.komodo.hardwarelib.Processor;

public class ProcessorDefinition extends Configuration {

    @JsonProperty("nodeName")               public final String _nodeName;
    @JsonProperty("processorType")          public final Processor.ProcessorType _processorType;
    //  For MainStorageProcessor
    @JsonProperty("fixedStorageSize")       public Integer _fixedStorageSize;
    //  For SystemProcessor
    @JsonProperty("credentials")            public Credentials _adminCredentials;
    @JsonProperty("httpPort")               public final Integer _httpPort;
    @JsonProperty("httpsPort")              public final Integer _httpsPort;

    @JsonCreator
    public ProcessorDefinition(
        @JsonProperty("nodeName")           final String nodeName,
        @JsonProperty("processorType")      final Processor.ProcessorType processorType,
        @JsonProperty("fixedStorageSize")   final Integer fixedStorageSize,
        @JsonProperty("credentials")        final Credentials credentials,
        @JsonProperty("httpPort")           final Integer httpPort,
        @JsonProperty("httpsPort")          final Integer httpsPort
    ) {
        _nodeName = nodeName;
        _processorType = processorType;
        _fixedStorageSize = fixedStorageSize;
        _adminCredentials = credentials;
        _httpPort = httpPort;
        _httpsPort = httpsPort;
    }
}
