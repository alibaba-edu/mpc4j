package edu.alibaba.mpc4j.sml.opboost.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.sml.opboost.main.kendall.WeightedKendall;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.grad.ClsOpGradBoost;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.grad.RegOpGradBoost;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.xgboost.ClsOpXgBoost;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.xgboost.RegOpXgBoost;
import edu.alibaba.mpc4j.sml.opboost.main.overfit.ClsOverfitOpXgBoost;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * OpBoost主函数。
 *
 * @author Weiran Liu
 * @date 2022/5/5
 */
public class OpBoostMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpBoostMain.class);

    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        // 读取配置文件
        LOGGER.info("read config file");
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        // 读取协议类型
        String taskTypeString = PropertiesUtils.readString(properties, "task_type");
        OpBoostTaskType taskType = OpBoostTaskType.valueOf(taskTypeString);
        switch (taskType) {
            case REG_OP_GRAD_BOOST:
                RegOpGradBoost regOpGradBoost = new RegOpGradBoost(properties);
                regOpGradBoost.init();
                regOpGradBoost.run();
                break;
            case CLS_OP_GRAD_BOOST:
                ClsOpGradBoost clsOpGradBoost = new ClsOpGradBoost(properties);
                clsOpGradBoost.init();
                clsOpGradBoost.run();
                break;
            case REG_OP_XG_BOOST:
                RegOpXgBoost regOpXgBoost = new RegOpXgBoost(properties);
                regOpXgBoost.init();
                regOpXgBoost.run();
                break;
            case CLS_OP_XG_BOOST:
                ClsOpXgBoost clsOpXgBoost = new ClsOpXgBoost(properties);
                clsOpXgBoost.init();
                clsOpXgBoost.run();
                break;
            case WEIGHTED_KENDALL:
                WeightedKendall weightedKendall = new WeightedKendall(properties);
                weightedKendall.init();
                weightedKendall.run();
                break;
            case CLS_OVERFIT_OP_XG_BOOST:
                ClsOverfitOpXgBoost clsOverfitOpXgBoost = new ClsOverfitOpXgBoost(properties);
                clsOverfitOpXgBoost.init();
                clsOverfitOpXgBoost.run();
                break;
            default:
                throw new IllegalArgumentException("Invalid task_type: " + taskType);
        }
        System.exit(0);
    }
}
