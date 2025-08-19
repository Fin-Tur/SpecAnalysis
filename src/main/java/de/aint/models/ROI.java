package de.aint.models;

import de.aint.builders.SpectrumBuilder;

public class ROI {

    public Peak[] peaks;
    public double[] fitParams;

    private final Spectrum spectrum;
    private double startEnergy;
    private double endEnergy;
    private Spectrum backgroundSpectrum;

    // Getters
    public Spectrum getSpectrum() {
        return spectrum;
    }

    public Spectrum getBackgroundSpectrum() {
        return backgroundSpectrum;
    }
    public double getStartEnergy() {
        return startEnergy;
    }   
    public double getEndEnergy() {
        return endEnergy;
    }



    //Setter
    public void setStartEnergy(double startEnergy) {
        this.startEnergy = startEnergy;
    }
    public void setEndEnergy(double endEnergy) {
        this.endEnergy = endEnergy;
    }
    public void setFitParams(double[] fitParams) {
            this.fitParams = fitParams;
    }


    //Constructor for a Region of Interest (ROI) in a Spectrum
    public ROI(Spectrum spec, Peak[] peaks, double startEnergy, double endEnergy) {
        this.spectrum = spec;
        this.backgroundSpectrum = SpectrumBuilder.createBackgroundSpectrum(SpectrumBuilder.createSmoothedSpectrumUsingGauss(spec, 3.0));
        this.peaks = peaks;
        this.startEnergy = startEnergy;
        this.endEnergy = endEnergy;

        //PeakDetection.detectAndSetPeakSizeUsingGradient(this, 3);
        //PeakDetection.detectAndSetPeakSizeUsingFWHM(this, 1);
    }


}
