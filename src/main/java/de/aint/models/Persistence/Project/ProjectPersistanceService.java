package de.aint.models.Persistence.Project;

import java.util.List;

import org.springframework.stereotype.Service;

import de.aint.models.Project;
import de.aint.models.Spectrum;
import de.aint.models.Persistence.Spec.SpectrumEntity;
import de.aint.models.Persistence.Spec.SpectrumMapper;
import de.aint.models.Persistence.Spec.SpectrumRepository;
import jakarta.transaction.Transactional;

@Service
public class ProjectPersistanceService {
    private final ProjectRepository projectRepository;
    private final SpectrumRepository spectrumRepository;
    private final ProjectMapper projectMapper;
    private final SpectrumMapper spectrumMapper;

    public ProjectPersistanceService(ProjectRepository projectRepository, SpectrumRepository spectrumRepository, SpectrumMapper spectrumMapper, ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.spectrumRepository = spectrumRepository;
        this.spectrumMapper = spectrumMapper;
        this.projectMapper = projectMapper;
    }

    @Transactional
    public Project load(String name) {
        ProjectEntity entity = projectRepository.findByName(name);
        if (entity == null) {
            throw new IllegalArgumentException("Project not found");
        }
        long size = entity.getSpecEnts().size(); //Force loading of lazy spectra
        return projectMapper.toDomain(entity);
    }

    @Transactional
    public ProjectEntity save(Project pr){
        ProjectEntity entity = projectMapper.toEntity(pr);
        projectRepository.save(entity);
        return entity;
    }

    @Transactional
    public void addSpectrum(Long projectID, Long spectrumID){
        ProjectEntity pEntity = projectRepository.findById(projectID).orElseThrow();
        SpectrumEntity sEntity = spectrumRepository.findById(spectrumID).orElseThrow();
        sEntity.setProject(pEntity);
        pEntity.getSpecEnts().add(sEntity);
    }

    @Transactional
    public void delete(String name) {
        ProjectEntity entity = projectRepository.findByName(name);
        if (entity == null) {
            throw new IllegalArgumentException("Project not found");
        }
        for(var specEnt : entity.getSpecEnts()) {
            specEnt.setProject(null);;
        }
        projectRepository.delete(entity);
    }

    @Transactional
    public Long getIDFromName(String name){
        ProjectEntity entity = projectRepository.findByName(name);
        return entity != null ? entity.getId() : null;
    }

    @Transactional
    public List<Project> listProjects() {
        return projectRepository.findAll().stream()
                .map(projectMapper::toDomain)
                .toList();
    }

    @Transactional
    public List<Spectrum> getSpectraForProject(Long projectID) {
        ProjectEntity entity = projectRepository.findById(projectID).orElseThrow();
        return entity.getSpecEnts().stream()
                .map(spectrumMapper::toDomain)
                .toList();
    }
}
