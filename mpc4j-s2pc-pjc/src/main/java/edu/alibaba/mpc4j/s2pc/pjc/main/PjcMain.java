package edu.alibaba.mpc4j.s2pc.pjc.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pjc.main.pid.PidMain;
import edu.alibaba.mpc4j.s2pc.pjc.main.pmid.PmidMain;
import edu.alibaba.mpc4j.s2pc.pso.main.PsoMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * PJC协议主函数。
 *
 * @author Weiran Liu
 * @date 2022/11/15
 */
public class PjcMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsoMain.class);

    /**
     * 主函数。
     *
     * @param args 只有一个输入：配置文件。
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        // 读取配置文件
        LOGGER.info("read PTO config");
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        // 读取协议类型
        String ptoType = PropertiesUtils.readString(properties, "pto_type");
        LOGGER.info("pto_type = " + ptoType);
        switch (ptoType) {
            case PidMain.PTO_TYPE_NAME:
                PidMain pidMain = new PidMain(properties);
                pidMain.runNetty();
                break;
            case PmidMain.PTO_TYPE_NAME:
                PmidMain pmidMain = new PmidMain(properties);
                pmidMain.runNetty();
                break;
            default:
                throw new IllegalArgumentException("Invalid pto_type: " + ptoType);
        }
        System.exit(0);
    }
}
