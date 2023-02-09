package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.SecureBernoulliSampler;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HgHhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Advanced Hot HeavyGuardian-based Heavy Hitter LDP client.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class AdvHhgHhLdpClient extends AbstractHgHhLdpClient implements HhgHhLdpClient {
    /**
     * the privacy parameter allocation parameter α
     */
    private final double alpha;
    /**
     * p1 = e^ε_1 / (e^ε_1 + 1)
     */
    protected double p1;
    /**
     * q1 = 1 / (e^ε_1 + 1)
     */
    protected double q1;
    /**
     * p2 = e^ε_2 / (e^ε_2 + λ_h - 1)
     */
    protected double p2;
    /**
     * q2 = 1 / (e^ε_2 + λ_h - 1)
     */
    protected double q2;
    /**
     * p3 = e^ε_3 / (e^ε_3 + d - λ_h - 1)
     */
    private final double[] p3s;
    /**
     * q3 = 1 / (e^ε_3 + d - λ_h - 1)
     */
    private final double[] q3s;

    public AdvHhgHhLdpClient(HhLdpConfig config) {
        super(config);
        HgHhLdpConfig hgHhLdpConfig = (HgHhLdpConfig) config;
        alpha = hgHhLdpConfig.getAlpha();
        double alphaWindowEpsilon = windowEpsilon * alpha;
        double remainedWindowEpsilon = windowEpsilon - alphaWindowEpsilon;
        // compute p1 and p1
        double expAlphaWindowEpsilon = Math.exp(alphaWindowEpsilon);
        p1 = expAlphaWindowEpsilon / (expAlphaWindowEpsilon + 1);
        q1 = 1 / (expAlphaWindowEpsilon + 1);
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
        Map<String, Double> currentBucket = hgServerContext.getBudget(bucketIndex);
        Map<String, Double> copyCurrentBucket = new HashMap<>(currentBucket);
        // fill the budget with 0-count dummy items
        if (copyCurrentBucket.size() < lambdaH) {
            Set<String> remainedBudgetDomainSet = new HashSet<>(bucketDomain.getBucketDomainSet(bucketIndex));
            remainedBudgetDomainSet.removeAll(currentBucket.keySet());
            for (String remainedBudgetDomainItem : remainedBudgetDomainSet) {
                if (copyCurrentBucket.size() == lambdaH) {
                    break;
                }
                copyCurrentBucket.put(remainedBudgetDomainItem, 0.0);
            }
        }
        if (bucketDomain.getD(bucketIndex) == lambdaH) {
            // if the domain size equals to λ_h, then there is no cold item, use M2
            return userMechanism2(copyCurrentBucket.keySet(), item, random).getBytes(HhLdpFactory.DEFAULT_CHARSET);
        }
        // M1
        boolean flag = userMechanism1(copyCurrentBucket.keySet(), item, random);
        // M2
        if (flag) {
            // v is determined as hot
            return userMechanism2(copyCurrentBucket.keySet(), item, random).getBytes(HhLdpFactory.DEFAULT_CHARSET);
        } else {
            // v is determined as cold
            return userMechanism3(bucketIndex, copyCurrentBucket, item, random).getBytes(HhLdpFactory.DEFAULT_CHARSET);
        }
    }

    protected boolean userMechanism1(Set<String> currentBucketItemSet, String item, Random random) {
        // Let b = Ber(e^ε_1 / (e^ε_1 + 1))
        SecureBernoulliSampler bernoulliSampler = new SecureBernoulliSampler(random, p1);
        boolean b = bernoulliSampler.sample();
        // if b == 1: if v ∈ HG, flag = 1, else flag = 0, if b == 0: if v ∈ HG, flag = 0, else flag = 1
        // this is identical to (b XOR v ∈ HG)
        return b == currentBucketItemSet.contains(item);
    }

    protected String userMechanism2(Set<String> currentBucketItemSet, String item, Random random) {
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

    protected String userMechanism3(int bucketIndex, Map<String, Double> currentBudget, String item, Random random) {
        int bucketD = bucketDomain.getD(bucketIndex);
        // find the weakest guardian
        List<Map.Entry<String, Double>> currentBucketList = new ArrayList<>(currentBudget.entrySet());
        currentBucketList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Map.Entry<String, Double> weakestCurrentCell = currentBucketList.get(0);
        double weakestCurrentCount = weakestCurrentCell.getValue();
        // Honestly creating a remained set and randomly picking an element is slow, here we use re-sample technique.
        if (weakestCurrentCount <= 1.0) {
            // an item in HG is about to be evicted
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
        } else {
            // return BOT
            return HhLdpFactory.BOT_PREFIX + bucketIndex;
        }
    }

    @Override
    public double getAlpha() {
        return alpha;
    }
}
