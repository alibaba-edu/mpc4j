package edu.alibaba.mpc4j.crypto.algs.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.crypto.algs.main.popf.PopfMain;

import java.util.Properties;

/**
 * Crypto Algs main.
 *
 * @author Liqiang Peng
 * @date 2024/5/15
 */
public class AlgsMain {
    /**
     * main.
     *
     * @param args one input: config file name.
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        String taskType = MainPtoConfigUtils.readPtoType(properties);
        switch (taskType) {
            case PopfMain.ALGS_TYPE_NAME:
                PopfMain popfMain = new PopfMain(properties);
                popfMain.run();
                break;
            default:
                throw new IllegalArgumentException("Invalid task_type: " + taskType);
        }
        System.exit(0);
    }
}
