/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.Run;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

public class RunCardInfo {

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

    private String _runid ;
    private Character _schedulingPriority;
    private Long _optionWord;
    private Character _processorPriority;
    private String _accountId;
    private String _userId;
    private String _projectId;
    private Long _maxTime; // in seconds
    private Instant _deadlineTime; // time to be finished by
    private Long _maxPages;
    private Long _maxCards;
    private Instant _startTime; // time before which we cannot be started

    public RunCardInfo() {
    }

    public String getAccountId() { return _accountId; }
    public Instant getDeadlineTime() { return _deadlineTime; }
    public Long getMaxCards() { return _maxCards; }
    public Long getMaxPages() { return _maxPages; }
    public Long getMaxTime() { return _maxTime; }
    public Long getOptionWord() { return _optionWord; }
    public Character getProcessorPriority() { return _processorPriority; }
    public String getProjectId() { return _projectId; }
    public String getRunId() { return _runid; }
    public Character getSchedulingPriority() { return _schedulingPriority; }
    public Instant getStartTime() { return _startTime; }
    public String getUserId() { return _userId; }

    public RunCardInfo setAccountId(final String value) { _accountId = value; return this; }
    public RunCardInfo setDeadlineTime(final Instant value) { _deadlineTime = value; return this; }
    public RunCardInfo setMaxCards(final Long value) { _maxCards = value; return this; }
    public RunCardInfo setMaxPages(final Long value) { _maxPages = value; return this; }
    public RunCardInfo setMaxTime(final Long value) { _maxTime = value; return this; }
    public RunCardInfo setOptionWord(final Long value) { _optionWord = value; return this; }
    public RunCardInfo setProcessorPriority(final Character value) { _processorPriority = value; return this; }
    public RunCardInfo setProjectId(final String value) { _projectId = value; return this; }
    public RunCardInfo setRunId(final String value) { _runid = value; return this; }
    public RunCardInfo setSchedulingPriority(final Character value) { _schedulingPriority = value; return this; }
    public RunCardInfo setStartTime(final Instant value) { _startTime = value; return this; }
    public RunCardInfo setUserId(final String value) { _userId = value; return this; }

    /**
     * Parses a @RUN card from a given image for a particular Run (usually the exec)
     * @return a RunCardInfo representing the content of the @RUN image, null if it is not a @RUN image
     * @throws Parser.SyntaxException with descriptive text
     */
    public static RunCardInfo parse(
        final Run run,
        final String image
    ) throws Parser.SyntaxException {
        var statement = Interpreter.parseControlStatement(run, image);
        return statement._mnemonic.equalsIgnoreCase("RUN") ? new RunCardInfo().parse(statement) : null;
    }

    /**
     * Parses a @RUN card from the statement in the RunCardInfo object
     * format:
     *      '@RUN' [,scheduling-priority/options/processor-dispatching-priority {wsp}
     *          run-id,acct-id/user-id,project-id,run-time/deadline,pages/cards,start-time]
     * @return this object
     * @throws Parser.SyntaxException with descriptive text
     */
    public RunCardInfo parse(final ParsedStatement statement) throws Parser.SyntaxException {
        var opts = statement._optionsFields;
        var operands = statement._operandFields;

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
            if (statement._optionsFields.size() > 1) {
                _optionWord = parseOptionWord(opts.get(1));
                if (statement._optionsFields.size() > 2) {
                    _processorPriority = parsePriorityCharacter(opts.get(2));
                }
            }
        }

        _runid = operands.get(SS_RUNID);
        if (_runid != null) {
            _runid = _runid.toUpperCase();
            if (!Exec.isValidRunid(_runid)) {
                throw new Parser.SyntaxException("Invalid RunId");
            }
        }

        _accountId = operands.get(SS_ACCOUNT_ID);
        if (_accountId != null) {
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
        if (_projectId != null) {
            _projectId = _projectId.toUpperCase();
            if (!Exec.isValidProjectId(_projectId)) {
                throw new Parser.SyntaxException("Invalid ProjectId");
            }
        }

        _maxTime = parseInteger(operands.get(SS_RUN_TIME));
        _deadlineTime = parseTimeSpecification(operands.get(SS_DEADLINE));
        _maxPages = parseInteger(operands.get(SS_PAGES));
        _maxCards = parseInteger(operands.get(SS_CARDS));
        _startTime = parseTimeSpecification(operands.get(SS_START_TIME));

        return this;
    }

    private Long parseInteger(
        final String source
    ) throws Parser.SyntaxException {
        Long result = null;
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

    private Instant parseTimeSpecification(
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

            var now = Instant.now();
            if (isClockTime) {
                var result = now.atZone(ZoneOffset.UTC)
                                .withHour(hour)
                                .withMinute(minute)
                                .withSecond(0)
                                .withNano(0)
                                .toInstant();
                if (result.isBefore(now)) {
                    result = result.plus(1, ChronoUnit.DAYS);
                }
                return result;
            } else {
                return Instant.now().plus(hour, ChronoUnit.HOURS).plus(minute, ChronoUnit.MINUTES);
            }
        }
    }
}
