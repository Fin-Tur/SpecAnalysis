package de.aint;

import org.apache.commons.math3.analysis.function.Add;

import de.aint.models.Spectrum;
import de.aint.operations.AddOperator;
import de.aint.operations.OvulationOperator;
import de.aint.readers.Reader;
import io.javalin.Javalin;

public class Api {
    public static void main(String[] args) {

        Spectrum spec = Reader.readFile(
                "C:/Users/f.willems/IdeaProjects/SpecAnalysis/src/main/resources/Leere_Kammer_85_40_50_1000_930_p_8k.Spe");
    
        Spectrum smoothedSpec = OvulationOperator.smoothSpectrum(spec, 2001, 2, false);

        Spectrum spec2 = Reader.readFile(
                "C:/Users/f.willems/IdeaProjects/SpecAnalysis/src/main/resources/Leeres_Fass_3600s.Spe");

        Spectrum addition = AddOperator.add(spec, spec2, spec.getChannel_count());
        Spectrum additionSm = OvulationOperator.smoothSpectrum(addition, 11, 2, true);

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
        }).start(7000);

        app.get("/", ctx -> ctx.json(spec));
        app.get("/smoothed", ctx -> ctx.json(smoothedSpec));




        app.get("/addition", ctx -> ctx.json(additionSm));

    }
}
