package de.aint.models.Persistence.Project;

import java.util.List;

import de.aint.models.Persistence.Spec.SpectrumEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class ProjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique=true)
    private String name;

    @OneToMany(mappedBy="project", fetch=FetchType.LAZY)
    private List<SpectrumEntity> specEnts;

    protected ProjectEntity(){} //JPA requires a default constructor

    public ProjectEntity(String name) {
        this.name = name;
    }

    //Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<SpectrumEntity> getSpecEnts() {
        return specEnts;
    }

    //setters
    public void setSpectres(List<SpectrumEntity> specEnts) {
        this.specEnts = specEnts;
    }
}
