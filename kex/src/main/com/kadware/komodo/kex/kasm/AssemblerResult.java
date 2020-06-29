/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.kex.RelocatableModule;
import com.kadware.komodo.kex.kasm.diagnostics.Diagnostics;
import com.kadware.komodo.kex.kasm.dictionary.Dictionary;

/**
 * Resulting diagnostics and the parsed code from a call to assemble()
 */
public class AssemblerResult {
    public final Dictionary _definitions;                   //  for definition mode assemblies
    public final RelocatableModule _relocatableModule;      //  for generation mode assemblies
    public final Diagnostics _diagnostics;
    public final TextLine[] _parsedCode;

    //  For successful generation mode assemblies
    AssemblerResult(
        final RelocatableModule relocatableModule,
        final Diagnostics diagnostics,
        final TextLine[] parsedCode
    ) {
        _definitions = null;
        _relocatableModule = relocatableModule;
        _diagnostics = diagnostics;
        _parsedCode = parsedCode;
    }

    //  For successful definition mode assemblies
    AssemblerResult(
        final Dictionary definitions,
        final Diagnostics diagnostics,
        final TextLine[] parsedCode
    ) {
        _definitions = definitions;
        _relocatableModule = null;
        _diagnostics = diagnostics;
        _parsedCode = parsedCode;
    }

    //  For fatal error returns where neither module nor dictionary was created
    AssemblerResult(
        final Diagnostics diagnostics,
        final TextLine[] parsedCode
    ) {
        _definitions = null;
        _relocatableModule = null;
        _diagnostics = diagnostics;
        _parsedCode = parsedCode;
    }
}
