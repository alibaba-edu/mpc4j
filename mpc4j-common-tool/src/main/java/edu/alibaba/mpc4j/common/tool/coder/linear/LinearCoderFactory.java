package edu.alibaba.mpc4j.common.tool.coder.linear;

/**
 * 线性编码器工厂。
 *
 * @author Weiran Liu
 * @date 2021/12/14
 */
public class LinearCoderFactory {
    /**
     * 私有构造函数。
     */
    private LinearCoderFactory() {
        // empty
    }

    /**
     * 线性编码器类型
     */
    public enum LinearCoderType {
        /**
         * 输出128比特长的重复编码
         */
        REPUTATION_001_128,
        /**
         * 输出256比特长的Hadamard编码
         */
        HADAMARD_008_256,
        /**
         * BCH_065_448
         */
        BCH_065_448,
        /**
         * BCH_072_462
         */
        BCH_072_462,
        /**
         * BCH_076_511
         */
        BCH_076_511,
        /**
         * BCH_084_495
         */
        BCH_084_495,
        /**
         * BCH_090_495
         */
        BCH_090_495,
        /**
         * BCH_132_583
         */
        BCH_132_583,
        /**
         * BCH_138_594
         */
        BCH_138_594,
        /**
         * BCH_144_605
         */
        BCH_144_605,
        /**
         * BCH_150_616
         */
        BCH_150_616,
        /**
         * BCH_156_627
         */
        BCH_156_627,
        /**
         * BCH_162_638
         */
        BCH_162_638,
        /**
         * BCH_168_649
         */
        BCH_168_649,
        /**
         * BCH_174_660
         */
        BCH_174_660,
        /**
         * BCH_210_732
         */
        BCH_210_732,
        /**
         * BCH_217_744
         */
        BCH_217_744,
        /**
         * BCH_231_768
         */
        BCH_231_768,
        /**
         * BCH_238_776
         */
        BCH_238_776,
    }

    /**
     * 构建线性编码。
     *
     * @param linearCoderType 线性编码类型。
     * @return 线性编码。
     */
    public static LinearCoder getInstance(LinearCoderType linearCoderType) {
        switch (linearCoderType) {
            case REPUTATION_001_128:
                return new ReputationCoder(128);
            case HADAMARD_008_256:
                return new HadamardCoder(8);
            case BCH_065_448:
                return Bch065By448Coder.getInstance();
            case BCH_072_462:
                return Bch072By462Coder.getInstance();
            case BCH_076_511:
                return Bch076By511Coder.getInstance();
            case BCH_084_495:
                return Bch084By495Coder.getInstance();
            case BCH_090_495:
                return Bch090By495Coder.getInstance();
            case BCH_132_583:
                return Bch132By583Coder.getInstance();
            case BCH_138_594:
                return Bch138By594Coder.getInstance();
            case BCH_144_605:
                return Bch144By605Coder.getInstance();
            case BCH_150_616:
                return Bch150By616Coder.getInstance();
            case BCH_156_627:
                return Bch156By627Coder.getInstance();
            case BCH_162_638:
                return Bch162By638Coder.getInstance();
            case BCH_168_649:
                return Bch168By649Coder.getInstance();
            case BCH_174_660:
                return Bch174By660Coder.getInstance();
            case BCH_210_732:
                return Bch210By732Coder.getInstance();
            case BCH_217_744:
                return Bch217By744Coder.getInstance();
            case BCH_231_768:
                return Bch231By768Coder.getInstance();
            case BCH_238_776:
                return Bch238By776Coder.getInstance();
            default:
                throw new IllegalArgumentException("Invalid LinearCoderType: " + linearCoderType.name());
        }
    }

    /**
     * 根据输入比特长度，选择输出长度最短的线性编码。
     *
     * @param inputBitLength 输入比特长度。
     * @return 输出长度最短的线性编码。
     */
    public static LinearCoder getInstance(int inputBitLength) {
       assert inputBitLength > 0 : "InputBitLength must be greater than 0: " + inputBitLength;
        if (inputBitLength == 1) {
            return LinearCoderFactory.getInstance(LinearCoderType.REPUTATION_001_128);
        } else if (inputBitLength <= 8) {
            return LinearCoderFactory.getInstance(LinearCoderType.HADAMARD_008_256);
        } else if (inputBitLength <= Bch065By448Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_065_448);
        } else if (inputBitLength <= Bch072By462Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_072_462);
        } else if (inputBitLength <= Bch076By511Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_076_511);
        } else if (inputBitLength <= Bch084By495Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_084_495);
        } else if (inputBitLength <= Bch090By495Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_090_495);
        } else if (inputBitLength <= Bch132By583Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_132_583);
        } else if (inputBitLength <= Bch138By594Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_138_594);
        } else if (inputBitLength <= Bch144By605Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_144_605);
        } else if (inputBitLength <= Bch150By616Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_150_616);
        } else if (inputBitLength <= Bch156By627Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_156_627);
        } else if (inputBitLength <= Bch162By638Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_162_638);
        } else if (inputBitLength <= Bch168By649Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_168_649);
        } else if (inputBitLength <= Bch174By660Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_174_660);
        } else if (inputBitLength <= Bch210By732Coder.getInstance().getCodewordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_210_732);
        } else if (inputBitLength <= Bch217By744Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_217_744);
        } else if (inputBitLength <= Bch231By768Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_231_768);
        } else if (inputBitLength <= Bch238By776Coder.getInstance().getDatawordBitLength()) {
            return LinearCoderFactory.getInstance(LinearCoderType.BCH_238_776);
        } else {
            throw new IllegalArgumentException("InputBitLength must be in range [1, "
                + Bch238By776Coder.getInstance().getDatawordBitLength() + "]: " + inputBitLength);
        }
    }
}
