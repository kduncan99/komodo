/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

/**
 * Describes a ring/domain combination.
 * Specific-user override of AccessInfo.
 */
public class AccessLock extends AccessInfo {

    public AccessLock() { super(); }
    public AccessLock(final int domain, final short ring) { super(domain, ring); }
    public AccessLock(final long value) { super(value); }

    /**
     * Given the special and general permissions corresponding to some entity,
     * we choose the one or the other based on a comparison of the given key to this lock.
     */
    public AccessPermissions getEffectivePermissions(
        final AccessKey key,
        final AccessPermissions generalPermissions,
        final AccessPermissions specialPermissions
    ) {
        if (key.isMasterKey()) {
            return AccessPermissions.ALL;
        } else if (key.getRing() < getRing() || (key.getDomain() == getDomain())) {
            return specialPermissions;
        } else {
            return generalPermissions;
        }
    }
}

