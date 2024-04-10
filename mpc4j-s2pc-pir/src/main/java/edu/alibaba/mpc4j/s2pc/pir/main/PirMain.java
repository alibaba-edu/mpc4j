package edu.alibaba.mpc4j.s2pc.pir.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.main.ccpsi.CcpsiMain;
import edu.alibaba.mpc4j.s2pc.pir.main.payablepir.PayablePirMain;
import edu.alibaba.mpc4j.s2pc.pir.main.scpsi.ScpsiMain;
import edu.alibaba.mpc4j.s2pc.pir.main.cppir.index.SingleCpPirMain;
import edu.alibaba.mpc4j.s2pc.pir.main.cppir.keyword.SingleCpKsPirMain;
import edu.alibaba.mpc4j.s2pc.pir.main.kwpir.KwPirMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * PIR main.
 *
 * @author Liqiang Peng
 * @date 2022/04/23
 */
public class PirMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PirMain.class);

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
            case ScpsiMain.TASK_NAME:
                ScpsiMain scpsiMain = new ScpsiMain(properties);
                scpsiMain.run();
                break;
            case CcpsiMain.TASK_NAME:
                CcpsiMain ccpsiMain = new CcpsiMain(properties);
                ccpsiMain.run();
                break;
            case SingleCpPirMain.TASK_NAME:
                SingleCpPirMain singleCpPirMain = new SingleCpPirMain(properties);
                singleCpPirMain.run();
                break;
            case SingleCpKsPirMain.TASK_NAME:
                SingleCpKsPirMain singleCpKsPirMain = new SingleCpKsPirMain(properties);
                singleCpKsPirMain.run();
                break;
            case KwPirMain.TASK_NAME:
                KwPirMain kwPirMain = new KwPirMain(properties);
                kwPirMain.run();
                break;
            case PayablePirMain.TASK_NAME:
                PayablePirMain payablePirMain = new PayablePirMain(properties);
                payablePirMain.run();
                break;
            default:
                throw new IllegalArgumentException("Invalid task_type: " + taskType);
        }
        System.exit(0);
    }
}
