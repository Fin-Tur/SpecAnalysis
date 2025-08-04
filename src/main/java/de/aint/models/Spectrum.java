package de.aint.models;

import java.util.ArrayList;
import java.util.Arrays;

public class Spectrum {

    //Counts in channels oa Counts per channel(energy range)
    private final int[] counts;
    //Number of channels
    private final int channel_count;
    //Energy - channels : initialized w/ channelsToEnergy
    private double[] energy_per_channel;

    //Variables for channel - energy calculation
    private double ec_offset;
    private double ec_slope;
    private double ec_quad;

    public Spectrum(int[] counts){
        this.channel_count = counts.length;
        this.counts = Arrays.copyOf(counts, counts.length);
    }

    //Function to channel data
    public void channelsToEnergy(){
        for(int channel = 0; channel < channel_count; channel++){
            energy_per_channel[channel] = ec_offset + ec_slope*(channel) + ec_quad*(channel*channel);
        }
    }

}
