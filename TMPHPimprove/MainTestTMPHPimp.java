package THUIM;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * Example of how to use the THUIM algorithm from the source code.
 *
 * @author Philippe Fournier-Viger, 2010
 *
 */
public class MainTestTMPHPimp {

    public static void main(String[] arg) throws IOException {

        String input = "Chess_phm.txt";
        //String input = "data_test.txt";
        String output = ".//PTHUIM_output.txt";
        //Integer[] tarHUISubsume = {8, 10, 12, 26};
        Integer[] tarHUISubsume = {18,52,66}; //1-2-1-3
        //Integer[] tarHUISubsume = {7, 11}; //1-5000-1-2000  9580330
        int min_utility = 1000000; //

        int minPeriodicity = 1;  // minimum periodicity parameter (a number of transactions)
        int maxPeriodicity = 1000;  // maximum periodicity parameter (a number of transactions)
        int minAveragePeriodicity = 5;  // minimum average periodicity (a number of transactions)
        int maxAveragePeriodicity = 500;  // maximum average periodicity (a number of transactions)


        // Applying the THUIM algorithm
        PTHUIMAlgo thuim = new PTHUIMAlgo();
        thuim.THUIM(input, output, min_utility, tarHUISubsume,
                minPeriodicity, maxPeriodicity, minAveragePeriodicity,
                maxAveragePeriodicity);
        thuim.printStats(input, min_utility, tarHUISubsume);

    }

    public static String fileToPath(String filename) throws UnsupportedEncodingException {
        URL url = MainTestTMPHPimp.class.getResource(filename);
        return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
    }
}
