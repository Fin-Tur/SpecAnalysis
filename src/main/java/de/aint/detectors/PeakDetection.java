package de.aint.detectors;

import de.aint.models.*;
import de.aint.operations.*;
import de.aint.readers.IsotopeReader;

import java.util.ArrayList;


public class PeakDetection {

    public static ROI[] detectPeaks(Spectrum spec, Spectrum background){

            ArrayList<ROI> peaks = new ArrayList<>();
            Spectrum smoothed = OvulationOperator.smoothSpectrum(spec, 11, 2, true, 1);

            Spectrum clearedSpectrum = SubstractOperator.substract(smoothed, background, smoothed.getChannel_count());

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
                    System.out.printf("%f\t%f\t%d\t%f\n", smoothed.getEnergy_per_channel()[i], counts[i], i, treshhold);
                    peaks.add(new ROI(spec, energy[i], energy[i], energy[i]));
                }
            }

        //Match ROIs w Isotopes
        IsotopeReader isotopeReader = new IsotopeReader();
        for(ROI roi : peaks) {
            Isotop matchedIso = MatchRoiWithIsotop.matchRoiWithIsotop(roi, isotopeReader, 1);
            roi.setEstimatedIsotope(matchedIso);
            //System.out.println("Isotop matched!");
            
        }
        //Return peaks
        return peaks.toArray(new ROI[0]);
    }

}
