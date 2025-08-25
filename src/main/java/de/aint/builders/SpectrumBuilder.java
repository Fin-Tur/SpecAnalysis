package de.aint.builders;

import de.aint.models.*;
import de.aint.operations.*;
import de.aint.operations.fitters.*;
import de.aint.readers.IsotopeReader;
import java.util.ArrayList;

import org.apache.commons.math3.special.Erf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpectrumBuilder {

    private static final Logger logger = LoggerFactory.getLogger(SpectrumBuilder.class);

    private SpectrumBuilder() {
        // private constructor to prevent instantiation
    }

    //=====================PEAK_FITTING======================================
    public static Spectrum createPeakFitSpectrum(Spectrum spec, ROI[] rois) {
    double[] energies = spec.getEnergy_per_channel();   // E[i] in keV
    int n = energies.length;

    //original untouched
    double[] fitCurve = new double[n];                  // sum of gaussians
    boolean[] touched = new boolean[n];                 //which bins r written to

    for (ROI roi : rois) {
        if(roi.getFitParams() == null || roi.getFitParams().length == 0){
            try{
                roi.fitGaussCurve();
            } catch (Exception e){
                logger.error("Error fitting ROI from {} keV to {} keV: {}", roi.getStartEnergy(), roi.getEndEnergy(), e.getMessage());
                continue;
            }
        }
        double[] p = roi.getFitParams(); // p = [B, σ, A1, μ1, T1, G1, A2, μ2, T2, G2, ...]

        double B   = p[0];
        double sigma = p[1];
        int nPeaks = (p.length - 2) / 5;

        int iStart = Helper.findChannelFromEnergy(roi.getStartEnergy(), energies);
        int iEnd = Helper.findChannelFromEnergy(roi.getEndEnergy(),   energies);

        iStart = Math.max(0, iStart);
        iEnd = Math.min(n-1, iEnd);

        double inv2s2 = 1.0 / (2.0 * sigma * sigma);

        for (int i = iStart; i <= iEnd; i++) {
            double Ei = energies[i];
            double sumPeaks = 0.0;
            for (int k = 0; k < nPeaks; k++) {
                double A  = p[2 + 5*k];
                double mu = p[3 + 5*k];
                double T  = p[4 + 5*k];
                double G  = p[5 + 5*k];
                double S  = p[6 + 5*k];
                double z  = Ei - mu;
                double delta = Math.sqrt(2) * sigma;

                double base = Math.exp(- z*z * inv2s2);
                double tail = 0.5 * T * Math.exp(z / (G * delta)) * Erf.erfc((z / delta) + 1.0 / (2.0 * G));
                double step = 0.5 * S * Erf.erfc((z / delta));

                sumPeaks += A * (base + tail + step);
            }

            fitCurve[i] += sumPeaks+B; 
            touched[i] = true;
        }

    }

       double[] composed = spec.getCounts().clone();
       for (int i = 0; i < n; i++) if (touched[i]) composed[i] = fitCurve[i];
       logger.info("Created peak-fitted Spectrum.");
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
        logger.info("Created custom Spectrum.");
        return customSpectrum;
    }

    //===============BACKGROUND===================

    public static Spectrum createBackgroundSpectrum(Spectrum spec) {
        FittingData fitData = new FittingData(spec);
        logger.info("Created background Spectrum.");
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
        logger.info("Created smoothed Spectrum using Savitzky-Golay.");
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
        logger.info("Created smoothed Spectrum using Gaussian.");
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
        Spectrum gauss = createSmoothedSpectrumUsingGauss(spec, 0);
        variants[3] = createBackgroundSpectrum(gauss);

        return variants;
    }


    }
