/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kos;

public class BootStrap {

    public static final String[] SOURCE = {
        ". BOOTSTRAP",
        ". Copyright (C) 2019 by Kurt Duncan - All Rights Reserved",
        ".",
        ". This code lives in the first block of the operating system loaded",
        ". from either disk or tape. The actual IPL sequence is performed by",
        ". the System Processor as follows:",
        ".   Identify load path including the following components:",
        ".     Instruction Processor (IP)",
        ".     Main Storage Processor (MSP)",
        ".     Input-Output Processor (IOP)",
        ".     Channel Module (CM)",
        ".     Boot Device (containing the media which contains the OS",
        ".   Clear all the IPs, IOPs, and MSPs",
        ".   Obtain three suitably-sized blocks of memory from the MSP",
        ".   Load this first block from the boot media into block 1",
        ".   Write the Configuration Bank to block 2",
        ".   Set the following registers in the IP:",
        ".     B0 to refer to the first block (containing boot code)",
        ".     B2 to refer to the second block (configuration bank)",
        ".     PAR.PC L,BDI set to zero, and PC to the first address of the",
        ".         boot code (which we define, via B0 base register)",
        ".     Designator register set to 000001000010",
        ".     Indicator/Key register as appropriate",
        ".        ring/domain 0:0",
        ".   Send UPI interrupt to the IP so that it starts up",
        ".",
        ". Description of configuration bank:",
        ".      +---------+---------+---------+---------+---------+---------+",
        ". +00  | IPL_IP  | IPL_IOP | IPL_MSP |IPL_CHMOD|IPL_DEVNM|IPL_FLAGS|",
        ".      +---------+---------+---------+---------+---------+---------+",
        ". +01  |                             |          IPL_PREPF          |",
        ".      +---------+---------+---------+---------+---------+---------+",
        ". +02  |                         JUMP KEYS                         |",
        ".      +---------+---------+---------+---------+---------+---------+",
        ". IPL_IP:     UPI of the IP which is in control of the IPL",
        ". IPL_IOP:    UPI of the IOP which we are using for the IPL",
        ". IPL_MSP:    UPI of the MSP into which we load the OS",
        ". IPL_CHMOD:  Channel module index for the boot path",
        ". IPL_DEVNM:  Device number from which we are booting",
        ". IPL_FLAGS:  Bit35: true for tape boot, false for disk boot",
        ". IPL_PREPF:  For disk boots, the block size of a single IO",
        ".               for the pack which we are booting from.",
        ". JUMP_KEYS:  Jump keys which control the IPL and startup processes",
        "",
        "          $EXTEND",
        "          $ASCII",
        "",
        ". ---------------- Procs ------------------",
        "",
        ". Proc to create a stack and base it on a bank",
        ". Parameter 1,0 is the frame size",
        ". Parameter 1,1 is the total stack size",
        ". Parameter 2,0 is the base register",
        ". Parameter 2,1 is the index register",
        "GENSTACK  $PROC",
        "          LA,S3     A0,0,B2             . get MSP UPI",
        "          SA,S2     A0,ALLOCPKT,,B0     . Store it in alloc pkt",
        "          LA,U      A0,GENSTACK(1,1)    . Get stack size",
        "          SA,W      A0,ALLOCPKT+1,,B0   . Store in alloc pkt",
        "          SA,H2     A0,LBEDPKT+1,,B0    . and in Upper limit for LBED",
        "          SYSC      ALLOCPKT,,B0        . allocate space for the stack",
        "",
        "          SZ,Q1     LBEDPKT+1,,B0       . Lower limit for LBED is 0",
        "          LA,W      A0,ALLOCPKT+2,,B0   . Get segment for storage and",
        "          SA,W      A0,LBEDPKT+2,,B0    . store it in the LBED pkt",
        "          LA,S3     A0,0,B2             . MSP UPI again, zero offset",
        "          LSSL      A0,32",
        "          SA,W      A0,LBEDPKT+3,,B0    . store in LBED pkt",
        "          LBED      P(2,0),LBEDPKT,,B0  . Base the stack bank",
        "",
        "          LXI,U     P(2,1),P(1,0)       . set up stack pointer",
        "          LXM       P(2,1),ALLOCPKT+1,,B0",
        "",
        "          $ENDP",
        "",
        "",
        ". ---------------- Constants -------------------",
        "",
        "ICSFRAMESZ  $EQU 8       . size of an ICS frame",
        "ICSFRAMECNT $EQU 4       . arbitrary number of ICS frames",
        "ICSSIZE     $EQU ICSFRAMECNT*ICSFRAMESZ",
        "",
        "RCSFRAMESZ  $EQU 2       . RCS frames are always 2 words",
        "RCSFRAMECNT $EQU 4       . Arbitrary stack depth for loader",
        "RCSSIZE     $EQU RCSFRAMESZ*RCSFRAMECNT",
        "",
        "",
        ". ---------------- Data -------------------",
        "",
        "$(0)      $LIT . Put literals into the data location counter",
        "          . Data - to be based on B0 along with the code.",
        "          . Note that this requires the containing bank to be read/write",
        "",
        "B0BR      . BaseRegister 0 contents",
        "          $RES 4",
        "",
        "ALLOCPKT  . SYSC packet for allocating memory",
        "          + 0200000,0 . subfunc 020, MSP UPI goes in S2",
        "          + 0         . size requested",
        "          + 0         . segment index returned here",
        "",
        "LBEDPKT   . Base register content for LBED",
        "          + 030000,0   . GAP:0, SAP:RW, Ring/Domain 0:0",
        "          + 0          . Lower limit in Q1, Upper limit in H2",
        "          + 0          . Base addr segment in bits 5-35",
        "          + 0          . Base addr MSP UPI in bits 0-3,",
        "                       .            offset in bits 5-35",
        "",
        "IOPKT     . Packet for reading from load device",
        "          + 040,0,0,0,0,0   . Func,IOP,CMOD,DEV,0,Flags",
        "          + 002,040,0,0,0,0 . Write,TypeC|Fwd,Status,Residue,0,0",
        "          + 0,0             . buffer size in H1, words xferd in H2",
        "          + 0               . IO buffer absolute address",
        "          + 0               .   (word 2 of abs addr)",
        "          + 0               . Device block address (for disk)",
        "",
        "",
        ". ---------------- Code -------------------",
        "$(1)",
        "START$*",
        "          PAIJ      $+1                 . Don't bother us for a while",
        "          SBUD      B0,B0BR,,B0         . Save B0 contents",
        "          LD        (000001,000010),,B0 . Make sure DR is set correctly",
        "",
        "          . Set up the ICS and RCS",
        "          GENSTACK  ICSFRAMESZ,ICSSIZE B26,EX1",
        "          GENSTACK  RCSFRAMESZ,RCSSIZE B25,EX0",
        "",
        ". The block which follows this boot block contains the level 0",
        ". Bank Descriptor Table. This table describes all of the other banks",
        ". which comprise the rest of the operating system.",
        ". For disks, the boot block is in block 0, but the successive blocks",
        ". are located beginning at block 3 (block 1 unused, block 2 is the",
        ". VOL1 label",
        "",
        ". Set up the non-variant portions of the IO packet",
        "          LA,S2     A0,0,,B2            . UPI of load IOP",
        "          SA,S2     A0,IOPKT,,B0",
        "          LA,S4     A0,0,,B2            . Channel Module Index",
        "          SA,S3     A0,IOPKT,,B0",
        "          LA,S5     A0,0,,B2            . Device Index",
        "          SA,S4     A0,IOPKT,,B0",
        "",
        ". Load the first bank from the boot media.",
        ". This is the level 0 BDT including the interrupt vectors.",
        ". Once loaded, base it on B16.",
        "          LA,U      A0,LEVEL0BDT$SZ     . How big is the BDT?",
        "          SA,H2     A0,LBEDPKT+1,,B0",
        "          LOCL      LOADBANK",
        "          DS        A0,LBEDPKT+2,,B0",
        "          LBED      B16,LBEDPKT,,B0",
        "",
        ". Now iterate over the bank descriptors in the level 0 BDT.",
        ". Each successive BD describes banks stored consecutively on the",
        ". boot media. For each BD, load the described bank, then update",
        ". the BD with the absolute address of the loaded bank.",
        "          LXI,U     X8,8                . BD pointer",
        "          LXM,U     X8,8*32",
        "          LA,U      A0,LEVEL0BDT$SZ     . Set to the number of",
        "          ANA,U     A8,8*32             . banks in the BDT",
        "          SSL       A8,3",
        "          ANA,U     A8,1                . and one less, for the loop",
        "",
        "LOOP",
        ". Get the upper and normalized lower limits, to determine the size",
        ". of the bank for this bank descriptor.  It won't be a large bank...",
        "          LA,H2     A0,1,X8,B16",
        "          LA,Q1     A1,1,X8,B16",
        "          LSSL      A1,9",
        "          ANA       A0,A1               . A0 is now the bank size",
        "          LOCL      LOADBANK            . Load the bank",
        "          DS        A0,2,*X8,B16        . Update the BD",
        "          JGD       A8,LOOP",
        "",
        ". All done - go to the os initialization code",
        "          GOTO      KOS$INIT",
        ".",
        ". Allocates space for a bank, then loads one or more blocks from the",
        ". boot medium to fill up the bank.",
        ". Parameters A0:    Size of the bank to be loaded",
        ".            A1:    Disk block address (zero for tape)",
        ". Returns in A0/A1: Absolute Address of the loaded bank",
        ".            A2:    Blocks read",
        "LOADBANK",
        ". If this is a disk boot, increase size to the next multiple",
        ". of the disk prep factor.",
        "          LA        A2,0,,B2",
        "          AND,U     A2,01",
        "          JNZ       A3,LOADBANK010      . Jump if tape boot",
        "",
        "          SA        A1,IOPKT+5,,B0      . Save disk block address",
        "",
        "          LA,U      A2,0                . Divide bank size by prep",
        "          LA        A3,A0               .  factor and add the remainder",
        "          DI,H2     A2,1,,B2            .  to the bank size.",
        "          AA        A0,A3",
        "",
        "LOADBANK010",
        ". Allocate a block of memory to hold the bank we're going to load.",
        "          LA,S3     A1,0,B2             . get MSP UPI",
        "          SA,S2     A1,ALLOCPKT,,B0     . Store it in alloc pkt",
        "          SA,W      A0,ALLOCPKT+1,,B0   . Store req size in alloc pkt",
        "          SYSC      ALLOCPKT,,B0        . allocate space for the load",
        "",
        ". Set up initial values in the IO packet.",
        ". Block address is already there if this is disk.",
        "          SA,H1     A0,IOPKT+2,,B0      . Current buffer size",
        "",
        "          LA,W      A0,ALLOCPKT+2,,B0   . MSP segment index",
        "          LA,S2     A1,ALLOCPKT,,B0     . UPI of MSP",
        "          LSSL      A1,32               . offset is zero",
        "          DS        A0,IOPKT+3,,B0      . A0/A1 is buffer abs addr",
        "",
        "          LA,U      A2,0                . Block count init",
        "",
        "LOADBANKIO",
        "          SYSC      IOPKT,,B0           . Start the IO",
        "",
        "LOADBANKWAIT",
        "          LA,S3     A0,IOPKT+1,,B0      . Wait until the IO is done",
        "          TE,U      A0,040",
        "          J         LOADBANK040",
        "          BT        X0,0,X0,0",
        "          J         LOADBANKWAIT",
        "",
        "          TE,U      A0,0                . Successful?",
        "          IAR       0145                . No. NO BOOT FOR YOU!",
        "",
        ". Increment blocks-read counter",
        "          AA,U      A2,1",
        "",
        ". Update buffer size to remaining size by subtracting words xfrd.",
        ". Check whether anything is left to be done.",
        "          LA,H1     A0,IOPKT+2,,B0",
        "          AN,H2     A0,IOPKT+2,,B0",
        "          SA,H1     A0,IOPKT+2,,B0",
        "",
        ". Update offset by adding words xfrd.",
        "          LA        A0,IOPKT+4,,B0",
        "          AA,H2     A0,IOPKT+2,,B0",
        "          SA        A0,IOPKT+4,,B0",
        "",
        ". Increment block address and go back for more.",
        "          ADD1      IOPKT+5,,B0",
        "          J         LOADBANKIO",
        "",
        "LOADBANKDONE",
        ". Retrieve bank absolute address from ALLOCPKT",
        "          LA,W      A0,ALLOCPKT+2,,B0   . MSP segment index",
        "          LA,S2     A1,ALLOCPKT,,B0     . UPI of MSP",
        "          LSSL      A1,32               . offset is zero",
        "",
        "          RTN",
    };
}
