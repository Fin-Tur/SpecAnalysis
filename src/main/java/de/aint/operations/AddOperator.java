package de.aint.operations;

import de.aint.models.Spectrum;

import java.util.Arrays;
import java.util.stream.IntStream;


public class AddOperator {

    public static Spectrum add(Spectrum spec1, Spectrum spec2, int channel_count){
        //Equal calibrations
        if(spec1.getChannel_count() == spec2.getChannel_count() && Arrays.equals(spec1.getEnergy_per_channel(), spec2.getEnergy_per_channel())){
            return Helper.operationOnEqualCalibrations(spec1, spec2, channel_count, true);
        }//Else

        return Helper.operationOnDiverseCalibrations(spec1, spec2, channel_count, true);
    }
}
