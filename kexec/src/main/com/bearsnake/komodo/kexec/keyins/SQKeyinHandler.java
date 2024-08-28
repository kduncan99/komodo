/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.genf.queues.InputQueue;
import com.bearsnake.komodo.kexec.exec.genf.queues.OutputQueue;

class SQKeyinHandler extends KeyinHandler {

    private static enum Action {
        DisplaySummary,
        MoveExistingQueue,
        MoveFuture,
    }

    private static final String[] HELP_TEXT = {
        "SQ [ queue_name [*]]",
        "  Displays summary of files queued to one (or each) print or punch queue.",
        "  If * is given, a list of all files is displayed instead of a summary.",
        "SQ USR*ID",
        "  Displays summary of files queued to all user-ids.",
        "SQ run_id * R",
        "  Displays files queued for the given run-id.",
        "SQ,R run_id,...",
        "  Displays file information for files queued by the list of run-ids.",
        "SQ user_id * U",
        "  Displays files queued for the given user-ids.",
        "SQ,U user_id,...",
        "  Displays file information for files queued by the list of user-ids.",
        "SQ run-id [ file_name ] priority",
        "  Changes the priority for one or all files queued by a run-id.",
        "SQ queue_name QTO queue_name",
        "  Redirects files queued to the first queue, to the second queue.",
        "SQ run-id file-name QTO [ queue_name | user_id/U ]",
        "  Redirects a specific file to the given queue or user-name.",
        "SQ queue-name TO queue-name",
        "  Redirects future files for the first queue, to the second.",
    };

    public static final String COMMAND = "SQ";

    private Action _action;
    private String _queueName1;
    private String _queueName2;

    public SQKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        /*
SQ-CANNOT REDIRECT TO symbiont2 'symbiont2 REDIRECTED TO symbiont3'
(Exec) The device to which you were trying to redirect files has been redirected
to another device (Only one level of redirection is allowed).

SQ - FUTURE FILES FOR symbiont1 WILL BE DIRECTED TO symbiont2
(Exec) This message is displayed after the SQ keyin for future queue redirection.

SQ KEYIN IGNORED - THE SPECIFIED RUN-ID IS INVALID
(Exec) You specified a run-id that is longer than six characters.

SQ-symbiont1 MUST BE LOCKED OUT AND IDLE
(Exec) When you receive this message, one of the following problems exists:
 A SYMACT entry did not exist for the device.
 The device was not locked out.
 The device was active.

SQ-SYMBIONT NAMES MUST BE SPECIFIC DEVICES
(Exec) You either did not supply a device name or both names are the same name. Correct the SQ keyin and try again.

SQ - SYMBIONT NAME NOT VALID
One or more of the symbiont names specified on the SQ keyin is too long or is not a configured symbiont device, or both symbiont names are the same.

SQ KEYIN NOT ALLOWED

SQ - TOO MANY TOKENS
Too many tokens were specified in a field of the SQ keyin. A token is a sequence of characters not containing space and comma characters.

SQ - TOKEN #n IS TOO LONG
Token number n, entered on the SQ keyin, is too long.

SQ KEY ERROR
The SQ keyin is invalid.

SQ KEYIN IGNORED -- THE SPECIFIED USER-ID IS INVALID
The user-id entered on the SQ keyin is invalid or contains masking/wildcard characters where they are not permitted.

SQ - INVALID OR MISSING QUALIFIER
The qualifier supplied as part of the file name on the SQ keyin is invalid or missing.

SQ - INVALID OR MISSING FILENAME
The file name supplied on the SQ keyin is invalid or missing.

SQ - INVALID CYCLE
The file cycle number supplied on the SQ keyin is invalid.

SQ - No, duplicate or invalid option.  Only R,U,A are allowed.
The SQ keyin handler encountered an invalid option or expected an option when none was supplied.

SQ - Either R- or U-option must be specified. Not both.
The combination of R and U options on the SQ keyin is illegal.

SQ - A-option can only be specified with the U option.
The A option is only legal with the U option on an SQ keyin.

 USR*ID NOT CONFIGURED
The system is not configured to queue symbiont files to user-ids.

 UNSOLICITED DATA TRANSMISSION TO A TERMINAL NOT ALLOWED
The security configuration does not allow a user to send unsolicited data to a terminal.

SQ-QUEUING ERROR-NO OPEN SLOT
No space could be found in the priority queue for a symbiont file. Notify the system administrator.

sname HAS NO PRINT/PUNCH FILES QUEUED
The symbiont queue specifed on the SQ keyin has no files queued.

Symbiont Buffer Bank depleted
The bank used to stage information by the SQ keyin handler is full. The SQ keyin is aborted. If another SQ keyin is processing, let it complete and try the keyin again. If it still fails, notify the system administrator.

SQ - run-id #n is invalid or missing
SQ - user-id #n is invalid or missing
The nth ID in the list passed to the SQ keyin handler is invalid or missing.
*/
        var exec = Exec.getInstance();
        var gfi = exec.getGenFileInterface();
        if (gfi == null) {
            return false;
        }

