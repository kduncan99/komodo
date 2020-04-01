/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object describing identification, version, and other stuffs for an SP.
 * For GET to /identifiers, this is returned fully-populated by the SystemProcessor.
 * For PUT to /identifiers, those entities which can be updated, will be.
 */
public class SystemProcessorIdentifiers {
    //  Identifies the server - expected value is "Komodo System Processor Interface"
    @JsonProperty("Identifier") public String _identifier;

    //  User-specified system identifier - expected to be 1 to 12 alphanumeric characters
    //  Updateable
    @JsonProperty("SystemIdentifier") public String _systemIdentifier;

    //  Copyright notice, just because
    @JsonProperty("Copyright") public String _copyright;

    //  Versioning information
    @JsonProperty("MajorVersion") public int _majorVersion;
    @JsonProperty("MinorVersion") public int _minorVersion;
    @JsonProperty("Patch") public int _patch;
    @JsonProperty("BuildNumber") public int _buildNumber;
    @JsonProperty("VersionString") public String _versionString;
}
