package de.aint.models;
import de.aint.operations.AreaCalculator;

public class ROI {

    private final Spectrum spectrum;
    private final double startEnergy;
    private final double endEnergy;

    private double areaOverBackground;
    private String estimatedIsotope = null;

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

    public String getEstimatedIsotope() {
        return estimatedIsotope;}

    //Setter
    public void setAreaOverBackground() {
        this.areaOverBackground = AreaCalculator.calculateAreaOverBackground(this.spectrum, this.startEnergy, this.endEnergy);
    }

    public void setEstimatedIsotope(Isotop isotope) {
        if(isotope == null) this.estimatedIsotope="unk";
        else{
            this.estimatedIsotope = isotope.symbol + "--" + isotope.id;
        }
    }

    //Constructor for a Region of Interest (ROI) in a Spectrum
    public ROI(Spectrum spec, double startEnergy, double endEnergy) {
        this.spectrum = spec;
        this.startEnergy = startEnergy;
        this.endEnergy = endEnergy;

    }



}
