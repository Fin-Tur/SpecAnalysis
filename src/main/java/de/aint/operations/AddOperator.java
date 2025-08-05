package de.aint.operations;

import de.aint.models.Spectrum;

import static java.util.Collections.min;

public class AddOperator {

    private static int findChannelFromEnergy(double energy, double[] eRanges){
        int idx = 0;
        double minDiff = Math.abs(energy - eRanges[0]);
        for (int i = 1; i < eRanges.length; i++) {
            double diff = Math.abs(energy - eRanges[i]);
            if (diff < minDiff) {
                minDiff = diff;
                idx = i;
            }
        }
        return idx;
    }

    public static Spectrum add(Spectrum spec1, Spectrum spec2, int channel_count){
        //Normalize counts
        spec1.normalizeCounts();
        spec2.normalizeCounts();

        //Create channel/energy range
        double[] energy1 = spec1.getEnergy_per_channel();
        double[] energy2 = spec2.getEnergy_per_channel();

        double energy_lowest = Math.min(energy1[0], energy2[0]);
        double energy_highest = Math.max(energy1[energy1.length-1], energy2[energy2.length-1]);
        float channel_size = (float) (Math.abs(energy_lowest - energy_highest)/channel_count);

        double[] eRanges = new double[channel_count];
        for(int i = 0; i < channel_count; i++){
            eRanges[i] = energy_lowest + i*channel_size;
        }

        //Create counts
        double[] counts1 = spec1.getCounts();
        double[] counts2 = spec2.getCounts();

        double[] counts = new double[channel_count];
        //Add counts from spec 1 to new spec
        for(int i = 0; i < counts1.length; i++){
            int idx = findChannelFromEnergy(energy1[i], eRanges);
            if(idx >= 0 && idx < counts.length) counts[idx] += counts1[i];
        }
        //Add counts from spec 2 to new spec
        for(int i = 0; i < counts2.length; i++){
            int idx = findChannelFromEnergy(energy2[i], eRanges);
            if(idx >= 0 && idx < counts.length) counts[idx] += counts2[i];
        }


        return new Spectrum(eRanges, counts);
    }
}
