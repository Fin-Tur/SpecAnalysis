package de.aint.detectors;
import de.aint.models.*;
import de.aint.readers.IsotopeReader;

public class MatchRoiWithIsotop {

    //Calculates matching energy incl. toleranze
    private static boolean matches_energy(double roi_energy, double isotop_energy, double tolerance) {
        return Math.abs(roi_energy - isotop_energy) <= tolerance;
    }

    private static boolean matches_intensity(double roi_intensity, double isotop_intensity, double tolerance) {
        return Math.abs(roi_intensity - isotop_intensity) <= tolerance;
    }

    // Method to match a region of interest (ROI) with isotopic data
    public static Isotop matchRoiWithIsotop(ROI roi, IsotopeReader isoReader, double tolerance) {
        //Prepare roi + isotopes for matching
        //roi.setAreaOverBackground();

        for (Isotop isotop : isoReader.isotopes) {
            if( matches_energy((roi.getPeakCenter()), isotop.energy, tolerance)) {
                // If both energy and intensity match, return the isotop
                return isotop;
            }
        }

        return isoReader.isotopes.getFirst();
    }
}