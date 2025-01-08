package edu.alibaba.mpc4j.s2pc.pir.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.main.cppir.index.CpIdxPirMain;
import edu.alibaba.mpc4j.s2pc.pir.main.cppir.keyword.SingleCpKsPirMain;
import edu.alibaba.mpc4j.s2pc.pir.main.kspir.SingleKsPirMain;
import edu.alibaba.mpc4j.s2pc.pir.main.kwpir.StdKwPirMain;

import java.util.Properties;

/**
 * PIR main.
 *
 * @author Liqiang Peng
 * @date 2022/04/23
 */
public class PirMain {
    /**
     * main.
     *
     * @param args one input: config file name.
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        String ownName = args[1];
        String ptoType = MainPtoConfigUtils.readPtoType(properties);
        switch (ptoType) {
            case CpIdxPirMain.PTO_TYPE_NAME:
                CpIdxPirMain cpIdxPirMain = new CpIdxPirMain(properties, ownName);
                cpIdxPirMain.runNetty();
                break;
            case SingleCpKsPirMain.PTO_TYPE_NAME:
                SingleCpKsPirMain singleCpKsPirMain = new SingleCpKsPirMain(properties, ownName);
                singleCpKsPirMain.runNetty();
                break;
            case SingleKsPirMain.PTO_TYPE_NAME:
                SingleKsPirMain singleKsPirMain = new SingleKsPirMain(properties, ownName);
                singleKsPirMain.runNetty();
            case StdKwPirMain.PTO_NAME_KEY:
                StdKwPirMain stdKwPirMain = new StdKwPirMain(properties, ownName);
                stdKwPirMain.runNetty();
                break;
            default:
                throw new IllegalArgumentException("Invalid pto_type: " + ptoType);
        }
        System.exit(0);
    }
}
