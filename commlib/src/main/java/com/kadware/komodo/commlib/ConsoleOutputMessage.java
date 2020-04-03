/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConsoleOutputMessage {
    @JsonProperty("identifier")             public Long _identifier;    //  internal identifier of this message
    @JsonProperty("pinned")                 public boolean _pinned;     //  true if this message should be prevented from ageing out, scrolling off
    @JsonProperty("text")                   public String[] _text;      //  text of the message
}
