package de.aint.models.Persistence.Spec;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.aint.models.Spectrum;

@Component
public class SpectrumMapper {
    private final ObjectMapper mapper = new ObjectMapper();

    public SpectrumEntity toEntity(String name, Spectrum spec){
        try{
            String countsJson = mapper.writeValueAsString(spec.getCounts());
            SpectrumEntity specEntity = new SpectrumEntity(name, spec.getEc_offset(), spec.getEc_slope(), spec.getEc_quad(), spec.getSrcForce(), countsJson);
            return specEntity;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping Spectrum to SpectrumEntity", e);
        }
    }

    public Spectrum toDomain(SpectrumEntity entity){
        try{
            double[] counts = mapper.readValue(entity.getCountsJson(), double[].class);
            Spectrum spec = new Spectrum(entity.getName(), counts, entity.getEc_offset(), entity.getEc_slope(), entity.getEc_quad(), entity.getSrcForce());
            return spec;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping SpectrumEntity to Spectrum", e);
        }
    }
}
