
package de.aint.services;

import org.springframework.stereotype.Service;

import de.aint.models.Project;
import de.aint.models.Persistence.Project.ProjectEntity;
import de.aint.models.Persistence.Project.ProjectPersistanceService;
import jakarta.transaction.Transactional;

@Service
public class ProjectService {

    ProjectPersistanceService projectPersistanceService;

    public ProjectService(ProjectPersistanceService projectPersistanceService) {
        this.projectPersistanceService = projectPersistanceService;
    }

    @Transactional
    public void createProject(String name) {
        if (projectPersistanceService.getIDFromName(name) != null) {
            throw new IllegalArgumentException("Project already exists");
        }
        Project p = new Project(name);
        projectPersistanceService.save(p);
    }

    @Transactional
    public Project loadProject(String projectName){
        return projectPersistanceService.load(projectName);
    }

    @Transactional
    public void saveProject(Project project) {
        projectPersistanceService.save(project);
    }

    @Transactional
    public void addSpectrumToProject(Long projectID, Long spectrumID) {
        projectPersistanceService.addSpectrum(projectID, spectrumID);
    }
    @Transactional
    public Long getIDFromName(String name){
        return projectPersistanceService.getIDFromName(name);
    }
}
