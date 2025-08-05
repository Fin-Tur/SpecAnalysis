package de.aint.readers;

import de.aint.models.Spectrum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpeReader extends Reader{

    @Override
    public Spectrum readSpectrum(String src) throws IOException {
        File file = new File(src);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean inData = false;
            boolean inCal = false;
            int dataStart = -1, dataEnd = -1;
            List<Integer> counts = new ArrayList<>();
            double[] calibration = null;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                //Look for $DATA seq
                if (line.startsWith("$DATA:")) {
                    inData = true;
                    //get length
                    String[] range = br.readLine().trim().split("\\s+");
                    dataStart = Integer.parseInt(range[0]);
                    dataEnd = Integer.parseInt(range[1]);
                    continue;
                }
                //Look for$MCA_CAL (energy Calibration)
                if (line.startsWith("$MCA_CAL:")) {
                    inCal = true;
                    //get calibrations
                    String calLine = br.readLine();
                    if (calLine == null) break;
                    calLine = calLine.trim();
                    String[] parts = calLine.split("\\s+");
                    if (parts.length == 1) {
                        //Args in next line ->
                        calLine = br.readLine().trim();
                        parts = calLine.split("\\s+");
                    }
                    calibration = new double[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        calibration[i] = Double.parseDouble(parts[i].replace("E", "e"));
                    }
                    inCal = false;
                    continue;
                }

                // Lese Zähldaten bis Abschnitt Ende oder bis Zeile mit "$"
                if (inData) {
                    if (line.startsWith("$")) {
                        inData = false;
                        continue;
                    }
                    if (!line.isEmpty()) {
                        counts.add(Integer.parseInt(line));
                    }
                }
            }


            int[] countsArr = counts.stream().mapToInt(Integer::intValue).toArray();
        }

        return null;
    }

}
