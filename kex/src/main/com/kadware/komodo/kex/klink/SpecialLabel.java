/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.klink;

enum SpecialLabel {
    BDI("BDI$", 0),
    BDIREF("BDIREF$", 1),
    FIRST("FIRST$", 0),
    LBDI("LBDI$", 0),
    LBDIREF("LBDIREF$", 1);

    final String _text;
    final int _parameterCount;

    SpecialLabel(
        String text,
        int parameterCount
    ) {
        _text = text;
        _parameterCount = parameterCount;
    }

    static SpecialLabel getFrom(
        final String text
    ) {
        return switch (text) {
            case "BDI$" -> BDI;
            case "BDIREF$" -> BDIREF;
            case "FIRST$" -> FIRST;
            case "LBDI$" -> LBDI;
            case "LBDIREF$" -> LBDIREF;
            default -> null;
        };
    }
}
