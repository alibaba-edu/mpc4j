package edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc;

import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpParam;

/**
 * the parameter of lowmc prp
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class LowMcParam implements SoprpParam {
    /**
     * the num of boxes
     */
    public final int numOfBoxes;
    /**
     * input block size
     */
    public final int blockSize;
    /**
     * evaluation rounds
     */
    public final int rounds;

    public LowMcParam(int numOfBoxes, int blockSize, int rounds){
        this.numOfBoxes = numOfBoxes;
        this.blockSize = blockSize;
        this.rounds = rounds;
    }

}
