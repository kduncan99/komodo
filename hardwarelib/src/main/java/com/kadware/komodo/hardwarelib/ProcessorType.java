/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.exceptions.InternalErrorRuntimeException;

@SuppressWarnings("Duplicates")
public enum ProcessorType {
    SystemProcessor(0),
    InstructionProcessor(1),
    InputOutputProcessor(2),
    MainStorageProcessor(3);

    private final int _code;

    ProcessorType(int code) { _code = code; }

    public int getCode() { return _code; }

    public static ProcessorType getValue(
        final int code
    ) {
        switch (code) {
            case 0:     return SystemProcessor;
            case 1:     return InstructionProcessor;
            case 2:     return InputOutputProcessor;
            case 3:     return MainStorageProcessor;
        }

        throw new InternalErrorRuntimeException("Invalid code for ProcessorType.getValue()");
    }
}
