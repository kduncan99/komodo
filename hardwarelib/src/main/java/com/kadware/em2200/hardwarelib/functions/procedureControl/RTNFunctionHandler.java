/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.procedureControl;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.InstructionHandler;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;

/**
 * Handles the RTN instruction f=073 j=017 a=03
 */
public class RTNFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long[] rcsFrame = rcsPop(ip);
        if ((rcsFrame[1] & 0_000002_000000L) == 0) {
            //  return to extended mode
            //TODO
            /*
                1. A model_dependent check must be made for a possible RCS underflow as described in 4.6.4,
                    in the note prior to step 1.

                2. A determination is made of the Base_Register information to be loaded into B0 as described
                in 4.6.4, steps 3 through 9 (including any interrupts that may be generated); RCS.L,BDI is the
                Source L,BDI.
                2.3 Use RCS.L,BDI
                2.4 L,BDI in range 0,1 to 0,31 ?-> Addressing_Exception - if in interrupt sequence, we halt
                2.5 Void bank ?-> Addressing_Exception
                2.6 Go get the base descriptor
                2.7 banktype=extended - proceed
                            =basic - Addressing_exception
                            =gate - Adressing_exception
                            =indirect - Addressing_exception
                            =queue - Addressing_exception
                            =qbr - Addressing_exception
                            =reserved - Addressing_exception
                2.8 n/a (indirect bank already caused exception)
                2.9 n/a (gate bank already caused exception)

                3. The hard-held Access_Key := RCS.Access_Key and DB12–17 := RCS.DB12-17.

                4. Hard-held PAR.L,BDI := RCS.L,BDI and hard-held PAR.PC := RCS.Offset.

                5. The appropriate information (as determined in step 2 above) is loaded in B0.

                6. If the BD.G = 1 or the RCS.Trap = 1, a Terminal_Addressing_Exception interrupt occurs. Note:
                the environment stored on the Interrupt_Control_Stack reflects the environment after
                steps 3 and 4 above. No check for Enter access is made on RTN.
             */
        } else {
            //  return to basic mode
            //TODO
            /*
                1. A determination is made of the Base_Register information to be loaded as described in 4.6.4,
                steps 3 through 9 (including any interrupts that may be generated); RCS.L,BDI is the Source
                L,BDI.
                1.3 Use RCS.L,BDI
                1.4 L,BDI in range 0,1 to 0,31 ?-> Addressing_Exception - if in interrupt sequence, we halt
                1.5 Void bank ?-> load void bank (on B0) and done with step 1, else
                1.6 Go get the base descriptor
                1.7 banktype=extended - proceed
                            =basic - proceed
                            =gate - proceed
                            =indirect - Addressing_exception
                            =queue - Addressing_exception
                            =qbr - Addressing_exception
                            =reserved - Addressing_exception
                1.8 n/a (indirect bank already caused exception)
                1.9 If Gate BD: A Source BD.Type = Gate has now been fetched for an instruction that can invoke
                    Gate processing. If the Gate BD.G = 1, an Addressing_Exception interrupt occurs. If Enter
                    access to the Gate Bank is denied (current Access_Key is checked against the Access_Lock
                    of the Gate BD to select either GAP or SAP), an Addressing_Exception interrupt occurs*.
                    Otherwise, the Gate is fetched as follows:
                    a. Source Offset is limits checked against the Gate BD; if a limits violation is detected an
                    Addressing_Exception interrupt occurs.
                    b. If either (model_dependent) an absolute boundary violation is detected on the Gate
                    address or the Xa.Offset does not specify an 8-word Offset [implementation must detect
                    invalid Offset one way or the other], an Addressing_Exception interrupt occurs†. See
                    Section 8 for special Gate addressing rules.
                    c. Source Offset is applied to the Base_Address of the Gate BD and the Gate is fetched
                    from storage (paging is invoked on this access).
                    d. The current Access_Key is checked for Enter access against the Access_Lock, GAP and
                    SAP of the Gate (the GAP and SAP fields of the Gate correspond to the BD.GAP.E and
                    BD.SAP.E); an Addressing_Exception interrupt occurs if access is denied. Thus, to use a
                    Gate, one must have Enter access to both the Gate Bank (via the Gate BD) and the
                    particular Gate.
                    e. If a GOTO or an LBJ with Xa.IS = 1 operation is being performed, an Addressing_Exception
                    interrupt occurs when the Gate.GI = 1 (GOTO_Inhibit), regardless of the Target BD.
                    f. If the Target L,BDI is in the range 0,0 to 0,31, an Addressing_Exception interrupt occurs*.
                    g. If the GateBD.LIB = 1 processing continues with step 9a.
                    h. The Designator Bits, Access_Key, Latent Parameters and B fields from the Gate must be
                    retained if enabled or applicable (see 3.1.3).
                    i. The Target BD is fetched as described in step 6, sub-steps a through d (except that any
                    Addressing_Exception interrupt generated is fatal).
                    j. The Target BD.Type is examined and if a BD.Type  Extended_Mode and
                    BD.Type  Basic_Mode, instruction results are Architecturally_Undefined (any
                    Addressing_Exceptions associated with the Source BD must be noted as
                    Terminal_Addressing_Exceptions for reporting in step 21). Otherwise, processing
                    continues with step 10. Note: the Target BD.Type determines the resulting
                    environment (Basic_Mode or Extended_Mode) and that step 21 does not check Enter
                    access in the Target BD on gated transfers.

                2. Because this is a RTN to Basic_Mode (RCS.DB16 = 1), one of Base_Register 12–15 is to be
                loaded, decided by RCS.B + 12. B0.V := 1 and hard-held PAR.L,BDI := 0,0, marking B0 as void.

                3. Hard-held Access_Key := RCS.Access_Key and DB12–17 := RCS.DB12-17.

                4. The ABT is updated as described in 4.6.4, step 18 and hard-held PAR.PC := RCS.Offset. Note:
                ABT.Offset := 0.

                5. The appropriate information (as determined in step 1 above) is loaded into the Base_Register
                selected in step 2.

                6. DB31 is toggled as described in 4.4.2.3.

                7. If the BD.G = 1 or the RCS.Trap = 1, a Terminal_Addressing_Exception interrupt occurs. Note:
                the selected Base_Register remains loaded with the BD information and that the
                environment stored on the Interrupt_Control_Stack reflects the environment after steps 4–
                6 above. No check for Enter Access, Validated Entry or Selection of Base_Register is made
                on RTN.
             */
        }

        //TODO 3 System Control Designators—Replaced by DB12–17 of the RCS frame. In Version E models, DB15 is Set_to_Zero. In
        //  Version E models, if DB14 is set DB17 is Set_to_Zero.
    }

    @Override
    public Instruction getInstruction() { return Instruction.RTN; }
}
