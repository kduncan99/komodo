/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.exec.RunControlEntry;
import com.bearsnake.komodo.kexec.facilities.FacStatusCode;
import com.bearsnake.komodo.logger.LogManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Interpreter {

    static final String LOG_SOURCE = "CSI";

    private static final Map<String, Handler> HANDLERS = new HashMap<>();
    static {
        // ADD
        // ASG
        // BRKPT
        // CAT
        // CKPT
        // FREE
        HANDLERS.put("LOG", new LogHandler());
        // MODE
        HANDLERS.put("QUAL", new QualHandler());
        // RSTRT
        // START
        // SYM
        // SYMCN
        // USE
    }

    public void handleControlStatement(
        final RunControlEntry runControlEntry,
        final StatementSource source,
        final ParsedStatement statement
    ) {
        LogManager.logTrace(LOG_SOURCE, "HandleControlStatement [%s]", runControlEntry.getRunId());
        var hp = new HandlerPacket(runControlEntry, source, statement);
        var handler = HANDLERS.get(statement._mnemonic);
        if (handler == null) {
            LogManager.logWarning(LOG_SOURCE,
                                  "[%s] invalid control statement:%s",
                                  runControlEntry.getRunId(),
                                  statement._originalStatement);
            runControlEntry.postContingency(012, 04, 040);
            statement._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage, null);
            statement._resultCode = 0_400000_000000L;
        } else if ((source == StatementSource.ER_CSI) && !handler.allowCSI()) {
            LogManager.logWarning(LOG_SOURCE,
                                  "[%s] %s not allowed for ER CSI$",
                                  runControlEntry.getRunId(),
                                  statement._mnemonic);
            runControlEntry.postContingency(012, 04, 042);
            statement._facStatusResult.postMessage(FacStatusCode.IllegalControlStatement, null);
            statement._resultCode = 0_400000_000000L;
        } else {
            handler.handle(hp);
        }

        LogManager.logTrace(LOG_SOURCE,
                            "HandleControlStatement [%s] returning code %012o",
                            runControlEntry.getRunId(),
                            statement._resultCode);
    }

    /**
     * ParseControlStatement takes raw text input and splits it into its particular parts with little regard
     * as to the semantics of any particular portion.
     * Format:
     *      '@' [wsp] [ label ':' ]
     *          [wsp] mnemonic [',' [wsp] option_field]
     *          wsp field [',' [wsp] field ]...
     *          [' ' '.' ' ' command]
     * or:
     *      '@' [wsp] ''' [wsp] [ label ':' ]
     *                    [wsp] mnemonic [',' [wsp] option_field]
     *                    wsp arbitrary-string
     *
     * option_field:
     *      subfield [ '/' [wsp] subfield ]...
     * field:
     *      subfield [ '/' [wsp] subfield ]...
     * wsp is whitespace, and is usually optional
     *
     * We do not handle continuation characters here - that must be dealt with at a higher level,
     * with the fully-composed multi-line statement passed to us as a single string.
     * We do not check image length here - that must be dealt with at a higher level.
     * @return ParseStatement object
     */
    public static ParsedStatement parseControlStatement(
        final RunControlEntry runControlEntry,
        final String statement
    ) {
        LogManager.logTrace(LOG_SOURCE, "parseControlStatement [%s] %s", runControlEntry.getRunId(), statement);

        var ps = new ParsedStatement();
        ps._originalStatement = statement;

        // trim statement down to a working string
        var working = (statement + " ");
        var commentIndex = working.indexOf(" . ");
        if (commentIndex >= 0) {
            working = working.substring(0, commentIndex);
        }

        // start parsing - look for masterspace
        var p = new Parser(working);
        if (p.atEnd() || !p.parseChar('@')) {
            LogManager.logWarning(LOG_SOURCE, "[%s] statement does not begin with '@'", runControlEntry.getRunId());
            runControlEntry.postContingency(012, 04, 040);

            ps._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage, null);
            ps._resultCode = 0_400000_000000L;
            LogManager.logTrace(LOG_SOURCE, "parseControlStatement stat=%012o", ps._resultCode);
            return ps;
        }

        //TODO look for a tic-mark to signify un-formatted processor call

        // get an identifier -- it is either a label or a command.
        p.skipSpaces();
        String ident = null;
        try {
            ident = p.parseIdentifier(6, ":, ");
        } catch (Parser.SyntaxException ex) {
            ps._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage, null);
            ps._resultCode = 0_400000_000000L;
            LogManager.logTrace(LOG_SOURCE, "parseControlStatement stat=%012o", ps._resultCode);
            return ps;
        } catch (Parser.NotFoundException ex) {
            // we have neither a label nor a command.
            // the only other thing we can accept here is trailing whitespace.
            p.skipSpaces();
            if (!p.atEnd()) {
                ps._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage, null);
                ps._resultCode = 0_400000_000000L;
                LogManager.logTrace(LOG_SOURCE, "parseControlStatement stat=%012o", ps._resultCode);
            } else {
                LogManager.logTrace(LOG_SOURCE, "empty statement");
            }
            return ps;
        }

        // If the next character is a colon, then the identifier is a label.
        // If so, then store the label and read another identifier - which will be a mnemonic
        // (or nothing, if we have an empty labeled statement).
        var isLabel = p.parseChar(':');
        if (isLabel) {
            ps._label = ident.toUpperCase();
            p.skipSpaces();
            try {
                ident = p.parseIdentifier(6, ":, ");
            } catch (Parser.SyntaxException ex) {
                ps._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage, null);
                ps._resultCode = 0_400000_000000L;
                LogManager.logTrace(LOG_SOURCE, "parseControlStatement stat=%012o", ps._resultCode);
                return ps;
            } catch (Parser.NotFoundException ex) {
                if (!p.atEnd()) {
                    ps._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage, null);
                    ps._resultCode = 0_400000_000000L;
                    LogManager.logTrace(LOG_SOURCE, "parseControlStatement stat=%012o", ps._resultCode);
                } else {
                    LogManager.logTrace(LOG_SOURCE, "empty statement");
                }
                return ps;
            }
        }
        ps._mnemonic = ident.toUpperCase();

        // Do we have subfields? (at this point, they'd be option subfields...)
        if (p.parseChar(',')) {
            p.skipSpaces();
            while (!p.atEnd()) {
                var sub = p.parseUntil(" /,");
                ps._optionsFields.add(sub);
                if (p.atEnd() || p.peekNext() == ' ') {
                    break;
                } else if (p.peekNext() == ',') {
                    ps._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage, null);
                    ps._resultCode = 0_400000_000000L;
                    LogManager.logTrace(LOG_SOURCE, "parseControlStatement stat=%012o", ps._resultCode);
                    return ps;
                } else {
                    p.skipNext();
                    p.skipSpaces();
                }
            }
        }

        // Now do the operand fields - @LOG gets special treatment
        p.skipSpaces();
        if (ps._mnemonic.equals("LOG")) {
            var str = p.parseRemaining();
            ps._operandFields.add(Collections.singletonList(str));
            LogManager.logTrace(LOG_SOURCE, "parseControlStatement exit");
            return ps;
        }

        // Anyone needing a file name with potential read/write keys gets special treatment...
        // in that we do not split the first operand field on forward slashes.
        var cutSet = (ps._mnemonic.equals("ASG") || ps._mnemonic.equals("CAT")) ? " ," : " /,";
        var opx = 0;
        var opy = 0;
        while (!p.atEnd()) {
            var sub = p.parseUntil(cutSet);
            while (ps._operandFields.size() <= opx) {
                ps._operandFields.add(new LinkedList<>());
            }
            while (ps._operandFields.get(opx).size() < opy) {
                ps._operandFields.get(opx).add("");
            }
            ps._operandFields.get(opx).add(sub);

            if (p.atEnd() || p.peekNext() == ' ') {
                break;
            } else if (p.peekNext() == ',') {
                opx++;
                opy = 0;
                cutSet = " /,";
                p.skipNext();
                p.skipSpaces();
            } else {
                opy++;
                p.skipNext();
                p.skipSpaces();
            }
        }

        LogManager.logTrace(LOG_SOURCE, "ParseControlStatement exit");
        return ps;
    }
}
