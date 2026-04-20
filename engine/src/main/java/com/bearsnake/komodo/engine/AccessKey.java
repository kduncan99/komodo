/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

/**
 * Describes a ring/domain combination.
 * Specific-user override of AccessInfo.
 */
public class AccessKey extends AccessInfo {
    public AccessKey() { super(); }
    public AccessKey(final AccessInfo source) { super(source); }
    public AccessKey(final short ring, final int domain) { super(ring, domain); }
    public AccessKey(final long value) { super(value); }

    public boolean isMasterKey() {
        return getRing() == 0 && getDomain() == 0;
    }
}

