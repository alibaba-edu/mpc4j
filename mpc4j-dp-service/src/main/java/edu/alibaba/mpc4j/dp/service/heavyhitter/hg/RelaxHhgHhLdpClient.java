package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.SecureBernoulliSampler;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HgHhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;

import java.util.*;

/**
 * Relaxed Hot HeavyGuardian-based Heavy Hitter LDP client.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class RelaxHhgHhLdpClient extends AdvHhgHhLdpClient {

    public RelaxHhgHhLdpClient(HhLdpConfig config) {
        super(config);
        // recompute p2 and q2
        double expWindowEpsilon = Math.exp(windowEpsilon);
        p2 = expWindowEpsilon / (expWindowEpsilon + lambdaH - 1);
        q2 = 1 / (expWindowEpsilon + lambdaH - 1);
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
        assert copyCurrentBucket.size() == lambdaH;
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
            // v is determined as code
            return userMechanism3(bucketIndex, copyCurrentBucket, item, random).getBytes(HhLdpFactory.DEFAULT_CHARSET);
        }
    }

    @Override
    protected boolean userMechanism1(Set<String> currentBucketItemSet, String item, Random random) {
        if (!currentBucketItemSet.contains(item)) {
            return false;
        } else {
            // Let b = Ber(e^ε_1 / (e^ε_1 + 1))
            SecureBernoulliSampler bernoulliSampler = new SecureBernoulliSampler(random, p1);
            return bernoulliSampler.sample();
        }
    }

    @Override
    protected String userMechanism2(Set<String> currentBucketItemSet, String item, Random random) {
        ArrayList<String> currentHeavyHitterArrayList = new ArrayList<>(currentBucketItemSet);
        double randomSample = random.nextDouble();
        // Randomly sample an integer in [0, k)
        int randomIndex = random.nextInt(currentBucketItemSet.size());
        // if v ∈ HG, use random response
        if (randomSample > p2 - q2) {
            // answer a random item in the current heavy hitter
            return currentHeavyHitterArrayList.get(randomIndex);
        } else {
            // answer the true item
            return item;
        }
    }
}
