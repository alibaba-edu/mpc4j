package edu.alibaba.mpc4j.common.tool.polynomial.power;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 幂次方工具类。存储计算高幂次方的路径，参考下述开源代码完成实现：
 * <p>
 * https://github.com/microsoft/APSI/blob/main/common/apsi/powers.cpp
 * </p>
 *
 * @author Liqiang Peng
 * @date 2022/8/3
 */
public class PowerUtils {
    /**
     * 计算给定范围内的幂次方。
     *
     * @param sourcePowers 源幂次方。
     * @param upperBound   上界。
     * @return 给定范围内的幂次方。
     */
    public static PowerNode[] computePowers(Set<Integer> sourcePowers, int upperBound) {
        assert upperBound > 1 : "upper bound must be greater than 1 : " + upperBound;
        Set<Integer> targetPowers = IntStream.rangeClosed(1, upperBound)
            .boxed()
            .collect(Collectors.toCollection(HashSet::new));
        Integer[] sortSourcePowers = Arrays.stream(sourcePowers.toArray(new Integer[0]))
            .sorted()
            .toArray(Integer[]::new);
        assert sortSourcePowers[0] == 1 : "Source powers must contain 1";
        assert sortSourcePowers[sortSourcePowers.length - 1] <= upperBound : "Source powers "
            + "must be a subset of target powers";
        PowerNode[] powerNodes = new PowerNode[upperBound];
        IntStream.range(0, sortSourcePowers.length)
            .forEach(i -> powerNodes[sortSourcePowers[i] - 1] = new PowerNode(sortSourcePowers[i], 0));
        int currDepth = 0;
        for (int currPower = 1; currPower <= upperBound; currPower++) {
            if (powerNodes[currPower - 1] != null) {
                continue;
            }
            int optimalDepth = currPower - 1;
            int optimalS1 = currPower - 1;
            int optimalS2 = 1;
            for (int s1 = 1; s1 <= targetPowers.size(); s1++) {
                if (s1 >= currPower) {
                    break;
                }
                int s2 = currPower - s1;
                if (!targetPowers.contains(s2)) {
                    continue;
                }
                int depth = Math.max(powerNodes[s1 - 1].getDepth(), powerNodes[s2 - 1].getDepth()) + 1;
                if (depth < optimalDepth) {
                    optimalDepth = depth;
                    optimalS1 = s1;
                    optimalS2 = s2;
                }
            }
            powerNodes[currPower - 1] = new PowerNode(currPower, optimalDepth, optimalS1, optimalS2);
            currDepth = Math.max(currDepth, optimalDepth);
        }
        return powerNodes;
    }
}