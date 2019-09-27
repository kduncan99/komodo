package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object encapsulating certain other objercts.
 * Client issues a GET on the /poll subdirectory, and we respond with anything that has changed since the last GET.
 * Any encapsulated entities which have not changed will be null.  If all items are null, nothing has changed.
 * A GET may be held for a finite period of time, pending the availability of content.
 */
public class SystemProcessorPoll {

    public static class Identifiers {
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

    public static class HardwareLogEntry {
        @JsonProperty("Timestamp")          public Long _timestamp;     //  system milliseconds
        @JsonProperty("Entity")             public String _entity;      //  reporting entity
        @JsonProperty("Message")            public String _message;
    }

    public static class StatusMessage {
        @JsonProperty("Message1")           public String _message1;
        @JsonProperty("Message2")           public String _message2;
    }

    public static class ReadOnlyMessage {
        @JsonProperty("Message")            public String _message;
    }

    public static class ReadReplyMessage {
        @JsonProperty("Identifier")         public Integer _identifier;
        @JsonProperty("Message")            public String _message;
        @JsonProperty("MaxReplySize")       public Integer _maxReplySize;
    }

    @JsonProperty("JumpKeys")               public Long _jumpKeys;
    @JsonProperty("Identifiers")            public Identifiers _identifiers;
    @JsonProperty("LogEntries")             public HardwareLogEntry[] _logEntries;
    @JsonProperty("StatusMessage")          public StatusMessage _statusMessage;
    @JsonProperty("ReadOnlyMessages")       public ReadOnlyMessage[] _readOnlyMessages;
    @JsonProperty("ReadReplyMessages")      public ReadReplyMessage[] _readReplyMessages;
}
