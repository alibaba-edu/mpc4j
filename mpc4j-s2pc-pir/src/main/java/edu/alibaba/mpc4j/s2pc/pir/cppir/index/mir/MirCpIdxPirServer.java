package edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.StreamCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.MirCpIdxPirPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.hint.MirHint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MIR client-specific preprocessing index PIR server.
 *
 * @author Weiran Liu
 * @date 2023/8/31
 */
public class MirCpIdxPirServer extends AbstractCpIdxPirServer implements StreamCpIdxPirServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MirCpIdxPirServer.class);
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

    public MirCpIdxPirServer(Rpc serverRpc, Party clientParty, MirCpIdxPirConfig config) {
        super(MirCpIdxPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(NaiveDatabase database, int maxBatchNum) throws MpcAbortException {
        setInitInput(database, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        chunkSize = MirCpIdxPirUtils.getChunkSize(n);
        chunkNum = MirCpIdxPirUtils.getChunkNum(n);
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
        roundQueryNum = MirCpIdxPirUtils.getRoundQueryNum(n);
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
        for (int blockChunkId = 0; blockChunkId < chunkNum; blockChunkId += MirHint.PRP_BLOCK_OFFSET_NUM) {
            LOGGER.info("preprocessing {} / {}", blockChunkId + 1, chunkNum);
            // send batched chunks
            for (int chunkId = blockChunkId; chunkId < blockChunkId + MirHint.PRP_BLOCK_OFFSET_NUM && chunkId < chunkNum; chunkId++) {
                // concatenate database into the whole byte buffer
                ByteBuffer byteBuffer = ByteBuffer.allocate(byteL * chunkSize);
                for (int offset = 0; offset < chunkSize; offset++) {
                    byteBuffer.put(paddingDatabase.getBytesData(chunkId * chunkSize + offset));
                }
                List<byte[]> streamRequestPayload = Collections.singletonList(byteBuffer.array());
                sendOtherPartyPayload(PtoStep.SERVER_SEND_STREAM_DATABASE_REQUEST.ordinal(), streamRequestPayload);
            }
            // receive response
            List<byte[]> streamResponsePayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_STREAM_DATABASE_RESPONSE.ordinal());
            MpcAbortPreconditions.checkArgument(streamResponsePayload.isEmpty());
        }
        // reset current query num
        currentQueryNum = 0;
        stopWatch.stop();
        long streamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, streamTime, "Server handles " + chunkNum + " chunk(s)");
    }

    @Override
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        for (int i = 0; i < batchNum; i++) {
            LOGGER.info("query {} / {}", i + 1, batchNum);
            List<byte[]> queryRequestPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
            int queryRequestSize = queryRequestPayload.size();
            MpcAbortPreconditions.checkArgument(queryRequestSize == 0 || queryRequestSize == 2);

            if (queryRequestSize == 0) {
                sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), new LinkedList<>());
            } else {
                // response actual query
                respondActualQuery(queryRequestPayload);
            }
        }
        logPhaseInfo(PtoState.PTO_END);
    }

    private void respondActualQuery(List<byte[]> queryRequestPayload) throws MpcAbortException {
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
        sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), queryResponsePayload);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.PTO_STEP, 1, 1, responseTime,
            "Server responses " + (currentQueryNum + 1) + "-th actual query"
        );

        currentQueryNum++;
        // when query num exceeds the maximum, rerun preprocessing.
        if (currentQueryNum > roundQueryNum) {
            preprocessing();
        }
    }

    @Override
    public void update(int[] xs, byte[][] entries) {
        MathPreconditions.checkEqual("xs.length", "entries.length", xs.length, entries.length);
        logPhaseInfo(PtoState.PTO_BEGIN);

        for (int round = 0; round < xs.length; round++) {
            stopWatch.start();
            int x = xs[round];
            byte[] entry = entries[round];
            MathPreconditions.checkNonNegativeInRange("x", x, n);
            Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(entry, byteL, l));
            // δ ← (i, D[i] ⊕ d)
            byte[] delta = new byte[byteL];
            BytesUtils.xori(delta, paddingDatabase.getBytesData(x));
            BytesUtils.xori(delta, entry);
            // D[i] ← d
            paddingDatabase.setBytesData(x, entry);
            // Return δ
            List<byte[]> serverUpdatePayload = new LinkedList<>();
            serverUpdatePayload.add(IntUtils.intToByteArray(x));
            serverUpdatePayload.add(delta);
            sendOtherPartyPayload(PtoStep.SERVER_SEND_UPDATE.ordinal(), serverUpdatePayload);
            stopWatch.stop();
            long updateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, updateTime, "Server updates " + (round + 1) + "-th entry");
        }

        logPhaseInfo(PtoState.PTO_END);
    }
}
