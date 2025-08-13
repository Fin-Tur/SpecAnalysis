package de.aint.libraries;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface SmoothingLib extends Library {
    // "smoothing" entspricht smoothing.dll (ohne .dll)
    //SmoothingLib INSTANCE = Native.load("smoothing", SmoothingLib.class);
    SmoothingLib INSTANCE = (SmoothingLib) Native.load("C:/Users/f.willems/Desktop/smoothing.dll", SmoothingLib.class);

    // cdecl ist Standard bei MinGW/gcc – daher Library, NICHT StdCallLibrary
    int estimate_background_als(double[] counts, int n,
                                double lambda, double p, int maxIterations,
                                double[] out);
}
