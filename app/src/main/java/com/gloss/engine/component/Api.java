package com.glass.engine.component;

public class Api {
    
    static {
        System.loadLibrary("client");
    }
    
    static public native String password();
    static public native String socklink();
}
