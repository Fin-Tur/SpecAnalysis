package de.aint.controller;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.aint.models.Project;
import de.aint.models.Spectrum;
import de.aint.models.Persistence.Project.ProjectEntity;
import de.aint.services.ProjectService;
import de.aint.services.SpectrumService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/projects")
@CrossOrigin("*")
public class ProjectController {

    private final Logger logger = LoggerFactory.getLogger(ProjectController.class);
    private final ProjectService projectService;
    private final SpectrumService spectrumService;

    public ProjectController(ProjectService projectService, SpectrumService spectrumService) {
        this.projectService = projectService;
        this.spectrumService = spectrumService;
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<ProjectEntity> createProject(@Valid @RequestBody CreateProjectRequest req) {
        ProjectEntity project = projectService.createProject(req.name());
        return ResponseEntity.created(URI.create("/projects/" + project.getName())).body(project);
    }

    @GetMapping
    public ResponseEntity<List<String>> listProjects() {
        List<String> projects = projectService.listProjects();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{name}/del")
    public ResponseEntity<Void> deleteProject(@PathVariable String name) {
        if (name == null) {
            return ResponseEntity.notFound().build();
        }
        Project pr = projectService.loadProject(name);
        projectService.deleteProject(pr);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{name}")
    public ResponseEntity<List<Spectrum>> getProject(@PathVariable String name) {
        Long projectId = projectService.getIDFromName(name);
        if (projectId == null) {
            return ResponseEntity.notFound().build();
        }
        List<Spectrum> spectra = projectService.getSpectraForProject(projectId);
        return ResponseEntity.ok(spectra);
    }

    @RequestMapping("addSpectrum")
    @PostMapping(consumes = "application/json")
    public ResponseEntity<String> addSpectrum(@RequestBody Spectrum spectrum, @RequestParam String projectName, @RequestParam String spectrumName) {
        System.out.println("Adding spectrum: " + spectrumName + " to project: " + projectName);
        if(spectrum == null || spectrumName == null || projectName == null) return ResponseEntity.badRequest().body("Invalid spectrum data.");
        Long specID = spectrumService.addSpectrum(spectrumName, spectrum);
        Long projID = projectService.getIDFromName(projectName);
        projectService.addSpectrumToProject(projID, specID);
        logger.info("Added spectrum: {} to project: {}", spectrumName, projectName);
        return ResponseEntity.status(HttpStatus.CREATED).body("Spectrum added to project.");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleException(IllegalStateException e) {
         return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    record CreateProjectRequest(@NotBlank String name) {}

}

