/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

import java.util.Collections;
import java.util.Set;

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

    public Parser(final String text) {
        _text = text;
        _index = 0;
    }

    public void setTerminators(final String terminators) {
        _terminators.clear();
        for (var ch : terminators.toCharArray()) {
            _terminators.add(ch);
        }
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

    public long parseUnsignedDecimalInteger() throws NotFoundException, SyntaxException {
        long value = 0;
        boolean found = false;
        while (!atEnd()) {
            var ch = _text.charAt(_index);
            if (_terminators.contains(ch)) {
                break;
            } else if ((ch < '0') || (ch > '9')) {
                throw new SyntaxException();
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
        if (!_text.substring(_index).startsWith("0x")
            && !_text.substring(_index).startsWith("0X")) {
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
            throw new SyntaxException();
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

    public long parseInteger() throws NotFoundException, SyntaxException {
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

    public char peekNext() {
        return atEnd() ? 0 : _text.charAt(_index);
    }

    public char parseOneOf(final String chars) {
        if (!atEnd() && chars.contains(_text.substring(_index, _index + 1))) {
            return _text.charAt(_index++);
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

    public void rewind() {
        _index = 0;
    }

    public void skipSpaces() {
        while (!atEnd() && _text.charAt(_index) == ' ') {
            _index++;
        }
    }
}
