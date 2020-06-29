/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.directives;

import com.kadware.komodo.kex.RelocatableModule;
import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.AssemblerOption;
import com.kadware.komodo.kex.kasm.AssemblerResult;
import com.kadware.komodo.kex.kasm.dictionary.Dictionary;
import com.kadware.komodo.kex.kasm.exceptions.ParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class Test_Include {

    private static final AssemblerOption[] DEFINITION_OPTIONS = {
        AssemblerOption.DEFINITION_MODE,
        AssemblerOption.EMIT_MODULE_SUMMARY,
        AssemblerOption.EMIT_SOURCE,
        AssemblerOption.EMIT_DICTIONARY
    };

    private static final AssemblerOption[] GENERATION_OPTIONS = {
        AssemblerOption.EMIT_MODULE_SUMMARY,
        AssemblerOption.EMIT_SOURCE,
        AssemblerOption.EMIT_GENERATED_CODE,
        AssemblerOption.EMIT_DICTIONARY
    };

    private static final Set<AssemblerOption> DEFINITION_OPTION_SET = new HashSet<>(Arrays.asList(DEFINITION_OPTIONS));
    private static final Set<AssemblerOption> GENERATION_OPTION_SET = new HashSet<>(Arrays.asList(GENERATION_OPTIONS));

    private static final Map<String, Dictionary> DEFINITION_SETS = new HashMap<>();

    public static void setupSet(
        final String name,
        final String[] source
    ) {
        Assembler asm = new Assembler.Builder().setModuleName(name)
                                               .setOptions(DEFINITION_OPTION_SET)
                                               .setSource(source)
                                               .build();
        AssemblerResult result = asm.assemble();
        DEFINITION_SETS.put(name, result._definitions);
    }

    @BeforeClass
    public static void setup() {
        String[] colors_source = {
            "C$BLACK*   $EQU 00",
            "C$BLUE*    $EQU 01",
            "C$GREEN*   $EQU 02",
            "C$CYAN*    $EQU 03",
            "C$RED*     $EQU 04",
            "C$MAGENTA* $EQU 05",
            "C$YELLOW*  $EQU 06",
            "C$WHITE*   $EQU 07"
        };

        String[] shapes_source = {
            "S$SQUARE*      $EQU 00",
            "S$CIRCLE*      $EQU 01",
            "S$RECTANGLE*   $EQU 02",
            "S$TRIANGLE*    $EQU 03",
            "S$PENTAGON*    $EQU 05",
            "S$OCTAGON*     $EQU 06"
        };

        String[] fruits_source = {
            "F$BANANA       $EQUF 0,,S1",
            "F$APPLE        $EQUF 0,,S2",
            "F$STRAWBERRY   $EQUF 0,,S3"
        };

        setupSet("COLORS", colors_source);
        setupSet("SHAPES", shapes_source);
        setupSet("FRUITS", fruits_source);
    }

    @Test
    public void simple(
    ) throws ParameterException {
        String[] source = {
            "          $BASIC",
            "          $INCLUDE 'COLORS'",
            "          $INCLUDE 'SHAPES'",
            "",
            "$(0) .",
            "          + C$CYAN",
            "          + S$PENTAGON",
            "          $END"
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setDefinitionSets(DEFINITION_SETS)
                                               .setOptions(GENERATION_OPTION_SET)
                                               .setSource(source)
                                               .build();
        AssemblerResult result = asm.assemble();
        assertTrue(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);
        RelocatableModule.RelocatablePool pool = result._relocatableModule.getLocationCounterPool(0);
        assertEquals(2, pool._content.length);
        assertEquals(03L, pool._content[0].getW());
        assertEquals(05L, pool._content[1].getW());
    }

    @Test
    public void not_found(
    ) throws ParameterException {
        String[] source = {
            "          $BASIC",
            "          $INCLUDE 'COLORS'",
            "          $INCLUDE 'SHAPES'",
            "          $INCLUDE 'FLOOBIES'",
            "",
            "$(0) .",
            "          + C$CYAN",
            "          + S$PENTAGON",
            "          $END"
        };

        Assembler asm = new Assembler.Builder().setModuleName("TEST")
                                               .setDefinitionSets(DEFINITION_SETS)
                                               .setOptions(GENERATION_OPTION_SET)
                                               .setSource(source)
                                               .build();
        AssemblerResult result = asm.assemble();
        assertFalse(result._diagnostics.isEmpty());
        assertNotNull(result._relocatableModule);
        RelocatableModule.RelocatablePool pool = result._relocatableModule.getLocationCounterPool(0);
        assertEquals(2, pool._content.length);
        assertEquals(03L, pool._content[0].getW());
        assertEquals(05L, pool._content[1].getW());
    }
}
