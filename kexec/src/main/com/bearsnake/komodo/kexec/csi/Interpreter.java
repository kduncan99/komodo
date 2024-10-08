/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.scheduleManager.Run;
import com.bearsnake.komodo.kexec.scheduleManager.TIPRun;
import com.bearsnake.komodo.kexec.facilities.FacStatusCode;
import com.bearsnake.komodo.kexec.facilities.FacStatusResult;
import com.bearsnake.komodo.logger.LogManager;
import java.util.HashMap;
import java.util.Map;

// Control Statement Interpreter main routine.
// Calling code should first invoke parseControlStatement(), and if that works out, should subsequently
// invoke handleControlStatement().
// This code does not handle certain run-oriented statements such as
//   @RUN, @FIN, @PASSWD, @TEST, @JUMP, @FILE, @ENDF, maybe others
//   It also does not handle processor calls, including FURPUR calls.
public class Interpreter {

    static final String LOG_SOURCE = "CSI";

    private static final Map<String, Handler> HANDLERS = new HashMap<>();
    static {
        addHandler(new AddHandler());
        addHandler(new AsgHandler());
        addHandler(new BrkptHandler());
        addHandler(new CatHandler());
        addHandler(new CatHandler());
        // END - should this be done here?
        // ENDX - should this be done here?
        // EOF - should this be done here?
        addHandler(new FreeHandler());
        addHandler(new HdgHandler());
        addHandler(new LogHandler());
        addHandler(new ModeHandler());
        addHandler(new MsgHandler());
        addHandler(new QualHandler());
        addHandler(new RstrtHandler());
        addHandler(new SetcHandler());
        addHandler(new StartHandler());
        addHandler(new SymHandler());
        addHandler(new SymcnHandler());
        addHandler(new UseHandler());
        addHandler(new XqtHandler());
    }

    private static void addHandler(final Handler handler) {
        HANDLERS.put(handler.getCommand(), handler);
    }

