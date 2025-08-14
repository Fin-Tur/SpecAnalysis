package de.aint.models;
import de.aint.detectors.PeakDetection;
import de.aint.operations.AreaCalculator;

public class ROI {

    private final Spectrum spectrum;
    private double startEnergy;
    private double endEnergy;
    private final double peakCenter;

    private double areaOverBackground;
    private String estimatedIsotope = null;
    private Isotop matchedIsotope = null;

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

    public double getPeakCenter() {
        return peakCenter;
    }

    public String getEstimatedIsotope() {
        return estimatedIsotope;}

    public Isotop getMatchedIsotope(){
        return matchedIsotope;
    }

    //Setter
    public void setStartEnergy(double startEnergy) {
        this.startEnergy = startEnergy;
    }
    public void setEndEnergy(double endEnergy) {
        this.endEnergy = endEnergy;
    }

    public void setAreaOverBackground() {
        this.areaOverBackground = AreaCalculator.calculateAreaOverBackground(this.spectrum, this.startEnergy, this.endEnergy);
    }

    public void setEstimatedIsotope(Isotop isotope) {
        if(isotope == null) {
            this.estimatedIsotope="unk"; 
        }else{
            this.estimatedIsotope = isotope.symbol;
            this.matchedIsotope = isotope;
        }
    }

    //Constructor for a Region of Interest (ROI) in a Spectrum
    public ROI(Spectrum spec, double peakCenter) {
        this.spectrum = spec;
        this.peakCenter = peakCenter;
        PeakDetection.detectAndSetPeakSize(this, 3);
        //PeakDetection.detectAndSetPeakSizeUsingFWHM(this, 3);
    }
    //Overload Constructor for full control
    public ROI(Spectrum spec, double startEnergy, double endEnergy, double peakCenter) {
        this.spectrum = spec;
        this.startEnergy = startEnergy;
        this.endEnergy = endEnergy;
        this.peakCenter = peakCenter;
    }

}
