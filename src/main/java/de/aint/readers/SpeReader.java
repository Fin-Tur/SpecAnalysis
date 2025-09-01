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

public class SpeReader extends Reader{

    private static final Logger logger = LoggerFactory.getLogger(SpeReader.class);
    
    @Override
    public Spectrum readSpectrum(String src) throws IOException {

        //OPen file
        File file = new File(src);
        //read..
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean inData = false;
            boolean inCal = false;
            int dataStart = -1, dataEnd = -1;
            List<Double> counts = new ArrayList<>();
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
                    //get arg len
                    String calLine = br.readLine();
                    if (calLine == null) break;
                    calLine = calLine.trim();
                    int argLen = Integer.parseInt(calLine);
                    //Read args
                    String[] parts = calLine.split("\\s+");
                    calLine = br.readLine().trim();
                    parts = calLine.split("\\s+");

                    assert argLen == parts.length;
                    calibration = new double[argLen];

                    for (int i = 0; i < parts.length; i++) {
                        calibration[i] = Double.parseDouble(parts[i].replace("E", "e"));
                    }
                    inCal = false;
                    continue;
                }

                //Read data
                if (inData) {
                    //BReakout statement
                    if (line.startsWith("$")) {
                        inData = false;
                        continue;
                    }
                    if (!line.isEmpty()) {
                        counts.add(Double.parseDouble(line));
                    }
                }
            }


            double[] countsArr = counts.stream().mapToDouble(Double::doubleValue).toArray();
            assert calibration != null;
            assert calibration.length == 3;
            logger.info("Read SPE spectrum from file: {}", src);
            return new Spectrum(countsArr, calibration[0], calibration[1], calibration[2]);
        }

    }

}
