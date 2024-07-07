/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration;

import com.bearsnake.komodo.kexec.configuration.parameters.Parameter;
import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.configuration.parameters.FixedConfigParameter;
import com.bearsnake.komodo.kexec.configuration.parameters.RestrictedConfigParameter;
import com.bearsnake.komodo.kexec.configuration.parameters.SettableConfigParameter;
import com.bearsnake.komodo.kexec.configuration.restrictions.IntegerRangeRestriction;
import com.bearsnake.komodo.kexec.configuration.restrictions.MnemonicRestriction;
import com.bearsnake.komodo.kexec.configuration.restrictions.PrimeNumberRestriction;
import com.bearsnake.komodo.kexec.configuration.restrictions.Restriction;
import com.bearsnake.komodo.kexec.exceptions.KExecException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.bearsnake.komodo.kexec.configuration.parameters.Tag.*;

public class Configuration {

    private static final Map<String, Parameter> CONFIG_PARAMETERS = new TreeMap<>();
    private static final Map<String, MnemonicInfo> MNEMONIC_TABLE = new HashMap<>();

    // discrete configuration values ------------------------------------------------------------------------------------

    static {
        // AACOUNTER is n/a
        // AATIPSYSFILE is n/a
        putRestrictedConfigParameter(ACCTINTRES, 0, true, true,
                                     "Initial reserve used when creating the SYS$*ACCOUNT$R1 and SEC@ACCTINFO files." +
                                         " The value must be between 0 and 4096 (tracks).",
                                     new IntegerRangeRestriction(0, 4096));

        putRestrictedConfigParameter(ACCTASGMNE, "F", true, true,
                                     "Assign mnemonic to be used when cataloging SYS$*ACCOUNT$R1 and SEC@ACCTINFO files.",
                                     new MnemonicRestriction(MNEMONIC_TABLE));

        putFixedConfigParameter(ACCTMSWTIME, true,
                                "If true, entries in the account file are stored in MODSWTIME. Otherwise, they are stored in TDATE$ format.");

        putRestrictedConfigParameter(ACCTON, 2, true, true,
                                     """
                                        Controls the quota level:
                                        0: Account file turned off
                                        1: Account file on, and account verification is enabled
                                        2: Account file on, and account verification and accumulation are enabled
                                        3: Account file on, account verification, accumulation, and account-level quote are enforced
                                        4: Account file on, account verification, accumulation, account-level quote, and run-level quota are enforced.""",
                                     new IntegerRangeRestriction(0, 4));

        putSettableConfigParameter(AFICM, true, false, true,
                                   "Determines whether divide fault interrupts are enabled.");

        // ALATXR is n/a for now

        putSettableConfigParameter(APLBDIGT4K, true, false, false,
                                   "If false, only the Linking System can create application-level banks with BDIs > 4095.\n"
                                       + "If true, user programs and applications are also allowed to create code and data banks with BDIs > 4095.");

        putRestrictedConfigParameter(APLDYNBDS, 04000, false, true,
                                     "Fixed number of bank descriptors to be dynamically allocated in the level 1 BDT."
                                         + " Note that MAXBDI + APLDYNBDS must be < 32766.",
                                     new IntegerRangeRestriction(0, 32766));

        putFixedConfigParameter(ASGSCN, false,
                                "If true, facility pre-scan code is enabled. If false, runs are opened without pre-scanning.");

        putSettableConfigParameter(ATATPASSENA, false, true, false,
                                   "If true, @@PASSWD is allowed.");

        putRestrictedConfigParameter(ATOCL, 01011, false, true,
                                     "Account file look-up table length (must be a prime number).",
                                     new PrimeNumberRestriction());






        /*

        putSettableConfigParameter(BATCHXMODES, "batch_run_x_options_modes", 0, true, false,
                                   """
                               0: SSRUNXOPT is required for @START and ST specifying X options, but not remote batch runs.
                               1: SSRUNXOPT is required for all X option specifications.
                               2: SSRUNXOPT not required for remote batch runs specifying X option; \
                               @START and ST rely on QUOTA capability of the deadline being given to the run
                               3: SSRUNXOPT not required for remote batch run, @START, or ST specifying X option.""");

        // BLOKCK not supported - we don't (currently) support open-reel tapes

        putFixedConfigParameter(BYFACHELDRUN, false,
                                "If true, lower priority runs may be opened when higher priority runs are held for facilities.");
        putSettableConfigParameter(CATON, false, true, true,
                                   "Enables quota enforcement for cataloged ffiles.");
        putSettableConfigParameter(CATP2F, 0, true, true,
                                   "Power-of-two factor for number of tracks defined as max limit allowed per quota group.");
        putSettableConfigParameter(CBUFCT, 32, true, true,
                                   "Length in words of main storage COMPOOL blocks");
        putSettableConfigParameter(CHKSUM, false, true, true,
                                   "True to perform checksum on mass storage COMPOOL blocks.");
        // CKRSFILEVER not supported - we don't currently do checkpoint/restart
        putSettableConfigParameter(CMPAN1, 50, true, false,
                                   "Percentage of COMPOOL available to trigger COMPOOL panic level 1");
        putSettableConfigParameter(CMPAN2, 20, true, false,
                                   "Percentage of COMPOOL available to trigger COMPOOL panic level 2");
        putSettableConfigParameter(CMPAN3, 10, true, false,
                                   "Percentage of COMPOOL available to trigger COMPOOL panic level 3");
        putSettableConfigParameter(CMPDIS, false, true, false,
                                   "If true, COMPOOL is disabled.");
        putSettableConfigParameter(CMPMAX, 0, true, true,
                                   "Max number of COMPOOL blocks per output message - if 0, there is no limit.");
        // CONTIM - not supported (no SIP)
        putSettableConfigParameter(COREFILE, false, true, true,
                                   "If true, KONS main storage file is available.");
        putSettableConfigParameter(CSHRCV, true, true, false,
                                   "If true, runs are held on a recovery boot until CS A is entered.");
        putSettableConfigParameter(CTLIMGSP, 0, true, false,
                                   """
                               If zero and PAGEJECT is true, a page eject occurs before processor statements are printed.
                               If zero and PAGEJECT is false, line spacing is three lines for all control statements.
                               Values 1-63 indicate the number of lines printed before control statements.
                               """);
        putSettableConfigParameter(C32NBR, 30, true, true,
                                   "Number of primary mass storage COMPOOL blocks.");
        putSettableConfigParameter(C82NBR, 30, true, true,
                                   "Number of secondary mass storage COMPOOL blocks.");
        putSettableConfigParameter(DAYBLKS, 56, true, true,
                                   "Number of words in a day block on the TIMER system file.");
        // DBLMFDSEARCH not supported since we don't do directories
        // DCLUTS not supported since we don't do lookup table entries
        // DCMAXRERR not supported - we don't implement DC keyin
        // DCSPLTDAD not supported - we don't implement DC keyin
        putSettableConfigParameter(DEDLIN, true, false, true,
                                   "true to enable deadline runs and switching levels.");
        // DEDRUNn not supported
        putSettableConfigParameter(DELAYSOL, false, true, true,
                                   "If true, solicitation period is delayed between invalid sign-on attempts.");
        putSettableConfigParameter(DELRMPRT, false, false, false,
                                   "Indicates whether print files for removed (RM) runs should be printed.");
        putSettableConfigParameter(DEMTOP, true, true, false,
                                   "Indicates whether the top separator page is to be printed by RSI demand batch printers.");
        putSettableConfigParameter(DEQIAD, false, true, false,
                                   "If true, recoverable COMPOOL transactions if all reads/writes to TIP files are done without after-looks.");
        // DESMIPS not supported
        // DIRSELECT not supported - we don't do directories
        // DISPNOWAIT not supported - I don't think we're going to implement a dispatcher
        // DIVINTERVAL n/a
        putSettableConfigParameter(DLOCASGMNE, "F", true, false,
                                   "Assign mnemonic used to create SYS$*DLOC$.");
        // DLTDCOMP not supported - we don't do DLT tapes
        putSettableConfigParameter(DMPAPLVAL, 1, true, false,
                                   """
                               Indicates the application-level subsystem banks to be included in system dumps.
                               0: No banks included
                               1: Application level home subsystem which contains the common banks
                               2: All application level subsystems""");
        putSettableConfigParameter(DYNDNPACK, false, false, true,
                                   "If true, a DN PACK is automatically issued when MFD I/O to a removable disk fails.");
        // ECHOREM - not sure if we need to think about this; what constitutes a remote console?
        putSettableConfigParameter(ERTDATE$OFF, false, true, false,
                                   "If true, ER TDATE$ fails with type 04 code 03 cgy type 012.");

        // EXPTRACE is n/a
        // EXTDDCSYNC is not supported
        // FCACHDEFAULT is not supported
        // FCDBSZ is not supported
















        putSettableConfigParameter(FCMXLK, null, 5, true, true,
                                   "Limits the number of TIP locks which a program can hold.");
        // FCNUSRDADBNK is n/a
        putSettableConfigParameter(FILIMAGCTRL, "file_ecl_control", false, true, false,
                                   "If true, any batch runstream from an input symbiont or @RUN,/B with an @FILE image is rejected."
                                       + " Otherwise, @FILE and @ENDF images are processed.");
        putSettableConfigParameter(FNSCR, null, 0, true, true,
                                   "Maximum number of files reserved for scratch file assignment.");
        putSettableConfigParameter(FNTMP, null, 0, true, true,
                                   "Maximum number of files reserved for temporary file assignment.");
        putSettableConfigParameter(FNTRN, null, 0, true, true,
                                   "Maximum number of permanent fixed files that can be used as training files.");
        putSettableConfigParameter(FORCETIPINIT, "force_tip_initialization", false, true, false,
                                   "Specifies one or more TIP parameters has changed, indicating a TIP initializaiton must be forced on next boot.");
        // FQ$MFP is n/a
        putSettableConfigParameter(FREESPACE, null, false, true, true,
                                   "Indicates whether TIP freespace functions are available.");
        // FSMSWTIME is always true
        putSettableConfigParameter(GCCMIN, null, 0, false, true,
                                   "Indicates the maximum number of terminals allowed concurrently active via RSI$."
                                       + " If GCCMIN > 0 then RSICNT must be > 0.");
        putSettableConfigParameter(GENFASGMNE, "genf_file_asg_mnemonic", "F", true, false,
                                   "Assign mnemonic used to create SYS$*GENF$.");
        putSettableConfigParameter(GENFINTRES, "genf_file_initial_reserve", 1000, true, false,
                                   "Initial reserve used to create SYS$*GENF$. Must be between 0 and 4096.");
        // HICDCOMP not supported - we don't do HIC tapes
        // HISDCOMP not supported - we don't do HIS tapes
        // HM2USER not supported
        putSettableConfigParameter(HVTIP, "hvtip_library_max", 0, true, true,
                                   "Max number of HVTIP libraries.");
        putSettableConfigParameter(HVTIPMACT, "allow_hvtip_multi_activity", false, true, true,
                                   "Controls whether HVTIP programs can have multiple activities during calls or transfers between HVTIP banks.");
        putSettableConfigParameter(INHBAUTOALLC, "inhibit_tip_auto_allocation", false, true, true,
                                   "Controls whether auto allocation can occur during TIP/Exec file writes.");
        putSettableConfigParameter(INHBTRANSM, "inhibit_transmission", false, true, false,
                                   "If true, a user is not allowed to use @@TM or @SYM, or to transmit unsolicited data to terminals.");
                                   */

        putSettableConfigParameter(IODBUG, false, false, true,
                                   "If true, all debug aids are enabled for I/O.");

        /*
        putSettableConfigParameter(IOMAXRTIME, "io_maximum_recovery_time", 0, true, false,
                                   "Maximum number of seconds to allow I/O timeout recovery per retry (0-63)."); // TODO maybe not supported
        // IOTASGMNE is n/a
        // IOTFLXTRAN is n/a
        // IOTINTRES is n/a
        // IOTMAXCYC is n/a
        // IOTMAXSIZ is n/a
        // IPIPRE not supported - I think
        putSettableConfigParameter(KONSBL, "u3_block_length", 0, true, true,
                                   "Length in words of a U3 KONS block.");
        putSettableConfigParameter(KONSEC, "u1_area_length", 4, true, true,
                                   "Length in words of the write-protected U1 area of KONS.");
        putSettableConfigParameter(KONSFL, "length_of_kons_areas", 0, true, true,
                                   "Length in words of U1, U2, U3, and security directory areas of KONS.");
        putSettableConfigParameter(KONSPW, "u3_password_required", false, true, true,
                                   "If true, a password is required for access to a U3 block.");
        putSettableConfigParameter(KONU3L, "u3_user_area_length", 0, true, true,
                                   "Length in words of KONS U3 area.");
        putSettableConfigParameter(KSECNB, "u3_security_entries", 0, true, true,
                                   "Number of entries in the security directory for U3.");
        putSettableConfigParameter(LABOUT, "label_print_output", false, true, false,
                                   "If true, security labels and sequence numbers are printed.");
        putSettableConfigParameter(LABPAG, "label_every_page", false, true, false,
                                   "If true, security labels appear on every page of printouts - otherwise, labels appear only on banner and trailer pages.");
        putFixedConfigParameter(LARGEFILES, "large_files_enabled", true,
                                "If true, large files can be allocated. This slightly alters the format of certain MFD entries.");
        putSettableConfigParameter(LIBASGMNE, "lib_file_asg_mnemonic", "F", true, false,
                                   "Assign mnemonic used to create SYS$*LIB$.");
        putSettableConfigParameter(LIBINTRES, "lib_file_initial_reserve", 0, true, false,
                                   "Initial reserve used to create SYS$*LIB$. Must be between 0 and 262143.");
        putSettableConfigParameter(LIBMAXSIZ, "lib_file_max_size", 99999, true, false,
                                   "Maximum size for SYS$*LIB$.");
        // LOG*** are all n/a
        putSettableConfigParameter(LOOKAHDRND, "allocation_look_ahead_random", 1, true, false,
                                   "Number of tracks to allocate (beyond initial reserve) when allocating space - 1 to 10000.");
        putSettableConfigParameter(LOOKAHDSEQ, "allocation_look_ahead_sequential", 32, true, false,
                                   "Number of tracks to allocate when allocating space at or beyond the end of a file.");
        putSettableConfigParameter(LNKBLKS, null, 28, true, true,
                                   "Number of words in a link block on the TIMER system file.");
        // LTODCOMP not supported - we don't do LTO
        putSettableConfigParameter(MACHGENPASS, "machine_generated_passwords", false, true, false,
                                   "If true, machine-generated passwords are enabled.");
        putSettableConfigParameter(MAXATMP, "max_sign_on_attempts", 5, true, false,
                                   "Max number of sign-on attempts a user is allowed to successfully sign on to a host.");
        putSettableConfigParameter(MAXBDI, null, 06061, false, true,
                                   "Max number of alternate file common banks.");
        putSettableConfigParameter(MAXCRD, "default_max_cards", 100, true, false,
                                   "Max number of cards for @RUN images which do not specify max cards.");
        // MAXDSP is not supported
        putSettableConfigParameter(MAXGRN, "default_max_granules", 256, true, false,
                                   "Max number of granules assigned to a file if not specified.");
        putSettableConfigParameter(MAXLOG, "max_user_log_entries", 10000, true, false,
                                   "Max number of log entries which can be created by a user during one task.");
        // MAXMIPS is not supported
        putSettableConfigParameter(MAXOPN, null, 6, false, true,
                                   "Maximum number of batch runs allowed open at any time.");
        putSettableConfigParameter(MAXPAG, "default_max_pages", 100, true, false,
                                   "Max number of pages for @RUN images which do not specify max cards.");
        putSettableConfigParameter(MAXPASSDAY, "default_max_days_password", 90, true, false,
                                   "Max number of days before a password must be replaced.");
        putSettableConfigParameter(MAXPASSLEN, "max_password_length", 18, true, false,
                                   "Maximum number of characters allowed for a password.");
        putSettableConfigParameter(MAXTIM, "default_max_run_time", 10, true, false,
                                   "Maximumn run time if not specified on @RUN image.");
        putSettableConfigParameter(MAXTUX, null, 30, false, false,
                                   "Seconds before the MAXTIM contingency is honored.");
        putSettableConfigParameter(MBUFCT, null, 56, true, true,
                                   "Length in words of primary and secondary mass storage COMPOOL blocks.");
        putSettableConfigParameter(MDFALT, "default_ms_asg_type", "F", true, false,
                                   "Mnemonic to be used when not specified on @CAT or @ASG");
        // MEMFLSZ not supported
        putFixedConfigParameter(MFDMSWTIME, "std_mfd_modified_swtime", true,
                                "If true, MFD timestamps are stored in MODSWTIME format. If false, they are stored in TDATE$ format.");
        // MFDONXPCSTD is n/a
        // MFDONSPCSHR is n/a
        // MFDSHMSWTIME is n/a
        putSettableConfigParameter(MINBLKS, null, 28, true, true,
                                   "Number of words in a minute block on the TIMER system file.");
        // MINMPS is n/a
        putSettableConfigParameter(MINPASSDAY, "default_min_days_password", 1, true, false,
                                   "Minimum number of days before a password may be replaced.");
        putSettableConfigParameter(MINPASSLEN, "min_password_length", 8, true, false,
                                   "Minimum number of characters for a new password.");
        putSettableConfigParameter(MSTRACC, null, null, true, false,
                                   "Master account - if null, the operator will be prompted at boot time.");
        // MMGRMSWTIME if we do media mgr, we'll always use modified SWTIME
        putSettableConfigParameter(MXTMIPSUPS, "max_time_ip_only", 0, true, false,
                                   """
                               Indicates method of calculating max time check.
                               0: Total SUPs used for demand, batch, and TIP.
                               1: Only IP SUPs are used for demand, batch, and TIP.
                               2: Total SUPs used for demand and batch; IP SUPs used for TIP.
                               3: Total SUPs are used for demand and batch; TIP programs depend on VALTAB S indicator.
                               4: Only IP SUPs are used for demand and batch; TIP programs depend on VALTAB S indicator.""");
        // NOTXPCDESTAG is n/a
        putSettableConfigParameter(NPECTRL, "npe_control", 1, true, false,
                                   """
                               Controls access to shared banks.
                               1: Prevents shared banks from being created with GAP of WRITE or EXECUTE.
                               2: Allows shared banks to be created with GAP equal to WRITE or EXECUTE.""");
        // OPASTHRTBF is n/a
        putSettableConfigParameter(OVRACC, "overhead_account_nbr", "INSTALLATION", true, true,
                                   "Account number used for system runs.");
        putSettableConfigParameter(OVRUSR, "overhead_userid", "INSTALLATION", true, true,
                                   "User-id for system runs.");
        putSettableConfigParameter(OWNEDRWKEYS, "owned_file_read_write_keys", false, true, true,
                                   "Indicates whether both ownership and read/write keys are enforced on owned files.");
        putSettableConfigParameter(PAGEJECT, "page_eject_on_processor_calls", true, true, false,
                                   "If set, page eject occurs before processor calls unless overridden by @SETC.");
        putSettableConfigParameter(PAKOVF, "pack_overflow_message", false, true, false,
                                   "If set, a console message is displayed if a removable disk allocation was rejected.");
        // PANICASGMNE is n/a
        putSettableConfigParameter(PASMAX, null, 0, true, false,
                                   "Max number of output and pass-off messages for a transaction. If 0, there is no limit.");
        // PBKDF2ITER is n/a... I think
        putSettableConfigParameter(PCHHDG, null, false, false, true,
                                   "If true, we do NOT punch a heading card.");
        // PCTMAX is n/a
        // PGMFLMSWTIME is always true
        // PRDDSTBOOT is n/a
        putSettableConfigParameter(PRIRTP, null, 0, true, true,
                                   "Max number of transaction programs that can be initialized as resident.");
        putSettableConfigParameter(PRIVAC, "tape_labeling_privileged_account", "123456", true, false,
                                   "Tape labeling blocks are not automatically read for runs with this account number.");
        // PROGLOGINTER is n/a
        putSettableConfigParameter(PRSPMX, null, 300, false, true,
                                   "Maximum line spacing permitted for each print request - max is 03777.");
        putFixedConfigParameter(R2MSWTIME, "initial_r2_modified_swtime", true,
                                "If true, R2 is loaded with MODSWTIME format. If false, it is loaded with TDATE$ format.");
        putSettableConfigParameter(RECSIZ, null, 4, true, true,
                                   "Sectors per record for account file.");
        putSettableConfigParameter(REJCONFLTOPT, "reject_options_conflicts", false, true, false,
                                   "If true, subsequent assign attempts which include options not specified on initial assign are rejected.\n"
                                       + "If false, no subsequent assign image is rejected (except for A, C, T, and U conflicts).");
        putSettableConfigParameter(RELUNUSEDREM, "release_unused_alloc_rem", 0, true, false,
                                   """
                               Action taken for unused initial reserve on removable disk files:
                               0: Unused allocated space is not released when the file is released.
                               1: Unused initial reserve > highest granule written is released.
                               2: Unused dynamic allocation > initial reserve and highest granule written is released.""");
        putSettableConfigParameter(RELUNUSEDRES, "release_unused_alloc_ms", 0, true, false,
                                   """
                               Action taken for unused allocations on fixed disk.
                               0: Unused initial reserve is not released.
                               1: Unused initial reserve is released when the file is released.
                               2: Unused dynamic allocation above initial reserve is released.""");
        putSettableConfigParameter(REMBATCHLPI, "remote_batch_6_lpi", false, true, false,
                                   """
                               Default number of lines-per-inch for remote batch printers when * is specified in LPI field.
                               false: Remote batch printers use 8 LPI.
                               true: Remote batch printers use 6 LPI.
                               """);
        putSettableConfigParameter(REMTOP, "rsi_remote_top_separator", true, true, false,
                                   "Indicates whether the top separator page is printed by RSI remote batch printers.");
        // REQNBRPST0 is n/a
        // REQNBRPST1 is n/a
        putSettableConfigParameter(RESDUCLR, "residue_clear", false, true, false,
                                   "Purges information left in mass storage and GRS to prevent exposure between users or applications.");
        putSettableConfigParameter(RESTRICT, "operator_assist_undef_account", false, true, false,
                                   "Restricts auto start of runs during SYS run.");
        putSettableConfigParameter(REWDRV, null, false, false, true,
                                   "If true, operator is asked whether reserve drives should have EOF written and be rewound at recovery boot.");
        putSettableConfigParameter(RLTIME, null, 12, false, true,
                                   "Max lock time in minutes for RL$ lock to be held.");
        putSettableConfigParameter(RPMU, "rem_pack_mfd_update", 0, true, false,
                                   """
                               Controls whether MFD on removable packs is automatically updated.
                               0: No automatic DAD table updates are done for removable pack files. Such information is updated only when the file is @FREE'd.
                               1: Auto updates are done for files registered with TIP.
                               2: Auto updates are done for all files on removable packs.""");
        putSettableConfigParameter(RSIBTM, "rsi_bottom_separator", true, true, false,
                                   "Indicates whether the bottom separator page is printed by RSI remote batch printers.");
        putSettableConfigParameter(RSICNT, null, 8, false, true,
                                   "Maximum number of terminals for RSI including RSI$ remote, batch, and demand.");
        putSettableConfigParameter(RUNASGMNE, "run_file_asg_mnemonic", "F2", true, false,
                                   "Assign mnemonic for SYS$*RUN$.");
        // RUNDEDLVL is n/a
        putSettableConfigParameter(RUNINTRES, "run_file_initial_reserve", 1, true, false,
                                   "Initial reserve used for creating SYS$*RUN$.");
        putSettableConfigParameter(RUNMAXSIZ, "run_file_max_size", 10000, true, false,
                                   "Maximum size for SYS$*RUN$.");
        putSettableConfigParameter(SACRDASGMNE, "sacrd_file_asg_mnemonic", "F", true, false,
                                   "Assign mnemonic for creating SEC@ACR$ file.");
        putSettableConfigParameter(SACRDINTRES, "sacrd_file_initial_reserve", 2, true, false,
                                   "Initial reserve used for creating SEC@ACR$ file.");
        putSettableConfigParameter(SAFHDG, null, 1, false, true,
                                   "Number of blank lines between heading and first line of print.");
        // SB**** is n/a - we do not do SIP
        // SCHKEYLVL - not supported for now
        // SCHKEYOPTS - not supported for now
        // SCPIPSUPS is n/a
        // SDYNDNPACK not supported - we don't do shared directories
        putSettableConfigParameter(SECOFFDEF, "security_officier", "", true, false,
                                   "Specifies a new security officer at boot time.");
        // SENTRY is currently not supported
        putSettableConfigParameter(SEPPAGE, null, false, false, true,
                                   "Indicates whether the sequence number is printed on top and bottom six lines of separator pages.");
        // SFTIMESTAMP is not supported
        putSettableConfigParameter(SHDGSP, null, 5, false, true,
                                   "Number of lines at top of standard page (including heading).");
        // SHRDFCDFLT not supported - not doing shared directories
        // SHRDMSAVL not supported - not doing shared directories
        // SHRDMSTRT not supported - not doing shared directories
        putSettableConfigParameter(SIMLUATECONS, "simulate_console_output", true, true, false,
                                   "If true, two leading spaces are inserted on all @@CONS read-only messages.");
        // SIPIO is n/a
        // SITEFMTAIL - i think this is n/a
        putSettableConfigParameter(SKDATA, null, true, false, true,
                                   "If true, skip-data is supported on I/O.");
        putSettableConfigParameter(SMALIN, null, true, false, true,
                                   "Any printers with formatter changes which might produce position errors will cause a "
                                       + "console message to be printed at the end of the file.");
        putSettableConfigParameter(SMDTFASGMNE, "smdtf_file_asg_mnemonic", "F", true, false,
                                   "Assign mnemonic used for creating SYS$*SMDTF$.");
        putSettableConfigParameter(SMDTFINTRES, "smdtf_file_initial_reserve", 1, true, false,
                                   "Initial reserve used for creating SMDTF$.");
        // SPAREPACKID is not supported
        // SRFHGH is not supported
        putSettableConfigParameter(SSDENS, "system_tape_density", "S", true, true,
                                   "Density for tape assignments used by certain system processors.");
        // SSDSOB is not supported
        putSettableConfigParameter(SSEQPT, "system_tape_asg_type", "T", true, false,
                                   "System standard assign mnemonic for tape assignments for exec and other processors.");
        putSettableConfigParameter(SSPBP, "files_private_by_account", true, true, true,
                                   "Files are private by account instead of by project-id.");
        putSettableConfigParameter(SSPROTECT, "subsystem_entry_protection", false, true, true,
                                   "Enables enforcement of subsystem entry point protection.");
        putSettableConfigParameter(STDBOT, null, 3, false, true,
                                   "Number of blank lines at the bottom of a standard page.");
        putSettableConfigParameter(STDMSAVL, "std_rolout_availability_goal", 3.00, true, false,
                                   "Percent of mass storage which should be available when ROLOUT completes.");
        putSettableConfigParameter(STDMSTRT, "std_rolout_start_threshold", 1.50, true, false,
                                   "ROLOUT starts when the percentage of accessible mass storage reaches or goes below this value.");
        putSettableConfigParameter(STDPAG, null, 66, false, true,
                                   "Total number of lines on a standard page");
        // STPAUL is n/a
        // SUA(x) needs to be thought about...
        putSettableConfigParameter(SUACURRENCY, "sua_currency_indicator", "$", true, false,
                                   "Currency indicator for SUAs on the log summary.");
        // SWGnBIAS is n/a
        // SWMANUFACTRD is n/a
        // SWQAFFLVL is n/a
        putSettableConfigParameter(SYMFBUF, "symb_file_buf", 224, true, false,
                                   "Size of symbiont buffers. Value must be a multiple of 224, and vary from 224 to 7168.");
        putSettableConfigParameter(SYMJK, "allow_sym_separator_options", false, true, false,
                                   "Indicates whether users may use the J and K options on @SYM");
        // SYSBASGMNE is n/a
        // SYSBINTRES is n/a
        putSettableConfigParameter(SYSTRMTSK, "sys_termination_task_schedule", true, true, false,
                                   "If true, the configured run termination task is scheduled for SYS at SYS FIN.");
        // TAPBUF is not supported - we do not do reel tapes
        putSettableConfigParameter(TDFALT, "default_tape_asg_type", "T", true, false,
                                   "Default mnemonic for tape files.");
        putSettableConfigParameter(TEMPP2F, "quota_temp_ms_factor", 0, true, true,
                                   "Power-of-two factor for number of tracks defined as max limit allowed per quote group for temporary files.");
        putSettableConfigParameter(TERMTASK, "run_termination_task", null, true, false,
                                   "Element name of a program in SYS$LIB$*RUN$ or SYS$*RUN$ which executes in all batch and demand runs" +
                                       " just before termination of the runs.");
        putSettableConfigParameter(TFCMSG, null, false, true, false,
                                   "Controls whether file control warning messages are issued.");
        putSettableConfigParameter(TFEXP, "default_tape_expiration", 60, true, true,
                                   "Number of days which a file written on tape is read- or write-protected.");
        putSettableConfigParameter(TFMAX, "max_tape_expiration", 360, true, true,
                                   "Maximum number of days that can be specified in the expiration field of a tape @ASG image.");
        putSettableConfigParameter(TFPMAX, null, 100, true, true,
                                   "Max number of permanent TIP files.");
        putSettableConfigParameter(TFSEC, "tip_file_security", false, true, true,
                                   "Enables TIP file security.");
        putSettableConfigParameter(TIMCTRL, null, false, true, true,
                                   "Enables TIMER control functions.");
        putSettableConfigParameter(TIMDAYS, null, 0, true, true,
                                   "Number of future days which TIMER scheduling requests are kept. Max value is 127.");
        putSettableConfigParameter(TIMINT, null, false, true, true,
                                   "Controls TIMER cleanup. If true, the TIMER file is cleared on all boots."
                                       + " Otherwise, the TIMER file is cleared only on initial boots.");
        putSettableConfigParameter(TIMLNKBLKS, null, 0, true, true,
                                   "Number of link blocks contained in the TIMER system file for use when a day or minute block overflows.");
        putSettableConfigParameter(TIP, null, true, true, true,
                                   "Controls whether TIP is enabled.");
        putSettableConfigParameter(TIPABSCALLSS, "tip_abs_call_ss", true, true, true,
                                   "Indicates whether basic mode transactions are loaded with extended mode stack banks.");
        putSettableConfigParameter(TIPAGFILES, "tip_application_group_files", 0, true, true,
                                   "If true, we support the system max of 262,000 TIP file numbers and 262,000 TIP/Exec files."
                                       + " Otherwise, the maximum number of either value is 4095.");
        // TIPDUPRSL is not supported
        putSettableConfigParameter(TIPFDR, null, "", true, true,
                                   """
                               Controls TIP file directory recovery on system boots.
                               blank: The operator is given the choice of initializing or recovering the TIP file directory
                               "R": The TIP file directory is recovered
                               "I": The TIP file directory is initialized""");
        putSettableConfigParameter(TIPFSMSGLMT, "tip_fs_message_limit", 8, true, false,
                                   "Max number of Freespace messages displayed on the system console over a 6-second interval. Max is 500.");
        putSettableConfigParameter(TIPINIT, null, false, true, true,
                                   "If true, TIP is initialized automatically. Otherwise, the operator is given the option of initializing TIP.");
        putSettableConfigParameter(TIPMEF, null, 100, true, true,
                                   "Max number of Exec files which can be registered with TIP file control.");
        // TODO TIPPK1, TIPPK2, TIPPK3, TIPPK4, TIPPK5, TIPPK6, TIPPK7, TIPPK8
        // TODO TIPRTPSDEACT
        // TODO TIPSECUSR
        // TODO TIPTPS
        // TODO TIPVIEWPRINT
        // TODO TIPWRD
        putSettableConfigParameter(TLAUTO, "automatic_tape_labeling", false, true, false,
                                   "If true, all tapes assigned without J option are labeled when written.");
        // TLAVREBREC is not supported
        // TLEBCDIC is not supported
        putFixedConfigParameter(TLPRE4, "create_pre_version4_tape", false,
                                "If true, tape labels are written in pre-version 4 format. If false, they are written in version 4 format.");
        putSettableConfigParameter(TLSIMP, "prelabeled_tapes_required", true, true, true,
                                   "If true, all tapes assigned without J option must be labeled.");
        putSettableConfigParameter(TLSUB, "reel_nbr_substitution_allowed", false, true, true,
                                   "If true, reel number substitution is allowed on WRONG REEL, BAD LABEL, and IS TAPE messages.");
        putSettableConfigParameter(TMPFILETOMEM, "temporary_files_to_memory", false, true, false,
                                   "When set, all temporary fixed file assignments are FMEM or DMEM, making the files memory files.");
        // TODO TMSEC (TIP)
        putSettableConfigParameter(TPBLKNBR, "tape_block_numbering", false, true, true,
                                   "If true, all tapes not specified as BLKOFF have block numbering turned on.");
        putSettableConfigParameter(TPDCOMP, "tape_data_compression", false, true, true,
                                   "If true, all tapes not specified as CMPOFF have data compression turned on.");
        // TODO TPDEV1 : TPDEV8 (TIP)
        // TPDIRI is n/a
        putSettableConfigParameter(TPFMAXSIZ, "tpf_file_max_size", 0, true, false,
                                   "Initial max size of TPF$ files.");
        putSettableConfigParameter(TPFTYP, "tpf_asg_type", "F", true, false,
                                   "Equipment type for TPF$ files.");
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
        putSettableConfigParameter(TPOWN, "tape_access_restrict_by_account", false, true, true,
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
        putSettableConfigParameter(TRMXCO, "terminate_runs_on_max_cards", false, true, false,
                                   "If true, a run is terminated when the maximum number of cards are punched.");
        putSettableConfigParameter(TRMXPO, "terminate_runs_on_max_pages", false, true, false,
                                   "If true, a run is terminated when the maximum number of pages are printed.");
        putSettableConfigParameter(TRMXT, "terminate_runs_on_max_time", false, true, false,
                                   "If true, a run is terminated when the maximum amount of time elapses, as specified on the @RUN statement.");
        // TODO TRMXVT (TIP)
        putSettableConfigParameter(TSS, "tss_control", true, true, true,
                                   "Enables terminal security capabilities.");
        // TVSL is n/a (for now)
        // UNIVAC is n/a
        putSettableConfigParameter(UNLRELVERIFY, "unlabeled_reel_verification", false, true, false,
                                   "If set, the reel-id of an unlabeled tape is verified before the first I/O is processed.");
        putSettableConfigParameter(USERASGMNE, "userid_file_asg_mnemonic", "F", true, false,
                                   "Assign mnemonic used to create SYS$*SEC@USERID$ file.");
        putSettableConfigParameter(USERINTRES, "userid_file_initial_reserve", 1, true, false,
                                   "Initial reserve used to create SYS$*SEC@USERID$ file.");
        putSettableConfigParameter(USERON, "quota_enforcement_by_userid", false, true, true,
                                   "Controls quota enforcement by user-ids.");
        // VRTQLVL is n/a
        // VTHRDISK is n/a
        // VTHSHARE is n/a
        // VTHSUBS is n/a
        // VTHXPC is n/a
        // TODO VTFLAB (TIP)
        putSettableConfigParameter(WDFALT, "deafult_wad_asg_type", "D", true, false,
                                   "Default mnemonic for word-addressable files.");
        // XCACHESECFIL is n/a
        // XPDLCLINTV is n/a
        // XPDLCLSTRT is n/a
        // XPDLOCAL is n/a
        // XPDMAXIO is n/a
        // XPDSHARED is n/a
        // XPDSHRDINTV is n/a
        // XPDSHRDSTRT is n/a
        putSettableConfigParameter(ZOPTBATCHREJ, "reject_Z_option_batch", true, true, false,
                                   """
                                             If true, a Z option on facility images for a batch run causes the image to be rejected.
                                             If false, a Z option is ignored for batch runs.""");
         */
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
        if (value instanceof Boolean bv) {
            return bv;
        } else {
            return null;
        }
    }

