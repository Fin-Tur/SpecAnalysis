package de.aint.models.Persistence.Spec;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpectrumRepository extends JpaRepository<SpectrumEntity, Long> {
    List<SpectrumEntity> findByName(String name);
    Optional<SpectrumEntity> findById(Long id);
}
