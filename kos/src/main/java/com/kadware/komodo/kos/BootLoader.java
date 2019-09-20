/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kos;

public class BootLoader {

    public static final String[] SOURCE = {
        ". BOOTLOADER",
        ". Copyright (C) 2019 by Kurt Duncan - All Rights Reserved",
        ".",
        ". This code lives in the first block of the operating system loaded from either disk or tape.",
        ". The actual IPL sequence is performed by the System Processor as follows:",
        ".   Identify load path including the following components:",
        ".     Instruction Processor (IP)",
        ".     Main Storage Processor (MSP)",
        ".     Input-Output Processor (IOP)",
        ".     Channel Module (CM)",
        ".     Boot Device (containing the media which contains the OS",
        ".   Clear all the IPs, IOPs, and MSPs",
        ".   Allocate the following segments of memory:",
        ".     Bank 0:   Configuration bank (populate it appropriately)",
        ".     Bank 1:   First bank (boot block) of OS (this code)",
        ".     Bank 2:   Level 0 BDT for the IPL process (populate it)",
        ".                 Interrupt vectors for classes 29 and 30 should reference first word of boot block",
        ".                 All other interrupt handlers should IAR",
        ".                 BDI 000040: Defines configuration bank",
        ".                 BDI 000041: Defines boot block bank",
        ".     Banks 3+: One Interrupt Control Stack per IP in the partition",
        ".   Load the first block of the OS into storage bank 1",
        ".   Set up all IPs in the partition as follows:",
        ".     B16 (Level 0 BDT) points to bank 2",
        ".     B26 (ICS bank register) and EX1 (ICS pointer) point to the ICS for the IP",
        ".   Invoke start() on the boot IP",
        ".",
        ". Description of configuration bank:",
        ".      +---------+---------+---------+---------+---------+---------+",
        ". +00  | IPL_SP  | IPL_IP  | IPL_IOP | IPL_MSP |IPL_CHMOD|IPL_DEVNM|",
        ".      +---------+---------+---------+---------+---------+---------+",
        ". +01  |IPL_FLAGS|                   |          IPL_PREPF          |",
        ".      +---------+---------+---------+---------+---------+---------+",
        ". +02  |                         JUMP KEYS                         |",
        ".      +---------+---------+---------+---------+---------+---------+",
        ". +03  |                      MAILBOX_ADDRESS                      |",
        ". +04  |                                                           |",
        ".      +---------+---------+---------+---------+---------+---------+",
        ". IPL_SP:          UPI of the SP which is controlling the IPL",
        ". IPL_IP:          UPI of the IP which is in control of the IPL",
        ". IPL_IOP:         UPI of the IOP which we are using for the IPL",
        ". IPL_MSP:         UPI of the MSP into which we load the OS",
        ". IPL_CHMOD:       Channel module index for the boot path",
        ". IPL_DEVNM:       Device number from which we are booting",
        ". IPL_FLAGS:",
        ".   Bit5:          true for tape boot, false for disk boot",
        ". IPL_PREPF:       For disk boots, the block size of a single IO",
        ".                    for the pack which we are booting from.",
        ". JUMP_KEYS:       Jump keys which control the IPL and startup processes",
        ". MAILBOX_ADDRESS: Absolute address of block of memory containing the",
        ".                    UPI mailboxes",
        ".",
        "          $EXTEND",
        "          $ASCII",
        "          $INFO 1 3",
        "          $INFO 10 1",
        ".",
        ".",
        ". ---------------- Equfs ------------------",
        ".",
        ". Bank descriptor equfs",
        "BD_GAPSAP    $EQUF      0,,S1",
        "BD_TYPE      $EQUF      0,,S2",
        "BD_ACCLOCK   $EQUF      0,,H2",
        "BD_LOWER     $EQUF      1,,Q1",
        "BD_UPPER     $EQUF      1,,W     . Actually, only bits 9-35",
        "BD_ADDR      $EQUF      2        . Abs Address 2 words",
        ".",
        ". Configuration bank equfs",
        "CFG_IPLSP    $EQUF      0,,S1",
        "CFG_IPLIP    $EQUF      0,,S2",
        "CFG_IPLIOP   $EQUF      0,,S3",
        "CFG_IPLMSP   $EQUF      0,,S4",
        "CFG_IPLCHMOD $EQUF      0,,S5",
        "CFG_IPLDEVNM $EQUF      0,,S6",
        "CFG_IPLFLAGS $EQUF      1,,S1",
        "CFG_IPLPREPF $EQUF      1,,H2",
        "CFG_JUMPKEYS $EQUF      2,,W",
        "CFG_MBOXADDR $EQUF      3",
        ".",
        ". LBED equfs",
        "LBED_GAPSAP  $EQUF      0,,S1",
        "LBED_ACCLOCK $EQUF      0,,H2",
        "LBED_LOWER   $EQUF      1,,Q1",
        "LBED_UPPER   $EQUF      1,,H2",
        "LBED_ADDRESS $EQUF      2",
        ".",
        ". SYSC equfs",
        "SC_FUNC      $EQUF      0,,S1",
        "SC_STATUS    $EQUF      0,,S2",
        ". for memory alloc/free/resize...",
        "SCMEM_MSPUPI $EQUF      0,,S3",
        "SCMEM_SEGX   $EQUF      1,,W",
        "SCMEM_SIZE   $EQUF      2,,W",
        ". for IO, we have...",
        "SCIO_IOPUPI  $EQUF      0,,S2",
        "SCIO_CHMODX  $EQUF      0,,S3",
        "SCIO_DEVX    $EQUF      0,,S4",
        "SCIO_FLAGS   $EQUF      0,,S6 . Bit30:   Prevent UPI when IO completes",
        "SCIO_OPERATN $EQUF      1,,S1",
        "SCIO_FLAGS2  $EQUF      1,,S2 . Bits6-7: Format 00=A, 01=B, 10=C, 11=D",
        "                              . Bit8:    Direction 0=fwd, 1=bkwd",
        "SCIO_STATUS  $EQUF      1,,S3",
        "SCIO_RESIDUE $EQUF      1,,S4 . Non-integral residue count",
        "SCIO_SIZE    $EQUF      2,,H1 . #Words to transfer on output, buffer size on input",
        "SCIO_XFERD   $EQUF      2,,H2 . Number of words transferred",
        "SCIO_BUFADDR $EQU       3     . Absolute address (2 words) of IO buffer",
        "SCIO_DEVADDR $EQU       5     . Device-relative address if applicable (e.g., block ID)",
        ".",
        ". SYSC function codes",
        "SCF_MEMALLOC $EQU       020",
        "SCF_MEMFREE  $EQU       021",
        "SCF_MEMSIZE  $EQU       022",
        "SCF_STARTIO  $EQU       040",
        ".",
        ". SYSC IO operations",
        "SCIOO_WR     $EQU       000 . Write Block",
        "SCIOO_WREOF  $EQU       001 . Write File Mark",
        "SCIOO_RD     $EQU       002 . Read Block",
        "SCIOO_SK     $EQU       003 . Skip Block",
        "SCIOO_SKEOF  $EQU       004 . Skip File Mark",
        "SCIOO_REW    $EQU       005 . Rewind",
        "SCIOO_REWILK $EQU       006 . Rewind with Interlock",
        ".",
        ". SYSC IO status",
        "SCIOS_OK     $EQU       000 . Successful",
        "SCIOS_BADUPI $EQU       001 . IOP UPI does not correspond to an IOP",
        "SCIOS_BADCM  $EQU       002 . Channel module does not exist",
        "SCIOS_BADDEV $EQU       003 . Device does not exist",
        "SCIOS_NOTRDY $EQU       004 . Device is not ready",
        "SCIOS_BUSY   $EQU       005 . Device is busy",
        "SCIOS_EOF    $EQU       006 . End of file mark read",
        "SCIOS_EOT    $EQU       007 . End of tape mark read",
        "SCIOS_BADADR $EQU       010 . Address out of range",
        "SCIOS_INPROG $EQU       040 . IO started successfully and is in progress",
        ".",
        ".",
        ". ---------------- Procs ------------------",
        "",
        ". Proc to create a stack and base it on a bank",
        ". Parameter 1,0 is the frame size",
        ". Parameter 1,1 is the total stack size",
        ". Parameter 2,0 is the base register",
        ". Parameter 2,1 is the index register",
        "GENSTACK  $PROC",
        ". Set up SYSC packet for allocating a piece of memory",
        "          L,U       A0,SCF_MEMALLOC     . function code",
        "          SA        A0,SC_FUNC,EX2,B0",
        "          LA        A0,CFG_IPLMSP,,B2   . UPI of IPL Path MSP",
        "          SA        A0,SCMEM_MSPUPI,EX2,B0",
        "          LA,U      A0,GENSTACK(1,1)    . Size of memory (stack size)",
        "          SA        A0,SCMEM_SIZE,EX2,B0",
        "          SYSC      0,EX2,B0            . allocate space for the stack",
        ". At this point, SCMEM_SEGX is an MSP segment index",
        ". describing the bank we allocated. Set up the base register",
        ". via the LBEDPKT.",
        "          SZ        LBED_LOWER,EX4,B0   . Lower limit for LBED is 0",
        "          LA,U      A0,GENSTACK(1,1)    . Get stack size for LBED upper limit",
        "          SA        A0,LBED_UPPER,EX4,B0 .   and in Upper limit for LBED",
        ". Absolute address is seg index in first word,",
        ".   MSP UPI in top 4 bits of second word,",
        ".   and offset into the seg index in the remaining bits of second word.",
        "          LA        A0,SCMEM_SEGX,EX2,B0",
        "          SA,W      A0,LBED_ADDRESS,EX4,B0",
        "          LA        A0,CFG_IPLMSP,,B2",
        "          LSSL      A0,32",
        "          SA        A0,LBED_ADDRESS+1,EX4,B0",
        "          LBED      P(2,0),0,EX4,B0  . Base the stack bank",
        ". Set up the stack pointer.",
        "          LXI,U     P(2,1),P(1,0)",
        "          LXM       P(2,1),SCMEM_SIZE,EX2,B0",
        ".",
        "          $ENDP",
        ".",
        ".",
        ". ---------------- Constants -------------------",
        ".",
        "ICSFRAMESZ  $EQU 8       . ICS frames are 8 words",
        "ICSFRAMECNT $EQU 8       . Arbitrary stack depth for an OS thread",
        "ICSSIZE     $EQU ICSFRAMESZ*ICSFRAMECNT",
        ".",
        "RCSFRAMESZ  $EQU 2       . RCS frames are always 2 words",
        "RCSFRAMECNT $EQU 32      . Arbitrary stack depth for an OS thread",
        "RCSSIZE     $EQU RCSFRAMESZ*RCSFRAMECNT",
        ".",
        ".",
        ". ---------------- Data -------------------",
        ".",
        ". Put literals into the data location counter - to be based on B0 along with the code.",
        ". Note that this requires the containing bank to be read/write.",
        ". Also note that this seg *must* follow $(1) (and is thus $(2) to make sure)",
        ". since the first word in the bank must be executable.",
        ".",
        "$(2)      $LIT",
        ".",
        "INITREADY    + 0                 . set to 1 when boot IP has completed loading the OS, and other IPs can run",
        "INITDESREG   + 000001,000010",
        "CFGBANKBDI   + 000040,0",
        "BOOTBANKBDI  + 000041,0",
        "MEMPKT       $RES 3",
        "IOPKT        + 0",
        "             + 0",
        "             + 0",
        "             + 0",
        "             + 0",
        "             + 0",
        "LBEDPKT      + 0",
        "             + 0",
        "             + 0",
        "             + 0",
        ".",
        ".",
        ". ---------------- Code -------------------",
        ".",
        "$(1)",
        "START$*",
        ". We get here via direct GOTO from the interrupt handler for either class 29 (IPL) or class 30 (UPI Initial)",
        ". The former is for the boot IP, the latter for all others (if any).",
        ". When we get here, we are still in the context of interrupt handling.",
        ". B26 and EX1 define the ICS, B16 defines a level 0 BDT, and that's all we have.",
        ". Oh, we do have the bank containing this code based on B0.",
        ".",
        ". Some index pointers for referring to various packets",
        "          LXM,U     EX2,MEMPKT           . SYSC allocating memory",
        "          LXM,U     EX3,IOPKT            . SYSC doing IO",
        "          LXM,U     EX4,LBEDPKT          . For SBED and LBED instructions",
        ".",
        ". Set up a return control stack which we can use for the entirety of the OS session.",
        ". Make sure Designator Register is set the way we want it, then allow interrupts.",
        ". Base configuration bank on B2.",
        "          GENSTACK  ICSFRAMESZ,ICSSIZE B26,EX1",
        "          GENSTACK  RCSFRAMESZ,RCSSIZE B25,EX0",
        "          LD        INITDESREG,,B0",
        "          AAIJ      $+1",
        "          LBU       B2,CFGBANKBDI,,B0",
        ".",
        ". If we are not the boot IP, go to join - check SSF field for interrupt class.",
        "          LA,S1     EA0,*2,EX1,B26",
        "          TE,U      EA0,29",
        "          J         IPJOIN",
        ".",
        ". Set up the non-variant portion of IOPKT.",
        "          LA,U      EA0,SCF_STARTIO",
        "          SA        EA0,SC_FUNC,EX3,B0",
        "          LA        EA0,CFG_IPLIOP,,B2   . UPI of load IOP",
        "          SA        EA0,SCIO_IOPUPI,EX3,B0",
        "          LA        EA0,CFG_IPLCHMOD,,B2 . Channel Module Index",
        "          SA        EA0,SCIO_CHMODX,EX3,B0",
        "          LA        EA0,CFG_IPLDEVNM,,B2 . Device Index",
        "          SA        EA0,SCIO_DEVX,EX3,B0",
        "          LA,U      EA0,040              . Prevent UPI on IO completion",
        "          SA        EA0,SCIO_FLAG,EX3,B0",
        "          LA,U      EA0,SCIOO_RD",
        "          SA        EA0,SCIO_OPERATN,EX3,B0",
        "          LA,U      EA0,010              . Forward read, C format",
        "          SA        EA0,SCIO_FLAGS2,EX3,B0",
        ".",
        ". Invariant portion of LBEDPKT.",
        "          LA,U      EA0,03",
        "          SA        EA0,LBED_GAPSAP,EX4,B0",
        "          SZ        EA0,LBED_ACCLOCK,EX4,B0",
        ".",
        ". The block which follows this boot block contains the level 0",
        ". Bank Descriptor Table. This table describes all of the other banks",
        ". which comprise the rest of the operating system.",
        ". For disks, the boot block is in block 0, but the successive blocks",
        ". are located beginning at block 3 (block 1 unused, block 2 is the VOL1 label",
        ". Load this block from the boot media, and base it on B16.",
        ". From here onward, we have the real level 0 BDT.",
        "          LA,U      EA0,LEVEL0BDT$SZ     . How big is the BDT?",
        "          LA,U      EA1,3                . Disk block ID",
        "          LOCL      LOADBANK",
        "          DS        EA0,LBED_ADDRESS,EX4,B0",
        "          SZ        LBED_LOWER,EX4,B0",
        "          LA,U      EA0,LEVEL0BDT$SZ",
        "          SA        LBED_UPPER,EX4,B0",
        "          LBED      B16,0,EX4,B0",
        ".",
        ". Now iterate over the bank descriptors in the level 0 BDT.",
        ". Each successive BD describes banks stored consecutively on the",
        ". boot media. For each BD, load the described bank, then update",
        ". the BD with the absolute address of the loaded bank.",
        ". We use EX8 as the indexer to the successive bank descriptors.",
        ". We use EA8 as the down-counter for the loop.",
        "          LXI,U     EX8,8               . Bank descriptors are 8 words",
        "          LXM,U     EX8,8*32            . The first BD is at offset of 32 entries",
        "          LA,U      EA8,LEVEL0BDT$SZ    . Get size of Level 0 BDT,",
        "          ANA,U     EA8,8*32            .   subtract the interrupt vectors",
        "          SSL       EA8,3               .   divide by the size of a BD,",
        "          ANA,U     EA8,1               .   and start counting at one less.",
        ".",
        "LOOP",
        ". Get the upper and normalized lower limits, to determine the size",
        ". of the bank for this bank descriptor.  It won't be a *large* bank",
        ". so we know how many bits to shift for lower limit normalization.",
        "          LA        EA0,BD_UPPER,EX8,B16",
        "          LSSL      EA0,9",
        "          SSL       EA0,9",
        "          LA        EA1,BD_LOWER,EX8,B16",
        "          LSSL      EA1,9",
        "          AN        EA0,A1               . Now we've got bank size",
        "          JN        EA0,LOOPITER         . Ignore void banks",
        ".",
        "          LOCL      LOADBANK             . Load the bank",
        "          DS        EA0,BD_ADDR,EX8,B16  . Update the bank descriptor",
        ".",
        "LOOPITER .",
        "          NOP       0,*X8",
        "          JGD       EA8,LOOP",
        ".",
        ". All done - store level 0 BDReg for non-load IPs,",
        ". and go to the os initialization code",
        "          SBED      B16,LBEDPKT,,B0",
        "          SP1       INITREADY,,B0",
        "          GOTO      KOS$INIT",
        ".",
        "IPJOIN",
        ". Load the real level 0 BDT and go off to KOS land.",
        "          LBED      B16,LBEDPKT,,B0",
        "          GOTO      KOS$JOIN",
        ".",
        ".",
        ". ....................................................................",
        ". Allocates space for a bank, then loads one or more blocks from the",
        ". boot medium to fill up the bank.",
        ". Parameters EA0:     Size of the bank to be loaded",
        ".            EA1:     Disk block address (ignored for tape)",
        ". Returns in EA0/EA1: Absolute Address of the loaded bank",
        ". ....................................................................",
        "LOADBANK",
        ". If this is a disk boot, increase size to the next multiple",
        ". of the disk prep factor.",
        "          LA        EA2,CFG_IPLPREPF,,B2",
        "          AND,U     EA2,01",
        "          JNZ       EA3,LOADBANK010         . Jump if tape boot",
        ".",
        "          SA        EA1,SCIO_DEVADDR,EX3,B0 . Save disk block address",
        ".",
        "          LA,U      EA2,0                   . Divide bank size by prep",
        "          LA        EA3,EA0                 .  factor and add the remainder",
        "          DI        EA2,CFG_IPLPREPF,,B2    .  to the bank size.",
        "          AA        EA0,EA3                 . #Words is a multiple of prep factor.",
        ".",
        "LOADBANK010",
        ". Allocate a block of memory to hold the bank we're going to load.",
        "          LA        EA1,CFG_IPLMSP,,B2",
        "          SA        EA1,SCMEM_MSPUPI,EX2,B0",
        "          SA        EA0,SCMEM_SIZE,EX2,B0   . Store req size in alloc pkt",
        "          SYSC      0,EX2,B0                . allocate space for the load",
        ".",
        ". Set up initial values in the IO packet.",
        ". Block address is already there if this is disk.",
        "          SA        EA0,SCIO_SIZE,EX3,B0    . Current buffer size",
        "          LA        EA0,SCMEM_SEGX,EX2,B0",
        "          LA        EA1,CFG_IPLMSP,,B2",
        "          LSSL      EA1,32",
        "          DS        EA0,SCIO_BUFADDR,EX3,B0",
        ".",
        "          LA,U      EA2,0                   . Block count init",
        ".",
        "LOADBANKIO",
        "          SYSC      0,EX3,B0                . Start the IO",
        ".",
        "LOADBANKWAIT",
        "          LA        EA0,SCIO_STATUS,EX3,B0  . Is the IO done?",
        "          TE,U      EA0,040",
        "          J         LOADBANK040",
        "          LR,U      ER1,010                 . No - Wait 10ms",
        "          BT        EX0,0,EX0,0",
        "          J         LOADBANKWAIT",
        ".",
        "          TE,U      EA0,0                   . Successful?",
        "          IAR       0145                    . No. NO BOOT FOR YOU!",
        "",
        ". Increment blocks-read counter",
        "          AA,U      EA2,1",
        ".",
        ". Update buffer size to remaining size by subtracting words xfrd.",
        ". Check whether anything is left to be done.",
        "          LA        EA0,SCIO_SIZE,EX3,B0",
        "          AN        EA0,SCIO_XFERD,EX3,B0",
        "          SA        EA0,SCIO_SIZE,EX3,B0",
        ".",
        ". Update offset by adding words xfrd.",
        "          LA        EA0,SCIO_BUFADDR+1,EX3,B0",
        "          AA        EA0,SCIO_XFERD,EX3,B0",
        "          SA        EA0,SCIO_BUFADDR+1,EX3,B0",
        ".",
        ". Increment block address and go back for more.",
        "          ADD1      SCIO_DEVADDR,EX3,B0",
        "          J         LOADBANKIO",
        ".",
        "LOADBANKDONE",
        ". Retrieve bank absolute address from ALLOCPKT and we're done.",
        "          DL        EA0,SCMEM_ADDR,EX2,B0",
        "          RTN",
        ".",
        "          $END",
    };
}
