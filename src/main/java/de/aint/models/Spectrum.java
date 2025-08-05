package de.aint.models;

import java.lang.reflect.Array;
import java.util.*;

public class Spectrum {

    //Counts in channels oa Counts per channel(energy range)
    private final double[] counts;
    //Number of channels
    private final int channel_count;
    //Energy - channels : initialized w/ channelsToEnergy
    private final double[] energy_per_channel;
    //srcForce in n/s || mcnp = cpunt*this
    private float srcForce;

    //Variables for channel - energy calculation
    private final double ec_offset;
    private final double ec_slope;
    private final double ec_quad;
    //Constructor for Spe files
    public Spectrum(double[] counts, double ec_offset, double ec_slope, double ec_quad){
        this.channel_count = counts.length;
        this.counts = Arrays.copyOf(counts, counts.length);
        this.ec_offset = ec_offset;
        this.ec_slope = ec_slope;
        this.ec_quad = ec_quad;
        energy_per_channel = new double[counts.length];
    }
    //Overloading for mncp specs
    public Spectrum(double[] energy, double[] counts){
        this.channel_count = energy.length;
        this.counts = Arrays.copyOf(counts, counts.length);
        this.ec_offset = 0;
        this.ec_slope = 0;
        this.ec_quad = 0;
        this.energy_per_channel = Arrays.copyOf(energy, energy.length);
    }

    //Function to channel data
    public void convertChannelsToEnergy(){
        for(int channel = 0; channel < channel_count; channel++){
            energy_per_channel[channel] = ec_offset + ec_slope*(channel) + ec_quad*(channel*channel);
        }
    }

    public void setSrcForce(float cntMult){
        this.srcForce = cntMult;
    }

}
