package edu.alibaba.mpc4j.dp.service.fo.rappor;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.config.RapporFoLdpConfig;
import org.apache.commons.math3.util.Precision;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.vector.DoubleVector;
import smile.data.vector.IntVector;
import smile.regression.ElasticNet;
import smile.regression.LASSO;
import smile.regression.LinearModel;
import smile.regression.RidgeRegression;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RAPPOR Frequency Oracle LDP server.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
public class RapporFoLdpServer extends AbstractFoLdpServer {
    /**
     * d is large when d > LARGE_D_THRESHOLD
     */
    private static final int LARGE_D_THRESHOLD = 1000;
    /**
     * alpha = 0.8
     */
    private static final double LASSO_ALPHA = 0.8;
    /**
     * max_iter = 10000
     */
    private static final int MAX_ITERATION = 10000;
    /**
     * l1_ratio, smile does not support l1_ratio = 0
     */
    private static final double LAMBDA_1_RATIO = 0.1;
    /**
     * l2_ratio
     */
    private static final double LAMBDA_2_RATIO = 0.9;
    /**
     * the label name
     */
    private static final String LABEL_NAME = "y";
    /**
     * the formula
     */
    private static final Formula FORMULA_Y = Formula.lhs(LABEL_NAME);
    /**
     * number of cohorts.
     */
    private final int cohortNum;
    /**
     * hash seeds
     */
    private final int[][] hashSeeds;
    /**
     * the size of the bloom filter
     */
    private final int m;
    /**
     * the byte size of the bloom filter
     */
    private final int mByteLength;
    /**
     * the IntHash
     */
    private final IntHash intHash;
    /**
     * f
     */
    private final double f;
    /**
     * the budget
     */
    private final int[][] budget;
    /**
     * num in each cohorts
     */
    private final int[] cohortCounts;
    /**
     * the learning rate
     */
    private final double learningRate;

    public RapporFoLdpServer(FoLdpConfig config) {
        super(config);
        RapporFoLdpConfig rapporConfig = (RapporFoLdpConfig)config;
        cohortNum = rapporConfig.getCohortNum();
        hashSeeds = rapporConfig.getHashSeeds();
        m = rapporConfig.getM();
        mByteLength = CommonUtils.getByteLength(m);
        intHash = IntHashFactory.fastestInstance();
        f = rapporConfig.getF();
        // init the bucket
        budget = new int[cohortNum][m];
        cohortCounts = new int[cohortNum];
        // init the learning rate, self.reg_const = 0.025 * self.f
        learningRate = 0.025 * f;
    }

    @Override
    public void insert(byte[] itemBytes) {
        // read the bloom filter bytes
        byte[] bloomFilterBytes = new byte[mByteLength];
        System.arraycopy(itemBytes, 0, bloomFilterBytes, 0, bloomFilterBytes.length);
        BitVector bloomFilter = BitVectorFactory.create(m, bloomFilterBytes);
        // read the cohort index
        int cohortIndexByteLength = IntUtils.boundedNonNegIntByteLength(cohortNum);
        byte[] cohortIndexBytes = new byte[cohortIndexByteLength];
        System.arraycopy(itemBytes, mByteLength, cohortIndexBytes, 0, cohortIndexBytes.length);
        int cohortIndex = IntUtils.byteArrayToBoundedNonNegInt(cohortIndexBytes, cohortNum);
        MathPreconditions.checkNonNegativeInRange("cohort index", cohortIndex, cohortNum);
        cohortCounts[cohortIndex]++;
        num++;
        IntStream.range(0, m).forEach(hashPosition -> {
            if (bloomFilter.get(hashPosition)) {
                budget[cohortIndex][hashPosition]++;
            }
        });
    }

