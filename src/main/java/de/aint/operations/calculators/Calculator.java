package de.aint.operations.calculators;

import java.util.Arrays;

import de.aint.models.ROI;
import de.aint.models.Spectrum;
import de.aint.operations.Helper;
import de.aint.operations.fitters.Fitter;
import de.aint.operations.fitters.FittingData;

public class Calculator {

    // ======== INTERFACES ==========

    private interface NumericCalculations{
        Spectrum calculate(CalculatorData data);
    }   

    private interface AreaCalculations{
        double calculateArea(CalculatorData data);
    }

    // ======== ENUMS ==========

    public enum CalculatingAlgos implements NumericCalculations{
        ADDITION {
            @Override
            public Spectrum calculate(CalculatorData data) {
                return RunAlgos.add(data);
            }
        },
        SUBTRACTION {
            @Override
            public Spectrum calculate(CalculatorData data) {
                return RunAlgos.subtract(data);
            }
                
        }
    }

    public enum AreaAlgos implements AreaCalculations{
        GAUSS {
            @Override
            public double calculateArea(CalculatorData data) {
                return RunAlgos.calculateAreaUsingGauss(data);
            }
        },
        COUNTS {
            @Override
            public double calculateArea(CalculatorData data) {
                return RunAlgos.calculateAreaUsingCounts(data);
            }
        }
    }
    

    //============== METHODS ================

    class RunAlgos{

        //============= ADDITION =============

        private static Spectrum add(CalculatorData data) {
        Spectrum spec1 = data.spectrum1;
        Spectrum spec2 = data.spectrum2;
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

        private static Spectrum subtract(CalculatorData data) {
            Spectrum spec1 = data.spectrum1;
            Spectrum spec2 = data.spectrum2;
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
        private static double calculateAreaUsingGauss(CalculatorData data) {

            if(data.roi == null) {
                throw new IllegalArgumentException("ROI must be provided for area calculation.");
            }

            ROI roi = data.roi;

            double[] params = Fitter.PeakFitAlgos.GAUSS.fit(roi);

            // The area under the Gaussian is given by the formula:
            // Area = A * sqrt(2 * pi) * sigma
            double amplitude = params[0];
            double mean = params[1];
            double sigma = params[2];

            //Returns Area
            return amplitude * Math.sqrt(2 * Math.PI) * sigma;

        }

    //================ AREA WITH COUNTS =========================
        public static double calculateAreaUsingCounts(CalculatorData data) {

            if(data.roi == null) {
                throw new IllegalArgumentException("ROI must be provided for area calculation.");
            }

            Spectrum spectrum = data.roi.getSpectrum();
            double startEnergy = data.roi.getStartEnergy();
            double endEnergy = data.roi.getEndEnergy();

            //Calculate Background using ALS    
            double[] estimatedBackground = Fitter.BackgroundFitAlgos.ALS_FAST.fit(new FittingData(spectrum));
            Spectrum backgroundSpectrum = new Spectrum(spectrum.getEnergy_per_channel(), estimatedBackground);
            //Substract Background from Spectrum
            CalculatorData calcData = new CalculatorData(CalculatorData.OperationType.NUMERIC, spectrum, backgroundSpectrum);
            Spectrum cleanedSpectrum = CalculatingAlgos.SUBTRACTION.calculate(calcData);

            //Find start and end channels
            int startChannel = Helper.findChannelFromEnergy(startEnergy, cleanedSpectrum.getEnergy_per_channel());
            int endChannel = Helper.findChannelFromEnergy(endEnergy, cleanedSpectrum.getEnergy_per_channel());

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
