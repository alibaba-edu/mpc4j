package edu.alibaba.mpc4j.work.db.dynamic.main3;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * main function for 3p
 */
public class DynamicDbMain3p {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDbMain3p.class);

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
                case DynamicDbOrderByMain3p.PTO_TYPE_NAME:
                    DynamicDbOrderByMain3p orderMain = new DynamicDbOrderByMain3p(properties, ownName);
                    orderMain.runNetty();
                    break;
                case DynamicDbGroupMain3p.PTO_TYPE_NAME:
                    DynamicDbGroupMain3p groupMain = new DynamicDbGroupMain3p(properties, ownName);
                    groupMain.runNetty();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid " + MainPtoConfigUtils.PTO_TYPE_KEY + ": " + ptoType);
            }
        }
        System.exit(0);
    }
}
