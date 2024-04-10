package edu.alibaba.mpc4j.s2pc.upso.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.upso.main.okvr.OkvrMain;
import edu.alibaba.mpc4j.s2pc.upso.main.ucpsi.UcpsiMain;
import edu.alibaba.mpc4j.s2pc.upso.main.upsu.UpsuMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * UPSO main.
 *
 * @author Liqiang Peng
 * @date 2022/04/23
 */
public class UpsoMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpsoMain.class);

    /**
     * main.
     *
     * @param args one input: config file name.
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        // read config file
        LOGGER.info("read PTO config");
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        // read task type
        String taskType = PropertiesUtils.readString(properties, "task_type");
        LOGGER.info("task_type = " + taskType);
        switch (taskType) {
            case UcpsiMain.TASK_NAME:
                UcpsiMain ucpsiMain = new UcpsiMain(properties);
                ucpsiMain.run();
                break;
            case OkvrMain.TASK_NAME:
                OkvrMain okvrMain = new OkvrMain(properties);
                okvrMain.run();
                break;
            case UpsuMain.TASK_NAME:
                UpsuMain upsuMain = new UpsuMain(properties);
                upsuMain.run();
                break;
            default:
                throw new IllegalArgumentException("Invalid task_type: " + taskType);
        }
        System.exit(0);
    }
}
