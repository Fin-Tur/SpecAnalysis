package de.aint.builders;

import de.aint.models.*;
import de.aint.operations.*;
import de.aint.readers.IsotopeReader;
import java.util.ArrayList;
import java.util.Arrays;
import de.aint.libraries.*;

public class SpectrumBuilder {
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

    public static Spectrum createBackgroundSpectrum(Spectrum spec, double lambda, double p, int maxIterations) {
        double[] background = new double[spec.getCounts().length];
        SmoothingLib.INSTANCE.estimate_background_als(spec.getCounts(), spec.getCounts().length, lambda, p, maxIterations, background);
        //Cancel weird formation since y axis is log in display
        double[] energy = spec.getEnergy_per_channel();
        for(int i = 0; i < background.length; i++) {
            if (background[i] < 0.2 && energy[i] > 10000) {
                background[i] = 0; // Avoid log(0)
            }
        }

        return new Spectrum(spec.getEnergy_per_channel(), background);
    }

    public static Spectrum createSmoothedSpectrumUsingSG(Spectrum spec, int window_size, int polynomial_degree, boolean eraseOutliers, int iterations) {   
        return OvulationOperator.smoothSpectrum(spec, window_size, polynomial_degree, eraseOutliers, iterations);
    }

    public static Spectrum createSmoothedSpectrumUsingGauss(Spectrum spec, double sigma){
        return OvulationOperator.smoothSpectrumUsingGauss(spec, sigma);
    }

    // 0 => Original Spectrum
    // 1 => Smoothed Spectrum
    // 2 => Background Spectrum
    // 3 => Smoothed Background Spectrum
    public static Spectrum[] createSpectrumVariants(Spectrum spec){
        //Declare Original / Smoothed Variants
        Spectrum[] variants = new Spectrum[4];
        variants[0] = spec;
        variants[1] = createSmoothedSpectrumUsingSG(spec, 11, 2, true, 1);
        //Declare Background variants
        variants[2] = createBackgroundSpectrum(spec, 2e4, 8e-4, 5);
        variants[3] = createBackgroundSpectrum(variants[1], 2e4, 8e-4, 5);

        return variants;
    }

    public static Spectrum createPeakFitSpectrum(Spectrum spec, ROI[] peaks) {

        double[] counts = Arrays.copyOf(spec.getCounts(), spec.getCounts().length);
        boolean[] isPeak = new boolean[counts.length];

        for (ROI peak : peaks) {
            double[] fit = CurveFitter.fitGaussCurveToRoi(peak);
            //Gather start and end energy and add 5 to smoothe out harsh curves
            int startPoint = Helper.findChannelFromEnergy(peak.getStartEnergy(), spec.getEnergy_per_channel())-5;
            int endPoint = Helper.findChannelFromEnergy(peak.getEndEnergy(), spec.getEnergy_per_channel())+5;

            if(startPoint<0) startPoint = 0;
            if(endPoint>=counts.length) endPoint = counts.length-1; 
            // Apply the Gaussian fit to the counts
            for (int i = startPoint; i < endPoint; i++) {
                if(isPeak[i]){
                    double contribution = fit[0] * Math.exp(-0.5 * Math.pow((i - fit[1]) / fit[2], 2));
                    counts[i] = (counts[i] + contribution)/2;
                }else{
                    counts[i] += fit[0] * Math.exp(-0.5 * Math.pow((i - fit[1]) / fit[2], 2));
                    isPeak[i] = true;
                }
            }
        }
        return new Spectrum(spec.getEnergy_per_channel(), counts);
    }

    }
