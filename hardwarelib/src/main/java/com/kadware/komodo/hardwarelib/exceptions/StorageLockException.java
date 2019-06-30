/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown when any instruction is ready to access storage, requests locks for all the storage it needs,
 * and at least one of those storage addresses is already under lock (by some other IP, presumably).
 */
public class StorageLockException extends Exception {

    //???? do we still need this?
    public StorageLockException(
    ) {
    }
}
