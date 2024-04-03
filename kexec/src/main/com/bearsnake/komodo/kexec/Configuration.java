/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

public class Configuration {

    public static long AccountInitialReserve          uint64 // initial reserve for SYS$*ACCOUNT$R1 and SYS$SEC@ACCTINFO files
    public static String AccountAssignMnemonic          string // assign mnemonic for SYS$*ACCOUNT$R1 and SYS$SEC@ACCTINFO files
    public static String DLOCAssignMnemonic             string // assign mnemonic for SYS$*DLOC$ file
    public static boolean FilesPrivateByAccount          bool   // if false, files are private by project-id
    public static long GenFInitialReserve             uint64 // initial reserve for SYS$*GENF$ file
    public static String GenFAssignMnemonic             string // assign mnemonic for SYS$*GENF$ file
    public static long LibInitialReserve              uint64 // initial reserve for SYS$*LIB$ file
    public static String LibAssignMnemonic              string // assign mnemonic for SYS$*LIB$ file
    public static long LibMaximumSize                 uint64 // max granules for SYS$*LIB$ file
    public static boolean LogConsoleMessages             bool
    public static boolean LogIOs                         bool
    public static String MasterAccountId                string // could be empty, in which case operator is prompted when ACCOUNT$R1 is created
    public static String MassStorageDefaultMnemonic     string // Usually 'F'
    public static long MaxCards                       uint64
    public static long MaxGranules                    uint64 // max granules if not specified on @ASG or @CAT
    public static long MaxPages                       uint64
    public static String OverheadAccountId              string // account ID for overhead runs such as SYS and ROLOUT/ROLBACK
    public static String OverheadUserId                 string // User ID for overhead runs
    public static String PrivilegedAccountId            string // account ID which can override reading tape label blocks
    public static boolean ReleaseUnusedReserve           bool
    public static boolean ReleaseUnusedRemovableReserve  bool
    public static boolean ResidueClear                   bool   // zero out tracks when allocated
    public static long RunInitialReserve              uint64 // initial reserve for SYS$*RUN$ file
    public static String RunAssignMnemonic              string // assign mnemonic for SYS$*RUN$ file
    public static long RunMaximumSize                 uint64 // max granules for SYS$*RUN$ file
    public static long SacrdInitialReserve            uint64 // initial reserve for SYS$*SEC@ACR$ file
    public static String SacrdAssignMnemonic            string // assign mnemonic for SYS$*SEC@ACR$ file
    public static double StandardRoloutAvailabilityGoal float32
    public static double StandardRoloutStartThreshold   float32
    public static String SecurityOfficerUserId          string // could be empty, in which case operator is prompted at boot time
    public static long SymbiontBufferSize             uint64 // Buffer size used for standard and alternate read/write buffers
    public static String SystemTapeEquipment            string // assign mnemonic for exec tape requests
    public static boolean TapeAccessRestrictedByAccount  bool
    public static String TapeDefaultMnemonic            string
    public static boolean TerminateMaxCards              bool
    public static boolean TerminateMaxPages              bool
    public static boolean TerminateMaxTime               bool
    public static String TIPQualifier                   string
    public static String TIPReadKey                     string
    public static String TIPWriteKey                    string
    public static String TPFAssignMnemonic              string
    public static long TPFMaxSize                     uint64
    public static long UserInitialReserve             uint64 // initial reserve for SYS$*SEC@USERID$ file
    public static String UserAssignMnemonic             string // assign mnemonic for SYS$*SEC@USERID$ file
    public static String WordAddressableDefaultMnemonic string
}
