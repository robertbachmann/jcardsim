package com.licel.jcardsim.android.isodep;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.test.AndroidTestCase;
import android.util.Log;

import com.licel.jcardsim.samples.HelloWorldApplet;
import com.licel.jcardsim.utils.AIDUtil;
import com.licel.jcardsim.utils.ByteUtil;

import java.io.IOException;

public class IsoDepTest extends AndroidTestCase {
    public void testTagA() throws IOException {
        NfcSimulator nfcSimulator = new NfcASimulator();
        nfcSimulator.installApplet(AIDUtil.create("cafecafe0001"), HelloWorldApplet.class);

        Tag nfcATag = nfcSimulator.getTag();
        assertNotNull(nfcATag);

        IsoDep isoDep = IsoDep.get(nfcATag);
        assertNotNull(isoDep);

        byte[] response = isoDep.transceive(AIDUtil.select("cafecafe0001"));
        assertEquals((short)0x9000, ByteUtil.getSW(response));

        Log.i("TestResultA", ByteUtil.hexString(response));
    }

    public void testTagB() throws IOException {
        NfcSimulator nfcSimulator = new NfcBSimulator();
        nfcSimulator.installApplet(AIDUtil.create("cafecafe0001"), HelloWorldApplet.class);

        Tag nfcBTag = nfcSimulator.getTag();
        assertNotNull(nfcBTag);

        IsoDep isoDep = IsoDep.get(nfcBTag);
        assertNotNull(isoDep);

        byte[] response = isoDep.transceive(AIDUtil.select("cafecafe0001"));
        assertEquals((short)0x9000, ByteUtil.getSW(response));

        Log.i("TestResultB", ByteUtil.hexString(response));
    }
}
