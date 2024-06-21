/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.Run;

import java.util.HashSet;
import java.util.Set;

public class RunCardInfo {

    public static class TimeSpecification {

        public boolean _isClockTime;
        public int _hourSpec;
        public int _minuteSpec;

        private TimeSpecification(
            final boolean isClockTime,
            final int hourSpec,
            final int minuteSpec
        ) {
            _isClockTime = isClockTime;
            _hourSpec = hourSpec;
            _minuteSpec = minuteSpec;
        }

        public boolean isClockTime() { return _isClockTime; }
        public boolean isRelativeTime() { return !_isClockTime; }
        public int getHourSpec() { return _hourSpec; }
        public int getMinuteSpec() { return _minuteSpec; }
    }

    private static final SubfieldSpecifier SS_RUNID = new SubfieldSpecifier(0, 0);
    private static final SubfieldSpecifier SS_ACCOUNT_ID = new SubfieldSpecifier(1, 0);
    private static final SubfieldSpecifier SS_USER_ID = new SubfieldSpecifier(1, 1);
    private static final SubfieldSpecifier SS_PROJECT_ID = new SubfieldSpecifier(2, 0);
    private static final SubfieldSpecifier SS_RUN_TIME = new SubfieldSpecifier(3, 0);
    private static final SubfieldSpecifier SS_DEADLINE = new SubfieldSpecifier(3, 1);
    private static final SubfieldSpecifier SS_PAGES = new SubfieldSpecifier(4, 0);
    private static final SubfieldSpecifier SS_CARDS = new SubfieldSpecifier(4, 1);
    private static final SubfieldSpecifier SS_START_TIME = new SubfieldSpecifier(5, 0);

    private static final Set<SubfieldSpecifier> VALID_SUBFIELDS = new HashSet<>();
    static {
        VALID_SUBFIELDS.add(SS_RUNID);
        VALID_SUBFIELDS.add(SS_ACCOUNT_ID);
        VALID_SUBFIELDS.add(SS_USER_ID);
        VALID_SUBFIELDS.add(SS_PROJECT_ID);
        VALID_SUBFIELDS.add(SS_RUN_TIME);
        VALID_SUBFIELDS.add(SS_DEADLINE);
        VALID_SUBFIELDS.add(SS_PAGES);
        VALID_SUBFIELDS.add(SS_CARDS);
        VALID_SUBFIELDS.add(SS_START_TIME);
    }

    private final ParsedStatement _statement;
    private String _runid;
    private Character _schedulingPriority;
    private Long _optionWord;
    private Character _processorPriority;
    private String _accountId;
    private String _userId;
    private String _projectId;
    private Long _maxTime; // in seconds
    private TimeSpecification _deadlineTime; // time to be finished by
    private Long _maxPages;
    private Long _maxCards;
    private TimeSpecification _startTime; // time to be started

    public RunCardInfo(
        final ParsedStatement statement
    ) {
        _statement = statement;
    }

    public static RunCardInfo parse(
        final Run run,
        final String image
    ) throws Parser.SyntaxException {
        var statement = Interpreter.parseControlStatement(run, image);
        return new RunCardInfo(statement).parse();
    }

