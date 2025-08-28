package de.aint.models;

public class Project {
    
    private String name;
    private Spectrum[] spectres;

//===============================
    public Project(String name) {
        this.name = name;
    }
//====================================
    public String getName() {
        return name;
    }

    public Spectrum[] getSpectres() {
        return spectres;
    }

    //=========================

    

}
