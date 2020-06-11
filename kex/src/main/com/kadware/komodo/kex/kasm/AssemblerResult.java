/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.kex.RelocatableModule;
import com.kadware.komodo.kex.kasm.diagnostics.Diagnostics;

/**
 * Resulting diagnostics and the parsed code from a call to assemble()
 */
public class AssemblerResult {
    public final RelocatableModule _relocatableModule;
    public final Diagnostics _diagnostics;
    public final TextLine[] _parsedCode;

    //  For normal returns
    AssemblerResult(
        final RelocatableModule relocatableModule,
        final Diagnostics diagnostics,
        final TextLine[] parsedCode
    ) {
        _relocatableModule = relocatableModule;
        _diagnostics = diagnostics;
        _parsedCode = parsedCode;
    }

    //  For error returns where no module was created
    AssemblerResult(
        final Diagnostics diagnostics,
        final TextLine[] parsedCode
    ) {
        _relocatableModule = null;
        _diagnostics = diagnostics;
        _parsedCode = parsedCode;
    }
}
