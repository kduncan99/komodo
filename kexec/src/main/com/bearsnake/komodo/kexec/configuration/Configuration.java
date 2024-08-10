/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.exceptions.SyntaxException;
import com.bearsnake.komodo.kexec.configuration.parameters.*;
import com.bearsnake.komodo.kexec.configuration.restrictions.*;
import com.bearsnake.komodo.kexec.configuration.values.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.bearsnake.komodo.kexec.configuration.parameters.Tag.*;
import static com.bearsnake.komodo.kexec.configuration.values.BooleanValue.FALSE;
import static com.bearsnake.komodo.kexec.configuration.values.BooleanValue.TRUE;
import static com.bearsnake.komodo.kexec.configuration.values.IntegerValue.ZERO;
import static com.bearsnake.komodo.kexec.configuration.values.ValueType.BOOLEAN;
import static com.bearsnake.komodo.kexec.configuration.values.ValueType.CHARACTER;
import static com.bearsnake.komodo.kexec.configuration.values.ValueType.FLOAT;
import static com.bearsnake.komodo.kexec.configuration.values.ValueType.INTEGER;
import static com.bearsnake.komodo.kexec.configuration.values.ValueType.STRING;

public class Configuration {

    private static final Map<String, Parameter> CONFIG_PARAMETERS = new TreeMap<>();
    private static final Map<String, MnemonicInfo> MNEMONIC_TABLE = new TreeMap<>();
    private static final Map<String, Node> NODES = new LinkedHashMap<>();

    // discrete configuration values ------------------------------------------------------------------------------------

