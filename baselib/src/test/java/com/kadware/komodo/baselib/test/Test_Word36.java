/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib.test;

import static org.junit.Assert.*;
import org.junit.Test;

import com.kadware.komodo.baselib.Word36;

/**
 * Unit tests for Word36 class
 */
public class Test_Word36 {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance method tests
    //  Where possible, the instance methods leverage the logic in the static methods.
    //  This simplifies unit testing as well as reducing the opportunity for bugs.
    //  ----------------------------------------------------------------------------------------------------------------------------

    //  Partial-word getters -------------------------------------------------------------------------------------------------------

    @Test
    public void getH1(
    ) {
        assertEquals(0_123456l, (new Word36(0_123456_654321l).getH1()));
    }

    @Test
    public void getH2(
    ) {
        assertEquals(0_654321l, (new Word36(0_123456_654321l).getH2()));
    }

    @Test
    public void getQ1_0(
    ) {
        Word36 w = new Word36(07_000_777_777_777l);
        assertEquals(0, w.getQ1());
    }

    @Test
    public void getQ1_0765(
    ) {
        Word36 w = new Word36(07_765_777_777_777l);
        assertEquals(0765, w.getQ1());
    }

    @Test
    public void getQ2_0(
    ) {
        Word36 w = new Word36(07_777_000_777_777l);
        assertEquals(0, w.getQ2());
    }

    @Test
    public void getQ2_0765(
    ) {
        Word36 w = new Word36(07_777_765_777_777l);
        assertEquals(0765, w.getQ2());
    }

    @Test
    public void getQ3_0(
    ) {
        Word36 w = new Word36(07_777_777_000_777l);
        assertEquals(0, w.getQ3());
    }

    @Test
    public void getQ3_0765(
    ) {
        Word36 w = new Word36(07_777_777_765_777l);
        assertEquals(0765, w.getQ3());
    }

    @Test
    public void getQ4_0(
    ) {
        Word36 w = new Word36(07_777_777_777_000l);
        assertEquals(0, w.getQ4());
    }

    @Test
    public void getQ4_0765(
    ) {
        Word36 w = new Word36(07_777_777_777_765l);
        assertEquals(0765, w.getQ4());
    }

    @Test
    public void getS1(
    ) {
        assertEquals(0_12l, (new Word36(0_123456_654321l).getS1()));
    }

    @Test
    public void getS2(
    ) {
        assertEquals(0_34l, (new Word36(0_123456_654321l).getS2()));
    }

    @Test
    public void getS3(
    ) {
        assertEquals(0_56l, (new Word36(0_123456_654321l).getS3()));
    }

    @Test
    public void getS4(
    ) {
        assertEquals(0_65l, (new Word36(0_123456_654321l).getS4()));
    }

    @Test
    public void getS5(
    ) {
        assertEquals(0_43l, (new Word36(0_123456_654321l).getS5()));
    }

    @Test
    public void getS6(
    ) {
        assertEquals(0_21l, (new Word36(0_123456_654321l).getS6()));
    }

    @Test
    public void getT1(
    ) {
        assertEquals(0_1234l, (new Word36(0_1234_5665_4321l).getT1()));
    }

    @Test
    public void getT2(
    ) {
        assertEquals(0_5665l, (new Word36(0_1234_5665_4321l).getT2()));
    }

    @Test
    public void getT3(
    ) {
        assertEquals(0_4321l, (new Word36(0_1234_5665_4321l).getT3()));
    }

    @Test
    public void getTwelfth_0(
    ) {
        assertEquals(07, (new Word36(0_70_00_00_00_00_00l).getTwelfth(0)));
    }

    @Test
    public void getTwelfth_6_static(
    ) {
        assertEquals(07, Word36.getTwelfth(0_70_00_00l, 6));
    }

    @Test
    public void getTwelfth_11_static(
    ) {
        assertEquals(0, Word36.getTwelfth(0777777_777770l, 11));
    }


    @Test
    public void getXH1_Pos(
    ) {
        assertEquals(0_012345l, (new Word36(0_012345_234567l).getXH1()));
    }

    @Test
    public void getXH1_Neg(
    ) {
        assertEquals(0_777777_403020l, (new Word36(0_403020_407060l).getXH1()));
    }

    @Test
    public void getXH2_Pos(
    ) {
        assertEquals(0_234567l, (new Word36(0_012345_234567l).getXH2()));
    }

    @Test
    public void getXH2_Neg(
    ) {
        assertEquals(0_777777_407060l, (new Word36(0_403020_407060l).getXH2()));
    }

