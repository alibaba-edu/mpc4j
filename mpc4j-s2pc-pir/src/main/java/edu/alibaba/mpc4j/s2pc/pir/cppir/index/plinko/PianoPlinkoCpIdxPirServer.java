package edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.StreamCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko.PianoPlinkoCpIdxPirPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Piano-based client-specific preprocessing index PIR server.
 *
 * @author Weiran Liu
 * @date 2024/10/9
 */
public class PianoPlinkoCpIdxPirServer extends AbstractCpIdxPirServer implements StreamCpIdxPirServer {
    /**
     * q
     */
    private final int specificQ;
    /**
     * block size
     */
    private int blockSize;
    /**
     * block num
     */
    private int blockNum;
    /**
     * padding database
     */
    private ZlDatabase paddingDatabase;
    /**
     * query num for each preprocessing round
     */
    private int q;
    /**
     * current query num
     */
    private int currentQueryNum;

    public PianoPlinkoCpIdxPirServer(Rpc serverRpc, Party clientParty, PianoPlinkoCpIdxPirConfig config) {
        super(PianoPlinkoCpIdxPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        specificQ = config.getQ();
    }

    @Override
    public void init(NaiveDatabase database, int maxBatchNum) throws MpcAbortException {
        setInitInput(database, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        blockSize = PianoPlinkoCpIdxPirUtils.getBlockSize(n);
        blockNum = PianoPlinkoCpIdxPirUtils.getBlockNum(n);
        assert blockSize * blockNum >= n
            : "blockSize * blockNum must be greater than or equal to n (" + n + "): " + blockSize * blockNum;
        // pad the database
        byte[][] paddingData = new byte[blockSize * blockNum][byteL];
        for (int x = 0; x < n; x++) {
            paddingData[x] = database.getBytesData(x);
        }
        for (int x = n; x < blockSize * blockNum; x++) {
            paddingData[x] = BytesUtils.randomByteArray(byteL, l, secureRandom);
        }
        paddingDatabase = ZlDatabase.create(l, paddingData);
        int defaultQ = PianoPlinkoCpIdxPirUtils.getRoundQueryNum(n);
        q = specificQ < 0 ? defaultQ : specificQ;
        stopWatch.stop();
        long paddingTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 0, 1, paddingTime,
            String.format(
                "Client sets params: n = %d, BlockSize = %d, BlockNum = %d, n (pad) = %d, default Q = %d, specific Q = %d, q = %d",
                n, blockSize, blockNum, blockSize * blockNum, defaultQ, specificQ, q
            )
        );

        // preprocessing
        preprocessing();

        logPhaseInfo(PtoState.INIT_END);
    }

    private void preprocessing() throws MpcAbortException {
        stopWatch.start();
        // stream sending the database. Note that here we need to send database by block, so that client only needs to
        // invoke iPRF.F^{-1} once for each Block ID (α).
        for (int blockId = 0; blockId < blockNum; blockId++) {
            // concatenate database into the whole byte buffer
            ByteBuffer byteBuffer = ByteBuffer.allocate(byteL * blockSize);
            for (int offset = 0; offset < blockSize; offset++) {
                byteBuffer.put(paddingDatabase.getBytesData(blockId * blockSize + offset));
            }
            List<byte[]> streamRequestPayload = Collections.singletonList(byteBuffer.array());
            sendOtherPartyPayload(PtoStep.SERVER_SEND_STREAM_DATABASE_REQUEST.ordinal(), streamRequestPayload);
            // receive response
            List<byte[]> streamResponsePayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_STREAM_DATABASE_RESPONSE.ordinal());
            MpcAbortPreconditions.checkArgument(streamResponsePayload.isEmpty());
        }
        // reset current query num
        currentQueryNum = 0;
        stopWatch.stop();
        long streamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, streamTime, "Server handles " + blockNum + " block(s)");
    }

    @Override
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        for (int i = 0; i < batchNum; i++) {
            List<byte[]> queryRequestPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
            int queryRequestSize = queryRequestPayload.size();
            MpcAbortPreconditions.checkArgument(queryRequestSize == 0 || queryRequestSize == 1);
            if (queryRequestSize == 0) {
                sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), new LinkedList<>());
            } else {
                respondActualQuery(queryRequestPayload);
            }
        }

        logPhaseInfo(PtoState.PTO_END);
    }

    private void respondActualQuery(List<byte[]> queryRequestPayload) throws MpcAbortException {
        stopWatch.start();
        // Parse (o_0, ..., o_{n/w} − 2) ← q
        byte[] queryByteArray = queryRequestPayload.get(0);
        MpcAbortPreconditions.checkArgument(queryByteArray.length == Short.BYTES * (blockNum - 1));
        ByteBuffer queryByteBuffer = ByteBuffer.wrap(queryByteArray);
        int[] os = new int[blockNum - 1];
        for (int i = 0; i < blockNum - 1; i++) {
            os[i] = queryByteBuffer.getShort();
        }
        // init all guesses
        byte[][] rs = new byte[blockNum][byteL];
        // r_0 ← ⊕_{i ∈ [n/w − 1]} {D[o_{i + 1} + (i + 1) · n/w]}. This guesses that the hold is the first one.
        byte[] parity = new byte[byteL];
        for (int i = 0; i < blockNum - 1; i++) {
            byte[] entry = paddingDatabase.getBytesData(os[i] + (i + 1) * blockSize);
            BytesUtils.xori(parity, entry);
        }
        rs[0] = BytesUtils.clone(parity);
        // For i = 0, ..., n/w − 1
        for (int missBlockId = 1; missBlockId < blockNum; missBlockId++) {
            // The hole originally is in the (i-1)-th block. Now the hole should be in the i-th block.
            int oi = os[missBlockId - 1];
            // prev ← D[o_i + i · n/w]
            byte[] prev = paddingDatabase.getBytesData((missBlockId - 1) * blockSize + oi);
            // next ← D[o_i + (i + 1) · n/w]
            byte[] next = paddingDatabase.getBytesData(missBlockId * blockSize + oi);
            // r_{i+1} ← r_i ⊕ prev ⊕ next
            BytesUtils.xori(parity, prev);
            BytesUtils.xori(parity, next);
            rs[missBlockId] = BytesUtils.clone(parity);
        }
        // Return (r_0, ..., r_{n/w − 1})
        ByteBuffer byteBuffer = ByteBuffer.allocate(blockNum * byteL);
        for (int i = 0; i < blockNum; i++) {
            byteBuffer.put(rs[i]);
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
        if (currentQueryNum > q) {
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
