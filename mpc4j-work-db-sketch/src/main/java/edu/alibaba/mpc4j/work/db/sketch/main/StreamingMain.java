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
 * Streaming main
 */
public class StreamingMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingMain.class);

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
                // to change
                case CMSZ2Main.PTO_NAME: {
                    String ptoImplType = PropertiesUtils.readString(properties, CMSZ2Main.PTO_NAME_KEY);
                    switch (ptoImplType) {
                        case CMSZ2Main.PTO_TYPE_NAME: {
                            CMSZ2Main main = new CMSZ2Main(properties, ownName);
                            main.runNetty();
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("Invalid " + CMSZ2Main.PTO_NAME_KEY + ": " + ptoImplType);
                    }
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