    @Override
    public Map<String, Double> estimate() {
        int[][] x = createX();
        DoubleVector yVector = DoubleVector.of(LABEL_NAME, createY());
        String[] featureNames = IntStream.range(0, d).mapToObj(String::valueOf).toArray(String[]::new);
        if (d > LARGE_D_THRESHOLD) {
            // If d is large, we perform feature selection to reduce computation time
            DataFrame lassoDataFrame = DataFrame.of(x, featureNames);
            lassoDataFrame = lassoDataFrame.merge(yVector);
            LinearModel lasso = LASSO.fit(FORMULA_Y, lassoDataFrame, LASSO_ALPHA);
            double[] lassoCoefficients = lasso.coefficients();
            // indexes = np.nonzero(lasso_model.coef_)[0]
            DataFrame dataFrame = DataFrame.of(yVector);
            int[] indexMap = new int[d];
            int tempIndex = 0;
            for (int dIndex = 0; dIndex < d; dIndex++) {
                // X_red = X[:, indexes]
                if (!Precision.equals(lassoCoefficients[dIndex], 0.0, 1)) {
                    indexMap[dIndex] = tempIndex;
                    tempIndex++;
                    int finalColumnIndex = dIndex;
                    int[] mergeColumn = IntStream.range(0, cohortNum * m)
                        .map(index -> x[index][finalColumnIndex])
                        .toArray();
                    dataFrame = dataFrame.merge(IntVector.of(String.valueOf(dIndex), mergeColumn));
                } else {
                    // index_map[d_index] = -1 means that this column should be removed in the training.
                    indexMap[dIndex] = -1;
                }
            }
            // model.fit(X_red, y)
            // The original implementation use ElasticNet with l1_ratio = 0. In SMILE, we cannot set l1_ratio = 0.
            // The comment in SMILE suggests using RidgeRegression instead of ElasticNet when l1_ratio = 0.
            // However, the test shows that due to the high memory consumption, we cannot use RidgeRegression.
            LinearModel model = ElasticNet.fit(FORMULA_Y, dataFrame, LAMBDA_1_RATIO, LAMBDA_2_RATIO, learningRate, MAX_ITERATION);
            double[] coefficients = model.coefficients();
            return IntStream.range(0, d)
                .boxed()
                .collect(Collectors.toMap(
                    domain::getIndexItem,
                    dIndex -> {
                        if (indexMap[dIndex] < 0) {
                            // index_map[d_index] = -1 means that this column should be removed in the training.
                            return 0.0;
                        } else {
                            return coefficients[indexMap[dIndex]] * cohortNum;
                        }
                    }
                ));
        } else {
            DataFrame dataFrame = DataFrame.of(x, featureNames);
            dataFrame = dataFrame.merge(yVector);
            LinearModel model = RidgeRegression.fit(FORMULA_Y, dataFrame);
            double[] coefficients = model.coefficients();
            return IntStream.range(0, d)
                .boxed()
                .collect(Collectors.toMap(
                    domain::getIndexItem,
                    dIndex -> coefficients[dIndex] * cohortNum
                ));
        }
    }

    private int[][] createX() {
        int[][] x = new int[cohortNum * m][d];
        for (int itemIndex = 0; itemIndex < d; itemIndex++) {
            for (int cohortIndex = 0; cohortIndex < cohortNum; cohortIndex++) {
                int hashNum = hashSeeds[cohortIndex].length;
                MathPreconditions.checkGreaterOrEqual("m", m, hashNum);
                byte[] itemIndexBytes = IntUtils.intToByteArray(itemIndex);
                int[] hashPositions = new int[hashNum];
                for (int hashIndex = 0; hashIndex < hashNum; hashIndex++) {
                    hashPositions[hashIndex] = Math.abs(intHash.hash(itemIndexBytes, hashSeeds[cohortIndex][hashIndex]) % m);
                }
                for (int hashPosition : hashPositions) {
                    x[cohortIndex * m + hashPosition][itemIndex] = 1;
                }
            }
        }
        return x;
    }

    private double[] createY() {
        double[] y = new double[cohortNum * m];
        for (int cohortIndex = 0; cohortIndex < cohortNum; cohortIndex++) {
            for (int hashPosition = 0; hashPosition < m; hashPosition++) {
                y[cohortIndex * m + hashPosition]
                    = (budget[cohortIndex][hashPosition] - 0.5 * f * cohortCounts[cohortIndex]) / (1 - f);
            }
        }
        return y;
    }
}
