package edu.alibaba.mpc4j.work.db.sketch.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.db.sketch.main.CMS.CMSZ2Main;
import edu.alibaba.mpc4j.work.db.sketch.main.GK.GKMain;
import edu.alibaba.mpc4j.work.db.sketch.main.HLL.HLLMain;
import edu.alibaba.mpc4j.work.db.sketch.main.SS.SSMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Streaming experiment main class for the S³ framework.
 * <p>
 * This class serves as the entry point for running various sketch experiments in a 3-party computation (3PC) environment.
 * It coordinates the execution of different sketch protocols including CMS, SS, HLL, and GK for streaming data processing.
 * The class supports both single configuration files and directories containing multiple configuration files.
 * </p>
 */
public class StreamingMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingMain.class);

    private String dataType = "UNIFORM";

    /**
     * Main entry point for the streaming sketch experiments.
     * <p>
     * This method loads configuration files and runs the appropriate sketch protocol based on the
     * protocol type specified in the configuration. It supports batch execution of multiple experiments
     * by reading all .conf files from a directory.
     * </p>
     *
     * @param args command line arguments: args[0] = config file or directory path, args[1] = party name
     * @throws Exception if any error occurs during experiment execution
     */
    public static void main(String[] args) throws Exception {
        // Load Log4j configuration for logging
        PropertiesUtils.loadLog4jProperties();
        File inputFile = new File(args[0]);
        String ownName = args[1];
        List<String> allFiles;
        if (inputFile.isDirectory()) {
            // Support directory containing many config files for batch execution
            File[] fs = inputFile.listFiles();
            allFiles = new LinkedList<>();
            assert fs != null;
            for (File f : fs) {
                if ((!f.isDirectory()) && f.getPath().endsWith(".conf")) {
                    allFiles.add(f.getPath());
                }
            }
        } else {
            // Single configuration file execution
            allFiles = List.of(inputFile.getPath());
        }
        // Sort configuration files to ensure consistent execution order
        String[] names = allFiles.stream().sorted().toArray(String[]::new);
        LOGGER.info(Arrays.toString(names));
        // Execute experiments for each configuration file
        for (String name : names) {
            Properties properties = PropertiesUtils.loadProperties(name);
            String ptoType = MainPtoConfigUtils.readPtoType(properties);
            // Dispatch to appropriate sketch protocol based on protocol type
            switch (ptoType) {
                case CMSZ2Main.PTO_NAME: {
                    CMSZ2Main main = new CMSZ2Main(properties, ownName);
                    main.runNetty();
                    break;
                }
                case SSMain.PTO_TYPE: {
                    SSMain main = new SSMain(properties, ownName);
                    main.runNetty();
                    break;
                }
                case HLLMain.PTO_TYPE: {
                    HLLMain main = new HLLMain(properties, ownName);
                    main.runNetty();
                    break;
                }
                case GKMain.PTO_TYPE: {
                    GKMain main = new GKMain(properties, ownName);
                    main.runNetty();
                    break;
                }
                default:
                    throw new IllegalArgumentException("Invalid " + MainPtoConfigUtils.PTO_TYPE_KEY + ": " + ptoType);
            }
        }
        System.exit(0);
    }

}
