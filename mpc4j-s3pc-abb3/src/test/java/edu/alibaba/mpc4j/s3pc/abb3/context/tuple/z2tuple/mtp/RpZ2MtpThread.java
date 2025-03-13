package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.mtp;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.FilePtoWorkType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.file.RpZ2FileMtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
                    int[] bitNums = Arrays.stream(tuple[j]).mapToInt(MpcZ2Vector::bitNum).toArray();
                    // send data to left and right
                    List<byte[]> sendData = Arrays.stream(tuple[j])
                        .map(x -> x.getBitVectors()[0].getBytes())
                        .collect(Collectors.toList());
                    DataPacketHeader sendRightHeader = new DataPacketHeader(
                        0, 0, j, j,
                        provider.getRpc().ownParty().getPartyId(), provider.rightParty().getPartyId()
                    );
                    provider.getRpc().send(DataPacket.fromByteArrayList(sendRightHeader, sendData));
                    DataPacketHeader sendLeftHeader = new DataPacketHeader(
                        0, 1, j, j,
                        provider.getRpc().ownParty().getPartyId(), provider.leftParty().getPartyId()
                    );
                    provider.getRpc().send(DataPacket.fromByteArrayList(sendLeftHeader, sendData));
                    // receive data from left and right
                    DataPacketHeader receiveLeftHeader = new DataPacketHeader(
                        0, 0, j, j,
                        provider.leftParty().getPartyId(), provider.getRpc().ownParty().getPartyId()
                    );
                    List<byte[]> leftList = provider.getRpc().receive(receiveLeftHeader).getPayload();
                    MathPreconditions.checkEqual("bitNums.length", "tmp.size()", bitNums.length, leftList.size());
                    BitVector[] fromLeft = IntStream.range(0, bitNums.length).mapToObj(index ->
                        BitVectorFactory.create(bitNums[index], leftList.get(index))).toArray(BitVector[]::new);
                    data[j] = new BitVector[tuple[j].length];
                    for (int k = 0; k < data[j].length; k++) {
                        data[j][k] = tuple[j][k].getBitVectors()[0].xor(tuple[j][k].getBitVectors()[1]).xor(fromLeft[k]);
                    }

                    DataPacketHeader receiveRightHeader = new DataPacketHeader(
                        0, 1, j, j,
                        provider.rightParty().getPartyId(), provider.getRpc().ownParty().getPartyId()
                    );
                    List<byte[]> rightList = provider.getRpc().receive(receiveRightHeader).getPayload();
                    MathPreconditions.checkEqual("bitNums.length", "tmp.size()", bitNums.length, rightList.size());
                    BitVector[] fromRight = IntStream.range(0, bitNums.length).mapToObj(index ->
                        BitVectorFactory.create(bitNums[index], rightList.get(index))).toArray(BitVector[]::new);
                    for (int k = 0; k < data[j].length; k++) {
                        Assert.assertEquals(tuple[j][k].getBitVectors()[1], fromRight[k]);
                    }
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
