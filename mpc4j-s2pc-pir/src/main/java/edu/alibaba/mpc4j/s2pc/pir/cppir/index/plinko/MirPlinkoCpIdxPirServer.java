package edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.StreamCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko.MirPlinkoCpIdxPirPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MIR-based Plinko client-preprocessing index PIR server.
 *
 * @author Weiran Liu
 * @date 2024/10/11
 */
public class MirPlinkoCpIdxPirServer extends AbstractCpIdxPirServer implements StreamCpIdxPirServer {
    /**
     * specific Q
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
     * block num (in byte)
     */
    private int byteBlockNum;
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

    public MirPlinkoCpIdxPirServer(Rpc serverRpc, Party clientParty, MirPlinkoCpIdxPirConfig config) {
        super(MirPlinkoCpIdxPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        this.specificQ = config.getQ();
    }

    @Override
    public void init(NaiveDatabase database, int matchBatchNum) throws MpcAbortException {
        setInitInput(database, matchBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        blockSize = MirPlinkoCpIdxPirUtils.getBlockSize(n);
        blockNum = MirPlinkoCpIdxPirUtils.getBlockNum(n);
        assert blockSize * blockNum >= n
            : "BlockSize * BlockNum must be greater than or equal to n (" + n + "): " + blockSize * blockNum;
        byteBlockNum = CommonUtils.getByteLength(blockNum);
        // pad the database
        byte[][] paddingData = new byte[blockSize * blockNum][byteL];
        for (int x = 0; x < n; x++) {
            paddingData[x] = database.getBytesData(x);
        }
        for (int x = n; x < blockSize * blockNum; x++) {
            paddingData[x] = BytesUtils.randomByteArray(byteL, l, secureRandom);
        }
        paddingDatabase = ZlDatabase.create(l, paddingData);
        int defaultQ = MirPlinkoCpIdxPirUtils.getRoundQueryNum(n);
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
        // stream sending the database
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
        MpcAbortPreconditions.checkArgument(BytesUtils.isFixedReduceByteArray(bitVectorByteArray, byteBlockNum, blockNum));
        BitVector bitVector = BitVectorFactory.create(blockNum, bitVectorByteArray);
        // the offset vector r
        byte[] queryByteArray = queryRequestPayload.get(1);
        MpcAbortPreconditions.checkArgument(queryByteArray.length == Short.BYTES * blockNum);
        ByteBuffer queryByteBuffer = ByteBuffer.wrap(queryByteArray);
        int[] offsets = new int[blockNum];
        for (int chunkId = 0; chunkId < blockNum; chunkId++) {
            offsets[chunkId] = queryByteBuffer.getShort();
        }
        // compute two possible parities
        byte[] leftParity = new byte[byteL];
        byte[] rightParity = new byte[byteL];
        int leftSetSize = 0;
        int rightSetSize = 0;
        for (int blockId = 0; blockId < blockNum; blockId++) {
            int x = blockId * blockSize + offsets[blockId];
            byte[] entry = paddingDatabase.getBytesData(x);
            if (bitVector.get(blockId)) {
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
