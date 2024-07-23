package edu.alibaba.mpc4j.s2pc.aby.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.main.osn.RosnMain;

import java.util.Properties;

/**
 * Aby3 main
 *
 * @author Feng Han
 * @date 2024/7/10
 */
public class AbyMain {
    /**
     * main function.
     *
     * @param args two arguments, config and party.
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        String ownName = args[1];
        String ptoType = MainPtoConfigUtils.readPtoType(properties);
        //noinspection SwitchStatementWithTooFewBranches
        switch (ptoType) {
            case RosnMain.PTO_TYPE_NAME:
                RosnMain rosnMain = new RosnMain(properties, ownName);
                rosnMain.runNetty();
                break;
            default:
                throw new IllegalArgumentException("Invalid " + MainPtoConfigUtils.PTO_TYPE_KEY + ": " + ptoType);
        }
        System.exit(0);
    }
}