    static {
        // AACOUNTER is n/a TODO TIP
        // AATIPSYSFILE is n/a TODO TIP

        putRestrictedConfigParameter(ACCTINTRES, INTEGER, ZERO, true, true,
                                     "Initial reserve used when creating the SYS$*ACCOUNT$R1 and SEC@ACCTINFO files." +
                                         " The value must be between 0 and 4096 (tracks).",
                                     new IntegerRangeRestriction(0, 4096));

        putRestrictedConfigParameter(ACCTASGMNE, STRING, new StringValue("F"), true, true,
                                     "Assign mnemonic to be used when cataloging SYS$*ACCOUNT$R1 and SEC@ACCTINFO files.",
                                     new MnemonicRestriction(MNEMONIC_TABLE));

        putFixedConfigParameter(ACCTMSWTIME, BOOLEAN, TRUE,
                                "If true, entries in the account file are stored in MODSWTIME. Otherwise, they are stored in TDATE$ format.");

        putRestrictedConfigParameter(ACCTON, INTEGER, new IntegerValue(2), true, true,
                                     """
                                        Controls the quota level:
                                        0: Account file turned off
                                        1: Account file on, and account verification is enabled
                                        2: Account file on, and account verification and accumulation are enabled
                                        3: Account file on, account verification, accumulation, and account-level quote are enforced
                                        4: Account file on, account verification, accumulation, account-level quote, and run-level quota are enforced.""",
                                     new IntegerRangeRestriction(0, 4));

        putSettableConfigParameter(AFICM, BOOLEAN,  TRUE, false, true,
                                   "Determines whether divide fault interrupts are enabled.");

        putSettableConfigParameter(ALATXR, BOOLEAN, FALSE, true, false,
                                   "If true, exec takes a 054 stop if the ASCII log is unavailable.");

        putSettableConfigParameter(APLBDIGT4K, BOOLEAN, TRUE, false, false,
                                   "If false, only the Linking System can create application-level banks with BDIs > 4095.\n"
                                       + "If true, user programs and applications are also allowed to create code and data banks with BDIs > 4095.");

        putRestrictedConfigParameter(APLDYNBDS, INTEGER, new IntegerValue(04000, true), false, true,
                                     "Fixed number of bank descriptors to be dynamically allocated in the level 1 BDT."
                                         + " Note that MAXBDI + APLDYNBDS must be < 32766.",
                                     new IntegerRangeRestriction(0, 32766));

        putFixedConfigParameter(ASGSCN, BOOLEAN, FALSE,
                                "If true, facility pre-scan code is enabled. If false, runs are opened without pre-scanning.");

        putSettableConfigParameter(ATATPASSENA, BOOLEAN, FALSE, true, false,
                                   "If true, @@PASSWD is allowed.");

        putRestrictedConfigParameter(ATOCL, INTEGER, new IntegerValue(01011, true), false, true,
                                     "Account file look-up table length (must be a prime number).",
                                     new PrimeNumberRestriction());

        putRestrictedConfigParameter(BATCHXMODES, INTEGER, ZERO, true, false,
                                   """
                               0: SSRUNXOPT is required for @START and ST specifying X options, but not remote batch runs.
                               1: SSRUNXOPT is required for all X option specifications.
                               2: SSRUNXOPT not required for remote batch runs specifying X option; \
                               @START and ST rely on QUOTA capability of the deadline being given to the run
                               3: SSRUNXOPT not required for remote batch run, @START, or ST specifying X option.""",
                                     new IntegerRangeRestriction(0, 3));

        putSettableConfigParameter(BLOKCK, BOOLEAN, FALSE, true, false,
                                   "If true, block counts in tape labels are compared against actual block counts.");

        putSettableConfigParameter(BYFACHELDRUN, BOOLEAN, FALSE, true, false,
                                   "If true, lower priority runs may be opened when higher priority runs are held for facilities.");

        putSettableConfigParameter(CATON, BOOLEAN, FALSE, true, true,
                                   "Enables quota enforcement for cataloged files.");

        putRestrictedConfigParameter(CATP2F, INTEGER, ZERO, true, true,
                                     "Power-of-two factor for number of tracks defined as max limit allowed per quota group.",
                                     new IntegerRangeRestriction(0, 17));

        /*
        putSettableConfigParameter(CBUFCT, 32, true, true,
                                   "Length in words of main storage COMPOOL blocks"); // TODO TIP
        putSettableConfigParameter(CHKSUM, false, true, true,
                                   "True to perform checksum on mass storage COMPOOL blocks."); // TODO TIP

        // CKRSFILEVER not supported - we don't currently do checkpoint/restart // TODO CKRS

        putSettableConfigParameter(CMPAN1, 50, true, false,
                                   "Percentage of COMPOOL available to trigger COMPOOL panic level 1"); // TODO TIP
        putSettableConfigParameter(CMPAN2, 20, true, false,
                                   "Percentage of COMPOOL available to trigger COMPOOL panic level 2"); // TODO TIP
        putSettableConfigParameter(CMPAN3, 10, true, false,
                                   "Percentage of COMPOOL available to trigger COMPOOL panic level 3"); // TODO TIP
        */

        putSettableConfigParameter(CMPDIS, BOOLEAN, FALSE, true, false,
                                   "If true, COMPOOL is disabled.");

        /*
        putSettableConfigParameter(CMPMAX, 0, true, true,
                                   "Max number of COMPOOL blocks per output message - if 0, there is no limit."); // TODO TIP

        // CONTIM - not supported (no SIP) // TODO SIP

        putSettableConfigParameter(COREFILE, false, true, true,
                                   "If true, KONS main storage file is available."); // TODO TIP
        */

        putSettableConfigParameter(CSHRCV, BOOLEAN, TRUE, true, false,
                                   "If true, runs are held on a recovery boot until CS A is entered.");

        putRestrictedConfigParameter(CTLIMGSP, INTEGER, ZERO, true, false,
                                   """
                               If zero and PAGEJECT is true, a page eject occurs before processor statements are printed.
                               If zero and PAGEJECT is false, line spacing is three lines for all control statements.
                               Values 1-63 indicate the number of lines printed before control statements.
                               """,
                                     new IntegerRangeRestriction(0, 63));

        /*
        putSettableConfigParameter(C32NBR, 30, true, true,
                                   "Number of primary mass storage COMPOOL blocks."); // TODO TIP
        putSettableConfigParameter(C82NBR, 30, true, true,
                                   "Number of secondary mass storage COMPOOL blocks."); // TODO TIP
        putSettableConfigParameter(DAYBLKS, 56, true, true,
                                   "Number of words in a day block on the TIMER system file."); // TODO TIP
        */

        // DBLMFDSEARCH not supported since we don't do directories // TODO SHARED
        // DCLUTS not supported since we don't do lookup table entries
        // DCMAXRERR not supported - we don't implement DC keyin // TODO DC KEYIN
        // DCSPLTDAD not supported - we don't implement DC keyin // TODO DC KEYIN

        putSettableConfigParameter(DEDLIN, BOOLEAN, TRUE, false, true,
                                   "true to enable deadline runs and switching levels.");

        // DEDRUNn not supported

        putSettableConfigParameter(DELAYSOL, BOOLEAN, FALSE, true, true,
                                   "If true, solicitation period is delayed between invalid sign-on attempts.");

        putSettableConfigParameter(DELRMPRT, BOOLEAN, FALSE, false, false,
                                   "Indicates whether print files for removed (RM) runs should be printed.");

        putSettableConfigParameter(DEMTOP, BOOLEAN, TRUE, true, false,
                                   "Indicates whether the top separator page is to be printed by RSI demand batch printers.");

        putSettableConfigParameter(DEQIAD, BOOLEAN, FALSE, true, false,
                                   "If true, recoverable COMPOOL transactions if all reads/writes to TIP files are done without after-looks.");

        // DESMIPS not supported
        // DIRSELECT not supported - we don't do directories // TODO SHARED
        // DISPNOWAIT not supported - I don't think we're going to implement a dispatcher
        // DIVINTERVAL n/a

        putRestrictedConfigParameter(DLOCASGMNE, STRING, new StringValue("F"), true, false,
                                     "Assign mnemonic used to create SYS$*DLOC$.",
                                     new MnemonicRestriction(MNEMONIC_TABLE));

        // DLTDCOMP not supported - we don't do DLT tapes

        putRestrictedConfigParameter(DMPAPLVAL, INTEGER, new IntegerValue(1), true, false,
                                   """
                               Indicates the application-level subsystem banks to be included in system dumps.
                               0: No banks included
                               1: Application level home subsystem which contains the common banks
                               2: All application level subsystems""",
                                     new IntegerRangeRestriction(0, 2));

        putSettableConfigParameter(DYNDNPACK, BOOLEAN, FALSE, false, true,
                                   "If true, a DN PACK is automatically issued when MFD I/O to a removable disk fails.");

        // ECHOREM - not sure if we need to think about this; what constitutes a remote console? // TODO CONSOLE

        putSettableConfigParameter(ERTDATE$OFF, BOOLEAN, FALSE, true, false,
                                   "If true, ER TDATE$ fails with type 04 code 03 cgy type 012.");

        // EXPTRACE is n/a
        // EXTDDCSYNC is not supported
        // FCACHDEFAULT is not supported
        // FCDBSZ is not supported

        /*
        putSettableConfigParameter(FCMXLK, null, 5, true, true,
                                   "Limits the number of TIP locks which a program can hold."); // TODO TIP
        */

        // FCNUSRDADBNK is n/a

        putSettableConfigParameter(FILIMAGCTRL, BOOLEAN, FALSE, true, false,
                                   "If true, any batch runstream from an input symbiont or @RUN,/B with an @FILE image is rejected."
                                       + " Otherwise, @FILE and @ENDF images are processed.");

        /*
        putSettableConfigParameter(FNSCR, null, 0, true, true,
                                   "Maximum number of files reserved for TIP scratch file assignment."); // TODO TIP
        putSettableConfigParameter(FNTMP, null, 0, true, true,
                                   "Maximum number of files reserved for TIP temporary file assignment."); // TODO TIP
        putSettableConfigParameter(FNTRN, null, 0, true, true,
                                   "Maximum number of permanent fixed files that can be used as TIP training files."); // TODO TIP
        putSettableConfigParameter(FORCETIPINIT, "force_tip_initialization", false, true, false,
                                   "Specifies one or more TIP parameters has changed, indicating a TIP initialization must be forced on next boot."); // TODO TIP
        // FQ$MFP is n/a
        */

        putSettableConfigParameter(FREESPACE, BOOLEAN, FALSE, true, true,
                                   "Indicates whether TIP freespace functions are available.");

        putFixedConfigParameter(FSMSWTIME, BOOLEAN, TRUE,
                                "If true, freespace timestamps are formatted as MODSWTIME$ - otherwise, as TDATE$.");

        putSettableConfigParameter(GCCMIN, INTEGER, ZERO, false, true,
                                   "Indicates the maximum number of terminals allowed concurrently active via RSI$."
                                       + " If GCCMIN > 0 then RSICNT must be > 0.");

        putRestrictedConfigParameter(GENFASGMNE, STRING, new StringValue("F"), true, false,
                                     "Assign mnemonic used to create SYS$*GENF$.",
                                     new MnemonicRestriction(MNEMONIC_TABLE));

        putRestrictedConfigParameter(GENFINTRES, INTEGER, new IntegerValue(1000), true, false,
                                     "Initial reserve used to create SYS$*GENF$. Must be between 0 and 4096.",
                                     new IntegerRangeRestriction(0, 4096));

        // HICDCOMP not supported - we don't do HIC tapes
        // HISDCOMP not supported - we don't do HIS tapes
        // HM2USER not supported

        /*
        putSettableConfigParameter(HVTIP, "hvtip_library_max", 0, true, true,
                                   "Max number of HVTIP libraries."); // TODO TIP
        putSettableConfigParameter(HVTIPMACT, "allow_hvtip_multi_activity", false, true, true,
                                   "Controls whether HVTIP programs can have multiple activities during calls or transfers between HVTIP banks."); // TODO TIP
        putSettableConfigParameter(INHBAUTOALLC, "inhibit_tip_auto_allocation", false, true, true,
                                   "Controls whether auto allocation can occur during TIP/Exec file writes."); // TODO TIP

        putSettableConfigParameter(INHBTRANSM, "inhibit_transmission", false, true, false,
                                   "If true, a user is not allowed to use @@TM or @SYM, or to transmit unsolicited data to terminals.");
        */

        putSettableConfigParameter(IODBUG, BOOLEAN, FALSE, false, true,
                                   "If true, all debug aids are enabled for I/O.");

        /*
        putSettableConfigParameter(IOMAXRTIME, "io_maximum_recovery_time", 0, true, false,
                                   "Maximum number of seconds to allow I/O timeout recovery per retry (0-63)."); // TODO maybe not supported
        */

        // IOTASGMNE is n/a
        // IOTFLXTRAN is n/a
        // IOTINTRES is n/a
        // IOTMAXCYC is n/a
        // IOTMAXSIZ is n/a
        // IPIPRE not supported - I think

        /*
        putSettableConfigParameter(KONSBL, "u3_block_length", 0, true, true,
                                   "Length in words of a U3 KONS block."); // TODO TIP
        putSettableConfigParameter(KONSEC, "u1_area_length", 4, true, true,
                                   "Length in words of the write-protected U1 area of KONS."); // TODO TIP
        putSettableConfigParameter(KONSFL, "length_of_kons_areas", 0, true, true,
                                   "Length in words of U1, U2, U3, and security directory areas of KONS."); // TODO TIP
        putSettableConfigParameter(KONSPW, "u3_password_required", false, true, true,
                                   "If true, a password is required for access to a U3 block."); // TODO TIP
        putSettableConfigParameter(KONU3L, "u3_user_area_length", 0, true, true,
                                   "Length in words of KONS U3 area."); // TODO TIP
        putSettableConfigParameter(KSECNB, "u3_security_entries", 0, true, true,
                                   "Number of entries in the security directory for U3."); // TODO TIP
        */

        putSettableConfigParameter(LABOUT, BOOLEAN, FALSE, true, false,
                                   "If true, security labels and sequence numbers are printed.");

        putSettableConfigParameter(LABPAG, BOOLEAN, FALSE, true, false,
                                   "If true, security labels appear on every page of printouts - otherwise, labels appear only on banner and trailer pages.");

        putFixedConfigParameter(LARGEFILES, BOOLEAN, TRUE,
                                "If true, large files can be allocated. This slightly alters the format of certain MFD entries.");

        putRestrictedConfigParameter(LIBASGMNE, STRING, new StringValue("F"), true, false,
                                     "Assign mnemonic used to create SYS$*LIB$.",
                                     new MnemonicRestriction(MNEMONIC_TABLE));

        putRestrictedConfigParameter(LIBINTRES, INTEGER, ZERO, true, false,
                                     "Initial reserve used to create SYS$*LIB$. Must be between 0 and 262143.",
                                     new IntegerRangeRestriction(0, 262143));

        putRestrictedConfigParameter(LIBMAXSIZ, INTEGER, new IntegerValue(99999), true, false,
                                     "Maximum size for SYS$*LIB$.",
                                     new IntegerRangeRestriction(1000, 262143));

        // LOG*** are all n/a

        putRestrictedConfigParameter(LOOKAHDRND, INTEGER, new IntegerValue(1), true, false,
                                     "Number of tracks to allocate (beyond initial reserve) when allocating space - 1 to 10000.",
                                     new IntegerRangeRestriction(1, 10000));

        putRestrictedConfigParameter(LOOKAHDSEQ, INTEGER, new IntegerValue(32), true, false,
                                     "Number of tracks to allocate when allocating space at or beyond the end of a file.",
                                     new IntegerRangeRestriction(1, 10000));

        /*
        putSettableConfigParameter(LNKBLKS, null, 28, true, true,
                                   "Number of words in a link block on the TIMER system file."); // TODO TIP
        */

        // LTODCOMP not supported - we don't do LTO

        putSettableConfigParameter(MACHGENPASS, BOOLEAN, FALSE, true, false,
                                   "If true, machine-generated passwords are enabled.");

        putRestrictedConfigParameter(MAXATMP, INTEGER, new IntegerValue(5), true, false,
                                     "Max number of sign-on attempts a user is allowed to successfully sign on to a host.",
                                     new IntegerRangeRestriction(0, 16));

        putRestrictedConfigParameter(MAXBDI, INTEGER, new IntegerValue(06061, true), false, true,
                                     "Max number of alternate file common banks.",
                                     new IntegerRangeRestriction(06061, 07777));

        putRestrictedConfigParameter(MAXCRD, INTEGER, new IntegerValue(100), true, false,
                                     "Max number of cards for @RUN images which do not specify max cards.",
                                     new IntegerRangeRestriction(10, 131071));

        // MAXDSP is not supported

        putRestrictedConfigParameter(MAXGRN, INTEGER, new IntegerValue(256), true, false,
                                     "Max number of granules assigned to a file if not specified.",
                                     new IntegerRangeRestriction(1, 0777777));

        putRestrictedConfigParameter(MAXLOG, INTEGER, new IntegerValue(10000), true, false,
                                     "Max number of log entries which can be created by a user during one task.",
                                     new IntegerRangeRestriction(100, 0777777));

        // MAXMIPS is not supported

        putRestrictedConfigParameter(MAXOPN, INTEGER, new IntegerValue(6), false, true,
                                     "Maximum number of batch runs allowed open at any time.",
                                     new IntegerRangeRestriction(0, 99999));

        putRestrictedConfigParameter(MAXPAG, INTEGER, new IntegerValue(100), true, false,
                                     "Max number of pages for @RUN images which do not specify max cards.",
                                     new IntegerRangeRestriction(10, 131071));

        putRestrictedConfigParameter(MAXPASSDAY, INTEGER, new IntegerValue(90), true, false,
                                     "Max number of days before a password must be replaced.",
                                     new IntegerRangeRestriction(1, 262142));

        putRestrictedConfigParameter(MAXPASSLEN, INTEGER, new IntegerValue(18), true, false,
                                     "Maximum number of characters allowed for a password.",
                                     new IntegerRangeRestriction(1, 18));

        putRestrictedConfigParameter(MAXTIM, INTEGER, new IntegerValue(10), true, false,
                                     "Maximum run time if not specified on @RUN image.",
                                     new IntegerRangeRestriction(1, 2184));

        putRestrictedConfigParameter(MAXTUX, INTEGER, new IntegerValue(30), false, false,
                                     "Seconds before the MAXTIM contingency is honored.",
                                     new IntegerRangeRestriction(1, 60));

        /*
        putSettableConfigParameter(MBUFCT, null, 56, true, true,
                                   "Length in words of primary and secondary mass storage COMPOOL blocks."); // TODO TIP
        */

        putRestrictedConfigParameter(MDFALT, STRING, new StringValue("F"), true, false,
                                     "Mnemonic to be used when not specified on @CAT or @ASG",
                                     new MnemonicRestriction(MNEMONIC_TABLE));

        // MEMFLSZ not supported

        putFixedConfigParameter(MFDMSWTIME, BOOLEAN, TRUE,
                                "If true, MFD timestamps are stored in MODSWTIME format. If false, they are stored in TDATE$ format.");

        // MFDONXPCSTD is n/a // TODO SHARED
        // MFDONSPCSHR is n/a // TODO SHARED
        // MFDSHMSWTIME is n/a // TODO SHARED

        /*
        putSettableConfigParameter(MINBLKS, null, 28, true, true,
                                   "Number of words in a minute block on the TIMER system file."); // TODO TIP
        */

        // MINMPS is n/a

        putRestrictedConfigParameter(MINPASSDAY, INTEGER, new IntegerValue(1), true, false,
                                     "Minimum number of days before a password may be replaced.",
                                     new IntegerRangeRestriction(0, 262142));

        putRestrictedConfigParameter(MINPASSLEN, INTEGER, new IntegerValue(8), true, false,
                                     "Minimum number of characters for a new password.",
                                     new IntegerRangeRestriction(1, 18));

        putRestrictedConfigParameter(MSTRACC, STRING, null, true, false,
                                     "Master account - if null, the operator will be prompted at boot time.",
                                     new AccountIdRestriction());

        putFixedConfigParameter(MMGRMSWTIME, BOOLEAN, TRUE,
                                "If true, media manager timestamps are MODSWTIME format - otherwise they are TDATE$.");

        putRestrictedConfigParameter(MXTMIPSUPS, INTEGER, ZERO, true, false,
                                   """
                               Indicates method of calculating max time check.
                               0: Total SUPs used for demand, batch, and TIP.
                               1: Only IP SUPs are used for demand, batch, and TIP.
                               2: Total SUPs used for demand and batch; IP SUPs used for TIP.
                               3: Total SUPs are used for demand and batch; TIP programs depend on VALTAB S indicator.
                               4: Only IP SUPs are used for demand and batch; TIP programs depend on VALTAB S indicator.""",
                                     new IntegerRangeRestriction(0, 4));

        // NOTXPCDESTAG is n/a // TODO SHARED

        putRestrictedConfigParameter(NPECTRL, INTEGER, new IntegerValue(1), true, false,
                                   """
                               Controls access to shared banks.
                               1: Prevents shared banks from being created with GAP of WRITE or EXECUTE.
                               2: Allows shared banks to be created with GAP equal to WRITE or EXECUTE.""",
                                     new IntegerRangeRestriction(1, 2));

        // OPASTHRTBF is n/a

        putRestrictedConfigParameter(OVRACC, STRING, new StringValue("INSTALLATION"), true, true,
                                     "Account number used for system runs.",
                                     new AccountIdRestriction());
        putRestrictedConfigParameter(OVRUSR, STRING, new StringValue("INSTALLATION"), true, true,
                                     "User-id for system runs.",
                                     new UserIdRestriction());

        /*
        putSettableConfigParameter(OWNEDRWKEYS, "owned_file_read_write_keys", false, true, true,
                                   "Indicates whether both ownership and read/write keys are enforced on owned files."); // TODO SENTRY
        */

        putSettableConfigParameter(PAGEJECT, BOOLEAN, TRUE, true, false,
                                   "If set, page eject occurs before processor calls unless overridden by @SETC.");

        putSettableConfigParameter(PAKOVF, BOOLEAN, FALSE, true, false,
                                   "If set, a console message is displayed if a removable disk allocation was rejected.");

        // PANICASGMNE is n/a

        /*
        putSettableConfigParameter(PASMAX, null, 0, true, false,
                                   "Max number of output and pass-off messages for a transaction. If 0, there is no limit."); // TODO TIP
        */

        // PBKDF2ITER is n/a... I think

        putSettableConfigParameter(PCHHDG, BOOLEAN, FALSE, false, true,
                                   "If true, we do NOT punch a heading card.");

        // PCTMAX is n/a

        putFixedConfigParameter(PGMFLMSWTIME, BOOLEAN, TRUE,
                                "If true, program file timestamps are MODSWTIME format - otherwise they are TDATE$.");

        // PRDDSTBOOT is n/a // TODO SHARED XPC

        putRestrictedConfigParameter(PRIRTP, INTEGER, ZERO, true, true,
                                     "Max number of transaction programs that can be initialized as resident.",
                                     new IntegerRangeRestriction(0, 1000));

        putRestrictedConfigParameter(PRIVAC, STRING, new StringValue("123456"), true, false,
                                     "Tape labeling blocks are not automatically read for runs with this account number.",
                                     new AccountIdRestriction());

        // PROGLOGINTER is n/a

        putRestrictedConfigParameter(PRSPMX, INTEGER, new IntegerValue(300), false, true,
                                     "Maximum line spacing permitted for each print request - max is 03777.",
                                     new IntegerRangeRestriction(1, 03777));

        putFixedConfigParameter(R2MSWTIME, BOOLEAN, TRUE,
                                "If true, R2 is loaded with MODSWTIME format. If false, it is loaded with TDATE$ format.");

        putRestrictedConfigParameter(RECSIZ, INTEGER, new IntegerValue(4), true, true,
                                     "Sectors per record for account file.",
                                     new IntegerRangeRestriction(1, 8));

        putSettableConfigParameter(REJCONFLTOPT, BOOLEAN, FALSE, true, false,
                                   "If true, subsequent assign attempts which include options not specified on initial assign are rejected.\n"
                                       + "If false, no subsequent assign image is rejected (except for A, C, T, and U conflicts).");

        putRestrictedConfigParameter(RELUNUSEDREM, INTEGER, ZERO, true, false,
                                   """
                               Action taken for unused initial reserve on removable disk files:
                               0: Unused allocated space is not released when the file is released.
                               1: Unused initial reserve > highest granule written is released.
                               2: Unused dynamic allocation > initial reserve and highest granule written is released.""",
                                     new EnumeratedRestriction(
                                         new Value[]{
                                             new IntegerValue(0),
                                             new IntegerValue(1),
                                             new IntegerValue(2), }));

        putRestrictedConfigParameter(RELUNUSEDRES, INTEGER, ZERO, true, false,
                                   """
                               Action taken for unused allocations on fixed disk.
                               0: Unused initial reserve is not released.
                               1: Unused initial reserve is released when the file is released.
                               2: Unused dynamic allocation above initial reserve is released.""",
                                     new EnumeratedRestriction(
                                         new Value[]{
                                             new IntegerValue(0),
                                             new IntegerValue(1),
                                             new IntegerValue(2), }));

        putSettableConfigParameter(REMBATCHLPI, BOOLEAN, FALSE, true, false,
                                   """
                               Default number of lines-per-inch for remote batch printers when * is specified in LPI field.
                               false: Remote batch printers use 8 LPI.
                               true: Remote batch printers use 6 LPI.
                               """);

        putSettableConfigParameter(REMTOP, BOOLEAN, TRUE, true, false,
                                   "Indicates whether the top separator page is printed by RSI remote batch printers.");

        // REQNBRPST0 is n/a
        // REQNBRPST1 is n/a

        putSettableConfigParameter(RESDUCLR, BOOLEAN, FALSE, true, false,
                                   "Purges information left in mass storage and GRS to prevent exposure between users or applications.");

        putSettableConfigParameter(RESTRICT, BOOLEAN, FALSE, true, false,
                                   "Restricts auto start of runs during SYS run.");

        putSettableConfigParameter(REWDRV, BOOLEAN, FALSE, false, true,
                                   "If true, operator is asked whether reserve drives should have EOF written and be rewound at recovery boot.");

        putRestrictedConfigParameter(RLTIME, INTEGER, new IntegerValue(12), false, true,
                                     "Max lock time in minutes for RL$ lock to be held.",
                                     new IntegerRangeRestriction(1, 60));

        putRestrictedConfigParameter(RPMU, INTEGER, ZERO, true, false,
                                   """
                               Controls whether MFD on removable packs is automatically updated.
                               0: No automatic DAD table updates are done for removable pack files. Such information is updated only when the file is @FREE'd.
                               1: Auto updates are done for files registered with TIP.
                               2: Auto updates are done for all files on removable packs.""",
                                     new IntegerRangeRestriction(0, 2));

        putSettableConfigParameter(RSIBTM, BOOLEAN, TRUE, true, false,
                                   "Indicates whether the bottom separator page is printed by RSI remote batch printers.");

        putRestrictedConfigParameter(RSICNT, INTEGER, new IntegerValue(8), false, true,
                                   "Maximum number of terminals for RSI including RSI$ remote, batch, and demand.",
                                     new IntegerRangeRestriction(1, 256));

        putRestrictedConfigParameter(RUNASGMNE, STRING, new StringValue("F2"), true, false,
                                     "Assign mnemonic for SYS$*RUN$.",
                                     new MnemonicRestriction(MNEMONIC_TABLE));

        // RUNDEDLVL is n/a

        putRestrictedConfigParameter(RUNINTRES, INTEGER, new IntegerValue(1), true, false,
                                     "Initial reserve used for creating SYS$*RUN$.",
                                     new IntegerRangeRestriction(0, 262143));

        putRestrictedConfigParameter(RUNMAXSIZ, INTEGER, new IntegerValue(10000), true, false,
                                     "Maximum size for SYS$*RUN$.",
                                     new IntegerRangeRestriction(100, 262143));

        putRestrictedConfigParameter(SACRDASGMNE, STRING, new StringValue("F"), true, false,
                                     "Assign mnemonic for creating SEC@ACR$ file.",
                                     new MnemonicRestriction(MNEMONIC_TABLE));

        putRestrictedConfigParameter(SACRDINTRES, INTEGER, new IntegerValue(2), true, false,
                                     "Initial reserve used for creating SEC@ACR$ file.",
                                     new IntegerRangeRestriction(0, 262143));

        putSettableConfigParameter(SAFHDG, INTEGER, new IntegerValue(1), false, true,
                                   "Number of blank lines between heading and first line of print.");

        // SB**** is n/a  TODO SIP
        // SCHKEYLVL - not supported for now
        // SCHKEYOPTS - not supported for now
        // SCPIPSUPS is n/a
        // SDYNDNPACK not supported // TODO SHARED

        putRestrictedConfigParameter(SECOFFDEF, STRING, null, true, false,
                                     "Specifies a new security officer user-id at boot time.",
                                     new UserIdRestriction());

        putSettableConfigParameter(SENTRY, BOOLEAN, FALSE, true, true,
                                   "Enables security levels beyond fundamental security as well as SENTRY control.");

        putSettableConfigParameter(SEPPAGE, BOOLEAN, FALSE, false, true,
                                   "Indicates whether the sequence number is printed on top and bottom six lines of separator pages.");

        putSettableConfigParameter(SFTIMESTAMP, BOOLEAN, FALSE, true, false,
                                   "Causes timestamps to appear on the console for START and FIN messages.");

        putRestrictedConfigParameter(SHDGSP, INTEGER, new IntegerValue(5), false, true,
                                     "Number of lines at top of standard page (including heading).",
                                     new IntegerRangeRestriction(0, 10));

        // SHRDFCDFLT not supported // TODO SHARED
        // SHRDMSAVL not supported // TODO SHARED
        // SHRDMSTRT not supported // TODO SHARED

        putSettableConfigParameter(SIMLUATECONS, BOOLEAN, TRUE, true, false,
                                   "If true, two leading spaces are inserted on all @@CONS read-only messages.");

        // SIPIO is n/a // TODO SIP

        putSettableConfigParameter(SITEFMTAIL, BOOLEAN, FALSE, true, true,
                                   "If true, a site-specific termination task produces the tail sheet.");

        putSettableConfigParameter(SKDATA, BOOLEAN, TRUE, false, true,
                                   "If true, skip-data is supported on I/O.");

        putSettableConfigParameter(SMALIN, BOOLEAN, TRUE, false, true,
                                   "Any printers with formatter changes which might produce position errors will cause a "
                                       + "console message to be printed at the end of the file.");

        putRestrictedConfigParameter(SMDTFASGMNE, STRING, new StringValue("F"), true, false,
                                   "Assign mnemonic used for creating SYS$*SMDTF$.",
                                     new MnemonicRestriction(MNEMONIC_TABLE));

        putRestrictedConfigParameter(SMDTFINTRES, INTEGER, new IntegerValue(1), true, false,
                                     "Initial reserve used for creating SMDTF$.",
                                     new IntegerRangeRestriction(0, 262142));

        // SPAREPACKID is not supported
        // SRFHGH is not supported

        putRestrictedConfigParameter(SSDENS, CHARACTER, new CharacterValue('S'), true, true,
                                     "Density for tape assignments used by certain system processors.",
                                     new EnumeratedRestriction(
                                         new Value[]{
                                             new CharacterValue('S'),
                                             new CharacterValue('H'),
                                             new CharacterValue('V')}));

        // SSDSOB is not supported

        putRestrictedConfigParameter(SSEQPT, STRING, new StringValue("T"), true, false,
                                     "System standard assign mnemonic for tape assignments for exec and other processors.",
                                     new MnemonicRestriction(MNEMONIC_TABLE));

        putSettableConfigParameter(SSPBP, BOOLEAN, TRUE, true, true,
                                   "Files are private by account instead of by project-id.");

        putSettableConfigParameter(SSPROTECT, BOOLEAN, FALSE, true, true,
                                   "Enables enforcement of subsystem entry point protection.");

        putRestrictedConfigParameter(STDBOT, INTEGER, new IntegerValue(3), false, true,
                                     "Number of blank lines at the bottom of a standard page.",
                                     new IntegerRangeRestriction(0, 10));

        putRestrictedConfigParameter(STDMSAVL, FLOAT, new FloatValue(3.00), true, false,
                                     "Percent of mass storage which should be available when ROLOUT completes.",
                                     new FloatRangeRestriction(1.0, 33.0));

        putRestrictedConfigParameter(STDMSTRT, FLOAT, new FloatValue(1.50), true, false,
                                     "ROLOUT starts when the percentage of accessible mass storage reaches or goes below this value.",
                                     new FloatRangeRestriction(0.05, 10.0));

        putSettableConfigParameter(STDPAG, INTEGER, new IntegerValue(66), false, true,
                                   "Total number of lines on a standard page");

        // STPAUL is n/a
        // SUA(x) needs to be thought about...

        putSettableConfigParameter(SUACURRENCY, CHARACTER, new CharacterValue('$'), true, false,
                                   "Currency indicator for SUAs on the log summary.");

        // SWGnBIAS is n/a
        // SWMANUFACTRD is n/a
        // SWQAFFLVL is n/a

        putRestrictedConfigParameter(SYMFBUF, INTEGER, new IntegerValue(224), true, false,
                                     "Size of symbiont buffers. Value must be a multiple of 224, and vary from 224 to 7168.",
                                     new IntegerRangeRestriction(224, 7168),
                                     new IntegerMultipleRestriction(224));
        putSettableConfigParameter(SYMJK, BOOLEAN, FALSE, true, false,
                                   "Indicates whether users may use the J and K options on @SYM");

        // SYSBASGMNE is n/a
        // SYSBINTRES is n/a

        putSettableConfigParameter(SYSTRMTSK, BOOLEAN, TRUE, true, false,
                                   "If true, the configured run termination task is scheduled for SYS at SYS FIN.");

        // TAPBUF is not supported - we do not do reel tapes

        putRestrictedConfigParameter(TDFALT, STRING, new StringValue("T"), true, false,
                                     "Default mnemonic for tape files.",
                                     new MnemonicRestriction(MNEMONIC_TABLE));

        /*
        putSettableConfigParameter(TEMPP2F, "quota_temp_ms_factor", 0, true, true,
                                   "Power-of-two factor for number of tracks defined as max limit allowed per quote group for temporary files.");
        */

        putRestrictedConfigParameter(TERMTASK, STRING, new StringValue(""), true, false,
                                     "Element name of a program in SYS$LIB$*RUN$ or SYS$*RUN$ which executes in all batch and demand runs" +
                                         " just before termination of the runs.",
                                     new ElementNameRestriction(true));

        putSettableConfigParameter(TFCMSG, BOOLEAN, FALSE, true, false,
                                   "Controls whether file control warning messages are issued.");

        putRestrictedConfigParameter(TFEXP, INTEGER, new IntegerValue(60), true, true,
                                     "Number of days which a file written on tape is read- or write-protected.",
                                     new IntegerRangeRestriction(0, 4095));

        putRestrictedConfigParameter(TFMAX, INTEGER, new IntegerValue(360), true, true,
                                     "Maximum number of days that can be specified in the expiration field of a tape @ASG image.",
                                     new IntegerRangeRestriction(0, 4095));

        /*
        putSettableConfigParameter(TFPMAX, null, 100, true, true,
                                   "Max number of permanent TIP files."); // TODO TIP
        putSettableConfigParameter(TFSEC, "tip_file_security", false, true, true,
                                   "Enables TIP file security."); // TODO TIP
        putSettableConfigParameter(TIMCTRL, null, false, true, true,
                                   "Enables TIMER control functions."); // TODO TIP
        putSettableConfigParameter(TIMDAYS, null, 0, true, true,
                                   "Number of future days which TIMER scheduling requests are kept. Max value is 127."); // TODO TIP
        putSettableConfigParameter(TIMINT, null, false, true, true,
                                   "Controls TIMER cleanup. If true, the TIMER file is cleared on all boots."
                                       + " Otherwise, the TIMER file is cleared only on initial boots."); // TODO TIP
        putSettableConfigParameter(TIMLNKBLKS, null, 0, true, true,
                                   "Number of link blocks contained in the TIMER system file for use when a day or minute block overflows."); // TODO TIP
        */

        putSettableConfigParameter(TIP, BOOLEAN, TRUE, true, true,
                                   "Controls whether TIP is enabled.");

        /*
        putSettableConfigParameter(TIPABSCALLSS, "tip_abs_call_ss", true, true, true,
                                   "Indicates whether basic mode transactions are loaded with extended mode stack banks."); // TODO TIP
        putSettableConfigParameter(TIPAGFILES, "tip_application_group_files", 0, true, true,
                                   "If true, we support the system max of 262,000 TIP file numbers and 262,000 TIP/Exec files."
                                       + " Otherwise, the maximum number of either value is 4095."); // TODO TIP
        */

        // TIPDUPRSL is not supported

        putRestrictedConfigParameter(TIPFDR, CHARACTER, CharacterValue.NUL, true, true,
                                   """
                               Controls TIP file directory recovery on system boots.
                               blank: The operator is given the choice of initializing or recovering the TIP file directory
                               "R": The TIP file directory is recovered
                               "I": The TIP file directory is initialized""",
                                     new EnumeratedRestriction(
                                         new Value[]{
                                             CharacterValue.NUL,
                                             new CharacterValue('I'),
                                             new CharacterValue('R'),
                                         }
                                     ));

        /*
        putSettableConfigParameter(TIPFSMSGLMT, "tip_fs_message_limit", 8, true, false,
                                   "Max number of Freespace messages displayed on the system console over a 6-second interval. Max is 500."); // TODO TIP
        */

        putSettableConfigParameter(TIPINIT, BOOLEAN, FALSE, true, true,
                                   "If true, TIP is initialized automatically. Otherwise, the operator is given the option of initializing TIP.");

        /*
        putSettableConfigParameter(TIPMEF, null, 100, true, true,
                                   "Max number of Exec files which can be registered with TIP file control."); // TODO TIP
        */

        // TODO TIPPK1, TIPPK2, TIPPK3, TIPPK4, TIPPK5, TIPPK6, TIPPK7, TIPPK8
        // TODO TIPRTPSDEACT
        // TODO TIPSECUSR
        // TODO TIPTPS
        // TODO TIPVIEWPRINT
        // TODO TIPWRD

        putSettableConfigParameter(TLAUTO, BOOLEAN, FALSE, true, false,
                                   "If true, all tapes assigned without J option are labeled when written.");

        // TLAVREBREC is not supported
        // TLEBCDIC is not supported

        putFixedConfigParameter(TLPRE4, BOOLEAN, FALSE,
                                "If true, tape labels are written in pre-version 4 format. If false, they are written in version 4 format.");

        putSettableConfigParameter(TLSIMP, BOOLEAN, TRUE, true, true,
                                   "If true, all tapes assigned without J option must be labeled.");

        putSettableConfigParameter(TLSUB, BOOLEAN, FALSE, true, true,
                                   "If true, reel number substitution is allowed on WRONG REEL, BAD LABEL, and IS TAPE messages.");

        putSettableConfigParameter(TMPFILETOMEM, BOOLEAN, FALSE, true, false,
                                   "When set, all temporary fixed file assignments are FMEM or DMEM, making the files memory files.");

        // TODO TMSEC (TIP)

        putSettableConfigParameter(TPBLKNBR, BOOLEAN, FALSE, true, true,
                                   "If true, all tapes not specified as BLKOFF have block numbering turned on.");

        putSettableConfigParameter(TPDCOMP, BOOLEAN, FALSE, true, true,
                                   "If true, all tapes not specified as CMPOFF have data compression turned on.");

        // TODO TPDEV1 : TPDEV8 (TIP)
        // TPDIRI is n/a

        putRestrictedConfigParameter(TPFMAXSIZ, INTEGER, ZERO, true, false,
                                     "Initial max size of TPF$ files.",
                                     new IntegerRangeRestriction(0, 252143));

        putRestrictedConfigParameter(TPFTYP, STRING, new StringValue("F"), true, false,
                                     "Equipment type for TPF$ files.",
                                     new MnemonicRestriction(MNEMONIC_TABLE));

        // TODO TPGF1 : TPFG11 (TIP)
        // TODO TPG0L1, TPG0L2 (TIP)
        // TODO TPG1L1, TPG1L2 (TIP)
        // TODO TPG2L1, TPG2L2 (TIP)
        // TODO TPG3L1, TPG3L2 (TIP)
        // TODO TPLIB (TIP)
        // TODO TPMMSK (TIP)
        // TODO TPMPRDMSK (TIP)
        // TODO TPMSTATE (TIP)
        // TODO TPMUSRMSK (TIP)
        // TODO TPNRTN (TIP)

        putSettableConfigParameter(TPOWN, BOOLEAN, FALSE, true, true,
                                   "If true, access to a tape is restricted by the account number.");

        // TODO TPQUAL (TIP)
        // TODO TPRKEY (TIP)
        // TODO TPSDV1 : TPSDV4 (TIP)
        // TODO TPSNUM (TIP)
        // TODO TPSPK1 : TPSPK4 (TIP)
        // TODO TPS0L1, TPS0L2 (TIP)
        // TODO TPS1L1, TPS1L2 (TIP)
        // TODO TPS2L1, TPS2L2 (TIP)
        // TODO TPS3L1, TPS3L2 (TIP)
        // TODO TPWKEY (TIP)

        putSettableConfigParameter(TRMXCO, BOOLEAN, FALSE, true, false,
                                   "If true, a run is terminated when the maximum number of cards are punched.");
        putSettableConfigParameter(TRMXPO, BOOLEAN, FALSE, true, false,
                                   "If true, a run is terminated when the maximum number of pages are printed.");
        putSettableConfigParameter(TRMXT, BOOLEAN, FALSE, true, false,
                                   "If true, a run is terminated when the maximum amount of time elapses, as specified on the @RUN statement.");

        // TODO TRMXVT (TIP)

        putSettableConfigParameter(TSS, BOOLEAN, TRUE, true, true,
                                   "Enables terminal security capabilities.");

        // TVSL is n/a (for now)
        // UNIVAC is n/a

        putSettableConfigParameter(UNLRELVERIFY, BOOLEAN, FALSE, true, false,
                                   "If set, the reel-id of an unlabeled tape is verified before the first I/O is processed.");

        putRestrictedConfigParameter(USERASGMNE, STRING, new StringValue("F"), true, false,
                                     "Assign mnemonic used to create SYS$*SEC@USERID$ file.",
                                     new MnemonicRestriction(MNEMONIC_TABLE));

        putRestrictedConfigParameter(USERINTRES, INTEGER, new IntegerValue(1), true, false,
                                     "Initial reserve used to create SYS$*SEC@USERID$ file.",
                                     new IntegerRangeRestriction(0, 262143));

        putSettableConfigParameter(USERON, BOOLEAN, FALSE, true, true,
                                   "Controls quota enforcement by user-ids.");

        // VRTQLVL is n/a
        // VTHRDISK is n/a
        // VTHSHARE is n/a
        // VTHSUBS is n/a
        // VTHXPC is n/a
        // TODO VTFLAB (TIP)

        putRestrictedConfigParameter(WDFALT, STRING, new StringValue("D"), true, false,
                                     "Default mnemonic for word-addressable files.",
                                     new MnemonicRestriction(MNEMONIC_TABLE));

        // XCACHESECFIL is n/a // TODO SHARED
        // XPDLCLINTV is n/a // TODO SHARED
        // XPDLCLSTRT is n/a // TODO SHARED
        // XPDLOCAL is n/a // TODO SHARED
        // XPDMAXIO is n/a // TODO SHARED
        // XPDSHARED is n/a // TODO SHARED
        // XPDSHRDINTV is n/a // TODO SHARED
        // XPDSHRDSTRT is n/a // TODO SHARED

        putSettableConfigParameter(ZOPTBATCHREJ, BOOLEAN, TRUE, true, false,
                                   """
                                             If true, a Z option on facility images for a batch run causes the image to be rejected.
                                             If false, a Z option is ignored for batch runs.""");
    }

