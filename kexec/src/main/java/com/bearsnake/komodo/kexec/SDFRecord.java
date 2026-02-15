/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import com.bearsnake.komodo.baselib.Word36;

public class SDFRecord {

    private final long _controlWord;
    private long[] _data;

    public SDFRecord(
        final long controlWord,
        final long[] data
    ) {
        _controlWord = controlWord;
        _data = data;
    }

    public int getControlRecordType() {
        return (int) Word36.getS1(_controlWord);
    }

    public long getControlWord() { return _controlWord; }
    public long[] getData() { return _data; }
    public void setData(final long[] value) { _data = value; }

    public boolean isControlRecord() {
        return Word36.isNegative(_controlWord);
    }
}
