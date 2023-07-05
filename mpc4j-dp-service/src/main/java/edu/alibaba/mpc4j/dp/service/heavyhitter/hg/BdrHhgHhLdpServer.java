package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.ExpBernoulliSampler;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.AbstractHhLdpServer;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.BdrHhgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.tool.BucketDoubleComparator;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HgHhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpServerState;
import edu.alibaba.mpc4j.dp.service.tool.BucketDomain;
import edu.alibaba.mpc4j.dp.service.tool.HeavyGuardianUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Budget-Division Randomization HeavyGuardian-based Heavy Hitter LDP server.
 *
 * @author Weiran Liu
 * @date 2022/11/19
 */
public class BdrHhgHhLdpServer extends AbstractHhLdpServer implements HhgHhLdpServer {
    /**
     * b = 1.08
     */
    private static final double B = 1.08;
    /**
     * ln(b)
     */
    private static final double LN_B = Math.log(B);
    /**
     * the non-cryptographic 32-bit hash function
     */
    private final IntHash intHash;
    /**
     * bucket comparator
     */
    private final BucketDoubleComparator bucketComparator;
    /**
     * budget num
     */
    private final int w;
    /**
     * λ_h, i.e., the cell num in each bucket
     */
    private final int lambdaH;
    /**
     * w buckets, each bucket has λ_h cells
     */
    private final ArrayList<Map<String, Double>> buckets;
    /**
     * the HeavyGuardian random state
     */
    private final Random hgRandom;
    /**
     * current de-bias nums for each budget
     */
    private final int[] ldpNums;
    /**
     * p1 = e^ε_1 / (e^ε_1 + 1)
     */
    private final double p1;
    /**
     * q1 = 1 / (e^ε_1 + 1)
     */
    private final double q1;
    /**
     * p2 = e^ε_2 / (e^ε_2 + λ_h - 1)
     */
    private final double p2;
    /**
     * q2 = 1 / (e^ε_2 + λ_h - 1)
     */
    private final double q2;
    /**
     * γ_h, proportion of hot items
     */
    private double gammaH;
    /**
     * the total number of insert items
     */
    private int num;

    public BdrHhgHhLdpServer(BdrHhgHhLdpConfig config) {
        super(config);
        bucketComparator = new BucketDoubleComparator();
        w = config.getW();
        lambdaH = config.getLambdaH();
        // set |Ω| in each bucket, and insert empty elements in the bucket
        BucketDomain bucketDomain = new BucketDomain(config.getDomainSet(), w, lambdaH);
        hgRandom = config.getHgRandom();
        // init buckets, full the budget with 0-count dummy items
        buckets = IntStream.range(0, w)
            .mapToObj(bucketIndex -> {
                ArrayList<String> bucketDomainArrayList = new ArrayList<>(bucketDomain.getBucketDomainSet(bucketIndex));
                assert bucketDomainArrayList.size() >= lambdaH;
                Map<String, Double> bucket = new HashMap<>(lambdaH);
                for (int i = 0; i < lambdaH; i++) {
                    bucket.put(bucketDomainArrayList.get(i), 0.0);
                }
                return bucket;
            })
            .collect(Collectors.toCollection(ArrayList::new));
        // init hash function
        intHash = IntHashFactory.fastestInstance();
        // init variables
        num = 0;
        ldpNums = new int[w];
        Arrays.fill(ldpNums, 0);
        // set privacy parameters
        double alpha = config.getAlpha();
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
        gammaH = config.getGammaH();
    }

    @Override
    public boolean warmupInsert(byte[] itemBytes) {
        checkState(HhLdpServerState.WARMUP);
        String item = new String(itemBytes, HhLdpFactory.DEFAULT_CHARSET);
        return insert(item);
    }

    @Override
    public void stopWarmup() {
        checkState(HhLdpServerState.WARMUP);
        double hotNum = 0;
        for (int budgetIndex = 0; budgetIndex < w; budgetIndex++) {
            Map<String, Double> budget = buckets.get(budgetIndex);
            // bias all counts and calculate λ
            for (Map.Entry<String, Double> entry : budget.entrySet()) {
                String item = entry.getKey();
                double value = entry.getValue();
                hotNum += value;
                value = value * getDebiasFactor();
                budget.put(item, value);
            }
        }
        // There are two ways of setting γ_H: (1) based on the priori knowledge; (2) warm-up setting.
        // If we manually set γ_H, it must be in range [0, 1], we do not need to update it. Otherwise, we compute it.
        if (gammaH < 0) {
            Preconditions.checkArgument(num > 0, "need warmup without manually set γ_H");
            gammaH = hotNum / num;
            assert gammaH >= 0 && gammaH <= 1 : "γ_h must be in range [0, 1]: " + gammaH;
        }
        hhLdpServerState = HhLdpServerState.STATISTICS;
    }

    @Override
    public boolean randomizeInsert(byte[] itemBytes) {
        checkState(HhLdpServerState.STATISTICS);
        String item = new String(itemBytes, HhLdpFactory.DEFAULT_CHARSET);
        return insert(item);
    }

    private double insertCount() {
        return 1.0;
    }

