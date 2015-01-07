package com.licel.jcardsim.android.isodep;

import android.nfc.Tag;

import com.licel.jcardsim.base.SimulatorRuntime;

public class NfcASimulator extends NfcSimulator {
    private final FakeNfcTag fakeNfcTag;

    public NfcASimulator() {
        this(new SimulatorRuntime());
    }

    public NfcASimulator(SimulatorRuntime simulatorRuntime) {
        super(simulatorRuntime);
        fakeNfcTag = new FakeNfcTag(this, FakeNfcTag.NFC_A);
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
