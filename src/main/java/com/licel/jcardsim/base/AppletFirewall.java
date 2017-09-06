package com.licel.jcardsim.base;

import com.licel.jcardsim.utils.AIDUtil;
import com.licel.jcardsim.utils.Supplier;
import javacard.framework.AID;
import javacard.framework.Shareable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public class AppletFirewall {
    private final Shareable firewalledShareable;
    private final AID packageAID;
    private final AID appletAID;
    private final Set<Method> whiteList;
    private final Supplier<AppletContextManager> appletContextManagerSupplier;
    private final Shareable shareable;

    public AppletFirewall(Supplier<AppletContextManager> appletContextManagerSupplier, Shareable shareable) {
        if (shareable == null) {
            throw new NullPointerException("shareable");
        }

        final Class<?> shareableClass = shareable.getClass();
        final AppletContextManager appletContextManager = appletContextManagerSupplier.get();
        final Class[] interfaces = allInterfaces(shareableClass);

        this.appletContextManagerSupplier = appletContextManagerSupplier;
        this.shareable = shareable;
        this.packageAID = appletContextManager.getActivePackageAID();
        this.appletAID = appletContextManager.getActiveAID();
        this.whiteList = buildWhiteList(interfaces);

        this.firewalledShareable = (Shareable) Proxy.newProxyInstance(shareableClass.getClassLoader(),
                interfaces,
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return AppletFirewall.this.invoke(proxy, method, args);
                    }
                });
    }

    protected static Class[] allInterfaces(final Class<?> shareableClass) {
        final List<Class> interfaces = new ArrayList<Class>();
        Class<?> currentClass = shareableClass;
        while (!currentClass.equals(Object.class)) {
            Collections.addAll(interfaces, currentClass.getInterfaces());
            currentClass = currentClass.getSuperclass();
        }
        return interfaces.toArray(new Class[interfaces.size()]);
    }

    protected static Set<Method> buildWhiteList(final Class[] interfaces) {
        final Set<Method> whiteList = new HashSet<Method>();
        for (Class anInterface : interfaces) {
            if (Shareable.class.isAssignableFrom(anInterface)) {
                Collections.addAll(whiteList, anInterface.getMethods());
            }
        }
        return whiteList;
    }

    protected Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (proxy != firewalledShareable) {
            throw new AssertionError("Wrong proxy");
        }

        final AppletContextManager contextManager = appletContextManagerSupplier.get();
        final AID callerPackage = contextManager.getActivePackageAID();

        if (method.getDeclaringClass().equals(Object.class) && method.getName().equals("toString")) {
            return "AppletFirewallProxy bound to " + AIDUtil.toString(packageAID) + " for " + shareable;
        }
        if (!packageAID.equals(callerPackage)) {
            if (!whiteList.contains(method)) {
                throw new SecurityException("Calling this method is not allowed");
            }
            contextManager.enterContext(packageAID, appletAID);
            try {
                return method.invoke(shareable, args);
            }
            finally {
                contextManager.leaveContext();
            }
        } else {
            return method.invoke(shareable, args);
        }
    }

    public Shareable getShareable() {
        return firewalledShareable;
    }
}