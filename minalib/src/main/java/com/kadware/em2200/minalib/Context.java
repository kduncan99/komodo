/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.dictionary.*;

/**
 * Represents the current context under which an assembly is being performed
 */
public class Context {

    public CharacterMode _characterMode = CharacterMode.ASCII;
    public CodeMode _codeMode = CodeMode.Extended;
    public int _currentGenerationLCIndex = 1;
    public int _currentLitLCIndex = 0;
    public Integer _defaultBaseRegister = null;
    public final Dictionary _dictionary;

    /**
     * General constructor
     */
    public Context(
        final Dictionary upperLevelDictionary
    ) {
        _dictionary = new Dictionary( upperLevelDictionary );
    }
}
