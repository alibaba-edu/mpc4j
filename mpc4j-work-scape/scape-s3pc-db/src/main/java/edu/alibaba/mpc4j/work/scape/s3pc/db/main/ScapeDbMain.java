package edu.alibaba.mpc4j.work.scape.s3pc.db.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.main.group.extreme.GroupExtremeMain;
import edu.alibaba.mpc4j.work.scape.s3pc.db.main.group.sum.GroupSumMain;
import edu.alibaba.mpc4j.work.scape.s3pc.db.main.join.general.GeneralJoinMain;
import edu.alibaba.mpc4j.work.scape.s3pc.db.main.join.pkfk.PkFkJoinMain;
import edu.alibaba.mpc4j.work.scape.s3pc.db.main.join.pkpk.PkPkJoinMain;
import edu.alibaba.mpc4j.work.scape.s3pc.db.main.orderby.OrderByMain;
import edu.alibaba.mpc4j.work.scape.s3pc.db.main.semijoin.general.GeneralSemiJoinMain;
import edu.alibaba.mpc4j.work.scape.s3pc.db.main.semijoin.pkpk.PkPkSemiJoinMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Scape Database main function.
 *
 * @author Feng Han
 * @date 2025/2/26
 */
public class ScapeDbMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScapeDbMain.class);

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
                case OrderByMain.PTO_TYPE_NAME:{
                    OrderByMain main = new OrderByMain(properties, ownName);
                    main.runNetty();
                    break;
                }
                case GroupSumMain.PTO_TYPE_NAME: {
                    GroupSumMain main = new GroupSumMain(properties, ownName);
                    main.runNetty();
                    break;
                }
                case GroupExtremeMain.PTO_TYPE_NAME: {
                    GroupExtremeMain main = new GroupExtremeMain(properties, ownName);
                    main.runNetty();
                    break;
                }
                case PkPkJoinMain.PTO_TYPE_NAME:{
                    PkPkJoinMain main = new PkPkJoinMain(properties, ownName);
                    main.runNetty();
                    break;
                }
                case PkFkJoinMain.PTO_TYPE_NAME:{
                    PkFkJoinMain main = new PkFkJoinMain(properties, ownName);
                    main.runNetty();
                    break;
                }
                case GeneralJoinMain.PTO_TYPE_NAME:{
                    GeneralJoinMain main = new GeneralJoinMain(properties, ownName);
                    main.runNetty();
                    break;
                }
                case GeneralSemiJoinMain.PTO_TYPE_NAME:{
                    GeneralSemiJoinMain main = new GeneralSemiJoinMain(properties, ownName);
                    main.runNetty();
                    break;
                }
                case PkPkSemiJoinMain.PTO_TYPE_NAME:{
                    PkPkSemiJoinMain main = new PkPkSemiJoinMain(properties, ownName);
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
