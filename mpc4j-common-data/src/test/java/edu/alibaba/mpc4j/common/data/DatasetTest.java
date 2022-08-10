package edu.alibaba.mpc4j.common.data;

import edu.alibaba.mpc4j.common.data.classification.BreastCancer;
import edu.alibaba.mpc4j.common.data.classification.Iris;
import edu.alibaba.mpc4j.common.data.classification.PenDigits;
import edu.alibaba.mpc4j.common.data.classification.Weather;
import edu.alibaba.mpc4j.common.data.regression.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;

/**
 * 数据集测试。
 *
 * @author Weiran Liu
 * @date 2021/10/03
 */
public class DatasetTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetTest.class);

    @Test
    public void testCpu() {
        testDataset(Cpu.class.getSimpleName(), Cpu.data, Cpu.data);
    }

    @Test
    public void testAbalone() {
        testDataset(Abalone.class.getSimpleName(), Abalone.train, Abalone.test);
    }

    @Test
    public void testAutoMpg() {
        testDataset(AutoMpg.class.getSimpleName(), AutoMpg.data, AutoMpg.data);
    }

    @Test
    public void testBostonHousing() {
        testDataset(BostonHousing.class.getSimpleName(), BostonHousing.data, BostonHousing.data);
    }

    @Test
    public void testKin8nm() {
        testDataset(Kin8nm.class.getSimpleName(), Kin8nm.data, Kin8nm.data);
    }

    @Test
    public void testWeather() {
        testDataset(Weather.class.getSimpleName(), Weather.data, Weather.data);
    }

    @Test
    public void testIris() {
        testDataset(Iris.class.getSimpleName(), Iris.data, Iris.data);
    }

    @Test
    public void testPenDigits() {
        testDataset(PenDigits.class.getSimpleName(), PenDigits.data, PenDigits.data);
    }

    @Test
    public void testBreastCancer() {
        testDataset(BreastCancer.class.getSimpleName(), BreastCancer.data, BreastCancer.data);
    }

    private void testDataset(String name, DataFrame trainDataFrame, DataFrame testDataFrame) {
        LOGGER.info("-----test dataset {}-----", name);
        LOGGER.info("train data:\n{}", trainDataFrame);
        LOGGER.info("test  data:\n{}", testDataFrame);
    }
}
