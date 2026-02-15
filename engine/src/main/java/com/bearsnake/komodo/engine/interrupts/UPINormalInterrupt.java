/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.interrupts;

/**
 * Represents a particular machine interrupt class
 * This interrupt is generated internally when the IP not in the 'cleared state' honors a UPI interrupt.
 */
public class UPINormalInterrupt extends MachineInterrupt {

    public final int _upi;

    /**
     * Constructor
     * @param synchrony Synchrony.Broadcast or Synchrony.Asynchronous
     * @param upi source of the interrupt
     */
    public UPINormalInterrupt(
        final Synchrony synchrony,
        final int upi
    ) {
        super(InterruptClass.UPINormal, ConditionCategory.NonFault, synchrony, Deferrability.Deferrable, InterruptPoint.MidExecution);
        _upi = upi;
    }
}
