package de.aint.models;
import de.aint.operations.AreaCalculator;

public class ROI {

    private final Spectrum spectrum;
    private final double startEnergy;
    private final double endEnergy;

    private double areaOverBackground;

    // Getters
    public Spectrum getSpectrum() {
        return spectrum;
    }
    public double getStartEnergy() {
        return startEnergy;
    }   
    public double getEndEnergy() {
        return endEnergy;
    }
    public double getAreaOverBackground() {
        return areaOverBackground;
    }

    //Setter
    public void setAreaOverBackground() {
        this.areaOverBackground = AreaCalculator.calculateAreaOverBackground(this.spectrum, this.startEnergy, this.endEnergy);
    }

    //Constructor for a Region of Interest (ROI) in a Spectrum
    public ROI(Spectrum spec, double startEnergy, double endEnergy) {
        this.spectrum = spec;
        this.startEnergy = startEnergy;
        this.endEnergy = endEnergy;

    }



}
