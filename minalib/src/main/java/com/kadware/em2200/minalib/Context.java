/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.minalib.dictionary.*;

/**
 * Represents the current context under which an assembly is being performed
 */
public class Context {

    private CharacterMode _characterMode = CharacterMode.ASCII;
    private CodeMode _codeMode = CodeMode.Extended;
    private final Dictionary _dictionary;

    /**
     * General constructor
     */
    public Context(
    ) {
        _dictionary = new MainLevelDictionary(new Dictionary());
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public CharacterMode getCharacterMode(
    ) {
        return _characterMode;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public CodeMode getCodeMode(
    ) {
        return _codeMode;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public Dictionary getDictionary(
    ) {
        return _dictionary;
    }

    /**
     * Setter
     * <p>
     * @param mode
     */
    public void setCharacterMode(
        final CharacterMode mode
    ) {
        _characterMode = mode;
    }

    /**
     * Setter
     * <p>
     * @param mode
     */
    public void setCodeMode(
        final CodeMode mode
    ) {
        _codeMode = mode;
    }
}
