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
        return entity.getId();
    }

    @Transactional
    public Spectrum load(Long id) {
        SpectrumEntity entity = spectrumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid spectrum ID"));
        return spectrumMapper.toDomain(entity);
    }
}
