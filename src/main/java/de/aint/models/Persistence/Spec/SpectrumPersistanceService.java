package de.aint.models.Persistence.Spec;

import org.springframework.stereotype.Service;

import de.aint.models.Spectrum;
import jakarta.transaction.Transactional;

@Service
public class SpectrumPersistanceService {
    private final SpectrumRepository spectrumRepository;
    private final SpectrumMapper spectrumMapper;

    public SpectrumPersistanceService(SpectrumRepository spectrumRepository, SpectrumMapper spectrumMapper) {
        this.spectrumRepository = spectrumRepository;
        this.spectrumMapper = spectrumMapper;
    }

    @Transactional
    public Long save(String name, Spectrum spec){
        SpectrumEntity entity = spectrumMapper.toEntity(name, spec);
        spectrumRepository.save(entity);
        spec.setId(entity.getId());
        return entity.getId();
    }

    @Transactional
    public Spectrum load(Long id) {
        SpectrumEntity entity = spectrumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid spectrum ID"));
        return spectrumMapper.toDomain(entity);
    }

    @Transactional
    public Spectrum getByID(Long id) {
        SpectrumEntity entity = spectrumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid spectrum ID"));
        return spectrumMapper.toDomain(entity);
    }

    @Transactional
    public void delete(Long id) {
        spectrumRepository.deleteById(id);
    }

    @Transactional
    public void update(Spectrum spectrum) {
        SpectrumEntity entity = spectrumMapper.toEntity(spectrum.getName(), spectrum);
        spectrumRepository.save(entity);
    }
}
