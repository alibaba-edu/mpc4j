package edu.alibaba.mpc4j.s2pc.pjc.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pjc.main.pid.PidMain;
import edu.alibaba.mpc4j.s2pc.pjc.main.pmid.PmidMain;

import java.util.Properties;

/**
 * PJC协议主函数。
 *
 * @author Weiran Liu
 * @date 2022/11/15
 */
public class PjcMain {
    /**
     * 主函数。
     *
     * @param args 只有一个输入：配置文件。
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        String ownName = args[1];
        String ptoType = MainPtoConfigUtils.readPtoType(properties);
        switch (ptoType) {
            case PidMain.PTO_TYPE_NAME:
                PidMain pidMain = new PidMain(properties, ownName);
                pidMain.runNetty();
                break;
            case PmidMain.PTO_TYPE_NAME:
                PmidMain pmidMain = new PmidMain(properties, ownName);
                pmidMain.runNetty();
                break;
            default:
                throw new IllegalArgumentException("Invalid " + MainPtoConfigUtils.PTO_TYPE_KEY + ": " + ptoType);
        }
        System.exit(0);
    }
}
