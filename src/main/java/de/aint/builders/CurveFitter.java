package de.aint.builders;

import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import de.aint.models.ROI;
import de.aint.operations.Helper;

public abstract class CurveFitter {

    public static double[] fitGaussCurveToRoi(ROI roi){
        double[] counts = roi.getSpectrum().getCounts();
        int start_channel = Helper.findChannelFromEnergy(roi.getStartEnergy(), roi.getSpectrum().getEnergy_per_channel());
        int end_channel = Helper.findChannelFromEnergy(roi.getEndEnergy(), roi.getSpectrum().getEnergy_per_channel());

        //Create a Gaussian fitter
        WeightedObservedPoints obs = new WeightedObservedPoints();
        for (int i = start_channel; i <= end_channel; i++) {
            if (i >= 0 && i < counts.length) {
                obs.add(i, counts[i]);
            }
        }
        GaussianCurveFitter fitter = GaussianCurveFitter.create().withMaxIterations(100000);
        // Fit the Gaussian curve to the observed points
        System.out.println("Fitting Gaussian curve " + roi.getPeakCenter());
        double[] gaussParams = {0, roi.getPeakCenter(), 1};

        try{
            gaussParams = fitter.fit(obs.toList());
        }catch(Exception e){
            System.err.println("Error fitting Gaussian curve: " + e.getMessage());
        }
           
        

        return gaussParams;
    }


}