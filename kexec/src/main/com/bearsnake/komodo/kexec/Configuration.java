/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

public class Configuration {

    public static long AccountInitialReserve            = 10;               // initial reserve for SYS$*ACCOUNT$R1 and SYS$SEC@ACCTINFO files
    public static String AccountAssignMnemonic          = "F";              // assign mnemonic for SYS$*ACCOUNT$R1 and SYS$SEC@ACCTINFO files
    public static String DLOCAssignMnemonic             = "F";              // assign mnemonic for SYS$*DLOC$ file
    public static boolean FilesPrivateByAccount         = true;             // if false, files are private by project-id
    public static long GenFInitialReserve               = 128;              // initial reserve for SYS$*GENF$ file
    public static String GenFAssignMnemonic             = "F";              // assign mnemonic for SYS$*GENF$ file
    public static long LibInitialReserve                = 128;              // initial reserve for SYS$*LIB$ file
    public static String LibAssignMnemonic              = "F";              // assign mnemonic for SYS$*LIB$ file
    public static long LibMaximumSize                   = 9999;             // max granules for SYS$*LIB$ file
    public static boolean LogConsoleMessages            = true;
    public static boolean LogIOs                        = true;
    public static String MasterAccountId                = "SYSTEM";         // could be empty, in which case operator is prompted when ACCOUNT$R1 is created
    public static String MassStorageDefaultMnemonic     = "F";              // Usually 'F'
    public static long MaxCards                         = 256;
    public static long MaxGranules                      = 256;              // max granules if not specified on @ASG or @CAT
    public static long MaxPages                         = 256;
    public static String OverheadAccountId              = "INSTALLATION";   // account ID for overhead runs such as SYS and ROLOUT/ROLBACK
    public static String OverheadUserId                 = "INSTALLATION";   // User ID for overhead runs
    public static String PrivilegedAccountId            = "123456";         // account ID which can override reading tape label blocks
    public static boolean ReleaseUnusedReserve          = true;
    public static boolean ReleaseUnusedRemovableReserve = false;
    public static boolean ResidueClear                  = false;            // zero out tracks when allocated
    public static long RunInitialReserve                = 10;               // initial reserve for SYS$*RUN$ file
    public static String RunAssignMnemonic              = "F";              // assign mnemonic for SYS$*RUN$ file
    public static long RunMaximumSize                   = 256;              // max granules for SYS$*RUN$ file
    public static long SacrdInitialReserve              = 10;               // initial reserve for SYS$*SEC@ACR$ file
    public static String SacrdAssignMnemonic            = "F";              // assign mnemonic for SYS$*SEC@ACR$ file
    public static double StandardRoloutAvailabilityGoal = 1.50;
    public static double StandardRoloutStartThreshold   = 3.00;
    public static String SecurityOfficerUserId          = "";               // could be empty, in which case operator is prompted at boot time
    public static long SymbiontBufferSize               = 224;              // Buffer size used for standard and alternate read/write buffers
    public static String SystemTapeEquipment            = "T";              // assign mnemonic for exec tape requests
    public static boolean TapeAccessRestrictedByAccount = false;
    public static String TapeDefaultMnemonic            = "T";
    public static boolean TerminateMaxCards             = false;
    public static boolean TerminateMaxPages             = false;
    public static boolean TerminateMaxTime              = false;
    public static String TIPQualifier                   = "TIP$";
    public static String TIPReadKey                     = "++++++";
    public static String TIPWriteKey                    = "++++++";
    public static String TPFAssignMnemonic              = "F";
    public static long TPFMaxSize                       = 128;
    public static long UserInitialReserve               = 10;               // initial reserve for SYS$*SEC@USERID$ file
    public static String UserAssignMnemonic             = "F";              // assign mnemonic for SYS$*SEC@USERID$ file
    public static String WordAddressableDefaultMnemonic = "D";
}
