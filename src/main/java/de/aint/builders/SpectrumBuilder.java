package de.aint.builders;

import de.aint.detectors.SumGaussNumeric;
import de.aint.models.*;
import de.aint.operations.*;
import de.aint.operations.calculators.Calculator.CalculatingAlgos;
import de.aint.operations.fitters.*;
import de.aint.readers.IsotopeReader;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.special.Erf;

public abstract class SpectrumBuilder {

    //=====================PEAK_FITTING======================================
    public static Spectrum createPeakFitSpectrum(Spectrum spec, ROI[] rois) {
    double[] energies = spec.getEnergy_per_channel();   // E[i] in keV
    int n = energies.length;

    // separate Fit-Kurve, damit das Original unangetastet bleibt
    double[] fitCurve = new double[n];                  // nur die Summe der Gauss-Beiträge
    boolean[] touched = new boolean[n];                 // welche Bins wurden von irgendeiner ROI beschrieben?

    for (ROI roi : rois) {
        
        double[] p = SumGaussNumeric.fitGaussToROI(roi);   // p = [B, σ, A1, μ1, T1, G1, A2, μ2, T2, G2, ...]

        double B   = p[0];
        double sigma = p[1];
        int nPeaks = (p.length - 2) / 4;

        // Kanalgrenzen der ROI (inklusive Ende!)
        int i0 = Helper.findChannelFromEnergy(roi.getStartEnergy(), energies);
        int i1 = Helper.findChannelFromEnergy(roi.getEndEnergy(),   energies);
        if (i0 > i1) { int t=i0; i0=i1; i1=t; }
        i0 = Math.max(0, i0);
        i1 = Math.min(n-1, i1);

        double inv2s2 = 1.0 / (2.0 * sigma * sigma);

        for (int i = i0; i <= i1; i++) {
            double Ei = energies[i];    // *** Energie in keV, nicht der Kanalindex! ***
            double sumPeaks = 0.0;
            for (int k = 0; k < nPeaks; k++) {
                double A  = p[2 + 4*k];
                double mu = p[3 + 4*k];
                double T  = p[4 + 4*k];
                double G  = p[5 + 4*k];
                double z  = Ei - mu;
                double delta = Math.sqrt(2) * sigma;

                double base = Math.exp(- z*z * inv2s2);
                double tail = 0.5 * T * Math.exp(z / (G * delta)) * Erf.erfc((z / delta) + 1.0 / (2.0 * G));

                sumPeaks += A * (base + tail);
            }
            // Nur die Peak-Summe ablegen; Baseline separat behandeln (s.u.)
            fitCurve[i] += sumPeaks+B;    // additiv erlaubt Überlappung mehrerer ROIs
            touched[i] = true;
        }

        //for (int i = i0; i <= i1; i++) fitCurve[i] += B;
    }

    // Variante A (empfohlen fürs Plotten):
    //   Rückgabe eines Spektrums, das NUR die fit-Kurve enthält (zum Overlay).
    //   Im Plot: Original (spec.counts) + Overlay (fitCurve) zeichnen.
    //Spectrum fittedCurves = new Spectrum(energies, fitCurve);
    //return CalculatingAlgos.ADDITION.calculate(spec, fittedCurves);

    // Variante B (synthetisches "gefitttes" Spektrum):
       double[] composed = spec.getCounts().clone();
       for (int i = 0; i < n; i++) if (touched[i]) composed[i] = fitCurve[i]; // oder composed[i] = Math.max(composed[i], fitCurve[i]);
       return new Spectrum(energies, composed);
}


    //=============CUSTOM==================
    public static Spectrum createCustomSpectrum(Spectrum spectrum, ArrayList<String> selectedIsotopesAsIDString, IsotopeReader isotopeReader) {
        ArrayList<Isotop> isotopes = isotopeReader.isotopes;
        Isotop[] selectedIsos = isotopes.stream().filter(iso -> selectedIsotopesAsIDString.contains(iso.id)).toArray(Isotop[]::new);

        //Create Spectrum with peaks, over selected Channels 
        double[] counts = spectrum.getCounts();

        for(var iso : selectedIsos){
            double energy = iso.energy;
            int channel = Helper.findChannelFromEnergy(energy, spectrum.getEnergy_per_channel());
            counts[channel] += 2.5*counts[channel];
        }

        Spectrum customSpectrum = new Spectrum(spectrum.getEnergy_per_channel(), counts);
        return customSpectrum;
    }

    //===============BACKGROUND===================

    public static Spectrum createBackgroundSpectrum(Spectrum spec) {
        FittingData fitData = new FittingData(spec);
        return new Spectrum(spec.getEnergy_per_channel(), Fitter.BackgroundFitAlgos.ALS_FAST.fit(fitData));
    }

    //=============SMOOTHED_SG=====================================

    //FOR STANDART VALS PUT 0
    public static Spectrum createSmoothedSpectrumUsingSG(Spectrum spec, int window_size, int polynomial_degree, boolean eraseOutliers, int iterations) {
        FittingData data = new FittingData(spec);

        if(window_size != 0){
            data.setSgWindowSize(window_size);
        }
        if(polynomial_degree != 0){
            data.setSgPolynomialDegree(polynomial_degree);
        }
        if(eraseOutliers){
            data.setSgEraseOutliers(eraseOutliers);
        }
        if(iterations != 0){
            data.setSgIters(iterations);
        }

        double[] newCounts = Fitter.SmoothingFitAlgos.SG.fit(data);
        return new Spectrum(spec.getEnergy_per_channel(), newCounts);
    }

    //=====================SMOOTHED_GAUSS======================================
    //put 0 for standart vals
    public static Spectrum createSmoothedSpectrumUsingGauss(Spectrum spec, double sigma){
        FittingData data = new FittingData(spec);
        if(sigma != 0){
            data.setGaussSigma(sigma);
        }
        
        double[] new_counts = Fitter.SmoothingFitAlgos.GAUSS.fit(data);
        return new Spectrum(spec.getEnergy_per_channel(), new_counts);
    }



    //=======================================STARTING_SET=================================================
    // 0 => Original Spectrum
    // 1 => Smoothed Spectrum
    // 2 => Background Spectrum
    // 3 => Smoothed Background Spectrum
    public static Spectrum[] createSpectrumVariants(Spectrum spec){
        //Declare Original / Smoothed Variants
        Spectrum[] variants = new Spectrum[4];
        variants[0] = spec;
        //0 for standart vals
        variants[1] = createSmoothedSpectrumUsingSG(spec, 0, 0, true, 0);
        
        //Declare Background variants
        variants[2] = createBackgroundSpectrum(spec);
        //Declare Gauss smoothed spectrum, because : SG => overshooting and : overshooting + ALS background => undershooting
        Spectrum gauss = createSmoothedSpectrumUsingGauss(spec, 3.0);
        variants[3] = createBackgroundSpectrum(gauss);

        return variants;
    }


    }
