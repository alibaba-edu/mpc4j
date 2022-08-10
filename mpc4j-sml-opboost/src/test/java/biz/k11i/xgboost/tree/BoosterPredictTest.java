package biz.k11i.xgboost.tree;

import biz.k11i.xgboost.Predictor;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 测试预测结果。
 *
 * @author Weiran Liu
 * @date 2022/4/12
 */
public class BoosterPredictTest {
    /**
     * 训练数据URL
     */
    private final String TRAIN_URL = Objects.requireNonNull(
        getClass().getClassLoader().getResource("agaricus.txt.train")
    ).getPath() + "?indexing_mode=1";
    /**
     * 预测数据URL
     */
    private final String TEST_URL = Objects.requireNonNull(
        getClass().getClassLoader().getResource("agaricus.txt.test")
    ).getPath() + "?indexing_mode=1";

    @Test
    public void testPredict() throws XGBoostError, IOException {
        DMatrix trainMat = new DMatrix(TRAIN_URL);
        DMatrix testMat = new DMatrix(TEST_URL);
        // 训练模型
        Booster booster = trainBooster(trainMat, testMat);
        // 写入文件
        booster.saveModel("model.deprecated");
        // 用predictor读取文件
        new Predictor(new FileInputStream("model.deprecated"));
        // 删除文件
        File modelFile = new File("model.deprecated");
        if (!modelFile.delete()) {
            throw new IllegalStateException("Cannot delete the test model file: " + modelFile.getName());
        }

    }

    private Booster trainBooster(DMatrix trainMat, DMatrix testMat) throws XGBoostError {
        // set params
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("eta", 1.0);
        paramMap.put("max_depth", 2);
        paramMap.put("silent", 1);
        paramMap.put("objective", "binary:logistic");
        paramMap.put("eval_metric", "error");
        // set watchList
        HashMap<String, DMatrix> watches = new HashMap<>();
        watches.put("train", trainMat);
        watches.put("test", testMat);
        // set round
        int round = 5;
        // train a boost model
        return XGBoost.train(trainMat, paramMap, round, watches, null, null);
    }
}
