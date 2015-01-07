package com.licel.jcardsim.android.isodep;

import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

import com.licel.jcardsim.base.Simulator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Helper class for faking a tag
 */
final class FakeNfcTag implements InvocationHandler {
    static String LOG_TAG = "jCardSimTag";

    static final int NFC_A = 1; // TagTechnology.NFC_A;
    static final int NFC_B = 2; // TagTechnology.NFC_B;
    static final int ISO_DEP = 3; // TagTechnology.ISO_DEP;

    private final Class<?> iNfcTagClass;
    private final Constructor<Tag> tagConstructor;
    private final Method tagSetConnectedTechnology;
    private final Constructor<?> transceiveResultConstructor;
    private final Simulator simulator;
    private final int tagTechnology;

    public FakeNfcTag(Simulator simulator, int tagTechnology) {
        this.simulator = simulator;
        this.tagTechnology = tagTechnology;
        final String protocol = tagTechnology == NFC_A ?
                "T=CL,TYPE_A,T1" : "T=CL,TYPE_B,T1";
        simulator.changeProtocol(protocol);

        try {
            iNfcTagClass = Class.forName("android.nfc.INfcTag");
            // get Tag(byte[] id, int[] techList, Bundle[] techListExtras, int serviceHandle, INfcTag tagService)
            tagConstructor = Tag.class.getDeclaredConstructor(
                    byte[].class, int[].class, Bundle[].class, int.class, iNfcTagClass
            );
            tagSetConnectedTechnology = Tag.class.getDeclaredMethod("setConnectedTechnology", int.class);
            Class<?> transceiveResultClass = Class.forName("android.nfc.TransceiveResult");
            transceiveResultConstructor = transceiveResultClass.getDeclaredConstructor(int.class, byte[].class);
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed", e);
        }
    }


    protected Tag getTag() {
        final byte[] id = new byte[0];
        final int[] techList = new int[]{ISO_DEP, tagTechnology};
        final Bundle[] techListExtras = new Bundle[]{new Bundle(), new Bundle()};
        final int serviceHandle = 0;
        try {
            final Object service = Proxy.newProxyInstance(
                    Thread.currentThread().getContextClassLoader(),
                    new Class[]{iNfcTagClass}, this);
            Tag tag = tagConstructor.newInstance(id, techList, techListExtras,
                    serviceHandle, service);
            tagSetConnectedTechnology.invoke(tag, ISO_DEP);
            return tag;
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed", e);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final String name = method.getName();

        // TODO: implement missing methods for INfcService
        if ("toString".equals(name)) {
            return "FakeNfcTag@" + System.identityHashCode(proxy);
        }
        if ("equals".equals(name)) {
            return (proxy == args[0]);
        }
        if ("hashCode".equals(name)) {
            return System.identityHashCode(proxy);
        }
        if ("transceive".equals(name)) {
            return transceive((byte[]) args[1]);
        }
        else {
            Log.e(LOG_TAG, "No implementation for " + name);
            throw new AssertionError("Not implemented " + name);
        }
    }

    private Object transceive(byte[] data) {
        int errorCode = 0;
        byte[] result;

        try {
            result = simulator.transmitCommand(data);
        }
        catch (Exception e) {
            Log.e(LOG_TAG, "Could not create transceiveResult: " + e.getMessage());
            result = new byte[0];
            errorCode = 1;
        }

        try {
            return transceiveResultConstructor.newInstance(errorCode, result);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not create transceiveResult: " + e.getMessage());
            throw new AssertionError("Could not create transceiveResult");
        }
    }
}
