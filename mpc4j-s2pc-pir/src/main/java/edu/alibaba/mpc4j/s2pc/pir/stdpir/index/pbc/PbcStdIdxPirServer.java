package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.pbc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.IntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.ArraySimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory.IntCuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pir.IdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.AbstractStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.pbc.PbcStdIdxPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.pbc.PbcStdIdxPirPtoDesc.getInstance;

/**
 * probabilistic batch code (PBC) batch index PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class PbcStdIdxPirServer extends AbstractStdIdxPirServer implements IdxPirServer {
    /**
     * cuckoo hash bin type
     */
    private final IntCuckooHashBinType cuckooHashBinType;
    /**
     * PBC index PIR server
     */
    private PbcableStdIdxPirServer[] server;
    /**
     * bin num
     */
    private int binNum;
    /**
     * PBC index PIR config
     */
    private final PbcableStdIdxPirConfig pbcableStdIdxPirConfig;

    public PbcStdIdxPirServer(Rpc serverRpc, Party clientParty, PbcStdIdxPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        cuckooHashBinType = config.getCuckooHashBinType();
        pbcableStdIdxPirConfig = config.getPbcStdIdxPirConfig();
    }

    @Override
    public void init(NaiveDatabase database, int maxBatchNum) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);
        // here we cannot directly invoke setInitInput, in which would change the party state to be initialized
        // so that we cannot add sub-protocols.
        checkInitInput(database, maxBatchNum);

        stopWatch.start();
        List<byte[]> serverKeys = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal());
        int hashNum = IntCuckooHashBinFactory.getHashNum(cuckooHashBinType);
        binNum = IntCuckooHashBinFactory.getBinNum(cuckooHashBinType, maxBatchNum);
        byte[][] hashKeys = BlockUtils.randomBlocks(hashNum, secureRandom);
        NaiveDatabase[] binDatabase = generateSimpleHashBin(database, hashKeys);
        sendOtherPartyPayload(
            PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), Arrays.stream(hashKeys).collect(Collectors.toList())
        );
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, hashTime);

        stopWatch.start();
        // init single index PIR server
        server = new PbcableStdIdxPirServer[binNum];
        for (int i = 0; i < binNum; i++) {
            server[i] = StdIdxPirFactory.createPbcableServer(getRpc(), otherParty(), pbcableStdIdxPirConfig);
            addSubPto(server[i]);
            server[i].init(serverKeys, binDatabase[i], 1);
        }
        stopWatch.stop();
        long initIndexPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initIndexPirTime);

        initState();
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < binNum; i++) {
            server[i].answer();
        }
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates response");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * generate simple hash bin.
     *
     * @param hashKeys hash keys.
     * @param database database.
     * @return bin database.
     */
    private NaiveDatabase[] generateSimpleHashBin(NaiveDatabase database, byte[][] hashKeys) {
        int[] totalIndex = IntStream.range(0, n).toArray();
        IntHashBin intHashBin = new ArraySimpleIntHashBin(envType, binNum, n, hashKeys);
        intHashBin.insertItems(totalIndex);
        int maxBinSize = IntStream.range(0, binNum).map(intHashBin::binSize).max().orElse(0);
        byte[][][] paddingCompleteHashBin = new byte[binNum][maxBinSize][byteL];
        byte[] paddingEntry = BytesUtils.randomByteArray(byteL, l, secureRandom);
        for (int i = 0; i < binNum; i++) {
            int size = intHashBin.binSize(i);
            for (int j = 0; j < size; j++) {
                paddingCompleteHashBin[i][j] = database.getBytesData(intHashBin.getBin(i)[j]);
            }
            int paddingNum = maxBinSize - size;
            for (int j = 0; j < paddingNum; j++) {
                paddingCompleteHashBin[i][j + size] = BytesUtils.clone(paddingEntry);
            }
        }
        return IntStream.range(0, binNum)
            .mapToObj(i -> NaiveDatabase.create(l, paddingCompleteHashBin[i]))
            .toArray(NaiveDatabase[]::new);
    }
}