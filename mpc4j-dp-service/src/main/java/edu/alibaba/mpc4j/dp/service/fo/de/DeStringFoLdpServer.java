package edu.alibaba.mpc4j.dp.service.fo.de;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Direct Encoding (DE) Frequency Oracle LDP server. The item is encoded via string.
 *
 * @author Weiran Liu
 * @date 2023/1/10
 */
public class DeStringFoLdpServer extends AbstractFoLdpServer {
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

    public DeStringFoLdpServer(FoLdpConfig config) {
        super(config);
        budget = new int[d];
        double expEpsilon = Math.exp(epsilon);
        pStar = expEpsilon / (expEpsilon + d - 1);
        qStar = 1 / (expEpsilon + d - 1);
    }

    @Override
    public void insert(byte[] itemBytes) {
        String item = new String(itemBytes, FoLdpFactory.DEFAULT_CHARSET);
        Preconditions.checkArgument(domain.contains(item), "%s is not in the domain", item);
        int itemIndex = domain.getItemIndex(item);
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
