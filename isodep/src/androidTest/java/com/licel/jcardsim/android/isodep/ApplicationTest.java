package com.licel.jcardsim.android.isodep;

import android.app.Application;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.test.ApplicationTestCase;
import android.util.Log;

import com.licel.jcardsim.samples.HelloWorldApplet;
import com.licel.jcardsim.utils.AIDUtil;
import com.licel.jcardsim.utils.ByteUtil;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    @Override
    protected void runTest() throws Throwable {
        TagSimulator tagSimulator = new TagSimulator();
        tagSimulator.installApplet(AIDUtil.create("cafecafe0001"), HelloWorldApplet.class);

        Tag nfcATag = tagSimulator.getNfcATag();
        assertNotNull(nfcATag);

        IsoDep isoDep = IsoDep.get(nfcATag);
        assertNotNull(isoDep);

        byte[] response = isoDep.transceive(AIDUtil.select("cafecafe0001"));
        assertEquals((short)0x9000, ByteUtil.getSW(response));

        Log.i("TestResultA", ByteUtil.hexString(response));

        Tag nfcBTag = tagSimulator.getNfcATag();
        assertNotNull(nfcATag);

        isoDep = IsoDep.get(nfcBTag);
        assertNotNull(isoDep);

        response = isoDep.transceive(AIDUtil.select("cafecafe0001"));
        assertEquals((short)0x9000, ByteUtil.getSW(response));

        Log.i("TestResultB", ByteUtil.hexString(response));
    }
}