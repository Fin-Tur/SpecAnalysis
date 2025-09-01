package de.aint.services;

import de.aint.builders.SpectrumBuilder;
import de.aint.detectors.PeakDetection;
import de.aint.models.Isotop;
import de.aint.models.ROI;
import de.aint.models.Spectrum;
import de.aint.models.Persistence.Roi.RoiDTO;
import de.aint.models.Persistence.Spec.SpectrumEntity;
import de.aint.models.Persistence.Spec.SpectrumPersistanceService;
import de.aint.operations.fitters.FittingData;
import de.aint.readers.IsotopeReader;
import de.aint.readers.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.TransactionScoped;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
public class SpectrumService {
    private static final Logger log = LoggerFactory.getLogger(SpectrumService.class);

    private final ResourceLoader resourceLoader;
    private final SpectrumPersistanceService spectrumPersistanceService;
    private final Map<String, Spectrum> spectrumCache = new ConcurrentHashMap<>();

    private final int[] channels = {1677, 391, 3722, 5740};
    private final double[] energies = {2223.248, 511, 4945.301, 7631.136};

    private IsotopeReader isotopeReader;
    private List<Isotop> isotopes = Collections.emptyList();

    public SpectrumService(ResourceLoader resourceLoader, SpectrumPersistanceService spectrumPersistanceService) {
        this.resourceLoader = resourceLoader;
        this.spectrumPersistanceService = spectrumPersistanceService;
    }

    @PostConstruct
    void initIsotopes() throws IOException {
        Resource res = resourceLoader.getResource("classpath:isotop_details.txt");
        File tmp = File.createTempFile("isotopes_", ".txt");
        try (var in = res.getInputStream()) {
            Files.copy(in, tmp.toPath(), REPLACE_EXISTING);
        } // TODO : Streams
        isotopeReader = new IsotopeReader(tmp.getAbsolutePath());
        isotopeReader.readIsotopes();
        this.isotopes = new ArrayList<>(isotopeReader.isotopes);
        log.info("Loaded {} isotopes", isotopes.size());
    }

    @Transaction
    public Long addSpectrum(String name, Spectrum spectrum){
        return spectrumPersistanceService.save(name, spectrum);
    }

    @Transaction 
    public void delSpectrum(Long id) {
        if(spectrumPersistanceService.getByID(id) != null) {
            spectrumPersistanceService.delete(id);
        }else{
            throw new IllegalArgumentException("Invalid spectrum ID");
        }
    }

    @Transaction
    public void renameSpectrum(Long id, String newName) {
        Spectrum spectrum = spectrumPersistanceService.getByID(id);
        if (spectrum == null) {
            throw new IllegalArgumentException("Invalid spectrum ID");
        }
        spectrum.setName(newName);
        spectrumPersistanceService.update(spectrum);

    }

    public Spectrum getSpectrumByID(Long id) {
        return spectrumPersistanceService.getByID(id);
    }

    public List<Isotop> getIsotopes() {
        return isotopes;
    }

    public Spectrum uploadAndParse(File file) throws IOException {
        Spectrum s = Reader.readFile(file.getAbsolutePath());
        s.setName(file.getName());
        s.changeEnergyCal(channels, energies);

        String name = s.getName();
        log.info("Uploaded and computed Spectrum: "+name);

        //Pre-Compute Basic Variants
        Spectrum[] v = SpectrumBuilder.createSpectrumVariants(s);

        spectrumCache.put(name, s);
        spectrumCache.put(name+"_smoothed_window" + FittingData.GenericOpts.sgWindowSize + "_poly2_outliersTrue_iters" + FittingData.GenericOpts.sgIters, v[1]);
        spectrumCache.put(name+"_als_default_lambda" + FittingData.GenericOpts.lambda + "_p" + FittingData.GenericOpts.p + "_maxIters" + FittingData.GenericOpts.maxIter, v[2]);
        spectrumCache.put(name+"_als_smoothed_lambda" + FittingData.GenericOpts.lambda + "_p" + FittingData.GenericOpts.p + "_maxIters" + FittingData.GenericOpts.maxIter, v[3]);

        return s;
    }

    private Spectrum ensureSpectrumLoaded(Long id) {
        Spectrum s = getSpectrumByID(id);
        if (s == null) {
            throw new IllegalStateException("No Spectrum is loaded. please select/upload Spectrum first.");
        }
        return s;
    }

    public Spectrum getSmoothedById(Long id, String algorithm, int windowSize, int iterations, int sigma) {
        Spectrum s = ensureSpectrumLoaded(id);
        String name = s.getName();
        if ("SG".equalsIgnoreCase(algorithm)) {
            String key = name+"_smoothed_window" + windowSize + "_poly2_outliersTrue_iters" + iterations;
            return spectrumCache.computeIfAbsent(key,
                    k -> SpectrumBuilder.createSmoothedSpectrumUsingSG(s, windowSize, 2, true, iterations));
        } else {
            String key = name+"_Gauss_sigma" + sigma;
            return spectrumCache.computeIfAbsent(key,
                    k -> SpectrumBuilder.createSmoothedSpectrumUsingGauss(s, sigma));
        }
    }

    public Spectrum getBackgroundById(Long id, String source) {
        Spectrum s = ensureSpectrumLoaded(id);
        String name = s.getName();
        if(source.equalsIgnoreCase("smoothed")){
            String key = name+"_als_smoothed_lambda" + FittingData.GenericOpts.lambda + "_p" + FittingData.GenericOpts.p + "_maxIters" + FittingData.GenericOpts.maxIter;
            return spectrumCache.computeIfAbsent(key,
                    k -> SpectrumBuilder.createBackgroundSpectrum(SpectrumBuilder.createSmoothedSpectrumUsingGauss(s, 0)));
        }else{
            String key = name+"_als_default_lambda" + FittingData.GenericOpts.lambda + "_p" + FittingData.GenericOpts.p + "_maxIters" + FittingData.GenericOpts.maxIter;
            return spectrumCache.computeIfAbsent(key,
                    k -> SpectrumBuilder.createBackgroundSpectrum(s));
        }

        
    }

    public Spectrum getCustomById(Long id, String source, List<String> selectedIsotopes) {
        Spectrum s = ensureSpectrumLoaded(id);
        Spectrum[] variants = SpectrumBuilder.createSpectrumVariants(s);
        if ("isotopes".equalsIgnoreCase(source)) {
            return SpectrumBuilder.createCustomSpectrum(variants[3], new ArrayList<>(selectedIsotopes), isotopeReader);
        } else if ("peaks".equalsIgnoreCase(source)) {
            ROI[] rois = PeakDetection.splitSpectrumIntoRois(s);
            return SpectrumBuilder.createPeakFitSpectrum(variants[3], rois);
        }
        return null;
    }

    public RoiDTO[] getPeaksById(Long id) {
        Spectrum s = ensureSpectrumLoaded(id);
        ROI[] rois = PeakDetection.splitSpectrumIntoRois(s);
        for (var roi : rois) roi.setAreaOverBackground();
        return Arrays.stream(rois).map(RoiDTO::new).toArray(RoiDTO[]::new);
    }
}
