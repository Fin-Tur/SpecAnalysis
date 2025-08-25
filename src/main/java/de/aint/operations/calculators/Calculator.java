package de.aint.operations.calculators;

import java.util.Arrays;

import org.apache.commons.math3.special.Erf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aint.models.ROI;
import de.aint.models.Spectrum;
import de.aint.operations.Helper;
import de.aint.operations.fitters.Fitter;
import de.aint.operations.fitters.FittingData;

public class Calculator {

    private static final Logger logger = LoggerFactory.getLogger(Calculator.class);

    // ======== INTERFACES ==========

    private interface NumericCalculations{
        Spectrum calculate(Spectrum spec1, Spectrum spec2);
    }   

    private interface AreaCalculations{
        double calculateArea(ROI roi);
    }

    // ======== ENUMS ==========

    public enum CalculatingAlgos implements NumericCalculations{
        ADDITION {
            @Override
            public Spectrum calculate(Spectrum spec1, Spectrum spec2) {
                return RunAlgos.add(spec1, spec2);
            }
        },
        SUBTRACTION {
            @Override
            public Spectrum calculate(Spectrum spec1, Spectrum spec2) {
                return RunAlgos.subtract(spec1, spec2);
            }
                
        }
    }

    public enum AreaAlgos implements AreaCalculations{
        GAUSS {
            @Override
            public double calculateArea(ROI roi) {
                return RunAlgos.calculateAreaUsingGauss(roi);
            }
        },
        COUNTS {
            @Override
            public double calculateArea(ROI roi) {
                return RunAlgos.calculateAreaUsingCounts(roi);
            }
        }
    }
    

    //============== METHODS ================

    private class RunAlgos{

        //============= ADDITION =============

        private static Spectrum add(Spectrum spec1, Spectrum spec2) {
        int channel_count = spec1.getChannel_count();

        if(spec1 == null || spec2 == null) {
            throw new IllegalArgumentException("Both spectra must be provided for addition.");
        }

        //Equal calibrations
        if(spec1.getChannel_count() == spec2.getChannel_count() && Arrays.equals(spec1.getEnergy_per_channel(), spec2.getEnergy_per_channel())){
            return CalculatorHelper.numericOperationOnEqualCalibrations(spec1, spec2, channel_count, true);
        }//Else

        return CalculatorHelper.numericOperationOnDiverseCalibrations(spec1, spec2, channel_count, true);
        }

    //============== SUBSTRACTION ===================================================

        private static Spectrum subtract(Spectrum spec1, Spectrum spec2) {
            int channel_count = spec1.getChannel_count();

            if(spec1 == null || spec2 == null) {
                throw new IllegalArgumentException("Both spectra must be provided for subtraction.");
            }

            //Equal calibrations
            if(spec1.getChannel_count() == spec2.getChannel_count() && Arrays.equals(spec1.getEnergy_per_channel(), spec2.getEnergy_per_channel())){
                return CalculatorHelper.numericOperationOnEqualCalibrations(spec1, spec2, channel_count, false);
            }//Else

            return CalculatorHelper.numericOperationOnDiverseCalibrations(spec1, spec2, channel_count, false);
        }

    //================ AREA WITH GAUSS===============================
        private static double calculateAreaUsingGauss(ROI roi) {

            int startChannel = Helper.findChannelFromEnergy(roi.getStartEnergy(), roi.getSpectrum().getEnergy_per_channel());
            int endChannel = Helper.findChannelFromEnergy(roi.getEndEnergy(), roi.getSpectrum().getEnergy_per_channel());

            double[] params = Fitter.PeakFitAlgos.GAUSSLM.fit(roi);
            double[] E = roi.getSpectrum().getEnergy_per_channel();
            double[] counts = roi.getSpectrum().getCounts();

            double sigma = params[1];
            double inv2s2 = 1.0 / (2.0 * sigma * sigma);

            int nPeaks = (params.length - 2) / 5;

            double area = 0.0;

            for(int i = 0; i < nPeaks; i++) {
                int offset = 2 + i * 5;
                double A = params[offset];
                double mu = params[offset + 1];
                double T = params[offset + 2];
                double G = params[offset + 3];
                double S = params[offset + 4];
                double delta = Math.sqrt(2)*sigma;

                for(int channel = startChannel; channel <= endChannel; channel++) {

                    double z  = E[i] - mu;
                    double core = Math.exp(-z*z * inv2s2);
                    double tail = 0.5*T * Math.exp(z / (G * delta)) * Erf.erfc((z / delta) + 1.0 / (2.0 * G));
                    double step = 0.5*S * Erf.erfc((z / delta));
                    area += A * (core + tail + step);
                }
            }
            logger.info("Calculated area using Gauss: {}", area);
            return area;

        }

    //================ AREA WITH COUNTS =========================
        public static double calculateAreaUsingCounts(ROI roi) {

            //========================================================================================
            //Calculate Background using ALS
            double[] estimatedBackground = Fitter.BackgroundFitAlgos.ALS_FAST.fit(new FittingData(roi.getSpectrum()));

            Spectrum backgroundSpectrum = new Spectrum(roi.getSpectrum().getEnergy_per_channel(), estimatedBackground);

            //Substract Background from Spectrum
            Spectrum cleanedSpectrum = CalculatingAlgos.SUBTRACTION.calculate(roi.getSpectrum(), backgroundSpectrum);
            //========================================================================================

            //Find start and end channels
            int startChannel = Helper.findChannelFromEnergy(roi.getStartEnergy(), cleanedSpectrum.getEnergy_per_channel());
            int endChannel = Helper.findChannelFromEnergy(roi.getEndEnergy(), cleanedSpectrum.getEnergy_per_channel());

            double area = 0.0;
            double[] counts = cleanedSpectrum.getCounts();
            //Calculate area over background
            if (startChannel >= 0 && endChannel >= 0 && startChannel < cleanedSpectrum.getChannel_count() && endChannel < cleanedSpectrum.getChannel_count()) {
                for (int i = startChannel; i <= endChannel; i++) {
                    area += counts[i];
                }  
            }
            return area; //Return Area
        }

    }

}
