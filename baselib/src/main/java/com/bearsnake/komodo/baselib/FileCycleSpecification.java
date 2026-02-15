/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib;

import com.bearsnake.komodo.baselib.exceptions.NotFoundParserException;
import com.bearsnake.komodo.baselib.exceptions.SyntaxErrorParserException;

public final class FileCycleSpecification {

    private final boolean _isAbsolute;
    private final int _cycle;

    private FileCycleSpecification(
        final boolean isAbsolute,
        final int cycle
    ) {
        _isAbsolute = isAbsolute;
        _cycle = cycle;
    }

    public static FileCycleSpecification newAbsoluteSpecification(
        final int cycle
    ) {
        if ((cycle < 1) || (cycle > 9999)) {
            throw new RuntimeException("Invalid absolute cycle");
        }
        return new FileCycleSpecification(true, cycle);
    }

    public static FileCycleSpecification newRelativeSpecification(
        final int cycle
    ) {
        if ((cycle > 1) || (cycle < -31)) {
            throw new RuntimeException("Invalid relative cycle");
        }
        return new FileCycleSpecification(false, cycle);
    }

    public int getCycle() { return _cycle; }
    public boolean isAbsolute() { return _isAbsolute; }
    public boolean isRelative() { return !_isAbsolute; }

    // TODO we need to allow callers (maybe several layers above us) to post
    //   E:242433 Illegal value specified for F-cycle.
    public static FileCycleSpecification parse(
        final Parser parser
    ) throws SyntaxErrorParserException {
        if (!parser.parseChar('(')) {
            return null;
        }

        boolean abs;
        int cyc;
        if (parser.parseChar('+')) {
            if (parser.parseChar('0')) {
                abs = false;
                cyc = 0;
            } else if (parser.parseChar('1')) {
                abs = false;
                cyc = 1;
            } else {
                throw new SyntaxErrorParserException("Invalid relative cycle");
            }
        } else if (parser.parseChar('-')) {
            try {
                abs = false;
                cyc = (int)parser.parseUnsignedDecimalInteger();
                if (cyc > 31) {
                    throw new SyntaxErrorParserException("Invalid relative cycle");
                }
            } catch (NotFoundParserException | SyntaxErrorParserException ex) {
                throw new SyntaxErrorParserException("Invalid relative cycle");
            }
        } else {
            try {
                abs = true;
                var terms = parser.getTerminators();
                parser.setTerminators(")");
                cyc = (int)parser.parseUnsignedDecimalInteger();
                if ((cyc < 1) || (cyc > 999)) {
                    throw new SyntaxErrorParserException("Invalid absolute cycle");
                }
                parser.setTerminators(terms);
            } catch (NotFoundParserException | SyntaxErrorParserException ex) {
                throw new SyntaxErrorParserException("Invalid absolute cycle");
            }
        }

        if (!parser.parseChar(')')) {
            throw new SyntaxErrorParserException("Invalid cycle specification");
        }

        return new FileCycleSpecification(abs, cyc);
    }
}
