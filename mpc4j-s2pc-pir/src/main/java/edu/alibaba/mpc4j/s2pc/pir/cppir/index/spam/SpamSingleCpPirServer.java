package edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractSingleCpPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleCpPirDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * SPAM client-specific preprocessing PIR server.
 *
 * @author Weiran Liu
 * @date 2023/8/31
 */
public class SpamSingleCpPirServer extends AbstractSingleCpPirServer {
    /**
     * chunk size
     */
    private int chunkSize;
    /**
     * chunk num
     */
    private int chunkNum;
    /**
     * chunk num (in byte)
     */
    private int byteChunkNum;
    /**
     * padding database
     */
    private ZlDatabase paddingDatabase;
    /**
     * query num for each preprocessing round
     */
    private int roundQueryNum;
    /**
     * current query num
     */
    private int currentQueryNum;

    public SpamSingleCpPirServer(Rpc serverRpc, Party clientParty, SpamSingleCpPirConfig config) {
        super(SpamSingleCpPirDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(ZlDatabase database) throws MpcAbortException {
        setInitInput(database);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        chunkSize = SpamSingleCpPirUtils.getChunkSize(n);
        chunkNum = SpamSingleCpPirUtils.getChunkNum(n);
        assert chunkSize * chunkNum >= n
            : "chunkSize * chunkNum must be greater than or equal to n (" + n + "): " + chunkSize * chunkNum;
        byteChunkNum = CommonUtils.getByteLength(chunkNum);
        // pad the database
        byte[][] paddingData = new byte[chunkSize * chunkNum][byteL];
        for (int x = 0; x < n; x++) {
            paddingData[x] = database.getBytesData(x);
        }
        for (int x = n; x < chunkSize * chunkNum; x++) {
            paddingData[x] = BytesUtils.randomByteArray(byteL, l, secureRandom);
        }
        paddingDatabase = ZlDatabase.create(l, paddingData);
        roundQueryNum = SpamSingleCpPirUtils.getRoundQueryNum(n);
        stopWatch.stop();
        long paddingTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 0, 1, paddingTime,
            String.format(
                "Server sets params: n = %d, ChunkSize = %d, ChunkNum = %d, n (pad) = %d, Q = %d",
                n, chunkSize, chunkNum, chunkSize * chunkNum, roundQueryNum
            )
        );

        // preprocessing
        preprocessing();

        logPhaseInfo(PtoState.INIT_END);
    }

    private void preprocessing() throws MpcAbortException {
        stopWatch.start();
        // stream sending the database
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            // concatenate database into the whole byte buffer
            ByteBuffer byteBuffer = ByteBuffer.allocate(byteL * chunkSize);
            for (int offset = 0; offset < chunkSize; offset++) {
                byteBuffer.put(paddingDatabase.getBytesData(chunkId * chunkSize + offset));
            }
            List<byte[]> streamRequestPayload = Collections.singletonList(byteBuffer.array());
            DataPacketHeader streamRequestHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_STREAM_DATABASE_REQUEST.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(streamRequestHeader, streamRequestPayload));

            // receive response
            DataPacketHeader streamResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_STREAM_DATABASE_RESPONSE.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> streamResponsePayload = rpc.receive(streamResponseHeader).getPayload();
            MpcAbortPreconditions.checkArgument(streamResponsePayload.size() == 0);
            extraInfo++;
        }
        // reset current query num
        currentQueryNum = 0;
        stopWatch.stop();
        long streamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, streamTime, "Server handles " + chunkNum + " chunk");
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();

        DataPacketHeader queryRequestHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryRequestPayload = rpc.receive(queryRequestHeader).getPayload();
        int queryRequestSize = queryRequestPayload.size();
        MpcAbortPreconditions.checkArgument(queryRequestSize == 0 || queryRequestSize == 2);

        if (queryRequestSize == 0) {
            // response empty query
            responseEmptyQuery();
        } else {
            // response actual query
            respondActualQuery(queryRequestPayload);
        }
    }

    private void responseEmptyQuery() {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader queryResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryResponseHeader, new LinkedList<>()));
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server responses empty query");
    }

    private void respondActualQuery(List<byte[]> queryRequestPayload) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // the bit vector b
        byte[] bitVectorByteArray = queryRequestPayload.get(0);
        MpcAbortPreconditions.checkArgument(BytesUtils.isFixedReduceByteArray(bitVectorByteArray, byteChunkNum, chunkNum));
        BitVector bitVector = BitVectorFactory.create(chunkNum, bitVectorByteArray);
        // the offset vector r
        byte[] queryByteArray = queryRequestPayload.get(1);
        MpcAbortPreconditions.checkArgument(queryByteArray.length == Short.BYTES * chunkNum);
        ByteBuffer queryByteBuffer = ByteBuffer.wrap(queryByteArray);
        int[] offsets = new int[chunkNum];
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            offsets[chunkId] = queryByteBuffer.getShort();
        }
        // compute two possible parities
        byte[] leftParity = new byte[byteL];
        byte[] rightParity = new byte[byteL];
        int leftSetSize = 0;
        int rightSetSize = 0;
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            int x = chunkId * chunkSize + offsets[chunkId];
            byte[] entry = paddingDatabase.getBytesData(x);
            if (bitVector.get(chunkId)) {
                leftSetSize++;
                BytesUtils.xori(leftParity, entry);
            } else {
                rightSetSize++;
                BytesUtils.xori(rightParity, entry);
            }
        }
        // verify |S_0| == |S_1|
        MpcAbortPreconditions.checkArgument(leftSetSize == rightSetSize);
        // respond the query
        ByteBuffer byteBuffer = ByteBuffer.allocate(byteL * 2);
        byteBuffer.put(leftParity);
        byteBuffer.put(rightParity);
        List<byte[]> queryResponsePayload = Collections.singletonList(byteBuffer.array());
        DataPacketHeader queryResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryResponseHeader, queryResponsePayload));
        // increase current query num
        currentQueryNum++;
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server responses query");
        // when query num exceeds the maximum, rerun preprocessing.
        if (currentQueryNum >= roundQueryNum) {
            preprocessing();
        }
        logPhaseInfo(PtoState.PTO_END);
    }
}
