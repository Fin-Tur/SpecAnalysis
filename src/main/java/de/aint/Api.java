package de.aint;
import de.aint.models.Spectrum;
import de.aint.operations.OvulationOperator;
import de.aint.readers.Reader;
import io.javalin.Javalin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Api {
    public static void main(String[] args) {
        Spectrum spec = Reader.readFile(
                "C:/Users/f.willems/IdeaProjects/SpecAnalysis/src/main/resources/Leere_Kammer_85_40_50_1000_930_p_8k.Spe");

        // Caches für verschiedene Iterationszahlen
        Map<Integer, Spectrum> smoothedCache = new ConcurrentHashMap<>();
        Map<Integer, Spectrum> backgroundCache = new ConcurrentHashMap<>();

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
                it -> OvulationOperator.smoothSpectrum(spec, windowSize, 3, true, iterations)
            );
            ctx.json(smoothed);
        });

        app.get("/background", ctx -> {
            int iterations = ctx.queryParamAsClass("iterations", Integer.class).getOrDefault(1);
            Spectrum backgroundSpectrum = backgroundCache.computeIfAbsent(iterations, it -> {
                double[] estimatedBackground = OvulationOperator.estimateBackgroundUsingARPLS(spec, 2e4, 8e-4, it);
                return new Spectrum(spec.getEnergy_per_channel(), estimatedBackground);
            });
            ctx.json(backgroundSpectrum);
        });
    }
}
