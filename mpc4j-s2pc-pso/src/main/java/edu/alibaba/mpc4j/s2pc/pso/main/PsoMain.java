package edu.alibaba.mpc4j.s2pc.pso.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.blackip.PsuBlackIpMain;
import edu.alibaba.mpc4j.s2pc.pso.main.pid.PidMain;
import edu.alibaba.mpc4j.s2pc.pso.main.pmid.PmidMain;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.PsuMain;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * PSO主函数。
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class PsoMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsoMain.class);
    /**
     * 主函数。
     *
     * @param args 只有一个输入：配置文件。
     */
    public static void main(String[] args) throws Exception {
        // 读取日志配置文件
        LOGGER.info("read log config");
        Properties log4jProperties = new Properties();
        log4jProperties.load(PsoMain.class.getResourceAsStream("/log4j.properties"));
        PropertyConfigurator.configure(log4jProperties);
        // 读取配置文件
        LOGGER.info("read PTO config");
        Properties properties = loadProperties(args[0]);
        // 读取协议类型
        String ptoType = PropertiesUtils.readString(properties, "pto_type");
        LOGGER.info("pto_type = " + ptoType);
        switch (ptoType) {
            case PsuBlackIpMain.PTO_TYPE_NAME:
                PsuBlackIpMain psuBlackIpMain = new PsuBlackIpMain(properties);
                psuBlackIpMain.run();
                break;
            case PsuMain.PTO_TYPE_NAME:
                PsuMain psuMain = new PsuMain(properties);
                psuMain.run();
                break;
            case PidMain.PTO_TYPE_NAME:
                PidMain pidMain = new PidMain(properties);
                pidMain.run();
                break;
            case PmidMain.PTO_TYPE_NAME:
                PmidMain pmidMain = new PmidMain(properties);
                pmidMain.run();
                break;
            default:
                throw new IllegalArgumentException("Invalid pto_type: " + ptoType);
        }
        System.exit(0);
    }

    private static Properties loadProperties(String file) {
        try (InputStream input = new FileInputStream(file)) {
            Properties properties = new Properties();
            // load a properties file
            properties.load(input);
            return properties;
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Failed to load config file: " +  file);
        }
    }
}
