package de.aint.operations;

import static java.util.Collections.min;

import java.util.stream.IntStream;

import de.aint.models.*;
public class Helper {

    //Finds channel with least abs distance to energy lvl
    public static int findChannelFromEnergy(double energy, double[] eRanges){
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

    public static double[] createFittingEnergyLevels(double[] energy1, double[] energy2, int channel_count){
        double energy_lowest = Math.min(energy1[0], energy2[0]);
        double energy_highest = Math.max(energy1[energy1.length-1], energy2[energy2.length-1]);
        float channel_size = (float) (Math.abs(energy_lowest - energy_highest)/channel_count);

        double[] eRanges = new double[channel_count];
        for(int i = 0; i < channel_count; i++){
            eRanges[i] = energy_lowest + i*channel_size;
        }

        return eRanges;
    }

}
