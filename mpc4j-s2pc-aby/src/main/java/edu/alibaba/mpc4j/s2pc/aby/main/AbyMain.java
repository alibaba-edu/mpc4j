package edu.alibaba.mpc4j.s2pc.aby.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.main.corr.ZlCorrMain;
import edu.alibaba.mpc4j.s2pc.aby.main.millionaire.MillionaireMain;
import edu.alibaba.mpc4j.s2pc.aby.main.trun.ZlTruncMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * ABY main.
 *
 * @author Liqiang Peng
 * @date 2022/10/12
 */
public class AbyMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbyMain.class);

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
            case ZlTruncMain.TASK_NAME:
                ZlTruncMain zlTruncMain = new ZlTruncMain(properties);
                zlTruncMain.run();
                break;
            case ZlCorrMain.TASK_NAME:
                ZlCorrMain zlCorrMain = new ZlCorrMain(properties);
                zlCorrMain.run();
                break;
            case MillionaireMain.TASK_NAME:
                MillionaireMain millionaireMain = new MillionaireMain(properties);
                millionaireMain.run();
                break;
            default:
                throw new IllegalArgumentException("Invalid task_type: " + taskType);
        }
        System.exit(0);
    }
}
