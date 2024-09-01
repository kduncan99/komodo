/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

public class Parser {

    public static class NotFoundException extends Exception {

        public NotFoundException() {
            super();
        }

        public NotFoundException(final String message) {
            super(message);
        }
    }

    public static class SyntaxException extends Exception {

        public SyntaxException() {
            super();
        }

        public SyntaxException(final String message) {
            super(message);
        }
    }

    private final String _text;
    private int _index;
    private Set<Character> _terminators = Collections.singleton(' ');

    private static boolean charInSequence(final char ch, final CharSequence seq) {
        return IntStream.range(0, seq.length()).anyMatch(sx -> ch == seq.charAt(sx));
    }

    public Parser(final String text) {
        _text = text;
        _index = 0;
    }

    public Collection<Character> getTerminators() { return _terminators; }
    public int getIndex() { return _index; }
    public String getText() { return _text; }

    public Parser setIndex(final int value) { _index = value; return this; }
    public Parser setTerminators(Collection<Character> terminators) {
        _terminators = new HashSet<>(terminators);
        return this;
    }
    public Parser setTerminators(final String terminators) {
        _terminators = new HashSet<>();
        for (var ch : terminators.toCharArray()) {
            _terminators.add(ch);
        }
        return this;
    }

    public static boolean isIdentifierCharacter(final char ch) {
        return Character.isAlphabetic(ch) || Character.isDigit(ch);
    }

    public boolean atEnd() {
        return _index >= _text.length();
    }

    public char next() {
        return atEnd() ? 0 : _text.charAt(_index++);
    }

    public boolean parseChar(final char ch) {
        if (!atEnd() && _text.charAt(_index) == ch) {
            _index++;
            return true;
        } else {
            return false;
        }
    }

    public String parseIdentifier(
        final int maxChars,
        final String cutSet
    ) throws NotFoundException, SyntaxException {
        if (!Character.isAlphabetic(peekNext())) {
            throw new NotFoundException();
        }

        var sb = new StringBuilder();
        sb.append(next());
        while (!atEnd()) {
            var ch = peekNext();
            if (charInSequence(ch, cutSet)) {
                break;
            } else if (!isIdentifierCharacter(ch)) {
                throw new SyntaxException();
            }
            sb.append(next());
        }

        if (sb.isEmpty()) {
            throw new NotFoundException();
        } else if (sb.length() > maxChars) {
            throw new SyntaxException();
        }

        return sb.toString();
    }

    public String parseRemaining() {
        var str = _text.substring(_index);
        _index = _text.length();
        return str;
    }

    public String parseUntil(final String cutSet) {
        var sb = new StringBuilder();
        while (!atEnd() && !charInSequence(peekNext(), cutSet)) {
            sb.append(next());
        }
        return sb.toString();
    }

    public char peekNext() {
        return atEnd() ? 0 : _text.charAt(_index);
    }

    public char parseOneOf(final String chars) {
        if (!atEnd() && charInSequence(peekNext(), chars)) {
            return next();
        } else {
            return 0;
        }
    }

    public boolean parseToken(final String token) {
        if (_text.substring(_index).startsWith(token)) {
            _index += token.length();
            return true;
        } else {
            return false;
        }
    }

    public long parseUnsignedDecimalInteger() throws NotFoundException, SyntaxException {
        long value = 0;
        boolean found = false;
        while (!atEnd()) {
            var ch = _text.charAt(_index);
            if (_terminators.contains(ch)) {
                break;
            } else if ((ch < '0') || (ch > '9')) {
                if (found) {
                    throw new SyntaxException();
                } else {
                    throw new NotFoundException();
                }
            } else {
                value = value * 10 + (ch - '0');
                _index++;
                found = true;
            }
        }

        if (!found) {
            throw new NotFoundException();
        }

        return value;
    }

    public long parseUnsignedHexInteger() throws NotFoundException, SyntaxException {
        if (!parseToken("0x") && !parseToken("0X")) {
            throw new NotFoundException();
        }

        long value = 0;
        boolean found = false;
        while (!atEnd()) {
            var ch = _text.charAt(_index);
            if (_terminators.contains(ch)) {
                break;
            } else if ((ch >= '0') && (ch <= '9')) {
                value = value * 16 + (ch - '0');
                _index++;
                found = true;
            } else if ((ch >= 'A') && (ch <= 'F')) {
                value = value * 16 + (ch - 'A' + 10);
                _index++;
                found = true;
            } else if ((ch >= 'a') && (ch <= 'f')) {
                value = value * 16 + (ch - 'a' + 10);
                _index++;
                found = true;
            } else {
                throw new SyntaxException();
            }
        }

        if (!found) {
            throw new NotFoundException();
        }

        return value;
    }

    public long parseUnsignedOctalInteger() throws NotFoundException, SyntaxException {
        if (atEnd() || _text.charAt(_index) != '0') {
            throw new NotFoundException();
        }

        long value = 0;
        while (!atEnd()) {
            var ch = _text.charAt(_index);
            if (_terminators.contains(ch)) {
                break;
            } else if ((ch < '0') || (ch > '8')) {
                throw new SyntaxException();
            }
            value = value * 8 + (ch - '0');
            _index++;
        }

        return value;
    }

    public long parseUnsignedInteger() throws NotFoundException, SyntaxException {
        try {
            return parseUnsignedHexInteger();
        } catch (NotFoundException ex) {
            // keep trying
        }

        try {
            return parseUnsignedOctalInteger();
        } catch (NotFoundException ex) {
            // keep trying
        }

        return parseUnsignedDecimalInteger();
    }

    public void rewind() {
        _index = 0;
    }

    public void skipNext() {
        if (!atEnd()) {
            _index++;
        }
    }

    public void skipSpaces() {
        while (!atEnd() && _text.charAt(_index) == ' ') {
            _index++;
        }
    }
}
