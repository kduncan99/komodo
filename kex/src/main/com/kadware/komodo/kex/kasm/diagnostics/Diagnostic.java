/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.diagnostics;

import com.kadware.komodo.kex.kasm.Locale;

/**
 * Base class for diagnostic messages
 */
public abstract class Diagnostic {

    public enum Level {
        Addressability,
        BaseRegister,
        Directive,
        Duplicate,
        Error,
        Fatal,
        Form,   //  We invented this one
        Go,
        Level,
        Quote,
        Relocation,
        Truncation,
        UndefinedReference,
        Value,
        Warning,
    }

    public final Locale _locale;
    public final String _message;

    /**
     * Constructor - when some error occurs which is not related to a particular line of source code
     * @param message message associated with this diagnostic
     */
    public Diagnostic(
        final String message
    ) {
        _locale = null;
        _message = message;
    }

    /**
     * Constructor
     * @param locale locale associated with this diagnostic
     * @param message message associated with this diagnostic
     */
    public Diagnostic(
        final Locale locale,
        final String message
    ) {
        _locale = locale;
        _message = message;
    }

    /**
     * Get the level associated with this instance
     * @return value
     */
    public abstract Level getLevel(
    );

    /**
     * Private getter, retrieve severity level as a displayable character
     * @return value
     */
    public final char getLevelIndicator(
    ) {
        return getLevelIndicator(getLevel());
    }

    /**
     * Retrieves the single-character level indicator for this particular Diagnostic Level
     * @param level level of diagnostic
     * @return character value
     */
    public static char getLevelIndicator(
        final Level level
    ) {
        return switch (level) {
            case Addressability -> 'A';
            case BaseRegister -> 'B';
            case Directive -> 'I';
            case Duplicate -> 'D';
            case Error -> 'E';
            case Fatal -> 'F';
            case Form -> 'M';
            case Go -> 'G';
            case Level -> 'L';
            case Quote -> 'Q';
            case Relocation -> 'R';
            case Truncation -> 'T';
            case UndefinedReference -> 'U';
            case Value -> 'V';
            case Warning -> 'W';
        };
    }

    /**
     * Construct and return a message to be displayed, describing this diagnostic
     * @return value
     */
    public final String getMessage() {
        return String.format("%c%s:%s",
                             getLevelIndicator(),
                             _locale == null ? "" : _locale.toString().replace(" ",""),
                             _message);
    }

    public abstract boolean isError();
}