    @Test
    public void getXT1_Pos(
    ) {
        assertEquals(0_1234l, (new Word36(0_1234_2345_3456l).getXT1()));
    }

    @Test
    public void getXT1_Neg(
    ) {
        assertEquals(0_7777_7777_4321l, (new Word36(0_4321_5432_6543l).getXT1()));
    }

    @Test
    public void getXT2_Pos(
    ) {
        assertEquals(0_2345l, (new Word36(0_1234_2345_3456l).getXT2()));
    }

    @Test
    public void getXT2_Neg(
    ) {
        assertEquals(0_7777_7777_5432l, (new Word36(0_4321_5432_6543l).getXT2()));
    }

    @Test
    public void getXT3_Pos(
    ) {
        assertEquals(0_3456l, (new Word36(0_1234_2345_3456l).getXT3()));
    }

    @Test
    public void getXT3_Neg(
    ) {
        assertEquals(0_7777_7777_6543l, (new Word36(0_4321_5432_6543l).getXT3()));
    }

    @Test
    public void getW(
    ) {
        assertEquals(0_777555_333111l, (new Word36(0_777555_333111l).getW()));
    }


    //  Partial-word setters -------------------------------------------------------------------------------------------------------

    @Test
    public void setH1(
    ) {
        Word36 word36 = new Word36(0_012345);
        word36.setH1(0_775533l);
        assertEquals(0_775533_012345l, word36.getW());
    }

    @Test
    public void setH2(
    ) {
        Word36 word36 = new Word36(0_012345_543210l);
        word36.setH2(0_775533l);
        assertEquals(0_012345_775533l, word36.getW());
    }

    @Test
    public void setQ1(
    ) {
        Word36 word36 = new Word36(0_525252_252525l);
        word36.setQ1(0252);
        assertEquals(0_252252_252525l, word36.getW());
    }

    @Test
    public void setQ2(
    ) {
        Word36 word36 = new Word36(0_525252_252525l);
        word36.setQ2(0525);
        assertEquals(0_525525_252525l, word36.getW());
    }

    @Test
    public void setQ3(
    ) {
        Word36 word36 = new Word36(0_525252_252525l);
        word36.setQ3(0525);
        assertEquals(0_525252_525525l, word36.getW());
    }

    @Test
    public void setQ4(
    ) {
        Word36 word36 = new Word36(0_525252_252525l);
        word36.setQ4(0252);
        assertEquals(0_525252_252252l, word36.getW());
    }

    @Test
    public void setS1(
    ) {
        Word36 word36 = new Word36(0_777777_777777l);
        word36.setS1(0);
        assertEquals(0_007777_777777l, word36.getW());
    }

    @Test
    public void setS2(
    ) {
        Word36 word36 = new Word36(0_777777_777777l);
        word36.setS2(0);
        assertEquals(0_770077_777777l, word36.getW());
    }

    @Test
    public void setS3(
    ) {
        Word36 word36 = new Word36(0_777777_777777l);
        word36.setS3(0);
        assertEquals(0_777700_777777l, word36.getW());
    }

    @Test
    public void setS4(
    ) {
        Word36 word36 = new Word36(0_777777_777777l);
        word36.setS4(0);
        assertEquals(0_777777_007777l, word36.getW());
    }

    @Test
    public void setS5(
    ) {
        Word36 word36 = new Word36(0_777777_777777l);
        word36.setS5(0);
        assertEquals(0_777777_770077l, word36.getW());
    }

    @Test
    public void setS6(
    ) {
        Word36 word36 = new Word36(0_777777_777777l);
        word36.setS6(0);
        assertEquals(0_777777_777700l, word36.getW());
    }

    @Test
    public void setT1(
    ) {
        Word36 word36 = new Word36(0_4444_4444_4444l);
        word36.setT1(03333);
        assertEquals(0_3333_4444_4444l, word36.getW());
    }

    @Test
    public void setT2(
    ) {
        Word36 word36 = new Word36(0_4444_4444_4444l);
        word36.setT2(03333);
        assertEquals(0_4444_3333_4444l, word36.getW());
    }

    @Test
    public void setT3(
    ) {
        Word36 word36 = new Word36(0_4444_4444_4444l);
        word36.setT3(03333);
        assertEquals(0_4444_4444_3333l, word36.getW());
    }

    @Test
    public void setW(
    ) {
        Word36 word36 = new Word36(0_4444_4444_4444l);
        word36.setW(0);
        assertEquals(0, word36.getW());
    }

