package edu.alibaba.work.femur.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;

import java.util.Properties;

/**
 * Range Keyword PIR main.
 *
 * @author Liqiang Peng
 * @date 2024/9/18
 */
public class FemurPirMain {
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
        if (ptoType.equals(FemurRpcPirMain.PTO_TYPE_NAME)) {
            FemurRpcPirMain femurRpcPirMain = new FemurRpcPirMain(properties, ownName);
            femurRpcPirMain.runNetty();
        } else {
            throw new IllegalArgumentException("Invalid pto_type: " + ptoType);
        }
        System.exit(0);
    }
}
