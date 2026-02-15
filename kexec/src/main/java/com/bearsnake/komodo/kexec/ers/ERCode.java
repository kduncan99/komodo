/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.ers;

import java.util.Arrays;

/**
 * Codes found in ER Programming Reference Appendix A
 */
public enum ERCode {
    ER_COND$(066, new HandleCOND$()),
    ER_SETC$(065, new HandleSETC$());

    public final int _code;
    public final ERHandler _handler;

    ERCode(final int code,
           final ERHandler handler) {
        _code = code;
        _handler = handler;
    }

    public static ERHandler getHandler(final int code) {
        return Arrays.stream(ERCode.values())
                     .filter(er -> er._code == code)
                     .findFirst().map(er -> er._handler)
                     .orElse(null);
    }
}
