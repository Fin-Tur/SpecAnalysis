package de.aint.operations;

import de.aint.models.*;
import de.aint.builders.CurveFitter;

public class AreaCalculator {
    
    public static double calculateAreaOverBackground(Spectrum spectrum, double startEnergy, double endEnergy) {

        //Calculate Background using ALS    
        double[] estimatedBackground = OvulationOperator.estimateBackgroundUsingALS(spectrum, 2e4, 8e-4, 50);
        Spectrum backgroundSpectrum = new Spectrum(spectrum.getEnergy_per_channel(), estimatedBackground);
        //Substract Background from Spectrum
        Spectrum cleanedSpectrum = SubstractOperator.substract(spectrum, backgroundSpectrum, spectrum.getChannel_count());

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

    
    public static double calculateAreaUsingGauss(ROI roi) {

        double[] params = CurveFitter.fitGaussCurveToRoi(roi);

        // The area under the Gaussian is given by the formula:
        // Area = A * sqrt(2 * pi) * sigma
        double amplitude = params[0];
        double mean = params[1];
        double sigma = params[2];

        //Returns Area
        return amplitude * Math.sqrt(2 * Math.PI) * sigma;

    }
}
