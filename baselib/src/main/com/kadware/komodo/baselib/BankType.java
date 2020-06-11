/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

/**
 * Indicates the type of the bank
 */
public enum BankType {
    ExtendedMode(0),
    BasicMode(1),     //  Requires BD.S == 0
    Gate(2),          //  Requires BD.S == 0
    Indirect(3),      //  Word1:H1 contains L,BDI of the target bank
    //  Only BD.ProcessorType, BD.Disp, BD.G, and BD.L are valid, Requires BD.S == 0
    Queue(4),
    QueueRepository(6);

    public final int _code;

    BankType(int code) { _code = code; }

    public static BankType get(
        final int code
    ) {
        switch (code) {
            case 0:     return ExtendedMode;
            case 1:     return BasicMode;
            case 2:     return Gate;
            case 3:     return Indirect;
            case 4:     return Queue;
            case 6:     return QueueRepository;
        }

        throw new RuntimeException(String.format("Bad code passed to BankType.get:%d", code));
    }
}
