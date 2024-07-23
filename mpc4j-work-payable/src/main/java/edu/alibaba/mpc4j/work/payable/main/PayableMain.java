package edu.alibaba.mpc4j.work.payable.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.payable.main.pir.PayablePirMain;
import edu.alibaba.mpc4j.work.payable.main.psi.PayablePsiMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Payable PIR/PSI main.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public class PayableMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayableMain.class);

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
            case PayablePirMain.TASK_NAME:
                PayablePirMain payablePirMain = new PayablePirMain(properties, ownName);
                payablePirMain.runNetty();
                break;
            case PayablePsiMain.TASK_NAME:
                PayablePsiMain payablePsiMain = new PayablePsiMain(properties, ownName);
                payablePsiMain.runNetty();
                break;
            default:
                throw new IllegalArgumentException("Invalid task_type: " + taskType);
        }
        System.exit(0);
    }
}
