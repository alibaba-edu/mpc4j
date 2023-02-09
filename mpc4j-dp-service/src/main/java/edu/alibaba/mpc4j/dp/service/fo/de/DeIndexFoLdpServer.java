package edu.alibaba.mpc4j.dp.service.fo.de;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Direct Encoding (DE) Frequency Oracle LDP server. The item is encoded via index.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
public class DeIndexFoLdpServer extends AbstractFoLdpServer {
    /**
     * d byte length
     */
    private final int dByteLength;
    /**
     * the bucket
     */
    private final int[] budget;
    /**
     * p* = e^ε / (e^ε + d - 1)
     */
    private final double pStar;
    /**
     * q* = 1 / (e^ε + d - 1)
     */
    private final double qStar;

    public DeIndexFoLdpServer(FoLdpConfig config) {
        super(config);
        dByteLength = IntUtils.boundedNonNegIntByteLength(d);
        budget = new int[d];
        double expEpsilon = Math.exp(epsilon);
        pStar = expEpsilon / (expEpsilon + d - 1);
        qStar = 1 / (expEpsilon + d - 1);
    }

    @Override
    public void insert(byte[] itemBytes) {
        MathPreconditions.checkEqual(
            "actual byte length", "expect byte length", itemBytes.length, dByteLength
        );
        int itemIndex = IntUtils.byteArrayToBoundedNonNegInt(itemBytes, d);
        MathPreconditions.checkNonNegativeInRange("item index", itemIndex, d);
        budget[itemIndex]++;
        num++;
    }

    @Override
    public Map<String, Double> estimate() {
        return IntStream.range(0, d)
            .boxed()
            .collect(Collectors.toMap(
                domain::getIndexItem,
                itemIndex -> (budget[itemIndex] - num * qStar) / (pStar - qStar)
            ));
    }
}
