package de.aint.operations;

import de.aint.models.Spectrum;

public class RemNoiseOperator {

    public static Spectrum removeNoiseWithBackgroundSpectrum(Spectrum spec, Spectrum background){
        return SubstractOperator.substract(spec, background, spec.getChannel_count());
    }

}
