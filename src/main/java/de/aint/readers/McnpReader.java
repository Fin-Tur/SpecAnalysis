package de.aint.readers;

import de.aint.models.Spectrum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McnpReader extends Reader{


    @Override
    public Spectrum readSpectrum(String src) throws IOException {
        //Open file
        File file = new File(src);
        //read..
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            ArrayList<Double> energys = new ArrayList<>();
            ArrayList<Double> counts = new ArrayList<>();
            while((line = br.readLine()) != null){
                String[] args = line.split(" {3}");
                double energy = Double.parseDouble(args[0]);
                double count = Double.parseDouble(args[1]);
                energys.add(energy);
                counts.add(count);
            }

            assert !energys.isEmpty();
            assert !counts.isEmpty();

            return new Spectrum(energys.stream().mapToDouble(Double::doubleValue).toArray(), counts.stream().mapToDouble(Double::doubleValue).toArray());
        }

    }
}
