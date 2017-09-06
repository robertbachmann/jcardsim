package com.licel.jcardsim.utils;

/**
 * Back-port of Java 8 <code>java.util.function.Supplier</code>.
 */
public interface Supplier<T> {
    T get();
}
