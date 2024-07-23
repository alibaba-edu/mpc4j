package edu.alibaba.mpc4j.s2pc.upso.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.upso.main.ucpsi.UcpsiMain;
import edu.alibaba.mpc4j.s2pc.upso.main.upsu.UpsuMain;

import java.util.Properties;

/**
 * UPSO main.
 *
 * @author Liqiang Peng
 * @date 2022/04/23
 */
public class UpsoMain {
    /**
     * main.
     *
     * @param args one input: config file name.
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        String ownName = args[1];
        String taskType = MainPtoConfigUtils.readPtoType(properties);
        switch (taskType) {
            case UcpsiMain.PTO_TYPE_NAME:
                UcpsiMain ucpsiMain = new UcpsiMain(properties, ownName);
                ucpsiMain.runNetty();
                break;
            case UpsuMain.PTO_TYPE_NAME:
                UpsuMain upsuMain = new UpsuMain(properties, ownName);
                upsuMain.runNetty();
                break;
            default:
                throw new IllegalArgumentException("Invalid task_type: " + taskType);
        }
        System.exit(0);
    }
}
