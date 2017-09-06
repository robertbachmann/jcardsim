package com.licel.jcardsim.base;

import com.licel.jcardsim.utils.AIDUtil;
import com.licel.jcardsim.utils.ByteUtil;
import javacard.framework.*;
import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.Set;

public class AppletFirewallWhiteListTest extends TestCase {
    private static final StringBuffer LOG = new StringBuffer();
    private static final AID PACKAGE1 = AIDUtil.create("F0AA000000");
    private static final AID AID1 = AIDUtil.create("F0AA000001");
    private static final AID PACKAGE2 = AIDUtil.create("F0BB000000");
    private static final AID AID2 = AIDUtil.create("F0BB000001");

    public void testWhiteListBuilding() throws Exception {
        final Set<Method> whiteList = AppletFirewall.buildWhiteList(
                AppletFirewall.allInterfaces(ConcreteDummyClass.class));
        for (Method m : whiteList) {
            System.out.println(m);
        }
        assertTrue(whiteList.contains(ShareableA.class.getMethod("s1")));
        assertTrue(whiteList.contains(ShareableA.class.getMethod("s2")));
        assertTrue(whiteList.contains(ShareableA.class.getMethod("s3")));
        assertTrue(whiteList.contains(ShareableAExtended.class.getMethod("s4")));
        assertTrue(whiteList.contains(ShareableB.class.getMethod("s5")));
        assertEquals(5, whiteList.size());
    }

    public void testCalls() throws Throwable {
        final SimulatorRuntime simulatorRuntime = new SimulatorRuntime();
        simulatorRuntime.loadLoadFile(new LoadFile(PACKAGE1, new Module(AID1, ServerApplet.class)));
        simulatorRuntime.loadLoadFile(new LoadFile(PACKAGE2, new Module(AID2, ClientApplet.class)));
        simulatorRuntime.installApplet(PACKAGE1, AID1, AID1, new byte[0], (short) 0, (byte) 0);
        simulatorRuntime.installApplet(PACKAGE2, AID2, AID2, new byte[0], (short)0, (byte)0);
        simulatorRuntime.transmitCommand(AIDUtil.select(AID2));

        final ShareableAExtended obj = (ShareableAExtended) simulatorRuntime.getSharedObject(AID1, (byte) 0);
        final AppletContextManager contextManager = simulatorRuntime.getAppletContextManagerForActiveChannel();
        assertNull(contextManager.getPreviousContextAID());


        // check all calls from applet with AID1 are allowed
        LOG.setLength(0);
        simulatorRuntime.transmitCommand(AIDUtil.select(AID1));
        obj.s1();
        ((AnotherInterface)obj).unsharedMethod();
        contextManager.leaveContext();
        assertEquals("s1 AID=F0AA000001 PREVIOUS CONTEXT=<null>\n" +
                "unsharedMethod AID=F0AA000001 PREVIOUS CONTEXT=<null>\n", LOG.toString());

        // check firewall works
        LOG.setLength(0);
        simulatorRuntime.transmitCommand(AIDUtil.select(AID2));
        obj.s1();
        try {
            ((AnotherInterface)obj).unsharedMethod();
            fail("No exception");
        } catch (SecurityException se) {
            // pass
        }
        obj.s2();
        assertEquals("s1 AID=F0AA000001 PREVIOUS CONTEXT=F0BB000001\n" +
                "s2 AID=F0AA000001 PREVIOUS CONTEXT=F0BB000001\n", LOG.toString());
    }

    public void testGetPreviousContextAID() {
        final AppletContextManager contextManager = new AppletContextManager();
        assertNull(contextManager.getPreviousContextAID());

        contextManager.enterContext(AIDUtil.create("F0AA000010"), AIDUtil.create("F0AA000011"));
        assertNull(contextManager.getPreviousContextAID());
        contextManager.enterContext(AIDUtil.create("F0AA000010"), AIDUtil.create("F0AA000012"));
        assertNull(contextManager.getPreviousContextAID());
        contextManager.enterContext(AIDUtil.create("F0AA000010"), AIDUtil.create("F0AA000013"));
        assertNull(contextManager.getPreviousContextAID());

        contextManager.enterContext(AIDUtil.create("F0BB000010"), AIDUtil.create("F0BB000001"));
        assertEquals("F0AA000013", AIDUtil.toString(contextManager.getPreviousContextAID()));
    }

    public static class ServerApplet extends Applet {
        public static void install(byte[] bArray, short bOffset, byte bLength) {
            new ServerApplet().register();
        }

        @Override
        public void process(APDU apdu) throws ISOException {
        }

        @Override
        public Shareable getShareableInterfaceObject(AID clientAID, byte parameter) {
            return new ConcreteDummyClass();
        }
    }

    public static class ClientApplet extends Applet {
        public static void install(byte[] bArray, short bOffset, byte bLength) {
            new ClientApplet().register();
        }

        @Override
        public void process(APDU apdu) throws ISOException {
        }
    }

    interface ShareableA extends Shareable {
        short s1();
        short s2();
        short s3();
    }

    interface ShareableAExtended extends ShareableA {
        short s4();
    }

    interface ShareableB extends Shareable {
        short s5();
    }

    interface AnotherInterface {
        short unsharedMethod();
    }

    public static class AbstractDummyClass implements ShareableAExtended, AnotherInterface {
        public short unsharedMethod() {
            log("unsharedMethod");
            return (short) 0;
        }

        public short s4() {
            log("s4");
            return (short) 4;
        }

        public short s1() {
            log("s1");
            return (short) 1;
        }

        public short s2() {
            log("s2");
            return (short) 2;
        }

        public short s3() {
            log("s3");
            return (short) 3;
        }

        protected void log(String method) {
            AID previousContextAID = JCSystem.getPreviousContextAID();
            LOG.append(method)
                .append(" AID=").append(AIDUtil.toString(JCSystem.getAID()))
                .append(" PREVIOUS CONTEXT=").append(previousContextAID == null ? "<null>" : AIDUtil.toString(previousContextAID))
                .append("\n");
        }
    }

    public static class ConcreteDummyClass extends AbstractDummyClass implements ShareableB {
        public short s5() {
            log("s5");
            return (short) 5;
        }
    }
}
