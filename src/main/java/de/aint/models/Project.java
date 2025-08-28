package de.aint.models;

import java.util.ArrayList;
import java.util.List;

public class Project {
    
    private String name;
    private List<Spectrum> spectres;

//===============================
    public Project(String name) {
        this.name = name;
        this.spectres = new ArrayList<>();
    }
//====================================
    public String getName() {
        return name;
    }

    public List<Spectrum> getSpectres() {
        return spectres;
    }

    //=========================

    public void addSpectres(List<Spectrum> spectres) {
        this.spectres.addAll(spectres);
    }
    

}