    public void handleControlStatement(
        final Run run,
        final StatementSource source,
        final ParsedStatement statement
    ) throws ExecStoppedException {
        LogManager.logTrace(LOG_SOURCE, "HandleControlStatement [%s]", run.getActualRunId());
        var hp = new HandlerPacket(run, source, statement);
        var handler = HANDLERS.get(statement._mnemonic);
        if (handler == null) {
            LogManager.logWarning(LOG_SOURCE,
                                  "[%s] invalid control statement:%s",
                                  run.getActualRunId(),
                                  statement._originalStatement);
            run.postContingency(012, 04, 040);
            statement._facStatusResult.postMessage(FacStatusCode.SyntaxErrorInImage);
            hp._statement._facStatusResult.mergeStatusBits(0_400000_000000L);
        } else if ((source == StatementSource.ER_CSI) && !handler.allowCSI()) {
            LogManager.logWarning(LOG_SOURCE,
                                  "[%s] %s not allowed for ER CSI$",
                                  run.getActualRunId(),
                                  statement._mnemonic);
            run.postContingency(012, 04, 042);
            statement._facStatusResult.postMessage(FacStatusCode.IllegalControlStatement);
            hp._statement._facStatusResult.mergeStatusBits(0_400000_000000L);
        } else if ((source == StatementSource.ER_CSF) && !handler.allowCSF()) {
            LogManager.logWarning(LOG_SOURCE,
                                  "[%s] %s not allowed for ER CSF$",
                                  run.getActualRunId(),
                                  statement._mnemonic);
            run.postContingency(012, 04, 042);
            statement._facStatusResult.postMessage(FacStatusCode.IllegalControlStatement);
            hp._statement._facStatusResult.mergeStatusBits(0_400000_000000L);
        } else if ((run instanceof TIPRun) && !handler.allowTIP()) {
            LogManager.logWarning(LOG_SOURCE,
                                  "[%s] %s not allowed in TIP program",
                                  run.getActualRunId(),
                                  statement._mnemonic);
            run.postContingency(012, 04, 042);
            statement._facStatusResult.postMessage(FacStatusCode.IllegalControlStatement);
            hp._statement._facStatusResult.mergeStatusBits(0_400000_000000L);
        } else {
            handler.handle(hp);
        }

        LogManager.logTrace(LOG_SOURCE,
                            "HandleControlStatement [%s] returning code %012o",
                            run.getActualRunId(),
                            statement._facStatusResult.getStatusWord());
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
     * option_field:
     *      subfield [ '/' [wsp] subfield ]...
     * field:
     *      subfield [ '/' [wsp] subfield ]...
     * wsp is whitespace, and is usually optional
     * We do not handle continuation characters here - that must be dealt with at a higher level,
     * with the fully-composed multi-line statement passed to us as a single string.
     * We do not check image length here - that must be dealt with at a higher level.
     * @return ParseStatement object
     */
    public static ParsedStatement parseControlStatement(
        final Run run,
        final String statement
    ) {
        LogManager.logTrace(LOG_SOURCE, "parseControlStatement [%s] %s", run.getActualRunId(), statement);

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
            LogManager.logWarning(LOG_SOURCE, "[%s] statement does not begin with '@'", run.getActualRunId());
            run.postContingency(012, 04, 040);
            postSyntaxError(ps._facStatusResult);
            return ps;
        }

        //TODO look for a tic-mark to signify un-formatted processor call

        // get an identifier -- it is either a label or a command.
        p.skipSpaces();
        String ident = null;
        try {
            ident = p.parseIdentifier(6, ":, ");
        } catch (Parser.SyntaxException ex) {
            postSyntaxError(ps._facStatusResult);
            return ps;
        } catch (Parser.NotFoundException ex) {
            // we have neither a label nor a command.
            // the only other thing we can accept here is trailing whitespace.
            p.skipSpaces();
            if (!p.atEnd()) {
                postSyntaxError(ps._facStatusResult);
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
                postSyntaxError(ps._facStatusResult);
                return ps;
            } catch (Parser.NotFoundException ex) {
                if (!p.atEnd()) {
                    postSyntaxError(ps._facStatusResult);
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
                    postSyntaxError(ps._facStatusResult);
                    return ps;
                } else {
                    p.skipNext();
                    p.skipSpaces();
                }
            }
        }

        // Now do the operand fields - @LOG and @MSG get special treatment
        p.skipSpaces();
        if (ps._mnemonic.equals("LOG") || ps._mnemonic.equals("MSG")) {
            var str = p.parseRemaining();
            ps._operandFields.put(new SubfieldSpecifier(0, 0), str);
            LogManager.logTrace(LOG_SOURCE, "parseControlStatement exit");
            return ps;
        }

        // Anyone needing a file name with potential read/write keys gets special treatment...
        // in that we do not split the first operand field on forward slashes.
        var cutSet = (ps._mnemonic.equals("ASG") || ps._mnemonic.equals("CAT")) ? " ," : " /,";
        var currentFieldIndex = 0;
        var currentSubfieldIndex = 0;
        while (!p.atEnd()) {
            var sub = p.parseUntil(cutSet);
            ps._operandFields.put(new SubfieldSpecifier(currentFieldIndex, currentSubfieldIndex), sub);

            if (p.atEnd() || p.peekNext() == ' ') {
                break;
            } else if (p.peekNext() == ',') {
                currentFieldIndex++;
                currentSubfieldIndex = 0;
                cutSet = " /,";
                p.skipNext();
                p.skipSpaces();
            } else {
                currentSubfieldIndex++;
                p.skipNext();
                p.skipSpaces();
            }
        }

        LogManager.logTrace(LOG_SOURCE, "ParseControlStatement exit");
        return ps;
    }

    private static void postSyntaxError(final FacStatusResult fsr) {
        fsr.postMessage(FacStatusCode.SyntaxErrorInImage);
        fsr.mergeStatusBits(0_400000_000000L);
        LogManager.logTrace(LOG_SOURCE, "parseControlStatement stat=%012o", fsr.getStatusWord());
    }
}
