package de.aint.readers;

import de.aint.models.Isotop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class IsotopeReader {
    public ArrayList<Isotop> isotopes = new ArrayList<>();

    public IsotopeReader() {
        isotopes.add(new Isotop("0", "UNK", 0, 0, 0));
        isotopes.add(new Isotop("1", "ANNH", 511, 0, 0));
        File file = new File("C:/Users/f.willems/IdeaProjects/SpecAnalysis/src/main/resources/isotop_details.txt");
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
            isotopes.add(new Isotop(id, symbol, energy, intensity, abundance));
            }
            

        } catch(Exception e) {
            e.printStackTrace();    

        }

        System.out.println("IsotopeReader initialized with " + isotopes.size() + " isotopes.");
    }
}
