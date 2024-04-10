package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.mtp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.FilePtoWorkType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.file.RpLongFileMtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * basic s3 zLong multiplication tuple provider thread
 *
 * @author Feng Han
 * @date 2024/01/30
 */
public class RpZl64MtpThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpZl64MtpThread.class);
    private final long totalData;
    private final RpLongMtp provider;
    private final List<int[]> requirement;

    private final FilePtoWorkType workType;

    RpZl64MtpThread(RpLongMtp provider, List<int[]> requirement, long totalData, FilePtoWorkType workType) {
        this.totalData = totalData;
        this.provider = provider;
        this.requirement = requirement;
        this.workType = workType;
    }

    @Override
    public void run() {
        try {
            provider.init(totalData);
            if (workType != null && workType.equals(FilePtoWorkType.READ_WRITE)) {
                ((RpLongFileMtp) provider).writeFiles(0);
            }
            long all = 0;
            for (int i = 0; i < requirement.size(); i++) {
                LOGGER.info("processing : {} / {}", i, requirement.size());
                all += Arrays.stream(requirement.get(i)).sum();
                LOGGER.info("current fetching data number : {}", all);
                TripletRpLongVector[][] tuple = provider.getTuple(requirement.get(i));
                LongVector[][] data = new LongVector[tuple.length][];
                for (int j = 0; j < data.length; j++) {
                    data[j] = provider.getEnv().open(tuple[j]);
                }
                if (provider.getRpc().ownParty().getPartyId() == 0) {
                    for (int j = 0; j < data[0].length; j++) {
                        Assert.assertEquals(data[0][j].mul(data[1][j]), data[2][j]);
                    }
                }
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
