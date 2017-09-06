package com.licel.jcardsim.smartcardio;

import javacard.framework.ISO7816;
import org.bouncycastle.util.encoders.Hex;

import javax.smartcardio.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class VICCExample {
    private static final ATR ETALON_ATR = new ATR(Hex.decode("3BFA1800008131FE454A434F5033315632333298"));
    private static final String TEST_APPLET_AID = "010203040506070809";

    public static void main(String args[]) throws InterruptedException, CardException {
        final String readerName = System.getProperty("os.name").startsWith("Windows")
                ? "Virtual Smart Card Architecture Virtual PCD 0" : "Virtual PCD 00 00";

        System.out.println("Connecting to reader: " + readerName);
        final TerminalFactory tf = TerminalFactory.getDefault();
        final CardTerminals ct = tf.terminals();
        final CardTerminal jcsTerminal = ct.getTerminal(readerName);

        // check terminal exists
        assertTrue(jcsTerminal != null);
        // check if card is present
        assertTrue(jcsTerminal.isCardPresent());
        // check card
        Card jcsCard = jcsTerminal.connect("*");
        assertTrue(jcsCard != null);
        // check card ATR
        assertEquals(jcsCard.getATR(), ETALON_ATR);
        // check card protocol
        assertEquals(jcsCard.getProtocol(), "T=1");
        // get basic channel
        CardChannel channel = jcsCard.getBasicChannel();
        assertTrue(channel != null);

        System.out.println("Sending APDUs");

        // create applet data = aid len (byte), aid bytes, params length (byte), param
        byte[] aidBytes = Hex.decode(TEST_APPLET_AID);
        byte[] createData = new byte[1+aidBytes.length+1+2+3];
        createData[0] = (byte) aidBytes.length;
        System.arraycopy(aidBytes, 0, createData, 1, aidBytes.length);
        createData[1+aidBytes.length] = (byte) 5;
        createData[2+aidBytes.length] = 0; // aid
        createData[3+aidBytes.length] = 0; // control
        createData[4+aidBytes.length] = 2; // params
        createData[5+aidBytes.length] = 0xF; // params
        createData[6+aidBytes.length] = 0xF; // params
        CommandAPDU createApplet = new CommandAPDU(0x80, 0xb8, 0, 0, createData);
        channel.transmit(createApplet);

        // select applet
        CommandAPDU selectApplet = new CommandAPDU(ISO7816.CLA_ISO7816, ISO7816.INS_SELECT, 4, 0, Hex.decode(TEST_APPLET_AID));
        ResponseAPDU response = channel.transmit(selectApplet);
        assertEquals(response.getSW(), 0x9000);
        // test NOP
        response = channel.transmit(new CommandAPDU(0x00, 0x02, 0x00, 0x00));
        assertEquals(0x9000, response.getSW());
        // test SW_INS_NOT_SUPPORTED
        response = channel.transmit(new CommandAPDU(0x00, 0x05, 0x00, 0x00));
        assertEquals(ISO7816.SW_INS_NOT_SUPPORTED, response.getSW());
        // test hello world from card
        response = channel.transmit(new CommandAPDU(0x00, 0x01, 0x00, 0x00));
        assertEquals(0x9000, response.getSW());
        assertEquals("Hello world !", new String(response.getData()));
        // test echo
        response = channel.transmit(new CommandAPDU(0x00, 0x01, 0x01, 0x00, ("Hello javacard world !").getBytes()));
        assertEquals(0x9000, response.getSW());
        assertEquals("Hello javacard world !", new String(response.getData()));
        // test echo v2
        response = channel.transmit(new CommandAPDU(0x00, 0x03, 0x00, 0x00, ("Hello javacard world !").getBytes()));
        assertEquals(0x9000, response.getSW());
        assertEquals("Hello javacard world !", new String(response.getData()));
        // test echo install params
        response = channel.transmit(new CommandAPDU(0x00, 0x04, 0x00, 0x00));
        assertEquals(0x9000, response.getSW());
        assertEquals(0xF, response.getData()[0]);
        assertEquals(0xF, response.getData()[1]);
        // application specific sw + data
        response = channel.transmit(new CommandAPDU(0x00, 0x07, 0x00, 0x00));
        assertEquals(0x9B00, response.getSW());
        assertEquals("Hello world !", new String(response.getData()));
        // sending maximum data
        response = channel.transmit(new CommandAPDU(0x00, 0x08, 0x00, 0x00));
        assertEquals(0x9000, response.getSW());

        System.out.println("Done");
    }
}
