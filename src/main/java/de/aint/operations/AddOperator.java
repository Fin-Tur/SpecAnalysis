package de.aint.operations;

import de.aint.models.Spectrum;

import java.util.Arrays;
import java.util.stream.IntStream;


public class AddOperator {

    public static Spectrum add(Spectrum spec1, Spectrum spec2, int channel_count){
        //Equal calibrations
        if(spec1.getChannel_count() == spec2.getChannel_count() && Arrays.equals(spec1.getEnergy_per_channel(), spec2.getEnergy_per_channel())){
            double[] counts1 = spec1.getCounts();
            double[] counts2 = spec2.getCounts();
            double[] counts = new double[spec1.getChannel_count()];
            //Addition
            IntStream.range(0, counts.length).forEach(i -> counts[i] = counts1[i]+counts2[i]);

            return new Spectrum(spec1.getEnergy_per_channel(), counts);
        }//Else

        //Normalize counts
        spec1.normalizeCounts();
        spec2.normalizeCounts();

        //Create channel/energy range
        double[] energy1 = spec1.getEnergy_per_channel();
        double[] energy2 = spec2.getEnergy_per_channel();
        double[] e_ranges = Helper.createFittingEnergyLevels(spec1.getEnergy_per_channel(), spec2.getEnergy_per_channel(), channel_count);

        //Create counts
        double[] counts1 = spec1.getCounts();
        double[] counts2 = spec2.getCounts();
        double[] counts = new double[channel_count];

        //Add counts from spec 1 to new spec
        for(int i = 0; i < counts1.length; i++){
            int idx = Helper.findChannelFromEnergy(energy1[i], e_ranges);
            if(idx >= 0 && idx < counts.length) counts[idx] += counts1[i];
        }

        //Add counts from spec 2 to new spec
        for(int i = 0; i < counts2.length; i++){
            int idx = Helper.findChannelFromEnergy(energy2[i], e_ranges);
            if(idx >= 0 && idx < counts.length) counts[idx] += counts2[i];
        }


        return new Spectrum(e_ranges, counts);
    }
}
