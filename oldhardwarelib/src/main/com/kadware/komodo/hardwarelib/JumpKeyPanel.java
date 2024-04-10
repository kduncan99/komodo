package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.Word36;

/**
 * A JumpKey is historically an operator-accessible hardware switch which software (either the operating system or an application)
 * can poll, and branch based on the switch setting. The settings are binary - set or cleared (on or off).
 * A panel is nothing more than a set of such switches.
 *
 * In the architecture we are implementing, there are 36 such switches, aligning with the architectural word size.
 * In our implementation, the panel is purely virtual, and is represented by exactly one instantiated object.
 * Which object, and where within the overall implementation, is undefined - excepting that whichever class is
 * represented by that object, must implement this interface. Consumers of this panel - i.e., any clients which ultimately
 * need to set, clear, or poll these virtual switches - should expect to invoke these methods on a reference to this
 * interface.
 *
 * Jump keys are identified by the consecutive numbers 1 to 36.
 * Jump key 1 is in the MSBit of 36 bits, Jum pkey 36 is in the LSbit.
 */
public interface JumpKeyPanel {

    boolean getJumpKey(final int jumpKeyId);
    Word36 getJumpKeys();
    void setJumpKey(final int jumpKeyId, final boolean value);
    void setJumpKeys(final Word36 value);
}
