/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.directives;

import com.kadware.komodo.minalib.Assembler;
import com.kadware.komodo.minalib.RelocatableModule;
import org.junit.Test;
import static org.junit.Assert.*;

public class Test_Equf {

    //TODO leaving this here for now, in case we find a reasonable way to implement $EQUF in minalib

//    @Test
//    public void operationJSubfield(
//    ) {
//        String[] source = {
//            "          $BASIC",
//            "LABEL     $EQUF,S3 . ",
//            "LA        A5,LABEL,X3"
//        };
//
//        Assembler.Option[] optionSet = {
//            Assembler.Option.EMIT_MODULE_SUMMARY,
//            Assembler.Option.EMIT_SOURCE,
//            Assembler.Option.EMIT_GENERATED_CODE,
//            Assembler.Option.EMIT_DICTIONARY,
//        };
//
//        Assembler asm = new Assembler();
//        asm.assemble("TEST", source, optionSet);
//        assertTrue(asm.getDiagnostics().isEmpty());
//    }

//    @Test
//    public void multipleReferences(
//    ) {
//        String[] source = {
//            "          $BASIC",
//            "LABEL     $EQUF,S3 . ",
//            "LA        A5,LABEL,LABEL"
//        };
//
//        Assembler.Option[] optionSet = {
//            Assembler.Option.EMIT_MODULE_SUMMARY,
//            Assembler.Option.EMIT_SOURCE,
//            Assembler.Option.EMIT_GENERATED_CODE,
//            Assembler.Option.EMIT_DICTIONARY,
//        };
//
//        Assembler asm = new Assembler();
//        asm.assemble("TEST", source, optionSet);
//        assertTrue(asm.getDiagnostics().isEmpty());
//    }

//    @Test
//    public void noFields(
//    ) {
//        String[] source = {
//            "          $BASIC",
//            "E         $EQUF . ",
//        };
//
//        Assembler.Option[] optionSet = {
//            Assembler.Option.EMIT_MODULE_SUMMARY,
//            Assembler.Option.EMIT_SOURCE,
//            Assembler.Option.EMIT_GENERATED_CODE,
//            Assembler.Option.EMIT_DICTIONARY,
//        };
//
//        Assembler asm = new Assembler();
//        asm.assemble("TEST", source, optionSet);
//        assertFalse(asm.getDiagnostics().isEmpty());
//    }

//    @Test
//    public void noLabel(
//    ) {
//        String[] source = {
//            "          $BASIC",
//            "          $EQUF 0",
//        };
//
//        Assembler.Option[] optionSet = {
//            Assembler.Option.EMIT_MODULE_SUMMARY,
//            Assembler.Option.EMIT_SOURCE,
//            Assembler.Option.EMIT_GENERATED_CODE,
//            Assembler.Option.EMIT_DICTIONARY,
//        };
//
//        Assembler asm = new Assembler();
//        asm.assemble("TEST", source, optionSet);
//        assertFalse(asm.getDiagnostics().isEmpty());
//    }

//    @Test
//    public void tooManyFields(
//    ) {
//        String[] source = {
//            "          $BASIC",
//            "E         $EQUF,S3 015,X5,,B3 FOO. ",
//        };
//
//        Assembler.Option[] optionSet = {
//            Assembler.Option.EMIT_MODULE_SUMMARY,
//            Assembler.Option.EMIT_SOURCE,
//            Assembler.Option.EMIT_GENERATED_CODE,
//            Assembler.Option.EMIT_DICTIONARY,
//        };
//
//        Assembler asm = new Assembler();
//        asm.assemble("TEST", source, optionSet);
//        assertFalse(asm.getDiagnostics().isEmpty());
//    }

//    @Test
//    public void tooManyJFields(
//    ) {
//        String[] source = {
//            "          $BASIC",
//            "E         $EQUF,S3 015,X5,S4,B3 . ",
//        };
//
//        Assembler.Option[] optionSet = {
//            Assembler.Option.EMIT_MODULE_SUMMARY,
//            Assembler.Option.EMIT_SOURCE,
//            Assembler.Option.EMIT_GENERATED_CODE,
//            Assembler.Option.EMIT_DICTIONARY,
//        };
//
//        Assembler asm = new Assembler();
//        asm.assemble("TEST", source, optionSet);
//        assertFalse(asm.getDiagnostics().isEmpty());
//    }

//    @Test
//    public void tooManyOperationSubfields(
//    ) {
//        String[] source = {
//            "          $BASIC",
//            "E         $EQUF,S3,X4 015,X5 . ",
//        };
//
//        Assembler.Option[] optionSet = {
//            Assembler.Option.EMIT_MODULE_SUMMARY,
//            Assembler.Option.EMIT_SOURCE,
//            Assembler.Option.EMIT_GENERATED_CODE,
//            Assembler.Option.EMIT_DICTIONARY,
//        };
//
//        Assembler asm = new Assembler();
//        asm.assemble("TEST", source, optionSet);
//        assertFalse(asm.getDiagnostics().isEmpty());
//    }

//    @Test
//    public void tooManyOperandSubfields(
//    ) {
//        String[] source = {
//            "          $BASIC",
//            "E         $EQUF,S3  015,X5,,,FOO . ",
//        };
//
//        Assembler.Option[] optionSet = {
//            Assembler.Option.EMIT_MODULE_SUMMARY,
//            Assembler.Option.EMIT_SOURCE,
//            Assembler.Option.EMIT_GENERATED_CODE,
//            Assembler.Option.EMIT_DICTIONARY,
//        };
//
//        Assembler asm = new Assembler();
//        asm.assemble("TEST", source, optionSet);
//        assertFalse(asm.getDiagnostics().isEmpty());
//    }
}
