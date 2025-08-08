package de.aint;


import de.aint.models.Spectrum;
import de.aint.operations.AddOperator;
import de.aint.operations.OvulationOperator;
import de.aint.readers.Reader;

public class Main {
    public static void main(String[] args){

        Spectrum spec1 = Reader.readFile("C:/Users/f.willems/IdeaProjects/SpecAnalysis/src/main/resources/Leere_Kammer_85_40_50_1000_930_p_8k.Spe");
        Spectrum smooth = OvulationOperator.smoothSpectrum(spec1, 17, 2, true, 2);
        double[] counts1 = smooth.getCounts();

        for(var count : counts1){
            System.out.print(count + ",");
        }

        Spectrum spec2 = Reader.readFile("C:/Users/f.willems/IdeaProjects/SpecAnalysis/src/main/resources/Leere_Kammer_85_40_50_1000_930_p_8k.Spe");


        Spectrum specRes = AddOperator.add(spec1, spec2, 8000);


        return;

    }
}