    private double debiasCount() {
        return -(gammaH * p1 * q2 + (1 - gammaH) * q1 / lambdaH);
    }

    private void debiasBucket(int bucketIndex) {
        switch (hhLdpServerState) {
            case WARMUP:
                return;
            case STATISTICS:
                Map<String, Double> bucket = buckets.get(bucketIndex);
                for (Map.Entry<String, Double> itemEntry : bucket.entrySet()) {
                    String item = itemEntry.getKey();
                    double value = itemEntry.getValue();
                    value += debiasCount();
                    bucket.put(item, value);
                }
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private double getDebiasFactor() {
        return p1 * (p2 - q2);
    }

    private double defaultDebiasCount(int bucketIndex) {
        return ldpNums[bucketIndex] * debiasCount();
    }

    private boolean insert(String item) {
        num++;
        // it first computes the hash function h(e) (1 ⩽ h(e) ⩽ w) to map e to bucket A[h(e)].
        int bucketIndex = HeavyGuardianUtils.getItemBucket(intHash, w, item);
        if (hhLdpServerState.equals(HhLdpServerState.STATISTICS)) {
            ldpNums[bucketIndex]++;
            debiasBucket(bucketIndex);
        }
        // find the weakest guardian
        Map<String, Double> bucket = buckets.get(bucketIndex);
        Map.Entry<String, Double> weakestCell = Collections.min(bucket.entrySet(), bucketComparator);
        String weakestItem = weakestCell.getKey();
        double weakestCount = weakestCell.getValue();
        // Case 1: e is in one cell in the heavy part of A[h(e)] (being a king or a guardian).
        if (bucket.containsKey(item)) {
            // HeavyGuardian just increments the corresponding frequency (the count field) in the cell by 1.
            double itemCount = bucket.get(item);
            itemCount += insertCount();
            bucket.put(item, itemCount);
            return true;
        }
        // Case 2: e is not in the heavy part of A[h(e)], and there are still empty cells.
        if (bucket.size() < lambdaH) {
            // It inserts e into an empty cell, i.e., sets the ID field to e and sets the count field to 1.
            bucket.put(item, insertCount() + defaultDebiasCount(bucketIndex));
            return true;
        }
        // Case 3: e is not in any cell in the heavy part of A[h(e)], and there is no empty cell.
        // We propose a novel technique named Exponential Decay: it decays (decrements) the count field of the weakest
        // guardian by 1 with probability P = b^{−C}, where b is a predefined constant number (e.g., b = 1.08), and C
        // is the value of the Count field of the weakest guardian.
        assert bucket.size() == lambdaH;
        // Sample a boolean value, with probability P = b^{−C}, the boolean value is 1
        // In LDP, the weakest count may be non-positive, if so, we do not need to sample, since it must be evicted.
        if (weakestCount > 0) {
            // Here we use the advanced Bernoulli(exp(−γ)) with γ = C * ln(b), and reverse the sample
            ExpBernoulliSampler expBernoulliSampler = new ExpBernoulliSampler(hgRandom, weakestCount * LN_B);
            // decay (decrement) the count field of the weakest guardian by 1 with probability P = b^{−C}
            boolean sample = expBernoulliSampler.sample();
            if (!sample) {
                weakestCount--;
            }
        }
        // After decay, if the count field becomes 0, it replaces the ID field of the weakest guardian with e,
        // and sets the count field to 1
        if (weakestCount <= 0) {
            bucket.remove(weakestItem);
            bucket.put(item, insertCount() + defaultDebiasCount(bucketIndex));
            return true;
        } else {
            bucket.put(weakestItem, weakestCount);
            return false;
        }
    }

    @Override
    public Map<String, Double> heavyHitters() {
        Set<String> flatKeySet = buckets.stream()
            .map(Map::keySet)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
        // we first iterate items in each budget
        Map<String, Double> countMap = flatKeySet.stream()
            .collect(Collectors.toMap(item -> item, this::response));
        List<Map.Entry<String, Double>> countList = new ArrayList<>(countMap.entrySet());
        countList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Collections.reverse(countList);
        if (flatKeySet.size() <= k) {
            // the current key set is less than k, return all items
            return countList.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return countList.subList(0, k).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    private double response(String item) {
        int bucketIndex = HeavyGuardianUtils.getItemBucket(intHash, w, item);
        // first, it checks the heavy part in bucket A[h(e)].
        Map<String, Double> bucket = buckets.get(bucketIndex);
        switch (hhLdpServerState) {
            case WARMUP:
                // return C
                return bucket.getOrDefault(item, 0.0);
            case STATISTICS:
                // return de-biased C
                double value = bucket.getOrDefault(item, defaultDebiasCount(bucketIndex)) / getDebiasFactor();
                return value < 0 ? 0 : value;
            default:
                throw new IllegalStateException("Invalid " + HhLdpServerState.class.getSimpleName() + ": " + hhLdpServerState);
        }
    }

    @Override
    public int getNum() {
        return num;
    }

    @Override
    public double getGammaH() {
        return gammaH;
    }

    @Override
    public HgHhLdpServerContext getServerContext() {
        return HgHhLdpServerContext.fromBuckets(buckets);
    }
}
