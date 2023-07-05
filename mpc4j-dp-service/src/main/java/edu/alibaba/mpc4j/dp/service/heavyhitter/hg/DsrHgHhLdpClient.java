package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.heavyhitter.AbstractHhLdpClient;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.DsrHgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.tool.BucketDoubleComparator;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HgHhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.tool.BucketDomain;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Domain-Shrinkage Randomization HeavyGuardian-based Heavy Hitter LDP client.
 *
 * @author Weiran Liu
 * @date 2023/3/20
 */
public class DsrHgHhLdpClient extends AbstractHhLdpClient {
    /**
     * bucket comparator
     */
    private final BucketDoubleComparator bucketComparator;
    /**
     * the bucket domain
     */
    private final BucketDomain bucketDomain;
    /**
     * λ_h, i.e., the cell num in each bucket
     */
    private final int lambdaH;
    /**
     * p= e^ε / (e^ε + (λ_h + 1) - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + (λ_h + 1) - 1)
     */
    private final double q;
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double[] ps;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double[] qs;

    public DsrHgHhLdpClient(DsrHgHhLdpConfig config) {
        super(config);
        bucketComparator = new BucketDoubleComparator();
        int w = config.getW();
        lambdaH = config.getLambdaH();
        bucketDomain = new BucketDomain(config.getDomainSet(), w, lambdaH);
        // compute p = e^ε / (e^ε + ( + 1) - 1)
        double expWindowEpsilon = Math.exp(windowEpsilon);
        p = expWindowEpsilon / (expWindowEpsilon + (lambdaH + 1) - 1);
        q = 1 / (expWindowEpsilon + (lambdaH + 1) - 1);
        // compute ps and qs
        ps = IntStream.range(0, w)
            .mapToDouble(bucketIndex -> {
                int bucketD = bucketDomain.getD(bucketIndex);
                return expWindowEpsilon / (expWindowEpsilon + bucketD - 1);
            })
            .toArray();
        qs = IntStream.range(0, w)
            .mapToDouble(bucketIndex -> {
                int bucketD = bucketDomain.getD(bucketIndex);
                return 1 / (expWindowEpsilon + bucketD - 1);
            })
            .toArray();
    }

    @Override
    public byte[] randomize(HhLdpServerContext serverContext, String item, Random random) {
        Preconditions.checkArgument(serverContext instanceof HgHhLdpServerContext);
        HgHhLdpServerContext hgServerContext = (HgHhLdpServerContext) serverContext;
        checkItemInDomain(item);
        int bucketIndex = bucketDomain.getItemBucket(item);
        assert bucketDomain.getBucketDomainSet(bucketIndex).contains(item);
        Map<String, Double> currentBucket = hgServerContext.getBucket(bucketIndex);
        assert currentBucket.size() == lambdaH;
        // there must be λ_h elements in the budget, randomize the item
        return mechanism(bucketIndex, currentBucket, item, random).getBytes(HhLdpFactory.DEFAULT_CHARSET);
    }

    private String mechanism(int bucketIndex, Map<String, Double> currentBucket, String item, Random random) {
        // find the weakest guardian
        Map.Entry<String, Double> weakestCell = Collections.min(currentBucket.entrySet(), bucketComparator);
        double weakestCount = weakestCell.getValue();
        if (weakestCount <= 1.0) {
            // an item in HG is about to be evicted, use basic mechanism to response
            int bucketD = bucketDomain.getD(bucketIndex);
            double randomSample = random.nextDouble();
            if (randomSample > ps[bucketIndex] - qs[bucketIndex]) {
                // answer a random item in the budget domain
                int randomIndex = random.nextInt(bucketD);
                return bucketDomain.getBucketIndexItem(bucketIndex, randomIndex);
            } else {
                // answer the true item
                return item;
            }
        } else {
            // no item in HG will be evicted, response using {h_1, ..., k_{λ_h}, ⊥}
            String botItem = HhLdpFactory.BOT_PREFIX + bucketIndex;
            ArrayList<String> sampleArrayList = new ArrayList<>(currentBucket.keySet());
            sampleArrayList.add(botItem);
            assert sampleArrayList.size() == lambdaH + 1;
            // if v ∈ HG, the target item is the item; otherwise, the target item is ⊥.
            String targetItem = currentBucket.containsKey(item) ? item : botItem;
            double randomSample = random.nextDouble();
            if (randomSample > p - q) {
                // answer a random item in {h_1, ..., k_{λ_h}, ⊥}
                int randomIndex = random.nextInt(lambdaH + 1);
                return sampleArrayList.get(randomIndex);
            } else {
                // answer the target item
                return targetItem;
            }
        }
    }
}
