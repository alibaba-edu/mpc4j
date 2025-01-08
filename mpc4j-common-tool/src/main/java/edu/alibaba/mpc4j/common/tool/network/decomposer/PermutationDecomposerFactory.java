package edu.alibaba.mpc4j.common.tool.network.decomposer;

/**
 * Permutation decomposer factory
 *
 * @author Feng Han
 * @date 2024/8/12
 */
public class PermutationDecomposerFactory {
    public enum DecomposerType {
        /**
         * CGP20
         */
        CGP20,
        /**
         * LLL24
         */
        LLL24
    }

    public static PermutationDecomposer createComposer(DecomposerType decomposerType, int n, int t){
        switch (decomposerType){
            case CGP20:
                return new Cgp20PermutationDecomposer(n, t);
            case LLL24:
                return new Lll24PermutationDecomposer(n, t);
            default:
                throw new IllegalArgumentException("Invalid " + DecomposerType.class.getSimpleName() + ": " + decomposerType.name());
        }
    }
}
