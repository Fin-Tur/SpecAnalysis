package de.aint;

import de.aint.readers.IsotopeReader;
import de.aint.readers.Reader;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.aint.models.*;
import de.aint.operations.fitters.FittingData;
import de.aint.builders.SpectrumBuilder;
import de.aint.detectors.PeakDetection;

public class Api {

    private static final Logger logger = LoggerFactory.getLogger(Api.class);

    static Spectrum spec = null;
    //Get Spectra variants
    static Spectrum[] variants = {null, null, null, null};

    public static void main(String[] args) {

        
        
        //Calibrate using Annihilation and H_1 peak
        //spec.changeEnergyCal(1677, 2223.248, 391, 511);
        int[] channels = {1677, 391, 3722, 5740};
        double[] energies = {2223.248, 511, 4945.301, 7631.136};
        
        //Caching
        HashMap<String, Spectrum> spectrumCache = new HashMap<>();

        //get Isotopes
        Path isoPath = Paths.get("src/main/resources/isotop_details.txt");
        IsotopeReader isotopeReader = new IsotopeReader(isoPath.toString());
        isotopeReader.readIsotopes();
        ArrayList<Isotop> isotopes = isotopeReader.isotopes;

        
        //Start Javalin Server
        logger.info("Started Javalin Server on localhost:7000");

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
        }).start(7000);

        app.get("/", ctx -> ctx.json(spec));

        app.get("/smoothed", ctx -> {
            int iterations = ctx.queryParamAsClass("iterations", Integer.class).getOrDefault(0);
            int windowSize = ctx.queryParamAsClass("window_size", Integer.class).getOrDefault(0);
            int sigma = ctx.queryParamAsClass("sigma", Integer.class).getOrDefault(0);
            String algorithm = ctx.queryParamAsClass("algorithm", String.class).getOrDefault("SG");
            Spectrum smoothed;
            if ("SG".equalsIgnoreCase(algorithm)) {
                String specKey = "smoothed_window"+windowSize+"_poly2_outlierstrue_iters"+iterations;
                if(spectrumCache.containsKey(specKey)){
                    smoothed = spectrumCache.get(specKey);
                }else{
                    smoothed = SpectrumBuilder.createSmoothedSpectrumUsingSG(spec, windowSize, 2, true, iterations);
                    spectrumCache.put(specKey, smoothed);
                }
            } else {
                String specKey = "Gauss_sigma" + sigma;
                if(spectrumCache.containsKey(specKey)){
                    smoothed = spectrumCache.get(specKey);
                }else{
                    smoothed = SpectrumBuilder.createSmoothedSpectrumUsingGauss(spec, sigma);
                    spectrumCache.put(specKey, smoothed);
                }
            }
            
            ctx.json(smoothed);
        });

        app.get("/background", ctx -> {
            String source = ctx.queryParamAsClass("source", String.class).getOrDefault("original");
            Spectrum backgroundSpectrum;
            if ("smoothed".equalsIgnoreCase(source)) {
                backgroundSpectrum = variants[3];
            } else {
                backgroundSpectrum = variants[2];
            }
            ctx.json(backgroundSpectrum);
        });

        app.get("/isotopes", ctx -> {
            ctx.json(isotopes);
        });

        app.get("/custom", ctx -> {
            String isotopesParam = ctx.queryParam("isotopes");
            ArrayList<String> selectedIsotopes = new ArrayList<>();
            if (isotopesParam != null && !isotopesParam.isEmpty()) {
                for (String id : isotopesParam.split(",")) {
                    selectedIsotopes.add(id.trim());
                }
            }
            String source = ctx.queryParamAsClass("source", String.class).getOrDefault("custom");
            Spectrum customSpectrum = null;

            if(source.equals("isotopes")){
                customSpectrum = SpectrumBuilder.createCustomSpectrum(variants[3], selectedIsotopes, isotopeReader);
            }else if(source.equals("peaks")){
                ROI[] rois = PeakDetection.splitSpectrumIntoRois(variants[0]);
                customSpectrum = SpectrumBuilder.createPeakFitSpectrum(variants[3], rois);
            }
           
            ctx.json(customSpectrum);
        });

        app.get("/peaks", ctx -> {
            ROI[] rois = PeakDetection.splitSpectrumIntoRois(variants[0]);
            for(var roi : rois) roi.setAreaOverBackground();
            RoiDTO[] rdtos = Arrays.stream(rois)
                    .map(RoiDTO::new)
                    .toArray(RoiDTO[]::new);
            ctx.json(rdtos);
        });

        app.post("/", ctx -> {
            UploadedFile uploadedFile = ctx.uploadedFile("file");
            if (uploadedFile != null) {
                // Schreibe die Datei temporär auf die Platte
                File tempFile = File.createTempFile("upload_", "_" + uploadedFile.filename());
                try (InputStream in = uploadedFile.content();
                     OutputStream out = new FileOutputStream(tempFile)) {
                    in.transferTo(out);
                }
                spec = Reader.readFile(tempFile.getAbsolutePath());
                spec.changeEnergyCal(channels, energies);
                variants = SpectrumBuilder.createSpectrumVariants(spec);
                spectrumCache.put("original", spec);
                String smoothedStandartKey = "smoothed_window"+FittingData.GenericOpts.sgWindowSize+"_poly2_outliers"+FittingData.GenericOpts.sgEraseOutliers+"_iters"+FittingData.GenericOpts.sgIters;
                spectrumCache.put(smoothedStandartKey, variants[1]);
                String backgroundStandartKey = "background_lambda"+FittingData.GenericOpts.lambda+"_p"+FittingData.GenericOpts.p+"_maxIter"+FittingData.GenericOpts.maxIter;
                spectrumCache.put(backgroundStandartKey, variants[2]);
                String backgroundSmoothedKey = "backgroundSmoothed_lambda"+FittingData.GenericOpts.lambda+"_p"+FittingData.GenericOpts.p+"_maxIter"+FittingData.GenericOpts.maxIter;
                spectrumCache.put(backgroundSmoothedKey, variants[3]);
                try {
                    java.nio.file.Files.delete(tempFile.toPath());
                } catch (java.io.IOException e) {
                    logger.warn("Could not delete temporary file: {}", tempFile.getAbsolutePath(), e);
                }
                ctx.json(variants[0]);
            } else {
                ctx.status(400).result("No file uploaded");
            }
        });

    }
}
