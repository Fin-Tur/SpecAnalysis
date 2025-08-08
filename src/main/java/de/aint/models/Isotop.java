package de.aint.models;

public class Isotop {
    //IDentifier
    public long id;
    //Symbol
    public String symbol;
    //energy (keV) of peak
    public double energy;
    //Intensity of peak (counts)
    public double intensity;
    //Relative commomness
    public double isotope_abundance;

    //Construcotr for Isotope
    public Isotop(long id, String symbol, double energy, double intensity, double isotope_abundance) {
        this.id = id;
        this.symbol = symbol;
        this.energy = energy;
        this.intensity = intensity;
        this.isotope_abundance = isotope_abundance;
    }
}
