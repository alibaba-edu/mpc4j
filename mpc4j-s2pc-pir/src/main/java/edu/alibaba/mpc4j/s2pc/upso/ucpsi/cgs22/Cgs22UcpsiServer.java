package edu.alibaba.mpc4j.s2pc.upso.ucpsi.cgs22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmReceiver;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.AbstractUcpsiServer;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.cgs22.Cgs22UcpsiPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.UrbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.UrbopprfFactory;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.UrbopprfSender;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CGS22 unbalanced circuit PSI server.
 *
 * @author Weiran Liu
 * @date 2023/4/20
 */
public class Cgs22UcpsiServer<T> extends AbstractUcpsiServer<T> {
    /**
     * unbalanced related batch OPPRF sender
     */
    private final UrbopprfSender urbopprfSender;
    /**
     * private set membership receiver
     */
    private final PdsmReceiver pdsmReceiver;
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
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * simple hash bin
     */
    private RandomPadHashBin<T> simpleHashBin;
    /**
     * target array
     */
    private byte[][] targetArray;
    /**
     * input arrays
     */
    private byte[][][] inputArrays;
    /**
     * target arrays
     */
    private byte[][][] targetArrays;

    public Cgs22UcpsiServer(Rpc serverRpc, Party clientParty, Cgs22UcpsiConfig config) {
        super(Cgs22UcpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        UrbopprfConfig urbopprfConfig = config.getUrbopprfConfig();
        urbopprfSender = UrbopprfFactory.createSender(serverRpc, clientParty, urbopprfConfig);
        d = urbopprfConfig.getD();
        addSubPtos(urbopprfSender);
        pdsmReceiver = PdsmFactory.createReceiver(serverRpc, clientParty, config.getPsmConfig());
        addSubPtos(pdsmReceiver);
        cuckooHashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        hashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(Set<T> serverElementSet, int maxClientElementSize) throws MpcAbortException {
        setInitInput(serverElementSet, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init batched OPPRF, where β_max = (1 + ε) * n_c, max_point_num = hash_num * n_s
        beta = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize);
        int pointNum = hashNum * serverElementSize;
        // l_psm = σ + log_2(d * β_max)
        psmL = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2((long) d * beta);
        int psmByteL = CommonUtils.getByteLength(psmL);
        // l_opprf = σ + log_2(max_point_num)
        int opprfL = Math.max(CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(pointNum), psmL);
        int opprfByteL = CommonUtils.getByteLength(opprfL);
        // simple hash
        hashKeys = CommonUtils.generateRandomKeys(hashNum, secureRandom);
        generateRbopprfInputs(opprfL);
        stopWatch.stop();
        long binTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, binTime, "Sender hash elements");

        stopWatch.start();
        // initialize unbalanced related batch opprf
        urbopprfSender.init(opprfL, inputArrays, targetArrays);
        inputArrays = null;
        targetArrays = null;
        stopWatch.stop();
        long opprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, opprfTime);

        stopWatch.start();
        targetArray = Arrays.stream(targetArray)
            .map(target -> {
                byte[] truncatedTarget = new byte[psmByteL];
                System.arraycopy(target, opprfByteL - psmByteL, truncatedTarget, 0, psmByteL);
                BytesUtils.reduceByteArray(truncatedTarget, psmL);
                return truncatedTarget;
            })
            .toArray(byte[][]::new);
        // initialize private set membership
        pdsmReceiver.init(psmL, d, beta);
        stopWatch.stop();
        long psmTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, psmTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector psi() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // P1 sends hash keys
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));

        stopWatch.start();
        // unbalanced related batch opprf
        urbopprfSender.opprf();
        stopWatch.stop();
        long opprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, opprfTime);

        stopWatch.start();
        // private set membership
        SquareZ2Vector z0 = pdsmReceiver.pdsm(psmL, d, targetArray);
        targetArray = null;
        stopWatch.stop();
        long psmTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, psmTime);

        logPhaseInfo(PtoState.PTO_END);
        return z0;
    }

    private void generateRbopprfInputs(int l) {
        int byteL = CommonUtils.getByteLength(l);
        simpleHashBin = new RandomPadHashBin<>(envType, beta, serverElementSize, hashKeys);
        simpleHashBin.insertItems(serverElementArrayList);
        // P1 generates the input arrays
        inputArrays = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                ArrayList<HashBinEntry<T>> bin = new ArrayList<>(simpleHashBin.getBin(batchIndex));
                return bin.stream()
                    .map(entry -> {
                        byte[] itemBytes = entry.getItemByteArray();
                        return ByteBuffer.allocate(itemBytes.length + Integer.BYTES)
                            .put(itemBytes)
                            .putInt(entry.getHashIndex())
                            .array();
                    })
                    .toArray(byte[][]::new);
            })
            .toArray(byte[][][]::new);
        simpleHashBin = null;
        // P1 samples uniformly random and independent target values t_1, ..., t_β ∈ {0,1}^κ
        targetArray = IntStream.range(0, beta)
            .mapToObj(batchIndex -> BytesUtils.randomByteArray(byteL, l, secureRandom)).toArray(byte[][]::new);
        targetArrays = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                int batchPointNum = inputArrays[batchIndex].length;
                byte[][] copyTargetArray = new byte[batchPointNum][byteL];
                for (int i = 0; i < batchPointNum; i++) {
                    copyTargetArray[i] = BytesUtils.clone(targetArray[batchIndex]);
                }
                return copyTargetArray;
            })
            .toArray(byte[][][]::new);
    }
}
