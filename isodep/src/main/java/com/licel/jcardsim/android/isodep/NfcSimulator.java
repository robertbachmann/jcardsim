package com.licel.jcardsim.android.isodep;

import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

import com.licel.jcardsim.base.Simulator;
import com.licel.jcardsim.base.SimulatorRuntime;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public abstract class NfcSimulator extends Simulator {
    protected NfcSimulator(SimulatorRuntime simulatorRuntime) {
        super(simulatorRuntime);
    }

    public abstract Tag getTag();

    public abstract void removeTag();
}
