package de.aint.detectors;

import de.aint.builders.SpectrumBuilder;
import de.aint.models.*;
import de.aint.operations.*;
import de.aint.operations.calculators.Calculator.CalculatingAlgos;
import de.aint.readers.IsotopeReader;

import java.util.ArrayList;


public class PeakDetection {

    public static ROI[] detectPeaks(Spectrum spec){

            ArrayList<ROI> peaks = new ArrayList<>();
            Spectrum smoothed = SpectrumBuilder.createSmoothedSpectrumUsingSG(spec, 0, 0, false, 0);
            Spectrum background = SpectrumBuilder.createBackgroundSpectrum(spec);

            Spectrum clearedSpectrum = CalculatingAlgos.SUBTRACTION.calculate(smoothed, background);

            double[] counts = clearedSpectrum.getCounts();
            double[] backgroundCnt = background.getCounts();

            double[] energy = spec.getEnergy_per_channel();
            System.out.println("ENERGY\tCOUNTS\tCHANNEL\tTRHESHOLD\n");
            for(int i = 1; i<counts.length - 1; i++) {
                //Initialize treshhold
                double treshhold = backgroundCnt[i] + 1.65f * Math.sqrt(backgroundCnt[i]);
                if(counts[i] > counts[i - 1] && counts[i] > counts[i + 1] && counts[i] > treshhold && energy[i] < 10000) {
                    // Found a peak
                    ////System.out.println("Found peak at " + smoothed.getEnergy_per_channel()[i] + " channel " + i + " with intensity " + counts[i]);
                    //System.out.printf("%f\t%f\t%d\t%f\n", smoothed.getEnergy_per_channel()[i], counts[i], i, treshhold);
                    peaks.add(new ROI(spec, energy[i]));
                }
            }

        //Match ROIs w Isotopes
        IsotopeReader isotopeReader = new IsotopeReader("C:/Users/f.willems/IdeaProjects/SpecAnalysis/src/main/resources/isotop_details.txt");
        isotopeReader.readIsotopes();
        for(ROI roi : peaks) {
            Isotop matchedIso = MatchRoiWithIsotop.matchRoiWithIsotop(roi, isotopeReader, 1);
            roi.setEstimatedIsotope(matchedIso);
            
        }

        //Return peaks
        return peaks.toArray(new ROI[0]);
    }
 
    public static ROI[] detectPeaksUsingSecondDerivative(Spectrum spec, Spectrum background) {
        //TODO
        return null; 

    }


    /*
     * Calculate the endpoint of a peak in a spectrum.
     * @Params
     * dir = Direction of the search (+ for right, - for left, value is window size) Warning: dir will be used for window adaptation through incrementing and returned
     * treshhold = counts/channel (good estimated value is at 5-10% of peak intensity)
     * maxSpan = Maximum span to search for peak endpoint
     */
    private static int calculatePeakEndpoint(double[] counts, int peakCenter, int dir, double threshold, int maxSpan){

        double gradient = Double.MAX_VALUE;
        int base = peakCenter;

        for(int i = 0; i < maxSpan; i++) {
            int x1 = peakCenter + dir;
            int x2 = base;

            //Endpoint of spectrum reached, return last possible endpoint
            if(x1 < 0 || x1 >= counts.length) {
                return x1 < 0 ? 0 : counts.length - 1;
            }
            //No gradient, make window bigger
            if(!(x2 - x1 == 0)){
                gradient = Math.abs(counts[x2] - counts[x1]) / (Math.abs(x2 - x1));
            }

            if(gradient <= threshold){
                return x1;
            }
           
            dir = dir > 0 ? dir + 1 : dir - 1;
            base = dir > 0 ? base + 1 : base - 1;
        }

        return peakCenter + dir;

    }

    public static void detectAndSetPeakSizeUsingGradient(ROI roi, int windowSize) {
        int peakCenter = Helper.findChannelFromEnergy(roi.getPeakCenter(), roi.getSpectrum().getEnergy_per_channel());
        double peakHeight = roi.getSpectrum().getCounts()[peakCenter];

        int startChannel = calculatePeakEndpoint(roi.getSpectrum().getCounts(), peakCenter, -windowSize, peakHeight * 0.05, 30);
        int endChannel = calculatePeakEndpoint(roi.getSpectrum().getCounts(), peakCenter, windowSize, peakHeight * 0.05, 30);

        roi.setStartEnergy(roi.getSpectrum().getEnergy_per_channel()[startChannel]);
        roi.setEndEnergy(roi.getSpectrum().getEnergy_per_channel()[endChannel]);

    }

    public static void detectAndSetPeakSizeUsingFWHM(ROI roi, double multiplicator) {
        final double FWHM = roi.getSpectrum().getFwhmForNumber(Helper.findChannelFromEnergy(roi.getPeakCenter(), roi.getSpectrum().getEnergy_per_channel()))*multiplicator;

        double startEnergy = Math.max(roi.getPeakCenter() - FWHM / 2, 0);
        double endEnergy = Math.min(roi.getPeakCenter() + FWHM / 2, roi.getSpectrum().getEnergy_per_channel()[roi.getSpectrum().getChannel_count() - 1]);
        System.out.println("FWHM Peak start: " + startEnergy + ", end: " + endEnergy + " FWHM: " + FWHM);
        roi.setStartEnergy(startEnergy);
        roi.setEndEnergy(endEnergy);

    }
}

