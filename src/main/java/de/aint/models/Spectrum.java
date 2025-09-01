package de.aint.models;

import java.util.*;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aint.models.Persistence.Project.ProjectEntity;
import de.aint.operations.fitters.Fitter;
import de.aint.operations.fitters.FittingData;



public class Spectrum {

    //Spring Use Only
    public Spectrum(){}

    public String name;
    private Long id;
    private Long projectId;

    private static final Logger logger = LoggerFactory.getLogger(Spectrum.class);

    
    //Peak width increasement given by shape_cal(keV) // x^0, x^1, x^2
    static double[] shape_cal = {1.14618E-3, 7.58067E-3, 4.98421E-7};

    //Counts in channels oa Counts per channel(energy range)
    private double[] counts;
    //Number of channels
    private int channel_count;
    //Energy - channels : initialized w/ channelsToEnergy
    private double[] energy_per_channel;
    //srcForce in n/s || mcnp = cpunt*this
    private double srcForce = 1;

    private double[] backgroundCounts;

    //Variables for channel - energy calculation
    private double ec_offset;
    private double ec_slope;
    private double ec_quad;
    //Constructor for Spe files
    public Spectrum(double[] counts, double ec_offset, double ec_slope, double ec_quad){
        this.channel_count = counts.length;
        this.counts = Arrays.copyOf(counts, counts.length);
        this.ec_offset = ec_offset;
        this.ec_slope = ec_slope;
        this.ec_quad = ec_quad;
        energy_per_channel = new double[counts.length];
        this.convertChannelsToEnergy();
        //this.backgroundCounts = Fitter.BackgroundFitAlgos.ALS_FAST.fit(new FittingData(this));
    }
    //Overloading for mncp specs
    public Spectrum(double[] energy, double[] counts){
        this.channel_count = energy.length;
        this.counts = Arrays.copyOf(counts, counts.length);
        this.ec_offset = 0;
        this.ec_slope = 0;
        this.ec_quad = 0;
        this.energy_per_channel = Arrays.copyOf(energy, energy.length);
        //this.backgroundCounts = Fitter.BackgroundFitAlgos.ALS_FAST.fit(new FittingData(this));
    }

    //Overloading for Domain/Entity Mapping
    public Spectrum(Long id, String name, double[] counts, double ec_offset, double ec_slope, double ec_quad, double srcForce){
        this.id = id;
        this.name = name;
        this.channel_count = counts.length;
        this.counts = Arrays.copyOf(counts, counts.length);
        this.ec_offset = ec_offset;
        this.ec_slope = ec_slope;
        this.ec_quad = ec_quad;
        this.srcForce = srcForce;
        energy_per_channel = new double[counts.length];
        this.convertChannelsToEnergy();
        //this.backgroundCounts = Fitter.BackgroundFitAlgos.ALS_FAST.fit(new FittingData(this));
    }

    //Function to channel data
    private void convertChannelsToEnergy(){
        for(int channel = 0; channel < channel_count; channel++){
            energy_per_channel[channel] = ec_offset + ec_slope*(channel) + ec_quad*(channel*channel);
        }
    }

    //Function to normalize cnts
    public void normalizeCounts(){
        if(srcForce == 1) return;
        IntStream.range(0,this.counts.length).forEach(i -> this.counts[i]*=this.srcForce);
    }


    public void changeEnergyCal(int[] channels, double[] energies) {
    if (channels.length != energies.length || channels.length < 2) {
        throw new IllegalArgumentException("At least two energy channel pairs required.");
    }

    int n = channels.length;
    double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

    //Sums for linear Regression
    for (int i = 0; i < n; i++) {
        sumX  += channels[i];
        sumY  += energies[i];
        sumXY += channels[i] * energies[i];
        sumX2 += channels[i] * channels[i];
    }
    if (n * sumX2 - sumX * sumX == 0) {
        throw new IllegalArgumentException("Invalid channel/energy pairs.");
    }
    double slope  = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
    double offset = (sumY - slope * sumX) / n;

    this.ec_offset = offset;
    this.ec_slope = slope;
    this.convertChannelsToEnergy();
    logger.info("Changed Spectrum's energy calibration.");
}


    public void setBackgroundCounts() {
        this.backgroundCounts = Fitter.BackgroundFitAlgos.ALS_FAST.fit(new FittingData(this));
    }

    public double getSrcForce(){
        return this.srcForce;
    }

    public double[] getCounts(){
        return this.counts;
    }

    public double getEc_offset() {
        return ec_offset;
    }

    public double getEc_slope() {
        return ec_slope;
    }

    public double getEc_quad() {
        return ec_quad;
    }


    public double[] getEnergy_per_channel() {
        return this.energy_per_channel;
    }

    public int getChannel_count(){
        return this.channel_count;
    }

    public double getFwhmForNumber(int channel){ // Full Width at Half Maximum
        return shape_cal[0] + shape_cal[1] * channel + shape_cal[2] * channel * channel;
    }

    public double[] getBackgroundCounts() {
        return this.backgroundCounts;
    }

    //Setter
    public void setCounts(double[] counts) {
        this.counts = counts;
    }

    public void setEnergy_per_channel(double[] energy_per_channel) {
        this.energy_per_channel = energy_per_channel;
    }

    public void setChannel_count(int channel_count) {
        this.channel_count = channel_count;
    }

    public void setEc_offset(double ec_offset) {
        this.ec_offset = ec_offset;
    }

    public void setEc_slope(double ec_slope) {
        this.ec_slope = ec_slope;
    }

    public void setEc_quad(double ec_quad) {
        this.ec_quad = ec_quad;
    }

    public void setBackgroundCounts(double[] backgroundCounts) {
        this.backgroundCounts = backgroundCounts;
    }

    public void setSrcForce(double srcForce) {
        this.srcForce = srcForce;
    }

    public void setShape_cal(double[] shape_cal) {
        this.shape_cal = shape_cal;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

}
