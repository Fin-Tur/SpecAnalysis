package de.aint.builders;

import de.aint.models.*;
import de.aint.operations.*;
import de.aint.operations.fitters.*;
import de.aint.readers.IsotopeReader;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class SpectrumBuilder {

    //=====================PEAK_FITTING======================================
    public static Spectrum createPeakFitSpectrum(Spectrum spec, ROI[] peaks) {

        double[] counts = Arrays.copyOf(spec.getCounts(), spec.getCounts().length);
        boolean[] isPeak = new boolean[counts.length];

        for (ROI peak : peaks) {
            double[] fit = Fitter.PeakFitAlgos.GAUSS.fit(peak);
            System.out.println(fit[0]);
            //Gather start and end energy and add 5 to smoothe out harsh curves
            int startPoint = Helper.findChannelFromEnergy(peak.getStartEnergy(), spec.getEnergy_per_channel())-5;
            int endPoint = Helper.findChannelFromEnergy(peak.getEndEnergy(), spec.getEnergy_per_channel())+5;

            if(startPoint<0) startPoint = 0;
            if(endPoint>=counts.length) endPoint = counts.length-1; 
            // Apply the Gaussian fit to the counts
            for (int i = startPoint; i < endPoint; i++) {
                if(isPeak[i]){
                    double contribution = fit[0] * Math.exp(-0.5 * Math.pow((i - fit[1]) / fit[2], 2));
                    //counts[i] = (counts[i] + contribution)/2;
                    counts[i] = Math.max(counts[i], contribution); // Keep the maximum value
                }else{
                    counts[i] += fit[0] * Math.exp(-0.5 * Math.pow((i - fit[1]) / fit[2], 2));
                    isPeak[i] = true;
                }
            }
        }
        return new Spectrum(spec.getEnergy_per_channel(), counts);
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
