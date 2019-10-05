package com.kadware.komodo.commlib;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;

/**
 * Object describing or requesting a change in the current jump key settings for a SystemProcessor.
 * For PUT or POST to /identifiers the following algorithm is employed:
 *      load working value from current settings
 *      overlay working value with the given composite value if provided
 *      set or clear the individual working value bits with any values specified in the component values
 *      update the system value with the final working value
 */
public class SystemProcessorJumpKeys {
    //  36-bit composite value, wrapped in a long.
    //  JK1 is left-most bit of 36, JK36 is right-most bit.
    @JsonProperty("CompositeValue") public Long _compositeValue;

    //  Individual values, per bit.
    //  Key is the integer from 1 to 36 in an ASCII string
    //  Value is true if jk is set (or to be set), false if jk is clear (or to be clear).
    //  For PUT an unspecified jk means the value is left as-is.
    @JsonProperty("ComponentValues") public HashMap<String, Boolean> _componentValues;
}
