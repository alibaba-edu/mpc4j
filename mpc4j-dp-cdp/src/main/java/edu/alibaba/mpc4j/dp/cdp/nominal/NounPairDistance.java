package edu.alibaba.mpc4j.dp.cdp.nominal;

/**
 * 名词对距离。两个名词的距离越大，意味着两个名词的关系越远。
 *
 * @author Weiran Liu
 * @date 2022/4/23
 */
public class NounPairDistance {
    /**
     * 名词对
     */
    private NounPair nounPair;
    /**
     * 距离
     */
    private double distance;

    /**
     * 构建名词对距离。
     *
     * @param noun1    第一个名词。
     * @param noun2    第二个名词。
     * @param distance 距离。
     * @return 名词对距离。
     */
    public static NounPairDistance createFromNouns(String noun1, String noun2, double distance) {
        assert distance >= 0.0 : "distance must be greater or equal than 0";
        NounPairDistance nounPairDistance = new NounPairDistance();
        nounPairDistance.nounPair = new NounPair(noun1, noun2);
        nounPairDistance.distance = distance;

        return nounPairDistance;
    }

    /**
     * 构建名词对距离。
     *
     * @param nounPair 名词对。
     * @param distance 距离。
     * @return 名词对距离。
     */
    public static NounPairDistance createFromNounPair(NounPair nounPair, double distance) {
        assert distance >= 0.0 : "distance must be greater or equal than 0";
        NounPairDistance nounPairDistance = new NounPairDistance();
        nounPairDistance.nounPair = nounPair;
        nounPairDistance.distance = distance;

        return nounPairDistance;
    }

    private NounPairDistance() {
        // empty
    }

    /**
     * 返回距离。
     *
     * @return 距离。
     */
    public double getDistance() {
        return distance;
    }

    /**
     * 返回名词对。
     *
     * @return 名词对。
     */
    public NounPair getNounPair() {
        return nounPair;
    }
}