    public Float getFloatValue(final Tag tag) {
        var value = getConfigValue(tag);
        if (value instanceof Float fv) {
            return fv;
        } else {
            return null;
        }
    }

    public Integer getIntegerValue(final Tag tag) {
        var value = getConfigValue(tag);
        if (value instanceof Long lv) {
            return (Integer)(int)(lv & 0xFFFFFFFFL);
        } else if (value instanceof Integer iv) {
            return iv;
        } else {
            return null;
        }
    }

    public Long getLongValue(final Tag tag) {
        var value = getConfigValue(tag);
        if (value instanceof Long lv) {
            return lv;
        } else if (value instanceof Integer iv) {
            return (Long)(long)iv;
        } else {
            return null;
        }
    }

    public String getStringValue(final Tag tag) {
        var value = getConfigValue(tag);
        if (value instanceof String sv) {
            return sv;
        } else {
            return null;
        }
    }

    private static void putFixedConfigParameter(
        final Tag tag,
        final Object defaultValue,
        final String description
    ) {
        var cp = new FixedConfigParameter(tag, defaultValue, description);
        CONFIG_PARAMETERS.put(tag.name(), cp);
    }

    private static void putRestrictedConfigParameter(
        final Tag tag,
        final Object defaultValue,
        final boolean isPrivileged,
        final boolean isRebootRequired,
        final String description,
        final Restriction restriction
    ) {
        var cp = new RestrictedConfigParameter(tag, defaultValue, isPrivileged, isRebootRequired, description, restriction);
        CONFIG_PARAMETERS.put(tag.name(), cp);
    }

    private static void putSettableConfigParameter(
        final Tag tag,
        final Object defaultValue,
        final boolean isPrivileged,
        final boolean isRebootRequired,
        final String description
    ) {
        var cp = new SettableConfigParameter(tag, defaultValue, isPrivileged, isRebootRequired, description);
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

    public void updateFromFile(final String filename) throws KExecException {
        // TODO
    }
}
