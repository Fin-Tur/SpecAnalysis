package de.aint.readers;

import de.aint.models.Isotop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class IsotopeReader {
    public Isotop[] isotopes = new Isotop[31616];

    public IsotopeReader() {
        File file = new File("src/main/resources/isotopes.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int index = 0;
            line = br.readLine(); // Skip header line
            String[] args = line.split(" ");
            long id = Long.parseLong(args[0]);
            String symbol = args[1];
            double energy = Double.parseDouble(args[2]);
            double intensity = Double.parseDouble(args[3]);
            double abundance = Double.parseDouble(args[4]);
            isotopes[index] = new Isotop(id, symbol, energy, intensity, abundance);
            index++;

        } catch(Exception e) {
            e.printStackTrace();    

        }
    }
}
