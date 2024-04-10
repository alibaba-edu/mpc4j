package edu.alibaba.mpc4j.s3pc.abb3.context.cr;

import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * s3 correlated randomness provider thread
 *
 * @author Feng Han
 * @date 2024/02/01
 */
public class S3pcCrProviderThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3pcCrProviderThread.class);
    private final S3pcCrProvider crProvider;

    private final int[] bitNums;

    private final int[] dataNums;

    private BitVector[] bitWithLeft;

    private BitVector[] bitWithRight;

    private BitVector[] bitZero;

    private LongVector[] longWithLeft;

    private LongVector[] longWithRight;

    private LongVector[] longZero;

    S3pcCrProviderThread(S3pcCrProvider crProvider, int[] bitNums, int[] dataNums) {
        this.crProvider = crProvider;
        this.bitNums = bitNums;
        this.dataNums = dataNums;
    }

    public BitVector[] getBitWithLeft() {
        return bitWithLeft;
    }

    public BitVector[] getBitWithRight() {
        return bitWithRight;
    }

    public BitVector[] getBitZero() {
        return bitZero;
    }

    public LongVector[] getLongWithLeft() {
        return longWithLeft;
    }

    public LongVector[] getLongWithRight() {
        return longWithRight;
    }

    public LongVector[] getLongZero() {
        return longZero;
    }

    @Override
    public void run() {
        crProvider.init();
        LOGGER.info("generating bitWithLeft");
        bitWithLeft = crProvider.randBitVector(bitNums, crProvider.leftParty());
        LOGGER.info("generating bitWithRight");
        bitWithRight = crProvider.randBitVector(bitNums, crProvider.rightParty());
        LOGGER.info("generating bitZero");
        bitZero = crProvider.randZeroBitVector(bitNums);

        LOGGER.info("generating longWithLeft");
        longWithLeft = crProvider.randZl64Vector(dataNums, crProvider.leftParty());
        LOGGER.info("generating longWithRight");
        longWithRight = crProvider.randZl64Vector(dataNums, crProvider.rightParty());
        LOGGER.info("generating longZero");
        longZero = crProvider.randZeroZl64Vector(dataNums);
    }
}
