package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.mtp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.FilePtoWorkType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.file.RpZ2FileMtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * basic s3 z2 multiplication tuple provider thread
 *
 * @author Feng Han
 * @date 2024/01/29
 */
public class RpZ2MtpThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpZ2MtpThread.class);
    private final long totalBits;
    private final RpZ2Mtp provider;
    private final List<int[]> requirement;

    private final FilePtoWorkType workType;

    RpZ2MtpThread(RpZ2Mtp provider, List<int[]> requirement, long totalBits, FilePtoWorkType workType) {
        this.totalBits = totalBits;
        this.provider = provider;
        this.requirement = requirement;
        this.workType = workType;
    }

    @Override
    public void run() {
        try {
            provider.init(totalBits);
            if (workType != null && workType.equals(FilePtoWorkType.READ_WRITE)) {
                ((RpZ2FileMtp) provider).writeFiles(0);
            }
            for (int i = 0; i < requirement.size(); i++) {
                LOGGER.info("processing : {} / {}", i, requirement.size());
                TripletRpZ2Vector[][] tuple = provider.getTuple(requirement.get(i));
                BitVector[][] data = new BitVector[tuple.length][];
                for (int j = 0; j < data.length; j++) {
                    data[j] = provider.getEnv().open(tuple[j]);
                }
                if (provider.getRpc().ownParty().getPartyId() == 0) {
                    for (int j = 0; j < data[0].length; j++) {
                        Assert.assertEquals(data[0][j].and(data[1][j]), data[2][j]);
                    }
                }
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
