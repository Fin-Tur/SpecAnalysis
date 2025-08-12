package de.aint;
import de.aint.operations.OvulationOperator;
import de.aint.operations.SubstractOperator;
import de.aint.readers.IsotopeReader;
import de.aint.readers.Reader;
import io.javalin.Javalin;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.aint.models.*;
import de.aint.builders.SpectrumBuilder;
import de.aint.detectors.PeakDetection;
import de.aint.libraries.SmoothingLib;

public class Api {




    public static void main(String[] args) {

        //Read spec
        Spectrum spec = Reader.readFile("C:/Users/f.willems/IdeaProjects/SpecAnalysis/src/main/resources/Leere_Kammer_85_40_50_1000_930_p_8k.Spe");
        
        //Calibrate using Annihilation and H_1 peak
        spec.changeEnergyCal(1677, 2223.248, 391, 511);
       
        // Caches für verschiedene Iterationszahlen
        Map<Integer, Spectrum> smoothedCache = new ConcurrentHashMap<>();
        Map<Integer, Spectrum> backgroundCache = new ConcurrentHashMap<>();

        //get Isotopes
        IsotopeReader isotopeReader = new IsotopeReader();
        ArrayList<Isotop> isotopes = isotopeReader.isotopes;

        //Get Spectra variants
        Spectrum[] variants = SpectrumBuilder.createSpectrumVariants(spec);
        smoothedCache.put(2*1000+11, variants[1]);
        backgroundCache.put(2*1000+5, variants[2]);
        backgroundCache.put(1*1000+5, variants[3]);

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
            Spectrum smoothed = smoothedCache.computeIfAbsent(
                iterations * 1000 + windowSize, // Kombiniere für eindeutigen Key
                it -> SpectrumBuilder.createSmoothedSpectrum(spec, windowSize, 2, true, iterations)
            );
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
            Spectrum customSpectrum = SpectrumBuilder.createCustomSpectrum(spec, selectedIsotopes, isotopeReader);

            ctx.json(customSpectrum);
        });

        app.get("/peaks", ctx -> {
            ROI[] peaks = PeakDetection.detectPeaks(variants[0], variants[3]);
            System.out.println("Sendin "+ peaks.length +" peaks");
            ctx.json(peaks);
        });

    }
}
