/*
 * Copyright 2013 Licel LLC.
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
package com.licel.jcardsim.io;

import javacard.framework.AID;

/**
 * Interface with JavaCard-specific functions
 * @author LICEL LLC
 */
public interface JavaCardInterface extends CardInterface {

    /**
     * Load <code>Applet</code> into Simulator
     *
     * @param aid applet aid
     * @param appletClassName fully qualified applet class name String
     * @param appletJarContents contains a byte array containing a jar file with an applet and its dependent classes
     * @return applet aid
     * @throws java.lang.NullPointerException if <coce>aid</coce> or <code>appletClassName</code> is null
     * @throws java.lang.IllegalArgumentException if <code>appletClass</code> does not extend <code>Applet</code>
     */
    public AID loadApplet(AID aid, String appletClassName, byte[] appletJarContents);

    /**
     * Load <code>Applet</code> into Simulator
     *
     * @param aid applet aid
     * @param appletClassName fully qualified applet class name String
     * @return applet aid
     * @throws java.lang.NullPointerException if <coce>aid</coce> or <code>appletClassName</code> is null
     * @throws java.lang.IllegalArgumentException if <code>appletClass</code> does not extend <code>Applet</code>
     */
    public AID loadApplet(AID aid, String appletClassName);

    /**
     * Create an <code>Applet</code> instance in Simulator
     * @param aid applet aid
     * @param bArray the array containing installation parameters
     * @param bOffset the starting offset in bArray
     * @param bLength the length in bytes of the parameter data in bArray
     * @return AID of the created applet
     * @throws java.lang.NullPointerException if <coce>aid</coce> or <code>appletClass</code> is null
     */
    public AID createApplet(AID aid, byte bArray[], short bOffset,
            byte bLength);

    /**
     * Install
     * <code>Applet</code> into Simulator.
     * This method is equal to:
     * <code>
     * loadApplet(...);
     * createApplet(...);
     * </code>
     * @param aid applet aid
     * @param appletClassName fully qualified applet class name Strin
     * @param bArray the array containing installation parameters
     * @param bOffset the starting offset in bArray
     * @param bLength the length in bytes of the parameter data in bArray
     * @return applet <code>AID</code>
     * @throws java.lang.IllegalArgumentException if <code>appletClass</code> does not extend <code>Applet</code>
     */
    public AID installApplet(AID aid, String appletClassName, byte bArray[], short bOffset,
            byte bLength);

    /**
     * Install
     * <code>Applet</code> into Simulator.
     * This method is equal to:
     * <code>
     * loadApplet(...);
     * createApplet(...);
     * </code>
     * @param aid applet aid or null
     * @param appletClassName fully qualified applet class name Strin
     * @param appletJarContents Contains a byte array containing a jar file with an applet and its dependent classes
     * @param bArray the array containing installation parameters
     * @param bOffset the starting offset in bArray
     * @param bLength the length in bytes of the parameter data in bArray
     * @return applet <code>AID</code>
     * @throws java.lang.IllegalArgumentException if <code>appletClass</code> does not extend <code>Applet</code>
     */
    public AID installApplet(AID aid, String appletClassName, byte[] appletJarContents, byte bArray[], short bOffset,
            byte bLength);

    /**
     * Select applet by it's AID
     * @param aid appletId
     * @return true if applet selection success
     */
    public boolean selectApplet(AID aid);

    /**
     * Select applet by it's AID
     * @param aid appletId
     * @return byte array
     */
    public byte[] selectAppletWithResult(AID aid);
}
