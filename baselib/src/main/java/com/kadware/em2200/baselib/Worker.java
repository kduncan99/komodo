/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib;

/**
 * A useful implementation of Runnable which does some nice things for us.
 */
public interface Worker extends Runnable {

    /**
     * Retrieves a unique displayable name for the particular implementor
     * <p>
     * @return
     */
    public String getWorkerName();
}
