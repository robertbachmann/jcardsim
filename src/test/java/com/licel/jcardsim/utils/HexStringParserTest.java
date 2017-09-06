package com.licel.jcardsim.utils;

import junit.framework.TestCase;

public class HexStringParserTest extends TestCase {
    public HexStringParserTest(String name) {
        super(name);
    }

    public void testValidInputs() throws Exception {
        assertEquals("CA", parse("ca"));
        assertEquals("CA", parse("Ca"));
        assertEquals("CA", parse("cA"));
        assertEquals("CA", parse("c a"));
        assertEquals("CAFE", parse("ca fe"));
        assertEquals("01CA", parse("#(ca)"));
        assertEquals("01CA", parse("#<ca>"));
        assertEquals("01CA", parse("#{ca}"));
        assertEquals("48454C4C4F01", parse("|HELLO|01"));
        assertEquals("48454C4C4F", parse("|HELLO"));
        assertEquals("0548454C4C4F", parse("#(|HELLO)"));
        assertEquals("0548454C4C4F", parse("|#(HELLO)"));
        assertEquals("0348453E", parse("|#(HE>)"));
        assertEquals("0348453C", parse("|#(HE<)"));
        assertEquals("0B48454C4C4F05574F524C44", parse("|#(HELLO#(WORLD))"));
        assertEquals("0B48454C4C4F05574F524C44", parse("|#(HELLO#<WORLD>)"));
        assertEquals("0748454C4C4F0101", parse("|#(HELLO#<|01>)"));
        assertEquals("0748454C4C4F0101", parse("#(|HELLO|#<01>)"));
        assertEquals("48454C4C4F20574F524C44", parse("|HELLO WORLD"));
        assertEquals("48454C4C4F23", parse("|HELLO#"));
        assertEquals("48454C4C4F2357", parse("|HELLO#W"));
        assertEquals("57", parse("|W"));
        assertEquals("", parse(""));
    }

    public void testInvalidInputs() throws Exception {
        assertEquals("Odd number of digits at position 4", parse("ca a"));
        assertEquals("Can not parse input at 8: #", parse("|HELLO|#"));
        assertEquals("Odd number of digits at position 1", parse("7|"));
        assertEquals("Can not parse input at 3: z", parse("12z"));
        assertEquals("Can not parse input at 3: }", parse("12}"));
        assertEquals("Can not parse input at 5: x", parse("cafe#x"));
        assertEquals("Missing closing '>'", parse("cafe#<22"));

        try {
            parse(null);
            fail("No exception");
        } catch (NullPointerException npe) {
            // ignore
        }
    }

    private String parse(String input) {
        try {
            byte[] bytes = HexStringParser.parse(input);
            return ByteUtil.hexString(bytes);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}
