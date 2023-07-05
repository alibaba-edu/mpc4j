package edu.alibaba.mpc4j.dp.service.fo.ue;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Optimized Unary Encoding (OUE) Frequency Oracle LDP server.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
public class OueFoLdpServer extends AbstractFoLdpServer {
    /**
     * p* = 0.5
     */
    private static final double P_STAR = 0.5;
    /**
     * the bucket
     */
    private final int[] budget;
    /**
     * q* = 1 / (e^ε + 1)
     */
    private final double qStar;

    public OueFoLdpServer(FoLdpConfig config) {
        super(config);
        budget = new int[d];
        qStar = 1 / (Math.exp(epsilon) + 1);
    }

    @Override
    public void insert(byte[] itemBytes) {
        BitVector bitVector = BitVectorFactory.create(d, itemBytes);
        num++;
        IntStream.range(0, d).forEach(bitIndex -> {
            if (bitVector.get(bitIndex)) {
                budget[bitIndex]++;
            }
        });
    }

    @Override
    public Map<String, Double> estimate() {
        return IntStream.range(0, d)
            .boxed()
            .collect(Collectors.toMap(
                domain::getIndexItem,
                itemIndex -> (budget[itemIndex] - num * qStar) / (P_STAR - qStar)
            ));
    }
}
