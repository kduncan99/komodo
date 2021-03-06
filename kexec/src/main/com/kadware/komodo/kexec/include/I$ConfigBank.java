/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kexec.include;

/**
 * Source code describing the config bank for kexec
 */
public class I$ConfigBank {

    /*  Description of configuration bank:
     *
     *       +---------+---------+---------+---------+---------+---------+
     *  +00  | IPL_SP  | IPL_IP  | IPL_IOP | IPL_MSP |IPL_CHMOD|IPL_DEVNM|
     *       +---------+---------+---------+---------+---------+---------+
     *  +01  |IPL_FLAGS|                   |          IPL_PREPF          |
     *       +---------+---------+---------+---------+---------+---------+
     *  +02  |                         JUMP KEYS                         |
     *       +---------+---------+---------+---------+---------+---------+
     *  +03  |                      MAILBOX_ADDRESS                      |
     *  +04  |                                                           |
     *       +---------+---------+---------+---------+---------+---------+
     *
     *  IPL_SP:          UPI of the SP which is controlling the IPL
     *  IPL_IP:          UPI of the IP which is in control of the IPL
     *  IPL_IOP:         UPI of the IOP which we are using for the IPL
     *  IPL_MSP:         UPI of the MSP into which we load the OS
     *  IPL_CHMOD:       Channel module index for the boot path
     *  IPL_DEVNM:       Device number from which we are booting
     *  IPL_FLAGS:
     *    Bit5:          true for tape boot, false for disk boot
     *  IPL_PREPF:       For disk boots, the block size of a single IO for the pack which we are booting from.
     *  JUMP_KEYS:       Jump keys which control the IPL and startup processes
     *  MAILBOX_ADDRESS: Absolute address of block of memory containing the UPI mailboxes
     */

    public static final String[] SOURCE = {
        ". .....................................",
        ". Configuration bank equfs",
        ".",
        "CFGBNK$IPLSP     $EQUF      0,,S1",
        "CFGBNK$IPLIP     $EQUF      0,,S2",
        "CFGBNK$IPLIOP    $EQUF      0,,S3",
        "CFGBNK$IPLMSP    $EQUF      0,,S4",
        "CFGBNK$IPLCHMOD  $EQUF      0,,S5",
        "CFGBNK$IPLDEVNM  $EQUF      0,,S6",
        "CFGBNK$IPLFLAGS  $EQUF      1,,S1",
        "CFGBNK$IPLPREPF  $EQUF      1,,H2",
        "CFGBNK$JUMPKEYS  $EQUF      2,,W",
        "CFGBNK$MBOXADDR  $EQUF      3",
        ". ....................................."
    };
}
