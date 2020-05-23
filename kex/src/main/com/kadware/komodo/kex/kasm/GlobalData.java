/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.kex.kasm.diagnostics.Diagnostics;
import com.kadware.komodo.kex.kasm.dictionary.Dictionary;
import com.kadware.komodo.kex.kasm.dictionary.SystemDictionary;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Entities which are not local to a particular assembler level
 */
class GlobalData {

    final Diagnostics _diagnostics = new Diagnostics();
    final Dictionary _globalDictionary = new Dictionary(new SystemDictionary());
    final Set<AssemblerOption> _options;
    final PrintStream _outputStream;

    boolean _arithmeticFaultCompatibilityMode = false;
    boolean _arithmeticFaultNonInterruptMode = false;
    boolean _quarterWordMode = false;
    boolean _thirdWordMode = false;

    ProgramStart _programStart = null;

    //  Map of LC indices to the various GeneratedPool objects...
    //  Keyed by location counter index.
    final Map<Integer, GeneratedPool> _generatedPools = new TreeMap<>();

    GlobalData(
        Set<AssemblerOption> options,
        PrintStream outputStream
    ) {
        _options = options;
        _outputStream = outputStream;
    }
}
