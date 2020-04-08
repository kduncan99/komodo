/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kexec;

public class Level0BDT {

    public static final String[] SOURCE = {
        ". LEVEL0BDT",
        ". Copyright (C) 2019 by Kurt Duncan - All Rights Reserved",
        ".",
        ". This code represents the BDT for level 0, which is global to the whole system.",
        ". It will contain BankDescriptors for all the Level 0 KOS banks.",
        ". Currently, we plan for this to include kernel-level stuff, but maybe not lower-level things",
        ". such as SMOQUE, MFD, PF, things like that.",
        ". We DO expect it to contain top-level interrupt handling, task scheduling, and memory management.",
        ".",
        "$(0)      $LIT",
        ".",
        "LEVEL0BDT$* . external label",
        ".",
        ". The first 64 words are interrupt vectors for corresponding interrupt classes.",
        ". The vector for interrupt class 0 is +0, for class 1 is +1, etc.",
        ". The vectors are in L,BDI,OFFSET format, and we expect the corresponding routines to be",
        ". named IH$ooo where ooo is the octal representation of the interrupt class.",
        ".",
        "          + BDIREF$+IH$000,IH$000",
        "          + BDIREF$+IH$001,IH$001",
        "          + BDIREF$+IH$002,IH$002",
        "          + BDIREF$+IH$003,IH$003",
        "          + BDIREF$+IH$004,IH$004",
        "          + BDIREF$+IH$005,IH$005",
        "          + BDIREF$+IH$006,IH$006",
        "          + BDIREF$+IH$007,IH$007",
        "          + BDIREF$+IH$010,IH$000",
        "          + BDIREF$+IH$011,IH$011",
        "          + BDIREF$+IH$012,IH$012",
        "          + BDIREF$+IH$013,IH$013",
        "          + BDIREF$+IH$014,IH$014",
        "          + BDIREF$+IH$015,IH$015",
        "          + BDIREF$+IH$016,IH$016",
        "          + BDIREF$+IH$017,IH$017",
        "          + BDIREF$+IH$020,IH$020",
        "          + BDIREF$+IH$021,IH$021",
        "          + BDIREF$+IH$022,IH$022",
        "          + BDIREF$+IH$023,IH$023",
        "          + BDIREF$+IH$024,IH$024",
        "          + BDIREF$+IH$025,IH$025",
        "          + BDIREF$+IH$026,IH$026",
        "          + BDIREF$+IH$027,IH$027",
        "          + BDIREF$+IH$030,IH$030",
        "          + BDIREF$+IH$031,IH$031",
        "          + BDIREF$+IH$032,IH$032",
        "          + BDIREF$+IH$033,IH$033",
        "          + BDIREF$+IH$034,IH$034",
        "          + BDIREF$+IH$035,IH$035",
        "          + BDIREF$+IH$036,IH$036",
        "          + BDIREF$+IH$037,IH$037",
        "          + BDIREF$+IH$040,IH$040",
        "          + BDIREF$+IH$041,IH$041",
        "          + BDIREF$+IH$042,IH$042",
        "          + BDIREF$+IH$043,IH$043",
        "          + BDIREF$+IH$044,IH$044",
        "          + BDIREF$+IH$045,IH$045",
        "          + BDIREF$+IH$046,IH$046",
        "          + BDIREF$+IH$047,IH$047",
        "          + BDIREF$+IH$050,IH$050",
        "          + BDIREF$+IH$051,IH$051",
        "          + BDIREF$+IH$052,IH$052",
        "          + BDIREF$+IH$053,IH$053",
        "          + BDIREF$+IH$054,IH$054",
        "          + BDIREF$+IH$055,IH$055",
        "          + BDIREF$+IH$056,IH$056",
        "          + BDIREF$+IH$057,IH$057",
        "          + BDIREF$+IH$060,IH$060",
        "          + BDIREF$+IH$061,IH$061",
        "          + BDIREF$+IH$062,IH$062",
        "          + BDIREF$+IH$063,IH$063",
        "          + BDIREF$+IH$064,IH$064",
        "          + BDIREF$+IH$065,IH$065",
        "          + BDIREF$+IH$066,IH$066",
        "          + BDIREF$+IH$067,IH$067",
        "          + BDIREF$+IH$070,IH$070",
        "          + BDIREF$+IH$071,IH$071",
        "          + BDIREF$+IH$072,IH$072",
        "          + BDIREF$+IH$073,IH$073",
        "          + BDIREF$+IH$074,IH$074",
        "          + BDIREF$+IH$075,IH$075",
        "          + BDIREF$+IH$076,IH$076",
        "          + BDIREF$+IH$077,IH$077",
        "",
        ". BDIs less than 0,32 are illegal.",
        ". The space taken by what would have been the Bank Descriptors for banks 0,0 to 0.7",
        ". are stuffed with the 64-word interrupt vector table, above.",
        ". There are no Bank Descriptors for banks 0,8 through 0,31 either.",
        ".",
        "          $RES 8*24",
        ".",
        ". The Bank Descriptors for banks 0,32 and upward, are located here.",
        //TODO
        "",
        "LEVEL0BDT$SZ* $EQU $-LEVEL0BDT$",
    };
}
