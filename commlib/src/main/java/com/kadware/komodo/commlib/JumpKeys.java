/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;

/**
 * Object describing or requesting a change in the current jump key settings for a SystemProcessor.
 */
public class JumpKeys {
    //  36-bit composite value, wrapped in a long.
    @JsonProperty("CompositeValue") public Long _compositeValue;

    //  Individual values, per bit.
    //  Key is the jump key identifier from 1 to 36
    //  Value is true if jk is set (or to be set), false if jk is clear (or to be clear).
    //  For PUT an unspecified jk means the value is left as-is.
    //  For GET values for all jump keys are returned.
    @JsonProperty("ComponentValues") public HashMap<Integer, Boolean> _componentValues;
}
