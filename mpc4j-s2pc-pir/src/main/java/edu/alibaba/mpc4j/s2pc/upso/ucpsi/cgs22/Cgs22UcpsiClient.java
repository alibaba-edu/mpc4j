package edu.alibaba.mpc4j.s2pc.upso.ucpsi.cgs22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.NoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmSender;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.AbstractUcpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiClientOutput;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.cgs22.Cgs22UcpsiPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.UrbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.UrbopprfFactory;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.UrbopprfReceiver;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * CGS22 unbalanced circuit PSI client.
 *
 * @author Weiran Liu
 * @date 2023/4/19
 */
public class Cgs22UcpsiClient extends AbstractUcpsiClient {
    /**
     * unbalanced related batched OPPRF receiver
     */
    private final UrbopprfReceiver urbopprfReceiver;
    /**
     * private set membership sender
     */
    private final PsmSender psmSender;
    /**
     * d
     */
    private final int d;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * cuckoo hash num
     */
    private final int hashNum;
    /**
     * β
     */
    private int beta;
    /**
     * l_psm
     */
    private int psmL;
    /**
     * l_sm in byte
     */
    private int psmByteL;
    /**
     * l_opprf in byte
     */
    private int opprfByteL;
    /**
     * cuckoo hash bin
     */
    private NoStashCuckooHashBin<ByteBuffer> cuckooHashBin;

    public Cgs22UcpsiClient(Rpc serverRpc, Party clientParty, Cgs22UcpsiConfig config) {
        super(Cgs22UcpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        UrbopprfConfig rbopprfConfig = config.getUrbopprfConfig();
        urbopprfReceiver = UrbopprfFactory.createReceiver(serverRpc, clientParty, rbopprfConfig);
        addSubPtos(urbopprfReceiver);
        d = rbopprfConfig.getD();
        psmSender = PsmFactory.createSender(serverRpc, clientParty, config.getPsmConfig());
        addSubPtos(psmSender);
        cuckooHashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        hashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxClientElementSize, int serverElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, serverElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // β = (1 + ε) * n_c
        beta = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize);
        // point_num = hash_num * n_s
        int pointNum = hashNum * serverElementSize;
        // l_psm = σ + log_2(d * β)
        psmL = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2((long) d * beta);
        psmByteL = CommonUtils.getByteLength(psmL);
        // l_opprf = σ + log_2(point_num)
        int opprfL = Math.max(CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(pointNum), psmL);
        opprfByteL = CommonUtils.getByteLength(opprfL);
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, paramTime);

        stopWatch.start();
        urbopprfReceiver.init(opprfL, beta, pointNum);
        stopWatch.stop();
        long opprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, opprfTime);

        stopWatch.start();
        psmSender.init(psmL, d, beta);
        stopWatch.stop();
        long psmTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, psmTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public UcpsiClientOutput psi(Set<ByteBuffer> clientElementSet) throws MpcAbortException {
        setPtoInput(clientElementSet);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive hash keys
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        stopWatch.start();
        handleCuckooHashKeyPayload(cuckooHashKeyPayload);
        stopWatch.stop();
        long binTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, binTime, "Server hashes elements");

        stopWatch.start();
        // unbalanced related batch opprf
        byte[][] inputArray = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                HashBinEntry<ByteBuffer> item = cuckooHashBin.getHashBinEntry(batchIndex);
                byte[] itemBytes = cuckooHashBin.getHashBinEntry(batchIndex).getItemByteArray();
                return ByteBuffer.allocate(itemBytes.length + Integer.BYTES)
                    .put(itemBytes)
                    .putInt(item.getHashIndex())
                    .array();
            })
            .toArray(byte[][]::new);
        byte[][][] targetArrays = urbopprfReceiver.opprf(inputArray);
        stopWatch.stop();
        long opprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, opprfTime);

        stopWatch.start();
        targetArrays = Arrays.stream(targetArrays)
            .map(targetArray ->
                Arrays.stream(targetArray)
                    .map(target -> {
                        byte[] truncatedTarget = new byte[psmByteL];
                        System.arraycopy(target, opprfByteL - psmByteL, truncatedTarget, 0, psmByteL);
                        BytesUtils.reduceByteArray(truncatedTarget, psmL);
                        return truncatedTarget;
                    })
                    .toArray(byte[][]::new))
            .toArray(byte[][][]::new);
        // private set membership
        SquareZ2Vector z1 = psmSender.psm(psmL, targetArrays);
        // create the table
        ByteBuffer[] table = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                HashBinEntry<ByteBuffer> item = cuckooHashBin.getHashBinEntry(batchIndex);
                if (item.getHashIndex() == HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                    return ByteBuffer.wrap(new byte[0]);
                } else {
                    return item.getItem();
                }
            })
            .toArray(ByteBuffer[]::new);
        UcpsiClientOutput clientOutput = new UcpsiClientOutput(table, z1);
        cuckooHashBin = null;
        stopWatch.stop();
        long psmTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, psmTime);

        logPhaseInfo(PtoState.PTO_END);
        return clientOutput;
    }

    private void handleCuckooHashKeyPayload(List<byte[]> cuckooHashKeyPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(cuckooHashKeyPayload.size() == hashNum);
        byte[][] hashKeys = cuckooHashKeyPayload.toArray(new byte[0][]);
        cuckooHashBin = CuckooHashBinFactory.createNoStashCuckooHashBin(envType, cuckooHashBinType, clientElementSize, hashKeys);
        cuckooHashBin.insertItems(clientElementArrayList);
        cuckooHashBin.insertPaddingItems(secureRandom);
    }
}
