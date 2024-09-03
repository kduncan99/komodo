/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.kexec.exec.ERIO$Status;

public class IOResult {

    private ERIO$Status _status;
    private int _wordsTransferred;

    public IOResult() {
        _status = ERIO$Status.Success;
        _wordsTransferred = 0;
    }

    public IOResult(
        final ERIO$Status status
    ) {
        _status = status;
        _wordsTransferred = 0;
    }

    public IOResult(
        final ERIO$Status status,
        final int wordsTransferred
    ) {
        _status = status;
        _wordsTransferred = wordsTransferred;
    }

    public void addWordsTransferred(
        final int wordsTransferred
    ) {
        _wordsTransferred += wordsTransferred;
    }

    public void clear() {
        _status = ERIO$Status.Success;
        _wordsTransferred = 0;
    }

    public ERIO$Status getStatus() {
        return _status;
    }

    public int getWordsTransferred() {
        return _wordsTransferred;
    }

    public IOResult setStatus(final ERIO$Status status) {
        _status = status;
        return this;
    }

    public IOResult setWordsTransferred(final int wordsTransferred) {
        _wordsTransferred = wordsTransferred;
        return this;
    }

    @Override
    public String toString() {
        return String.format("%s wds:%d", _status, _wordsTransferred);
    }
}
