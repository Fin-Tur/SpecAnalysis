package de.aint;
import de.aint.models.Spectrum;
import de.aint.operations.OvulationOperator;
import de.aint.readers.Reader;
import io.javalin.Javalin;

public class Api {
    public static void main(String[] args) {
        Spectrum spec = Reader.readFile(
                "C:/Users/f.willems/IdeaProjects/SpecAnalysis/src/main/resources/Leere_Kammer_85_40_50_1000_930_p_8k.Spe");
    
        Spectrum smoothedSpec = OvulationOperator.smoothSpectrum(spec, 17, 2, true);
        double[] estimatedBackground = OvulationOperator.estimateBackgroundUsingALS(spec, 2e4, 8e-4, 15);
        Spectrum backgroundSpectrum = new Spectrum(spec.getEnergy_per_channel(), estimatedBackground); 

        System.out.println("Started Javalin server on port 7000");

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
        }).start(7000);

        // CORS Preflight-Handler für alle Routen

        app.get("/", ctx -> ctx.json(spec));
        app.get("/smoothed", ctx -> ctx.json(smoothedSpec));
        app.get("/background", ctx -> ctx.json(backgroundSpectrum));

    }
}
