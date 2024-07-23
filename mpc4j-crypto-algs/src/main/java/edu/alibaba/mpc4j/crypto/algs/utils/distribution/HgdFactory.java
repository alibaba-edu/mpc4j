package edu.alibaba.mpc4j.crypto.algs.utils.distribution;

/**
 * HGD factory.
 *
 * @author Weiran Liu
 * @date 2024/5/15
 */
public class HgdFactory {
    /**
     * HGD type
     */
    public enum HgdType {
        /**
         * fast but not precise
         */
        FAST,
        /**
         * precise but not fast
         */
        PRECISE,
        /**
         * totally random
         */
        RANDOM,
    }

    /**
     * Gets an HGD instance.
     *
     * @param hgdType HGD type.
     * @return instance.
     */
    public static Hgd getInstance(HgdType hgdType) {
        switch (hgdType) {
            case RANDOM:
                return RandomHgd.getInstance();
            case FAST:
                return FastHgd.getInstance();
            case PRECISE:
                return PreciseHgd.getInstance();
            default:
                throw new IllegalArgumentException("Invalid " + HgdType.class.getSimpleName() + ": " + hgdType);
        }
    }
}
