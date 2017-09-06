package com.licel.jcardsim.base;

import java.util.Stack;
import javacard.framework.AID;

public final class AppletContextManager {
    /** the current context */
    private Stack<AppletContext> contextStack = new Stack<AppletContext>();

    public AppletContextManager() {
    }

    /**
     * Clear the context.
     */
    public void clear() {
        contextStack.clear();
    }

    /**
     * Enter a context
     * @param packageAID packageAID
     * @param appletAID appletAID
     */
    public void enterContext(AID packageAID, AID appletAID) {
        contextStack.push(new AppletContext(packageAID, appletAID));
    }

    /**
     * Leave a context
     */
    public void leaveContext() {
        if (!contextStack.isEmpty()) {
            contextStack.pop();
        }
    }

    /**
     * @return the AID of the active applet or <code>null</code>.
     */
    public AID getActiveAID() {
        return contextStack.empty() ? null : contextStack.peek().appletAID;
    }

    /**
     * @return the AID of the active applet or <code>null</code>.
     */
    public AID getActivePackageAID() {
        return contextStack.empty() ? null : contextStack.peek().packageAID;
    }


    /**
     * @return the AID of the previous applet or <code>null</code>.
     */
    public AID getPreviousContextAID() {
        if (contextStack.size() <= 1) {
            return null;
        }
        final AID currentPackage = contextStack.peek().packageAID;
        for (int i = contextStack.size() - 1; i >= 0; --i) {
            final AID p = contextStack.get(i).packageAID;
            if (!p.equals(currentPackage)) {
                return contextStack.get(i).appletAID;
            }
        }
        return null;
    }

    private static final class AppletContext {
        public final AID packageAID;
        public final AID appletAID;

        public AppletContext(AID packageAID, AID appletAID) {
            if (packageAID == null)
                throw new NullPointerException("packageAID");
            if (appletAID == null)
                throw new NullPointerException("appletAID");
            this.packageAID = packageAID;
            this.appletAID = appletAID;
        }

        @Override
        public String toString() {
            return "AppletContext{" +
                    "packageAID=" + packageAID +
                    ", appletAID=" + appletAID +
                    '}';
        }
    }
}
