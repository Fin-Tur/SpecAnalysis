package de.aint;

import de.aint.readers.IsotopeReader;
import de.aint.readers.Reader;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;

import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import de.aint.models.*;
import de.aint.builders.SpectrumBuilder;
import de.aint.detectors.PeakDetection;

public class Api {

    static Spectrum spec = null;
    //Get Spectra variants
    static Spectrum[] variants = {null, null, null, null};

    public static void main(String[] args) {

        
        
        //Calibrate using Annihilation and H_1 peak
        //spec.changeEnergyCal(1677, 2223.248, 391, 511);
        int[] channels = {1677, 391, 3722, 5740};
        double[] energies = {2223.248, 511, 4945.301, 7631.136};
        

        //get Isotopes
        IsotopeReader isotopeReader = new IsotopeReader();
        ArrayList<Isotop> isotopes = isotopeReader.isotopes;

        
        //Start Javalin Server
        System.out.println("Started Javalin server on port 7000");

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
        }).start(7000);

        app.get("/", ctx -> ctx.json(spec));

        app.get("/smoothed", ctx -> {
            int iterations = ctx.queryParamAsClass("iterations", Integer.class).getOrDefault(1);
            int windowSize = ctx.queryParamAsClass("window_size", Integer.class).getOrDefault(11);
            String algorithm = ctx.queryParamAsClass("algorithm", String.class).getOrDefault("SG");
            Spectrum smoothed;
            if ("SG".equalsIgnoreCase(algorithm)) {
                smoothed = SpectrumBuilder.createSmoothedSpectrumUsingSG(spec, windowSize, 2, true, iterations);
            } else {
                smoothed = SpectrumBuilder.createSmoothedSpectrumUsingGauss(spec, 4);
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
            System.out.println(isotopes.size() + " isotopes loaded!");
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
                ROI[] peaks = PeakDetection.detectPeaks(variants[0], variants[3]);
                customSpectrum = SpectrumBuilder.createPeakFitSpectrum(variants[3], peaks);
            }
           
            ctx.json(customSpectrum);
        });

        app.get("/peaks", ctx -> {
            ROI[] peaks = PeakDetection.detectPeaks(variants[0], variants[3]);
            ctx.json(peaks);
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
                if(!tempFile.delete()) System.out.println("Could not delete temporary file: " + tempFile.getAbsolutePath());
                ctx.json(spec);
            } else {
                ctx.status(400).result("No file uploaded");
            }
        });

    }
}
