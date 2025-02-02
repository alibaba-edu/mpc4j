package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.StreamCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint.PianoHint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * PIANO client-specific preprocessing index PIR server.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class PianoCpIdxPirServer extends AbstractCpIdxPirServer implements StreamCpIdxPirServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PianoCpIdxPirServer.class);
    /**
     * chunk size
     */
    private int chunkSize;
    /**
     * chunk num
     */
    private int chunkNum;
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

    public PianoCpIdxPirServer(Rpc serverRpc, Party clientParty, PianoCpIdxPirConfig config) {
        super(PianoCpIdxPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(NaiveDatabase database, int maxBatchNum) throws MpcAbortException {
        setInitInput(database, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        chunkSize = PianoCpIdxPirUtils.getChunkSize(n);
        chunkNum = PianoCpIdxPirUtils.getChunkNum(n);
        assert chunkSize * chunkNum >= n
            : "chunkSize * chunkNum must be greater than or equal to n (" + n + "): " + chunkSize * chunkNum;
        // pad the database
        byte[][] paddingData = new byte[chunkSize * chunkNum][byteL];
        for (int x = 0; x < n; x++) {
            paddingData[x] = database.getBytesData(x);
        }
        for (int x = n; x < chunkSize * chunkNum; x++) {
            paddingData[x] = BytesUtils.randomByteArray(byteL, l, secureRandom);
        }
        paddingDatabase = ZlDatabase.create(l, paddingData);
        roundQueryNum = PianoCpIdxPirUtils.getRoundQueryNum(n);
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
        for (int blockChunkId = 0; blockChunkId < chunkNum; blockChunkId += PianoHint.PRP_BLOCK_OFFSET_NUM) {
            LOGGER.info("preprocessing {} / {}", blockChunkId + 1, chunkNum);
            // send batched chunks
            for (int chunkId = blockChunkId; chunkId < blockChunkId + PianoHint.PRP_BLOCK_OFFSET_NUM && chunkId < chunkNum; chunkId++) {
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
        logStepInfo(PtoState.INIT_STEP, 1, 1, streamTime, "Server handles " + chunkNum + " chunk");
    }

    @Override
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        for (int i = 0; i < batchNum; i++) {
            LOGGER.info("query {} / {}", i + 1, batchNum);
            List<byte[]> queryRequestPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
            int queryRequestSize = queryRequestPayload.size();
            MpcAbortPreconditions.checkArgument(queryRequestSize == 0 || queryRequestSize == 1);
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
        byte[] queryByteArray = queryRequestPayload.get(0);
        MpcAbortPreconditions.checkArgument(queryByteArray.length == Short.BYTES * (chunkNum - 1));
        ByteBuffer queryByteBuffer = ByteBuffer.wrap(queryByteArray);
        int[] puncturedOffsets = new int[chunkNum - 1];
        for (int i = 0; i < chunkNum - 1; i++) {
            puncturedOffsets[i] = queryByteBuffer.getShort();
        }
        // Start to run PossibleParities
        // build the first guess assuming the punctured position is 0
        byte[] parity = new byte[byteL];
        for (int i = 0; i < chunkNum - 1; i++) {
            int chunkId = i + 1;
            int x = chunkId * chunkSize + puncturedOffsets[i];
            byte[] entry = paddingDatabase.getBytesData(x);
            BytesUtils.xori(parity, entry);
        }
        // init all guesses
        byte[][] guesses = new byte[chunkNum][byteL];
        // set the first guess
        guesses[0] = BytesUtils.clone(parity);
        // now build the rest of the guesses
        for (int misChunkId = 1; misChunkId < chunkNum; misChunkId++) {
            // The hole originally is in the (i-1)-th chunk. Now the hole should be in the i-th chunk.
            int offset = puncturedOffsets[misChunkId - 1];
            int oldX = misChunkId * chunkSize + offset;
            int newX = (misChunkId - 1) * chunkSize + offset;
            byte[] entryOld = paddingDatabase.getBytesData(oldX);
            byte[] entryNew = paddingDatabase.getBytesData(newX);
            BytesUtils.xori(parity, entryOld);
            BytesUtils.xori(parity, entryNew);
            guesses[misChunkId] = BytesUtils.clone(parity);
        }
        // respond the query
        ByteBuffer byteBuffer = ByteBuffer.allocate(chunkNum * byteL);
        for (int i = 0; i < chunkNum; i++) {
            byteBuffer.put(guesses[i]);
        }
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
