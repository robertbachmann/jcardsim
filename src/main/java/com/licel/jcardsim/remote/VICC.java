/*
 * Copyright 2015 Robert Bachmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.licel.jcardsim.remote;

import com.licel.jcardsim.base.CardManager;
import com.licel.jcardsim.base.Simulator;
import com.licel.jcardsim.base.SimulatorRuntime;
import com.licel.jcardsim.utils.ByteUtil;
import javacard.framework.ISO7816;
import javacard.framework.Util;

import javax.net.SocketFactory;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * TBD.
 */
public class VICC implements Runnable {
    private static final int CMD_POWER_OFF = 0;
    private static final int CMD_POWER_ON = 1;
    private static final int CMD_RESET = 2;
    private static final int CMD_ATR = 4;

    private final PrintStream printStream;
    private final Simulator simulator;
    private final String hostname;
    private final int port;
    private final CountDownLatch countdownLatch = new CountDownLatch(1);

    private volatile boolean connectionSuccessful = false;
    private volatile Socket socket = null;

    /**
     * Create new instance
     * @param cfg configuration properties
     * @param printStream output stream or <code>null</code>
     */
    public VICC(Properties cfg, final PrintStream printStream) {
        this.printStream = printStream;
        this.hostname = cfg.getProperty("hostname", "localhost");
        this.port = Integer.parseInt(cfg.getProperty("port", "35963"));
        this.simulator = new ViccSimulator(cfg);
    }

    /**
     * Connect to server.
     */
    public void run() {
        log(String.format("Connecting to %s:%d", hostname, port));
        try {
            socket = SocketFactory.getDefault().createSocket(hostname, port);
        } catch (IOException e) {
            log("Connection failed");
            countdownLatch.countDown();
            return;
        }

        connectionSuccessful = true;
        log("Connected");
        countdownLatch.countDown();

        try {
            doRun();
        } catch (Exception e) {
            if (printStream != null) {
                e.printStackTrace(printStream);
            }
        } finally {
            try {
                close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Wait until {@link #run()} has connected to the server. Call {@link #isConnectionSuccessful()} to
     * check if the connection was successful.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void awaitConnection() throws InterruptedException {
        countdownLatch.await();
    }

    /**
     * Wait until {@link #run()} has connected to the server. Call {@link #isConnectionSuccessful()} to
     * check if the connection was successful.
     *
     * @param timeout the maximum time to wait
     * @param timeoutUnit the time unit of the <code>timeout</code> argument
     * @return <ccode>false</ccode> if the waiting time elapsed before the count reached zero, otherwise <code>true</code>
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public boolean awaitConnection(long timeout, TimeUnit timeoutUnit) throws InterruptedException {
        return countdownLatch.await(timeout, timeoutUnit);
    }

    /**
     * @return <code>true</code> if a connection to the server was established
     */
    public boolean isConnectionSuccessful() {
        return connectionSuccessful;
    }

    private void doRun() throws IOException, InterruptedException {
        final byte[] socketInputBuffer = new byte[Short.MAX_VALUE + 4];
        final byte[] socketOutputBuffer = new byte[Short.MAX_VALUE + 4];
        byte[] atrOutputBuffer = null;

        InputStream in = socket.getInputStream();
        OutputStream output = socket.getOutputStream();

        while (!socket.isClosed()) {
            int r;
            if ((r = in.read(socketInputBuffer, 0, 2)) != 2) {
                throw new IOException("Illegal message, received only " + r + " bytes");
            }

            final int bufferLength = ((socketInputBuffer[0] & 0xFF) << 8) | (socketInputBuffer[1] & 0xFF);
            if (bufferLength > 0) {
                final int bytesRead = in.read(socketInputBuffer, r, bufferLength);
                if (bytesRead != bufferLength) {
                    throw new IOException("Invalid message");
                }
            }

            if (bufferLength > 1) {
                final byte[] responseApdu = handleApdu(Arrays.copyOfRange(socketInputBuffer, 2, bufferLength + 2));
                socketOutputBuffer[0] = (byte) (responseApdu.length >> 8);
                socketOutputBuffer[1] = (byte) responseApdu.length;
                System.arraycopy(responseApdu, 0, socketOutputBuffer, 2, responseApdu.length);
                output.write(socketOutputBuffer, 0, responseApdu.length + 2);
            } else if (bufferLength == 1) {
                final byte command = socketInputBuffer[2];
                switch (command) {
                    case CMD_POWER_OFF:
                        log("Power off");
                        break;
                    case CMD_POWER_ON:
                        log("Power on");
                        simulator.reset();
                        atrOutputBuffer = null;
                        break;
                    case CMD_RESET:
                        log("Reset");
                        simulator.reset();
                        atrOutputBuffer = null;
                        break;
                    case CMD_ATR:
                        if (atrOutputBuffer == null) {
                            byte[] atr = simulator.getATR();
                            atrOutputBuffer = new byte[2 + atr.length];
                            atrOutputBuffer[1] = (byte) atr.length;
                            atrOutputBuffer[0] = (byte) (atr.length >> 8);
                            System.arraycopy(atr, 0, atrOutputBuffer, 2, atr.length);

                            if (printStream != null) {
                                printStream.print("ATR:  ");
                                printStream.println(ByteUtil.hexString(atr));
                            }
                            output.write(atrOutputBuffer);
                        } else {
                            output.write(atrOutputBuffer);
                            Thread.sleep(500);
                        }
                        break;
                }
            }
        }
    }

    /**
     * Close connection to server.
     *
     * @throws IOException see {@link Socket#close()}
     */
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            log("Shutting down");
            socket.close();
        }
    }

    private byte[] handleApdu(byte[] buffer) {
        byte[] response;
        try {
            response = CardManager.dispatchApdu(simulator, buffer);
        } catch (Exception e) {
            response = new byte[2];
            Util.setShort(response, (short)0, ISO7816.SW_UNKNOWN);
        }

        if (printStream != null) {
            printStream.print("APDU: ");
            printStream.print(ByteUtil.hexString(buffer));
            printStream.print(" > ");
            printStream.println(ByteUtil.hexString(response));
            printStream.flush();

        }
        return response;
    }

    private final void log(String msg) {
        if (printStream != null) {
            printStream.println(msg);
            printStream.flush();
        }
    }

    public static void main(String args[]) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java com.licel.jcardsim.remote.VICC <jcardsim.cfg>");
            System.exit(-1);
        }
        Properties cfg = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(args[0]);
            cfg.load(fis);
        } catch (Throwable t) {
            System.err.println("Unable to load configuration " + args[0] + " due to: " + t.getMessage());
            System.exit(-1);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }

        final VICC vicc = new VICC(cfg, System.out);
        Thread thread = new Thread(vicc);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                try {
                    vicc.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }));

        thread.start();
    }

    private static final class ViccSimulator extends Simulator {
        public ViccSimulator(Properties cfg) {
            super(new SimulatorRuntime(), cfg);
        }
    }
}
