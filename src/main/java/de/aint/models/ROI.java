package de.aint.models;

import de.aint.builders.SpectrumBuilder;
import de.aint.operations.calculators.Calculator;
import de.aint.operations.calculators.Calculator.CalculatingAlgos;
import de.aint.operations.fitters.Fitter;

public class ROI {

    private final Peak[] peaks;
    private double[] fitParams;
    private double areaOverBackground;

    private final Spectrum spectrum;
    private double startEnergy;
    private double endEnergy;

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
    public double[] getFitParams() {
        return fitParams;
    }
    public Peak[] getPeaks() {
        return peaks;
    }
    public double getAreaOverBackground() {
        return areaOverBackground;
    }

    //Setter
    public void fitGaussCurve() {
        this.fitParams = Fitter.PeakFitAlgos.GAUSSLM.fit(this);//Fit the peaks in the ROI using the GAUSS-LM algorithm
    }
    public void setAreaOverBackground() {
        this.areaOverBackground = Calculator.AreaAlgos.GAUSS.calculateArea(this); //Calculate the area over background using the GAUSS params
    }

    //Constructor for a Region of Interest (ROI) in a Spectrum
    public ROI(Spectrum spec, Peak[] peaks, double startEnergy, double endEnergy) {
        this.spectrum = spec;
        this.peaks = peaks;
        this.startEnergy = startEnergy;
        this.endEnergy = endEnergy;

    }




}
