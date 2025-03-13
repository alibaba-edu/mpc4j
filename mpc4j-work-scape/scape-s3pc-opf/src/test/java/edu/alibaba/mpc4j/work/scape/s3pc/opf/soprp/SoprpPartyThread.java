package edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpOperations.PrpFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpOperations.PrpOp;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 3p soprp party thread
 *
 * @author Feng Han
 * @date 2024/03/04
 */
public class SoprpPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoprpPartyThread.class);
    /**
     * soprp party
     */
    private final SoprpParty soprpParty;
    /**
     * input data size
     */
    private final int dataNum;
    /**
     * input data dimension
     */
    private final int inputDim;

    public SoprpPartyThread(SoprpParty soprpParty, int dataNum, int inputDim) {
        this.soprpParty = soprpParty;
        this.dataNum = dataNum;
        this.inputDim = inputDim;
    }

    @Override
    public void run() {
        SecureRandom secureRandom = new SecureRandom();
        try {
            long computeBitTupleNum = soprpParty.setUsage(new PrpFnParam(PrpOp.ENC, dataNum, inputDim), new PrpFnParam(PrpOp.DEC, dataNum, inputDim))[0];
            soprpParty.init();
            TripletZ2Vector[] inputShare;
            BitVector[] inputPlain = null;
            if (soprpParty.getRpc().ownParty().getPartyId() == 0) {
                inputPlain = IntStream.range(0, inputDim).mapToObj(i ->
                    BitVectorFactory.createRandom(dataNum, secureRandom)).toArray(BitVector[]::new);
                inputShare = soprpParty.getAbb3Party().getZ2cParty().shareOwn(inputPlain);
            } else {
                inputShare = soprpParty.getAbb3Party().getZ2cParty().shareOther(
                    IntStream.range(0, inputDim).map(i -> dataNum).toArray(), soprpParty.getRpc().getParty(0));
            }

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            TripletZ2Vector[] encRes = soprpParty.enc(inputShare);
            TripletZ2Vector[] decRes = soprpParty.dec(encRes, inputDim);
            BitVector[] out = soprpParty.getAbb3Party().getZ2cParty().open(decRes);
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            LOGGER.info("P{} with soprp dataNum:{}, inputDim:{}, process time: {}ms",
                soprpParty.getRpc().ownParty().getPartyId(), dataNum, inputDim, time);

            if (soprpParty.getRpc().ownParty().getPartyId() == 0) {
                Assert.assertArrayEquals(out, inputPlain);
            }
            long usedBitTuple = computeBitTupleNum == 0 ? 0 : soprpParty.getAbb3Party().getTripletProvider().getZ2MtProvider().getAllTupleNum();
            LOGGER.info("computed bitTupleNum:{}, actually used bitTupleNum:{}", computeBitTupleNum, usedBitTuple);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

}
