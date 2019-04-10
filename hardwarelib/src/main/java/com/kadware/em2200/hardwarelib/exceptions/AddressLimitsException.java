/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.exceptions;

import com.kadware.em2200.hardwarelib.misc.AbsoluteAddress;

/**
 * Exception thrown when some entity discovers a specified address is outside of the limits for that address
 */
public class AddressLimitsException extends Exception {

    /**
     * For absolute addresses
     * <p>
     * @param address
     */
    public AddressLimitsException(
        final AbsoluteAddress address
    ) {
        super(String.format("Absolute address offset %o is out of range for MSP with UPI %d",
                            address._offset,
                            address._upi));
    }
}
