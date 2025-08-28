package de.aint.models.Persistence.Spec;

import jakarta.persistence.*;

@Entity
@Table(name = "spectra")
public class SpectrumEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    //Properties
    private double ec_offset;
    private double ec_slope;
    private double ec_quad;
    private double srcForce;
    

    @Lob
    @Column(columnDefinition = "TEXT")
    private String countsJson;

    protected SpectrumEntity(){} //JPA requires a default constructor

    public SpectrumEntity(String name, double ec_offset, double ec_slope, double ec_quad, double srcForce, String countsJson) {
        this.name = name;
        this.ec_offset = ec_offset;
        this.ec_slope = ec_slope;
        this.ec_quad = ec_quad;
        this.srcForce = srcForce;
        this.countsJson = countsJson;
    }

    //Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
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

    public double getSrcForce() {
        return srcForce;
    }

    public String getCountsJson() {
        return countsJson;
    }
}