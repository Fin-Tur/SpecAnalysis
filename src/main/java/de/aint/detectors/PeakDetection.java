package de.aint.detectors;

import de.aint.builders.SpectrumBuilder;
import de.aint.models.*;
import de.aint.operations.*;
import de.aint.operations.calculators.Calculator.CalculatingAlgos;
import de.aint.readers.IsotopeReader;

import java.util.ArrayList;


public class PeakDetection {

    public static ArrayList<Peak> detectPeaks(Spectrum spec){

            ArrayList<Peak> peaks = new ArrayList<>();
            Spectrum smoothed = SpectrumBuilder.createSmoothedSpectrumUsingSG(spec, 0, 0, true, 0);

            Spectrum background = SpectrumBuilder.createBackgroundSpectrum(SpectrumBuilder.createSmoothedSpectrumUsingGauss(spec, 3.0)); //Using gauss smoothed so no undershooting

            Spectrum clearedSpectrum = CalculatingAlgos.SUBTRACTION.calculate(smoothed, background);

            double[] counts = clearedSpectrum.getCounts();
            double[] backgroundCnt = background.getCounts();

            double[] energy = spec.getEnergy_per_channel();
            for(int i = 1; i<counts.length - 1; i++) {
                //Initialize treshhold
                double treshhold = backgroundCnt[i] + 1.65f * Math.sqrt(backgroundCnt[i]);
                if(counts[i] > counts[i - 1] && counts[i] > counts[i + 1] && counts[i] > treshhold && energy[i] < 10000) {
                    // Found a peak
                    peaks.add(new Peak(energy[i]));
                }
            }

        //Match Peaks w Isotopes
        IsotopeReader isotopeReader = new IsotopeReader("C:\\Users\\f.willems\\Projects\\SpecAnalysis\\src\\main\\resources\\isotop_details.txt");
        isotopeReader.readIsotopes();
        for(Peak peak : peaks) {
            Isotop matchedIso = MatchPeakWithIsotop.matchRoiWithIsotop(peak, isotopeReader, 1);
            peak.setEstimatedIsotope(matchedIso);
            
        }


        //Return peaks
        return peaks;
    }


    public static ROI[] splitSpectrumIntoRois(Spectrum spec) {
        ArrayList<Peak> peaks = PeakDetection.detectPeaks(spec);
        ArrayList<ROI> rois = new ArrayList<>();

        while(!peaks.isEmpty()) {
            ArrayList<Peak> currentPeaks = new ArrayList<>();
            Peak peak = peaks.remove(0);
            double FWHM = spec.getFwhmForNumber(Helper.findChannelFromEnergy(peak.getPeakCenter(), spec.getEnergy_per_channel()));
            double startEnergy = Math.max(0, peak.getPeakCenter() - FWHM);
            double endEnergy = Math.min(spec.getEnergy_per_channel()[spec.getChannel_count() - 1], peak.getPeakCenter() + FWHM);
            currentPeaks.add(peak);

            while(!peaks.isEmpty() && peaks.get(0).getPeakCenter() < endEnergy) {
                Peak nextPeak = peaks.remove(0);
                FWHM = spec.getFwhmForNumber(Helper.findChannelFromEnergy(nextPeak.getPeakCenter(), spec.getEnergy_per_channel()));
                endEnergy = Math.min(spec.getEnergy_per_channel()[spec.getChannel_count() - 1], nextPeak.getPeakCenter() + FWHM);
                currentPeaks.add(nextPeak);
            }

            rois.add(new ROI(spec, currentPeaks.toArray(new Peak[0]), startEnergy, endEnergy));
        }

        return rois.toArray(new ROI[0]);
    }
 

    /*
     * Calculate the endpoint of a peak in a spectrum.
     * @Params
     * dir = Direction of the search (+ for right, - for left, value is window size) Warning: dir will be used for window adaptation through incrementing and returned
     * treshhold = counts/channel (good estimated value is at 5-10% of peak intensity)
     * maxSpan = Maximum span to search for peak endpoint
     */
    /*private static int calculatePeakEndpoint(double[] counts, int peakCenter, int dir, double threshold, int maxSpan){

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

    }*/

    /*public static void detectAndSetPeakSizeUsingGradient(ROI roi, int windowSize) {
        int peakCenter = Helper.findChannelFromEnergy(roi.getPeakCenter(), roi.getSpectrum().getEnergy_per_channel());
        double peakHeight = roi.getSpectrum().getCounts()[peakCenter];

        int startChannel = calculatePeakEndpoint(roi.getSpectrum().getCounts(), peakCenter, -windowSize, peakHeight * 0.05, 30);
        int endChannel = calculatePeakEndpoint(roi.getSpectrum().getCounts(), peakCenter, windowSize, peakHeight * 0.05, 30);

        roi.setStartEnergy(roi.getSpectrum().getEnergy_per_channel()[startChannel]);
        roi.setEndEnergy(roi.getSpectrum().getEnergy_per_channel()[endChannel]);

    }*/

}

