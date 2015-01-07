package com.licel.jcardsim.android.isodep;

import android.nfc.Tag;

import com.licel.jcardsim.base.SimulatorRuntime;

public class NfcBSimulator extends NfcSimulator {
    private final FakeNfcTag fakeNfcTag;

    public NfcBSimulator() {
        this(new SimulatorRuntime());
    }

    public NfcBSimulator(SimulatorRuntime simulatorRuntime) {
        super(simulatorRuntime);
        fakeNfcTag = new FakeNfcTag(this, FakeNfcTag.NFC_B);
    }

    @Override
    public Tag getTag() {
        return fakeNfcTag.getTag();
    }

    @Override
    public void removeTag() {
        // TODO
    }
}
