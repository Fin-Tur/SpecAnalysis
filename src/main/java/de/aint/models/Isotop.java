package de.aint.models;

public class Isotop {
    //IDentifier
    public final String id;
    //Symbol
    public final String symbol;
    //energy (keV) of peak beginning
    public final double energy;
    //Intensity of peak keV each spaltung
    public final double intensity;
    //Relative commomness
    public final double isotope_abundance;

    //Construcotr for Isotope
    public Isotop(String id, String symbol, double energy, double intensity, double isotope_abundance) {
        this.id = id;
        this.symbol = symbol;
        this.energy = energy;
        this.intensity = intensity;
        this.isotope_abundance = isotope_abundance;
    }
}
