/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object encapsulating certain other objects.
 * Client issues a GET on the /poll subdirectory, and we respond with all the updated information.
 * An entity will be null if that entity has not been updated in the interim.
 */
public class SystemProcessorPollResult {

//    @JsonProperty("hardwareConfigurationUpdated")   public boolean _hardwareConfigurationUpdated = false;
//    @JsonProperty("jumpKeysUpdated")                public boolean _jumpKeysUpdated = false;
//    @JsonProperty("newLogEntries")                  public boolean _newLogEntries = false;
//    @JsonProperty("newReadOnlyMessages")            public boolean _newReadOnlyMessages = false;
//    @JsonProperty("systemConfigurationUpdated")     public boolean _systemConfigurationUpdated = false;
//    @JsonProperty("updatedReadReplyMessages")       public boolean _updatedReadReplyMessages = false;
//    @JsonProperty("updatedStatusMessage")           public boolean _updatedStatusMessage = false;

//    public static class Identifiers {
//        //  Identifies the server - expected value is "Komodo System Processor Interface"
//        @JsonProperty("Identifier") public String _identifier;
//
//        //  User-specified system identifier - expected to be 1 to 12 alphanumeric characters
//        //  Updateable
//        @JsonProperty("SystemIdentifier")   public String _systemIdentifier;
//
//        //  Copyright notice, just because
//        @JsonProperty("Copyright")          public String _copyright;
//
//        //  Versioning information
//        @JsonProperty("MajorVersion")       public int _majorVersion;
//        @JsonProperty("MinorVersion")       public int _minorVersion;
//        @JsonProperty("Patch")              public int _patch;
//        @JsonProperty("BuildNumber")        public int _buildNumber;
//        @JsonProperty("VersionString")      public String _versionString;
//    }

    public static class SystemLogEntry {
        @JsonProperty("timestamp")              public Long _timestamp;     //  system milliseconds
        @JsonProperty("entity")                 public String _entity;      //  reporting entity
        @JsonProperty("message")                public String _message;
    }

    public static class StatusMessage {
        @JsonProperty("identifier")             public Long _identifier;
        @JsonProperty("text")                   public String[] _text;
    }

    public static class ReadOnlyMessage {
        @JsonProperty("identifier")             public Long _identifier;
        @JsonProperty("text")                   public String[] _text;
        @JsonProperty("osIdentifier")           public Long _osIdentifier;
    }

    public static class ReadReplyMessage {
        @JsonProperty("identifier")             public Long _identifier;
        @JsonProperty("maxReplySize")           public Integer _maxReplySize;
        @JsonProperty("text")                   public String[] _text;
        @JsonProperty("osIdentifier")           public Long _osIdentifier;
    }

    @JsonProperty("jumpKeySettings")            public Long _jumpKeySettings;
    @JsonProperty("newLogEntries")              public SystemLogEntry[] _newLogEntries;
    @JsonProperty("latestStatusMessage")        public StatusMessage _latestStatusMessage;
    @JsonProperty("newReadOnlyMessages")        public ReadOnlyMessage[] _newReadOnlyMessages;
    @JsonProperty("extantReadReplyMessages")    public ReadReplyMessage[] _extantReadReplyMessages;
    //  TODO hardware config
    //  TODO system config
}
