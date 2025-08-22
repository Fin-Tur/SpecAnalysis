package de.aint.readers;

import de.aint.models.Isotop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IsotopeReader {

    private static final Logger logger = LoggerFactory.getLogger(IsotopeReader.class);

    private String filePath;
    public ArrayList<Isotop> isotopes = new ArrayList<>();

    public IsotopeReader(String filename) {
       this.filePath = filename;
    }

    public void readIsotopes(){
        this.isotopes.add(new Isotop("0", "UNK", 0, 0, 0));
        this.isotopes.add(new Isotop("1", "ANNH", 511, 0, 0));
        File file = new File(filePath);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            line = br.readLine(); // Skip header line
            line = br.readLine(); // Skip Annhilation Peak
            while ((line = br.readLine()) != null) {
            String[] args = line.trim().split("\\s+");
            String id = args[0];
            String symbol = args[1];
            double energy = Double.parseDouble(args[2]);
            double intensity = Double.parseDouble(args[3]);
            double abundance = Double.parseDouble(args[4]);
            this.isotopes.add(new Isotop(id, symbol, energy, intensity, abundance));
            }
            

        } catch(Exception e) {
            logger.error("Error reading isotopes from file: {}", filePath, e);

        }
    }
}
