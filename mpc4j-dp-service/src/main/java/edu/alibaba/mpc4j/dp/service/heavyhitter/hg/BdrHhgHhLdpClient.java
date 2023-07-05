package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.SecureBernoulliSampler;
import edu.alibaba.mpc4j.dp.service.heavyhitter.AbstractHhLdpClient;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.BdrHhgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HgHhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.tool.BucketDomain;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Budget-Division Randomization HeavyGuardian-based Heavy Hitter LDP client.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class BdrHhgHhLdpClient extends AbstractHhLdpClient {
    /**
     * the bucket domain
     */
    private final BucketDomain bucketDomain;
    /**
     * λ_h, i.e., the cell num in each bucket
     */
    private final int lambdaH;
    /**
     * p1 = e^ε_1 / (e^ε_1 + 1)
     */
    private final double p1;
    /**
     * p2 = e^ε_2 / (e^ε_2 + λ_h - 1)
     */
    private final double p2;
    /**
     * q2 = 1 / (e^ε_2 + λ_h - 1)
     */
    private final double q2;
    /**
     * p3 = e^ε_3 / (e^ε_3 + d - λ_h - 1)
     */
    private final double[] p3s;
    /**
     * q3 = 1 / (e^ε_3 + d - λ_h - 1)
     */
    private final double[] q3s;

    public BdrHhgHhLdpClient(BdrHhgHhLdpConfig config) {
        super(config);
        int w = config.getW();
        lambdaH = config.getLambdaH();
        bucketDomain = new BucketDomain(config.getDomainSet(), w, lambdaH);
        double alpha = config.getAlpha();
        double alphaWindowEpsilon = windowEpsilon * alpha;
        double remainedWindowEpsilon = windowEpsilon - alphaWindowEpsilon;
        // compute p1
        double expAlphaWindowEpsilon = Math.exp(alphaWindowEpsilon);
        p1 = expAlphaWindowEpsilon / (expAlphaWindowEpsilon + 1);
        // compute p2 and q2
        double expRemainedWindowEpsilon = Math.exp(remainedWindowEpsilon);
        p2 = expRemainedWindowEpsilon / (expRemainedWindowEpsilon + lambdaH - 1);
        q2 = 1 / (expRemainedWindowEpsilon + lambdaH - 1);
        // compute p3 and q3
        p3s = IntStream.range(0, w)
            .mapToDouble(bucketIndex -> {
                int bucketD = bucketDomain.getD(bucketIndex);
                return expRemainedWindowEpsilon / (expRemainedWindowEpsilon + bucketD - lambdaH - 1);
            })
            .toArray();
        q3s = IntStream.range(0, w)
            .mapToDouble(bucketIndex -> {
                int bucketD = bucketDomain.getD(bucketIndex);
                return 1 / (expRemainedWindowEpsilon + bucketD - lambdaH - 1);
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
        if (bucketDomain.getD(bucketIndex) == lambdaH) {
            // if the domain size equals to λ_h, then there is no cold item, use M2
            return mechanism2(currentBucket.keySet(), item, random).getBytes(HhLdpFactory.DEFAULT_CHARSET);
        }
        // M1
        boolean flag = mechanism1(currentBucket.keySet(), item, random);
        // M2
        if (flag) {
            // v is determined as hot
            return mechanism2(currentBucket.keySet(), item, random).getBytes(HhLdpFactory.DEFAULT_CHARSET);
        } else {
            // v is determined as cold
            return mechanism3(bucketIndex, currentBucket, item, random).getBytes(HhLdpFactory.DEFAULT_CHARSET);
        }
    }

    private boolean mechanism1(Set<String> currentBucketItemSet, String item, Random random) {
        // Let b = Ber(e^ε_1 / (e^ε_1 + 1))
        SecureBernoulliSampler bernoulliSampler = new SecureBernoulliSampler(random, p1);
        boolean b = bernoulliSampler.sample();
        // if b == 1: if v ∈ HG, flag = 1, else flag = 0, if b == 0: if v ∈ HG, flag = 0, else flag = 1
        // this is identical to (b XOR v ∈ HG)
        return b == currentBucketItemSet.contains(item);
    }

    private String mechanism2(Set<String> currentBucketItemSet, String item, Random random) {
        // note that we must return hot items in mechanism 2
        ArrayList<String> currentBudgetItemArrayList = new ArrayList<>(currentBucketItemSet);
        double randomSample = random.nextDouble();
        // Randomly sample an integer in [0, λ_h)
        int randomIndex = random.nextInt(lambdaH);
        if (currentBucketItemSet.contains(item)) {
            // if v ∈ HG, use random response
            if (randomSample > p2 - q2) {
                // answer a random item in the current heavy hitter
                return currentBudgetItemArrayList.get(randomIndex);
            } else {
                // answer the true item
                return item;
            }
        } else {
            // if v ∉ HG, choose a random item in the current heavy hitter
            return currentBudgetItemArrayList.get(randomIndex);
        }
    }

    private String mechanism3(int bucketIndex, Map<String, Double> currentBudget, String item, Random random) {
        // note that we must return cold items in mechanism 3
        int bucketD = bucketDomain.getD(bucketIndex);
        // Honestly creating a remained set and randomly picking an element is slow, here we use re-sample technique.
        if (!currentBudget.containsKey(item)) {
            // if v ∉ HG, use random response
            double randomSample = random.nextDouble();
            if (randomSample > p3s[bucketIndex] - q3s[bucketIndex]) {
                // answer a random item in the remained domain
                while (true) {
                    int randomIndex = random.nextInt(bucketD);
                    String randomizedItem = bucketDomain.getBucketIndexItem(bucketIndex, randomIndex);
                    if (!currentBudget.containsKey(randomizedItem)) {
                        return randomizedItem;
                    }
                }
            } else {
                // answer the true item
                return item;
            }
        } else {
            // if v ∈ HG, choose a random item in the remained domain
            while (true) {
                int randomIndex = random.nextInt(bucketD);
                String randomizedItem = bucketDomain.getBucketIndexItem(bucketIndex, randomIndex);
                if (!currentBudget.containsKey(randomizedItem)) {
                    return randomizedItem;
                }
            }
        }
    }
}
