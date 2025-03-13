package edu.alibaba.mpc4j.work.scape.s3pc.opf.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.main.agg.AggMain;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.main.permutation.PermutationMain;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.main.pgsort.PgSortMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Scape opf main function.
 *
 * @author Feng Han
 * @date 2025/2/26
 */
public class ScapeOpfMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScapeOpfMain.class);

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
                case PermutationMain.PTO_TYPE_NAME: {
                    PermutationMain main = new PermutationMain(properties, ownName);
                    main.runNetty();
                    break;
                }
                case PgSortMain.PTO_TYPE_NAME: {
                    PgSortMain main = new PgSortMain(properties, ownName);
                    main.runNetty();
                    break;
                }
                case AggMain.PTO_TYPE_NAME:{
                    AggMain main = new AggMain(properties, ownName);
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
