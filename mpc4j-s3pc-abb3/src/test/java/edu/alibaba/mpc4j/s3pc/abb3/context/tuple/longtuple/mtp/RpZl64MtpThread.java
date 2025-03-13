package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.mtp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.FilePtoWorkType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.file.RpLongFileMtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
                    int maxArrayLen = Integer.MAX_VALUE >> 3;
                    // send self first data
                    List<byte[]> sendData = Arrays.stream(tuple[j]).map(x -> {
                        MathPreconditions.checkGreaterOrEqual("(Integer.MAX_VALUE>>3) >= x.length", maxArrayLen, x.getNum());
                        return LongUtils.longArrayToByteArray(x.getVectors()[0].getElements());
                    }).collect(Collectors.toList());
                    // send self first data to right
                    DataPacketHeader sendHeader = new DataPacketHeader(
                        0, 0, j, j,
                        provider.getRpc().ownParty().getPartyId(), provider.rightParty().getPartyId()
                    );
                    provider.getRpc().send(DataPacket.fromByteArrayList(sendHeader, sendData));
                    // send self first data to left
                    DataPacketHeader sendSecondHeader = new DataPacketHeader(
                        0, 1, j, j,
                        provider.getRpc().ownParty().getPartyId(), provider.leftParty().getPartyId()
                    );
                    provider.getRpc().send(DataPacket.fromByteArrayList(sendSecondHeader, sendData));

                    // receive left data
                    DataPacketHeader receiveHeader = new DataPacketHeader(
                        0, 0, j, j,
                        provider.leftParty().getPartyId(), provider.getRpc().ownParty().getPartyId()
                    );
                    List<byte[]> receivedDate = provider.getRpc().receive(receiveHeader).getPayload();
                    LongVector[] recVec = receivedDate.stream()
                        .map(x -> LongVector.create(LongUtils.byteArrayToLongArray(x)))
                        .toArray(LongVector[]::new);
                    Assert.assertEquals(tuple[j].length, recVec.length);
                    data[j] = new LongVector[tuple[j].length];
                    for (int index = 0; index < recVec.length; index++) {
                        data[j][index] = tuple[j][index].getVectors()[0].add(tuple[j][index].getVectors()[1]).add(recVec[index]);
                    }
                    // receive right data
                    DataPacketHeader receiveSecondHeader = new DataPacketHeader(
                        0, 1, j, j,
                        provider.rightParty().getPartyId(), provider.getRpc().ownParty().getPartyId()
                    );
                    List<byte[]> receivedSecondDate = provider.getRpc().receive(receiveSecondHeader).getPayload();
                    LongVector[] recSecondVec = receivedSecondDate.stream()
                        .map(x -> LongVector.create(LongUtils.byteArrayToLongArray(x)))
                        .toArray(LongVector[]::new);

                    Assert.assertEquals(tuple[j].length, recSecondVec.length);
                    // verify the data consistency
                    for (int index = 0; index < recVec.length; index++) {
                        Assert.assertArrayEquals(tuple[j][index].getVectors()[1].getElements(), recSecondVec[index].getElements());
                    }
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
