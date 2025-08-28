package de.aint.models.Persistence.Project;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import de.aint.models.*;
import de.aint.models.Persistence.Spec.*;

@Component
public class ProjectMapper {
    private final SpectrumMapper spectrumMapper;

    public ProjectMapper(SpectrumMapper spectrumMapper) {
        this.spectrumMapper = spectrumMapper;
    }

    public Project toDomain(ProjectEntity entity){
        List<Spectrum> specs = new ArrayList<>();
        for(SpectrumEntity specEnt : entity.getSpecEnts()){
            specs.add(spectrumMapper.toDomain(specEnt));
        }
        Project pr = new Project(entity.getName());
        pr.addSpectres(specs);
        return pr;
    }

    public ProjectEntity toEntity(Project pr){
        ProjectEntity entity = new ProjectEntity(pr.getName());
        return entity;
    }
}