    static {
        putMnemonicInfo("F", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F2", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F3", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F4", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F14", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F14C", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F14D", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F17", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F24", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F25", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F30", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F33", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F34", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F36", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F40", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F50", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F50C", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F50F", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F50M", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F51", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F53", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F54", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F59F", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F59M", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F60", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F70C", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F70F", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F70M", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F80C", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F80M", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F81", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F81C", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F94", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("F94C", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("FCS", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("FMD", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("FMEM", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("FSCSI", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("FSSD", MnemonicType.SECTOR_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        //
        putMnemonicInfo("D", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D2", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D3", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D4", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D14", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D14C", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D14D", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D17", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D24", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D25", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D30", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D33", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D34", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D36", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D40", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D50", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D50C", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D50F", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D50M", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D51", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D53", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D54", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D59F", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D59M", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D60", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D70C", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D70F", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D70M", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D80C", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D80M", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D81", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D81C", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D94", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("D94C", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("DCS", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("DMD", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("DMEM", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("DSCSI", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        putMnemonicInfo("DSSD", MnemonicType.WORD_ADDRESSABLE_DISK, new EquipType[]{EquipType.FILE_SYSTEM_DISK});
        //
        putMnemonicInfo("T", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("U9", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("U9H", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("U9S", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("U9V", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("U47", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("U47LM", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("U47M", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("U47NL", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("HIC", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("HIC40", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("HIC51", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("HIC52", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("HICM", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("LCART", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("NLCART", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("DLT70", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("LTO", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("LTO3", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("LTO4", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("26N", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("22D", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("24D", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("30D", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("HIC", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("HIC51", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("HIC52", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("HICL", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("HICSL", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("HIS98", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("HIS98B", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("HIS98C", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("HIS98D", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("T10KA", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("VT", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("VTH", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
        putMnemonicInfo("DVDTP", MnemonicType.TAPE, new EquipType[]{EquipType.FILE_SYSTEM_TAPE});
    }

    // hardware configuration ------------------------------------------------------------------------------------------
    // this is the default - if there is any hardware configuration in the loaded configuration file
    // (see updateFromFile()) then this configuration is completely overridden.

    // -----------------------------------------------------------------------------------------------------------------

    public Object getConfigValue(final Tag tag) {
        var param = CONFIG_PARAMETERS.get(tag.name());
        if ((param != null) && param.hasEffectiveValue()) {
            return param.getEffectiveValue();
        } else {
            return null;
        }
    }

    public Boolean getBooleanValue(final Tag tag) {
        var value = getConfigValue(tag);
        if (value instanceof BooleanValue bv) {
            return bv.getValue();
        } else {
            return null;
        }
    }

    public Double getFloatValue(final Tag tag) {
        var value = getConfigValue(tag);
        if (value instanceof FloatValue fv) {
            return fv.getValue();
        } else {
            return null;
        }
    }

    public Long getIntegerValue(final Tag tag) {
        var value = getConfigValue(tag);
        if (value instanceof IntegerValue iv) {
            return iv.getValue();
        } else {
            return null;
        }
    }

    public String getStringValue(final Tag tag) {
        var value = getConfigValue(tag);
        if (value instanceof StringValue sv) {
            return sv.getValue();
        } else {
            return null;
        }
    }

    private static void putFixedConfigParameter(
        final Tag tag,
        final ValueType valueType,
        final Value defaultValue,
        final String description
    ) {
        var cp = new FixedConfigParameter(tag, valueType, defaultValue, description);
        CONFIG_PARAMETERS.put(tag.name(), cp);
    }

    private static void putRestrictedConfigParameter(
        final Tag tag,
        final ValueType valueType,
        final Value defaultValue,
        final boolean isPrivileged,
        final boolean isRebootRequired,
        final String description,
        final Restriction ... restrictions
    ) {
        var cp = new RestrictedConfigParameter(tag,
                                               valueType,
                                               defaultValue,
                                               isPrivileged,
                                               isRebootRequired,
                                               description,
                                               restrictions);
        CONFIG_PARAMETERS.put(tag.name(), cp);
    }

    private static void putSettableConfigParameter(
        final Tag tag,
        final ValueType valueType,
        final Value defaultValue,
        final boolean isPrivileged,
        final boolean isRebootRequired,
        final String description
    ) {
        var cp = new SettableConfigParameter(tag, valueType, defaultValue, isPrivileged, isRebootRequired, description);
        CONFIG_PARAMETERS.put(tag.name(), cp);
    }

    private static void putMnemonicInfo(
        final String mnemonic,
        final MnemonicType type,
        final EquipType[] equipmentTypes
    ) {
        var mi = new MnemonicInfo(mnemonic, type, List.of(equipmentTypes));
        MNEMONIC_TABLE.put(mi._mnemonic, mi);
    }

    public static MnemonicType getMnemonicType(final String mnemonic) {
        var mi = MNEMONIC_TABLE.get(mnemonic);
        return mi == null ? null : mi._type;
    }

    // configuration file handling -------------------------------------------------------------------------------------

    public String[] parseArgument(
        final Parser parser
    ) throws SyntaxException {
        String key;
        String value = null;

        key = parser.parseUntil("= ");
        if (key.isEmpty()) {
            return null;
        }

        if (parser.peekNext() == '=') {
            parser.skipNext();
            value = parser.parseUntil(" ");
        }

        if (value == null || value.isEmpty()) {
            return new String[]{ key };
        } else {
            return new String[]{ key, value };
        }
    }

    public Value parseLiteral(
        final Parser parser
    ) throws SyntaxException {
        if (parser.atEnd()) {
            throw new SyntaxException("Missing value for parameter");
        }

        Value obj = CharacterValue.parse(parser);
        if (obj == null) obj = StringValue.parse(parser);
        if (obj == null) obj = BooleanValue.parse(parser);
        if (obj == null) obj = IntegerValue.parse(parser);
        if ((obj == null) || !parser.atEnd()) {
            throw new SyntaxException();
        }
        return obj;
    }

    public Collection<StringValue> parseIdentifierList(
        final Parser parser
    ) throws Parser.SyntaxException {
        var list = new LinkedList<StringValue>();
        while (!parser.atEnd()) {
            try {
                var ident = parser.parseIdentifier(12, " ,");
                list.add(new StringValue(ident));
                if (parser.peekNext() == ' ') {
                    break;
                }
                parser.skipNext();
            } catch (Parser.NotFoundException pex) {
                return list.isEmpty() ? null : list;
            }
        }

        return list;
    }

    public String parseNodeType(
        final Parser parser
    ) throws SyntaxException {
        if (!Character.isAlphabetic(parser.peekNext())) {
            return null;
        }

        var sb = new StringBuilder();
        sb.append(parser.next());
        while (!parser.atEnd()) {
            var ch = parser.peekNext();
            if (ch == ' ') {
                break;
            } else if (!Character.isAlphabetic(ch) && !Character.isDigit(ch) && (ch != '-')) {
                throw new SyntaxException("Invalid character in identifier");
            }
            sb.append(parser.next());
        }

        if (sb.isEmpty()) {
            return null;
        } else if (sb.length() > 12) {
            throw new SyntaxException("Invalid identifier");
        }

        return sb.toString();
    }

    public void processNode(
        final Parser parser
    ) throws SyntaxException {
        // NODE name [ IS ] type [ [ AND ] CONNECTS [ TO ] name_list ] [ argument ... ]
        // name_list:
        //   name [ , name ]*
        // argument_list:
        //   argument [ space argument]*
        // argument:
        //   key[=value]
        parser.skipSpaces();
        try {
            var nodeName = parser.parseIdentifier(6, " ");
            parser.skipSpaces();
            if (parser.parseToken("IS")) {
                parser.skipSpaces();
            }

            var nodeType = parseNodeType(parser);
            parser.skipSpaces();

            if (parser.parseToken("AND")) {
                parser.skipSpaces();
            }

            // child nodes
            Collection<Node> childNodes = new LinkedList<>();
            if (parser.parseToken("CONNECTS")) {
                parser.skipSpaces();
                if (parser.parseToken("TO")) {
                    parser.skipSpaces();
                }

                var childNames = parseIdentifierList(parser);
                for (var name : childNames) {
                    var child = NODES.get(name.getValue());
                    if (child == null) {
                        throw new SyntaxException("Unknown node: " + name);
                    }
                    childNodes.add(child);
                }
                parser.skipSpaces();
            }

            var node = Node.createNode(nodeName, nodeType);
            if (node == null) {
                throw new SyntaxException("Unrecognized node type " + nodeType);
            }

            childNodes.forEach(node::addSubordinate);

            // arguments
            var keyVal = parseArgument(parser);
            while (keyVal != null) {
                if (keyVal.length == 1) {
                    node.addArgument(keyVal[0], null);
                } else {
                    node.addArgument(keyVal[0], keyVal[1]);
                }
                parser.skipSpaces();
                keyVal = parseArgument(parser);
            }

            if (!parser.atEnd()) {
                throw new SyntaxException("Syntax error at end of NODE statement");
            }

            NODES.put(node.getName(), node);
        } catch (Parser.NotFoundException pex) {
            throw new SyntaxException("Missing node name");
        } catch (Parser.SyntaxException pex) {
            throw new SyntaxException(pex.getMessage());
        }
    }

    public void processSymGroup(
        final Parser parser
    ) throws SyntaxException {
        throw new SyntaxException("SYMGROUP not yet implemented"); // TODO
    }

    public void processSymQueue(
        final Parser parser
    ) throws SyntaxException {
        throw new SyntaxException("SYMQUEUE not yet implemented"); // TODO
    }

    public void processParameter(
        final Parser parser
    ) throws ConfigurationException {
        parser.skipSpaces();
        var tagString = parser.parseUntil(" =");
        if (tagString == null) {
            throw new SyntaxException("Missing TAG specification");
        }

        var param = CONFIG_PARAMETERS.get(tagString.toUpperCase());
        if (param == null) {
            throw new SyntaxException("Unknown TAG specification: " + tagString);
        }

        parser.skipSpaces();
        var value = parseLiteral(parser);
        if (value == null) {
            throw new SyntaxException(tagString + " TAG missing value");
        }

        System.out.printf("Setting %s to %s\n", tagString, value);
        param.setValue(value);
    }

    public boolean updateFromFile(final String filename) throws IOException {
        System.out.println("Processing config file " + filename);
        boolean err = false;
        try(var reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                var lx = line.indexOf('#');
                if (lx >= 0) {
                    line = line.substring(0, lx);
                }
                line = line.trim();

                if (!line.isEmpty()) {
                    try {
                        var p = new Parser(line);
                        var token = p.parseUntil(" =").toUpperCase();
                        switch (token) {
                            case "NODE" -> processNode(p);
                            case "SYMGROUP" -> processSymGroup(p);
                            case "SYMQUEUE" -> processSymQueue(p);
                            case "PARAMETER" -> processParameter(p);
                            default ->
                                throw new SyntaxException("Unrecognized token " + token);
                        }
                    } catch (ConfigurationException ex) {
                        System.err.printf("ERROR[%d]:%s\n", lineNumber, ex.getMessage());
                        err = true;
                    }
                }
                lineNumber++;
            }
        }

        // Ensure all effective values are present and legal
        for (var param : CONFIG_PARAMETERS.values()) {
            try {
                param.checkValue();
            } catch (ConfigurationException ex) {
                System.err.printf("ERROR:Parameter %s:%s\n", param.getTag(), ex.getMessage());
            }
        }

        return !err;
    }
}
