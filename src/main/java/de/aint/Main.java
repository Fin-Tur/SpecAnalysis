package de.aint;


import de.aint.models.Spectrum;
import de.aint.readers.Reader;
import de.aint.readers.SpeReader;

import java.io.IOException;

public class Main {
    public static void main(String[] args){

        Spectrum spec = Reader.readFile("C:/Users/f.willems/IdeaProjects/SpecAnalysis/src/main/resources/Leere_Kammer_85_40_50_1000_930_p_8k.Spe");

        return;

    }
}