package de.aint.libraries;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface SmoothingLib extends Library {
    // "smoothing" entspricht smoothing.dll (ohne .dll)
    //SmoothingLib INSTANCE = Native.load("smoothing", SmoothingLib.class);
    Path libPath = Paths.get("src/main/resources/smoothing.dll");
    SmoothingLib INSTANCE = (SmoothingLib) Native.load(libPath.toString(), SmoothingLib.class);

    // cdecl ist Standard bei MinGW/gcc – daher Library, NICHT StdCallLibrary
    int estimate_background_als(double[] counts, int n,
                                double lambda, double p, int maxIterations,
                                double[] out);
}