    @Test
    public void setTwelfth_TW0_Into0(
    ) {
        Word36 w = new Word36(0);
        w.setTwelfth(7, 0);
        assertEquals(0_700000_000000l, w.getW());
    }

    @Test
    public void setTwelfth_TW0_Into3(
    ) {
        Word36 w = new Word36(0_333333_333333l);
        w.setTwelfth(7, 0);
        assertEquals(0_733333_333333l, w.getW());
    }

    @Test
    public void setTwelfth_TW1_Into0(
    ) {
        Word36 w = new Word36(0);
        w.setTwelfth(7, 1);
        assertEquals(0_070000_000000l, w.getW());
    }

    @Test
    public void setTwelfth_TW1_Into3(
    ) {
        Word36 w = new Word36(0_333333_333333l);
        w.setTwelfth(7, 1);
        assertEquals(0_373333_333333l, w.getW());
    }

    @Test
    public void setTwelfth_TW5_Into0(
    ) {
        Word36 w = new Word36(0);
        w.setTwelfth(7, 5);
        assertEquals(0_000007_000000l, w.getW());
    }

    @Test
    public void setTwelfth_TW5_Into3(
    ) {
        Word36 w = new Word36(0_333333_333333l);
        w.setTwelfth(7, 5);
        assertEquals(0_333337_333333l, w.getW());
    }

    @Test
    public void setTwelfth_TW11_Into0(
    ) {
        Word36 w = new Word36(0);
        w.setTwelfth(7, 11);
        assertEquals(0_000000_000007l, w.getW());
    }

    @Test
    public void setTwelfth_TW11X_Into3_static(
    ) {
        Word36 w = new Word36(0_333333_333333l);
        w.setTwelfth(7, 11);
        assertEquals(0_333333_333337l, w.getW());
    }


    //  Display --------------------------------------------------------------------------------------------------------------------

    @Test
    public void toASCII(
    ) {
        long word = 0_101_102_103_104l;
        assertEquals("ABCD", Word36.toASCII(word));
    }

    @Test
    public void toFieldata(
    ) {
        long word = 0_05_06_07_10_11_12l;
        assertEquals(" ABCDE", Word36.toFieldata(word));
    }

    @Test
    public void toOctal(
    ) {
        long word = 0_05_06_07_10_11_12l;
        assertEquals("050607101112", Word36.toOctal(word));
    }


    //  Misc -----------------------------------------------------------------------------------------------------------------------

    @Test
    public void stringToWord36ASCII(
    ) {
        Word36 w = Word36.stringToWord36ASCII("Help");
        assertEquals(0_110_145_154_160l, w.getW());
    }

    @Test
    public void stringToWord36ASCII_over(
    ) {
        Word36 w = Word36.stringToWord36ASCII("HelpSlop");
        assertEquals(0_110_145_154_160l, w.getW());
    }

    @Test
    public void stringToWord36ASCII_partial(
    ) {
        Word36 w = Word36.stringToWord36ASCII("01");
        assertEquals(0_060_061_040_040l, w.getW());
    }

    @Test
    public void stringToWord36Fieldata(
    ) {
        Word36 w = Word36.stringToWord36Fieldata("Abc@23");
        assertEquals(0_060710_006263l, w.getW());
    }

    @Test
    public void stringToWord36Fieldata_over(
    ) {
        Word36 w = Word36.stringToWord36Fieldata("A B C@D E F");
        assertEquals(0_060507_051000l, w.getW());
    }

    @Test
    public void stringToWord36Fieldata_partial(
    ) {
        Word36 w = Word36.stringToWord36Fieldata("1234");
        assertEquals(0_616263_640505l, w.getW());
    }


    //  Sign-extension tests -------------------------------------------------------------------------------------------------------

    @Test
    public void getSignExtended12_positive(
    ) {
        assertEquals(03765, Word36.getSignExtended12(03765));
    }

    @Test
    public void getSignExtended12_negative(
    ) {
        assertEquals(0_777777_774765l, Word36.getSignExtended12(04765));
    }

    @Test
    public void getSignExtended18_positive(
    ) {
        assertEquals(0_376500, Word36.getSignExtended18(0_376500));
    }

    @Test
    public void getSignExtended18_negative(
    ) {
        assertEquals(0_777777_400001l, Word36.getSignExtended18(0_400001));
    }

    @Test
    public void getSignExtended24_positive(
    ) {
        assertEquals(0_000037_776500l, Word36.getSignExtended24(0_000037_776500l));
    }

    @Test
    public void getSignExtended24_negative(
    ) {
        assertEquals(0_777767_776500l, Word36.getSignExtended24(0_000067_776500l));
    }
}