    /**
     * Parses a @RUN card from the statement in the RunCardInfo object
     * format:
     *      '@RUN' [,scheduling-priority/options/processor-dispatching-priority {wsp}
     *          run-id,acct-id/user-id,project-id,run-time/deadline,pages/cards,start-time]
     * @return this object
     * @throws Parser.SyntaxException with descriptive text
     */
    public RunCardInfo parse() throws Parser.SyntaxException {
        var opts = _statement._optionsFields;
        var operands = _statement._operandFields;
        var exec = Exec.getInstance();
        var cfg = exec.getConfiguration();

        if (opts.size() > 3) {
            throw new Parser.SyntaxException("Invalid field or subfield in @RUN card");
        }
        for (var subSpec : operands.keySet()) {
            if (!VALID_SUBFIELDS.contains(subSpec)) {
                throw new Parser.SyntaxException("Invalid field or subfield in @RUN card");
            }
        }

        if (!opts.isEmpty()) {
            _schedulingPriority = parsePriorityCharacter(opts.get(0));
            if (_statement._optionsFields.size() > 1) {
                _optionWord = parseOptionWord(opts.get(1));
                if (_statement._optionsFields.size() > 2) {
                    _processorPriority = parsePriorityCharacter(opts.get(2));
                }
            }
        }

        _runid = operands.get(SS_RUNID);
        if (_runid == null) {
            _runid = "RUN000";
        } else {
            _runid = _runid.toUpperCase();
            if (!Exec.isValidRunid(_runid)) {
                throw new Parser.SyntaxException("Invalid RunId");
            }
        }

        _accountId = operands.get(SS_ACCOUNT_ID);
        if (_accountId == null) {
            _accountId = "000000";
        } else {
            _accountId = _accountId.toUpperCase();
            if (!Exec.isValidAccountId(_accountId)) {
                throw new Parser.SyntaxException("Invalid AccountId");
            }
        }

        _userId = operands.get(SS_USER_ID);
        if (_userId != null) {
            _userId = _userId.toUpperCase();
            if (!Exec.isValidAccountId(_userId)) {
                throw new Parser.SyntaxException("Invalid UserId");
            }
        }

        _projectId = operands.get(SS_PROJECT_ID);
        if (_projectId == null) {
            _projectId = "Q$Q$Q$";
        } else {
            _projectId = _projectId.toUpperCase();
            if (!Exec.isValidProjectId(_projectId)) {
                throw new Parser.SyntaxException("Invalid ProjectId");
            }
        }

        _maxTime = parseInteger(operands.get(SS_RUN_TIME), cfg.getMaxTime());
        _deadlineTime = parseTimeSpecification(operands.get(SS_DEADLINE));
        _maxPages = parseInteger(operands.get(SS_PAGES), cfg.getMaxPages());
        _maxCards = parseInteger(operands.get(SS_CARDS), cfg.getMaxCards());
        _startTime = parseTimeSpecification(operands.get(SS_START_TIME));

        return this;
    }

    public String getAccountId() { return _accountId; }
    public TimeSpecification getDeadlineTime() { return _deadlineTime; }
    public Long getMaxCards() { return _maxCards; }
    public Long getMaxPages() { return _maxPages; }
    public Long getOptionWord() { return _optionWord; }
    public Character getProcessorPriority() { return _processorPriority; }
    public String getProjectId() { return _projectId; }
    public String getRunId() { return _runid; }
    public Long getMaxTime() { return _maxTime; }
    public Character getSchedulingPriority() { return _schedulingPriority; }
    public ParsedStatement getStatement() { return _statement; }
    public String getUserId() { return _userId; }

    private Long parseInteger(
        final String source,
        final Long defaultValue
    ) throws Parser.SyntaxException {
        long result = defaultValue;
        if (source != null) {
            try {
                result = Long.parseLong(source);
                if ((result < 0) || (result > 0_777777)) {
                    throw new Parser.SyntaxException("Invalid integer in @RUN statement");
                }
            } catch (NumberFormatException ex) {
                throw new Parser.SyntaxException("Invalid integer in @RUN statement");
            }
        }
        return result;
    }

    private Long parseOptionWord(
        final String source
    ) throws Parser.SyntaxException {
        long result = 0;
        for (var ch : source.toCharArray()) {
            if (!Character.isAlphabetic(ch)) {
                throw new Parser.SyntaxException("Invalid option in @RUN image");
            }
            ch = Character.toUpperCase(ch);
            var shift = 1L << ('Z' - ch);
            result |= shift;
        }
        return result;
    }

    private Character parsePriorityCharacter(
        final String source
    ) throws Parser.SyntaxException {
        if (source.length() > 1) {
            throw new Parser.SyntaxException();
        }

        if (source.length() == 1) {
            var ch = source.charAt(0);
            if (!Character.isAlphabetic(ch)) {
                throw new Parser.SyntaxException("Invalid priority in @RUN image");
            }
            return Character.toUpperCase(ch);
        }

        return null;
    }

    private TimeSpecification parseTimeSpecification(
        final String source
    ) throws Parser.SyntaxException {
        if (source == null) {
            return null;
        } else {
            boolean isClockTime = source.startsWith("D") || source.startsWith("d");
            var effective = isClockTime ? source.substring(1) : source;
            if (effective.length() != 4) {
                throw new Parser.SyntaxException("Invalid time specification in @RUN image");
            }

            for (var ch : effective.toCharArray()) {
                if (!Character.isDigit(ch)) {
                    throw new Parser.SyntaxException("Invalid time specification in @RUN image");
                }
            }

            var hStr = effective.substring(0, 2);
            var hour = Integer.parseInt(hStr);
            if (hour >= 24) {
                throw new Parser.SyntaxException("Invalid time specification in @RUN image");
            }

            var mStr = effective.substring(2, 4);
            var minute = Integer.parseInt(mStr);
            if (minute >= 60) {
                throw new Parser.SyntaxException("Invalid time specification in @RUN image");
            }

            return new TimeSpecification(isClockTime, hour, minute);
        }
    }
}
