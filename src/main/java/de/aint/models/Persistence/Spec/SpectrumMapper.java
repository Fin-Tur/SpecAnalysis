package de.aint.models.Persistence.Spec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.aint.models.Spectrum;
import de.aint.models.Persistence.Project.ProjectPersistanceService;

@Component
public class SpectrumMapper {
    private final ObjectMapper mapper = new ObjectMapper();
    ProjectPersistanceService projectPersistanceService;

    public SpectrumMapper(@Lazy ProjectPersistanceService projectPersistanceService) {
        this.projectPersistanceService = projectPersistanceService;
    }

    public SpectrumEntity toEntity(String name, Spectrum spec){
        try{
            String countsJson = mapper.writeValueAsString(spec.getCounts());

            SpectrumEntity entity = new SpectrumEntity(name, spec.getEc_offset(), spec.getEc_slope(), spec.getEc_quad(), spec.getSrcForce(), countsJson);
            if (spec.getId() != null) {
                entity.setId(spec.getId());
            }
            if(spec.getProjectId() != null){
                entity.setProject(projectPersistanceService.findById(spec.getProjectId()));
            }
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping Spectrum to SpectrumEntity", e);
        }
    }

    public Spectrum toDomain(SpectrumEntity entity){
        try{
            double[] counts = mapper.readValue(entity.getCountsJson(), double[].class);
            Spectrum spec = new Spectrum(entity.getId(), entity.getName(), counts, entity.getEc_offset(), entity.getEc_slope(), entity.getEc_quad(), entity.getSrcForce());
            if(entity.getProjectEntity() != null){
                spec.setProjectId(entity.getProjectEntity().getId());
            }
            return spec;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping SpectrumEntity to Spectrum", e);
        }
    }
}
