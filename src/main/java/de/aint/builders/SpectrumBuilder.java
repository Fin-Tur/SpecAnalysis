package de.aint.builders;

import de.aint.models.*;
import de.aint.readers.IsotopeReader;
import java.util.ArrayList;

public class SpectrumBuilder {
    public static Spectrum createCustomSpectrum(Spectrum spectrum, ArrayList<String> selectedIsotopesAsIDString, IsotopeReader isotopeReader) {
        //gather selected Isotopes
        ArrayList<Isotop> isotopes = isotopeReader.isotopes;
        Isotop[] selectedIsos = isotopes.stream().filter(iso -> selectedIsotopesAsIDString.contains(iso.id)).toArray(Isotop[]::new);

        //Create Spectrum with peaks, over selected Channels 

        return spectrum;
    }
}
