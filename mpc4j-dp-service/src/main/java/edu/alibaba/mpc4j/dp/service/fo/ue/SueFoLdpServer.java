package edu.alibaba.mpc4j.dp.service.fo.ue;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Symmetric Unary Encoding (SUE) Frequency Oracle LDP server.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
public class SueFoLdpServer extends AbstractFoLdpServer {
    /**
     * the bucket
     */
    private final int[] budget;
    /**
     * p* = e^(ε/2) / (e^(ε/2) - 1)
     */
    private final double pStar;
    /**
     * q* = 1 / (e^(ε/2) - 1)
     */
    private final double qStar;

    public SueFoLdpServer(FoLdpConfig config) {
        super(config);
        budget = new int[d];
        double expHalfEpsilon = Math.exp(epsilon / 2);
        pStar = expHalfEpsilon / (expHalfEpsilon + 1);
        qStar = 1 / (expHalfEpsilon + 1);
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
                itemIndex -> (budget[itemIndex] - num * qStar) / (pStar - qStar)
            ));
    }
}
