package edu.alibaba.mpc4j.common.structure.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * main for structures.
 *
 * @author Weiran Liu
 * @date 2024/1/23
 */
public class StructureMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureMain.class);

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
        //noinspection SwitchStatementWithTooFewBranches
        switch (taskType) {
            case Gf2eDokvsEfficiency.TASK_NAME:
                Gf2eDokvsEfficiency gf2eSparseDokvsEfficiency = new Gf2eDokvsEfficiency(properties);
                gf2eSparseDokvsEfficiency.testEfficiency();
                break;
            default:
                throw new IllegalArgumentException("Invalid task_type: " + taskType);
        }
        System.exit(0);
    }
}
