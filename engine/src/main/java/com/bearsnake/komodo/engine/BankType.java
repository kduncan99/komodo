/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

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
        return switch (code) {
            case 0 -> ExtendedMode;
            case 1 -> BasicMode;
            case 2 -> Gate;
            case 3 -> Indirect;
            case 4 -> Queue;
            case 6 -> QueueRepository;
            // TODO not sure I like throwing RTX below
            default -> throw new RuntimeException(String.format("Bad code passed to BankType.get:%d", code));
        };
    }
}
