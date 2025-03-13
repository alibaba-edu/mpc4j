package edu.alibaba.mpc4j.s3pc.abb3.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.main.predicate.PredicateMain;
import edu.alibaba.mpc4j.s3pc.abb3.main.shuffle.ShuffleMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * abb3 replicated sharing main function.
 *
 * @author Feng Han
 * @date 2025/3/3
 */
public class Abb3RpMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(Abb3RpMain.class);

    /**
     * main function.
     *
     * @param args two arguments, config and party.
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        File inputFile = new File(args[0]);
        String ownName = args[1];
        List<String> allFiles;
        if (inputFile.isDirectory()) {
            // we support directory containing many config files.
            File[] fs = inputFile.listFiles();
            allFiles = new LinkedList<>();
            assert fs != null;
            for (File f : fs) {
                if ((!f.isDirectory()) && f.getPath().endsWith(".conf")) {
                    allFiles.add(f.getPath());
                }
            }
        } else {
            // single file
            allFiles = List.of(inputFile.getPath());
        }
        String[] names = allFiles.stream().sorted().toArray(String[]::new);
        LOGGER.info(Arrays.toString(names));
        for (String name : names) {
            Properties properties = PropertiesUtils.loadProperties(name);
            String ptoType = MainPtoConfigUtils.readPtoType(properties);
            switch (ptoType) {
                case PredicateMain.PTO_TYPE_NAME: {
                    PredicateMain main = new PredicateMain(properties, ownName);
                    main.runNetty();
                    break;
                }
                case ShuffleMain.PTO_TYPE_NAME: {
                    ShuffleMain main = new ShuffleMain(properties, ownName);
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
