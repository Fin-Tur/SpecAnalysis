package de.aint;


import de.aint.detectors.PeakDetection;
import de.aint.detectors.SumGaussNumeric;
import de.aint.models.*;
import de.aint.readers.Reader;
import de.aint.builders.*;

//==================================================DEBUGGING-ONLY=================================================================================


public class Main {
    public static void main(String[] args){

        //Prepare Spec
        Spectrum spec = Reader.readFile("C:/Users/f.willems/IdeaProjects/SpecAnalysis/src/main/resources/Leere_Kammer_85_40_50_1000_930_p_8k.Spe");
        int[] channels = {1677, 391, 3722, 5740};
        double[] energies = {2223.248, 511, 4945.301, 7631.136};
        spec.changeEnergyCal(channels, energies);

        ROI[] rois = PeakDetection.splitSpectrumIntoRois(spec);

        ROI roi = rois[5];
        double[] fitParams = SumGaussNumeric.fitGaussToROI(roi);
        for(var param : fitParams) {
            System.out.println("Fitted Parameter: " + param);
        }

        return;

    }
}