        if (_arguments == null) {
            _action = Action.DisplaySummary;
            return _options == null;
        }

        var split = _arguments.split(" ");
//        if ((split.length == 1) && split[0].equalsIgnoreCase("USR*ID")) {
//            if (_options != null) {
//                return false;
//            }
//            _action = Action.Display;
//            _displayByUserIds = true;
//            return true;
//        }

        if ((split.length == 3) && split[1].equalsIgnoreCase("QTO")) {
            if (_options != null) {
                return false;
            }

            _queueName1 = split[0];
            _queueName2 = split[2];
            _action = Action.MoveExistingQueue;
            return true;
//        } else if ((split.length == 4) && split[2].equalsIgnoreCase("QTO")) {
//            if (_options != null) {
//                return false;
//            }
//            // TODO
        } else if ((split.length == 3) && split[1].equalsIgnoreCase("TO")) {
            if (_options != null) {
                return false;
            }
            // TODO
        }

        return false;
    }

    @Override
    String getCommand() { return COMMAND; }

    @Override
    String[] getHelp() { return HELP_TEXT; }

    @Override
    boolean isAllowed() {
        var genf = Exec.getInstance().getGenFileInterface();
        return genf != null && genf.isReady();
    }

    @Override
    void process() {
        switch (_action) {
        case DisplaySummary -> processDisplay();
        case MoveExistingQueue -> processMoveExistingQueue();
        case MoveFuture -> {
        }
        }
    }

    void processDisplay() {
        var exec = Exec.getInstance();
        var gfi = exec.getGenFileInterface();
        boolean found = false;
        for (var q : gfi.getPunchQueues()) {
            var msg = String.format("%s: %02d FILES %03d CARDS %02d TAPES", q.getQueueName(), 0, 0, 0);
            exec.sendExecReadOnlyMessage(msg, _source);
            found = true;
        }
        if (!found) {
            exec.sendExecReadOnlyMessage("NO PUNCH QUEUES CONFIGURED", _source);
        }

        found = false;
        for (var q : gfi.getPrintQueues()) {
            var msg = String.format("%s: %02d FILES %03d PAGES %02d TAPES", q.getQueueName(), 0, 0, 0);
            exec.sendExecReadOnlyMessage(msg, _source);
            found = true;
        }
        if (!found) {
            exec.sendExecReadOnlyMessage("NO PRINT QUEUES CONFIGURED", _source);
        }
    }

    void processMoveExistingQueue() {
        var exec = Exec.getInstance();
        var gfi = exec.getGenFileInterface();
        var queue1 = gfi.getQueue(_queueName1);
        var queue2 = gfi.getQueue(_queueName2);
        if (queue1 == null || queue2 == null || queue1 instanceof InputQueue || queue2 instanceof InputQueue) {
            exec.sendExecReadOnlyMessage("SQ - INVALID OR NON-EXISTING QUEUE", _source);
            return;
        } else if (queue1 == queue2) {
            exec.sendExecReadOnlyMessage("SQ - SAME QUEUE", _source);
            return;
        } else if (queue1.getClass() != queue2.getClass()) {
            exec.sendExecReadOnlyMessage("SQ - INCOMPATIBLE QUEUES", _source);
            return;
        }

        ((OutputQueue) queue1).moveTo((OutputQueue) queue2);
        // TODO what message(s) is/are sent as files are moved?
    }
}
