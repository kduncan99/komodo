package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;

/**
 * Object describing a certain number of entries from the in-memory hardware system log.
 */
public class SystemProcessorLogs {
    public class Entry {
        @JsonProperty("Identifier")         public Long _identifier;
        @JsonProperty("Timestamp")          public Date _timestamp;
        @JsonProperty("Entity")             public String _entity;
        @JsonProperty("Message")            public String _message;
    }

    //  Identifier of the first requested log entry on request
    //  Identifier of the first log entry in the array of entries on response
    @JsonProperty("FirstEntryIdentifier")   public Long _firstEntryIdentifier;

    @JsonProperty("Entries")                public Entry[] _entries;
}
