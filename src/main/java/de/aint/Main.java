package de.aint;


import de.aint.detectors.PeakDetection;
import de.aint.models.*;
import de.aint.readers.Reader;
import de.aint.builders.*;

public class Main {
    public static void main(String[] args){

        Spectrum spec1 = Reader.readFile("C:/Users/f.willems/IdeaProjects/SpecAnalysis/src/main/resources/Leere_Kammer_85_40_50_1000_930_p_8k.Spe");
        //spec1.changeEnergyCal(1677, 2223.248, 391, 511);
        
        Spectrum smooth = SpectrumBuilder.createSmoothedSpectrumUsingSG(spec1, 0, 0, false, 0);
        Spectrum bckgroundsmt = SpectrumBuilder.createBackgroundSpectrum(spec1);

    

        ROI[] detectedPeaks = PeakDetection.detectPeaks(spec1, bckgroundsmt);
        for(ROI peak : detectedPeaks) {
            System.out.println("Detected Isotope: " + peak.getEstimatedIsotope() + " @ energy [keV] "+ peak.getStartEnergy()+1);
        }

        return;

    }
}