package de.aint.libraries;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface SmoothingLib extends Library {
    Path libPath = Paths.get("src/main/resources/smoothing.dll");
    SmoothingLib INSTANCE = (SmoothingLib) Native.load(libPath.toString(), SmoothingLib.class);
    
    int estimate_background_als(double[] counts, int n,
                                double lambda, double p, int maxIterations,
                                double[] out);
}
