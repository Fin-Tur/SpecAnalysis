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
    private final java.util.concurrent.atomic.AtomicReference<Spectrum> spec = new java.util.concurrent.atomic.AtomicReference<>(null);
    private final Spectrum[] variants = new Spectrum[]{null, null, null, null};
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

    public Spectrum getCurrentSpectrum() {
        return spec.get();
    }

    public List<Isotop> getIsotopes() {
        return isotopes;
    }

    public Spectrum uploadAndParse(File file) throws IOException {
        Spectrum s = Reader.readFile(file.getAbsolutePath());
        s.changeEnergyCal(channels, energies);
        this.spec.set(s);

        Spectrum[] v = SpectrumBuilder.createSpectrumVariants(s);
        System.arraycopy(v, 0, variants, 0, variants.length);

        spectrumCache.put("original", s);
        spectrumCache.put("smoothed_window" + FittingData.GenericOpts.sgWindowSize + "_poly2_outliersTrue_iters" + FittingData.GenericOpts.sgIters, v[1]);
        spectrumCache.put("als_default_lambda" + FittingData.GenericOpts.lambda + "_p" + FittingData.GenericOpts.p + "_maxIters" + FittingData.GenericOpts.maxIter, v[2]);
        spectrumCache.put("als_smoothed_lambda" + FittingData.GenericOpts.lambda + "_p" + FittingData.GenericOpts.p + "_maxIters" + FittingData.GenericOpts.maxIter, v[3]);

        return v[0];
    }

    private void ensureSpectrumLoaded() {
        if (spec.get() == null) {
            throw new IllegalStateException("Kein Spektrum geladen. Bitte zuerst per POST / uploaden.");
        }
    }

    public Spectrum getSmoothed(String algorithm, int windowSize, int iterations, int sigma) {
        ensureSpectrumLoaded();
        if ("SG".equalsIgnoreCase(algorithm)) {
            String key = "smoothed_window" + windowSize + "_poly2_outliersTrue_iters" + iterations;
            return spectrumCache.computeIfAbsent(key,
                    k -> SpectrumBuilder.createSmoothedSpectrumUsingSG(spec.get(), windowSize, 2, true, iterations));
        } else {
            String key = "Gauss_sigma" + sigma;
            return spectrumCache.computeIfAbsent(key,
                    k -> SpectrumBuilder.createSmoothedSpectrumUsingGauss(spec.get(), sigma));
        }
    }

    public Spectrum getBackground(String source) {
        ensureSpectrumLoaded();
        return "smoothed".equalsIgnoreCase(source) ? variants[3] : variants[2];
    }

    public Spectrum getCustom(String source, List<String> selectedIsotopes) {
        ensureSpectrumLoaded();
        if ("isotopes".equalsIgnoreCase(source)) {
            return SpectrumBuilder.createCustomSpectrum(variants[3], new ArrayList<>(selectedIsotopes), isotopeReader);
        } else if ("peaks".equalsIgnoreCase(source)) {
            ROI[] rois = PeakDetection.splitSpectrumIntoRois(variants[0]);
            return SpectrumBuilder.createPeakFitSpectrum(variants[3], rois);
        }
        return null;
    }

    public RoiDTO[] getPeaks() {
        ensureSpectrumLoaded();
        ROI[] rois = PeakDetection.splitSpectrumIntoRois(variants[0]);
        for (var roi : rois) roi.setAreaOverBackground();
        return Arrays.stream(rois).map(RoiDTO::new).toArray(RoiDTO[]::new);
    }
}
