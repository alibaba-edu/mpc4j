package edu.alibaba.mpc4j.sml.opboost.main;

import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.sml.opboost.main.kendall.WeightedKendall;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.grad.ClsOpGradBoost;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.grad.RegOpGradBoost;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.xgboost.ClsOpXgBoost;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.xgboost.RegOpXgBoost;
import edu.alibaba.mpc4j.sml.opboost.main.overfit.ClsOverfitOpXgBoost;

import java.util.Properties;

/**
 * OpBoost主函数。
 *
 * @author Weiran Liu
 * @date 2022/5/5
 */
public class OpBoostMain {

    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        OpBoostTaskType taskType = MainPtoConfigUtils.readEnum(OpBoostTaskType.class, properties, "task_type");
        switch (taskType) {
            case REG_OP_GRAD_BOOST:
                RegOpGradBoost regOpGradBoost = new RegOpGradBoost(properties, args[1]);
                regOpGradBoost.init();
                regOpGradBoost.run();
                break;
            case CLS_OP_GRAD_BOOST:
                ClsOpGradBoost clsOpGradBoost = new ClsOpGradBoost(properties, args[1]);
                clsOpGradBoost.init();
                clsOpGradBoost.run();
                break;
            case REG_OP_XG_BOOST:
                RegOpXgBoost regOpXgBoost = new RegOpXgBoost(properties, args[1]);
                regOpXgBoost.init();
                regOpXgBoost.run();
                break;
            case CLS_OP_XG_BOOST:
                ClsOpXgBoost clsOpXgBoost = new ClsOpXgBoost(properties, args[1]);
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
