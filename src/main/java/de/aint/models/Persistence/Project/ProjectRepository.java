package de.aint.models.Persistence.Project;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long>{
    public ProjectEntity findByName(String name);
}
