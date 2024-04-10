package edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.*;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.AbstractPmidServer;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22.Zcl22MpPmidPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuServer;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 多点ZCL22PMID协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/5/10
 */
public class Zcl22MpPmidServer<T> extends AbstractPmidServer<T> {
    /**
     * 多点OPRF发送方
     */
    private final MpOprfSender mpOprfSender;
    /**
     * 多点OPRF接收方
     */
    private final MpOprfReceiver mpOprfReceiver;
    /**
     * PSU服务端
     */
    private final PsuServer psuServer;
    /**
     * σ的OKVS类型
     */
    private final Gf2eDokvsType sigmaOkvsType;
    /**
     * 服务端σ的OKVS密钥
     */
    private byte[][] serverSigmaOkvsHashKeys;
    /**
     * 客户端σ的OKVS密钥
     */
    private byte[][] clientSigmaOkvsHashKeys;
    /**
     * PMID字节长度
     */
    private int pmidByteLength;
    /**
     * PMID映射函数
     */
    private Hash pmidMap;
    /**
     * σ的OKVS值字节长度
     */
    private int sigmaOkvsValueByteLength;
    /**
     * σ的OKVS值映射函数
     */
    private Hash sigmaOkvsValueMap;
    /**
     * k_A
     */
    private MpOprfSenderOutput kaMpOprfKey;
    /**
     * {F_{k_B}(x) | x ∈ X}
     */
    private MpOprfReceiverOutput kbMpOprfOutput;
    /**
     * 服务端元素字节数组
     */
    private byte[][] serverElementByteArrays;
    /**
     * dx
     */
    private int[] dxArray;

    public Zcl22MpPmidServer(Rpc serverRpc, Party clientParty, Zcl22MpPmidConfig config) {
        super(Zcl22MpPmidPtoDesc.getInstance(), serverRpc, clientParty, config);
        mpOprfSender = OprfFactory.createMpOprfSender(serverRpc, clientParty, config.getMpOprfConfig());
        addSubPto(mpOprfSender);
        mpOprfReceiver = OprfFactory.createMpOprfReceiver(serverRpc, clientParty, config.getMpOprfConfig());
        addSubPto(mpOprfReceiver);
        psuServer = PsuFactory.createServer(serverRpc, clientParty, config.getPsuConfig());
        addSubPto(psuServer);
        sigmaOkvsType = config.getSigmaOkvsType();
    }

    @Override
    public void init(int maxServerSetSize, int maxServerU, int maxClientSetSize, int maxClientU) throws MpcAbortException {
        setInitInput(maxServerSetSize, maxServerU, maxClientSetSize, maxClientU);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // P1作为发送方，P2作为接收方的mpOPRF
        mpOprfSender.init(maxClientSetSize);
        // P2作为发送方，P1作为接收方的mpOPRF
        mpOprfReceiver.init(maxServerSetSize);
        // PSU，P1的最大量级为自己所有的元素都在交集里，放大k1 * k2倍；P2的最大量级是自己所有的元素都在交集里，放大k1 * k2倍
        psuServer.init(maxServerSetSize * maxServerU * maxClientU, maxClientSetSize * maxServerU * maxClientU);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, initTime);

        stopWatch.start();
        List<byte[]> serverKeysPayload = new LinkedList<>();
        int okvsHashKeyNum = Gf2eDokvsFactory.getHashKeyNum(sigmaOkvsType);
        // 服务端OKVS密钥，由服务端生成
        serverSigmaOkvsHashKeys = IntStream.range(0, okvsHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] serverSigmaOkvsHashKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(serverSigmaOkvsHashKey);
                serverKeysPayload.add(serverSigmaOkvsHashKey);
                return serverSigmaOkvsHashKey;
            })
            .toArray(byte[][]::new);
        DataPacketHeader serverKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverKeysHeader, serverKeysPayload));
        stopWatch.stop();
        long serverKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, serverKeyTime);

        stopWatch.start();
        DataPacketHeader clientKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientKeysPayload = rpc.receive(clientKeysHeader).getPayload();
        MpcAbortPreconditions.checkArgument(clientKeysPayload.size() == okvsHashKeyNum);
        // 读取客户端OKVS密钥
        clientSigmaOkvsHashKeys = clientKeysPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long clientKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, clientKeyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PmidPartyOutput<T> pmid(Set<T> serverElementSet, int clientSetSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientSetSize);
        return pmid();
    }

    @Override
    public PmidPartyOutput<T> pmid(Set<T> serverElementSet, int clientSetSize, int clientU) throws MpcAbortException {
        setPtoInput(serverElementSet, clientSetSize, clientU);
        return pmid();
    }

    @Override
    public PmidPartyOutput<T> pmid(Map<T, Integer> serverElementMap, int clientSetSize) throws MpcAbortException {
        setPtoInput(serverElementMap, clientSetSize);
        return pmid();
    }

    @Override
    public PmidPartyOutput<T> pmid(Map<T, Integer> serverElementMap, int clientSetSize, int clientU) throws MpcAbortException {
        setPtoInput(serverElementMap, clientSetSize, clientU);
        return pmid();
    }

    private PmidPartyOutput<T> pmid() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        initVariables();
        oprf();
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, oprfTime);

        stopWatch.start();
        if (serverU == 1 && clientU == 1) {
            clientEmptySigmaOkvs();
        } else if (serverU == 1) {
            clientSigmaOkvs();
        } else if (clientU == 1) {
            serverSigmaOkvs();
            clientEmptySigmaOkvs();
        } else {
            serverSigmaOkvs();
            clientSigmaOkvs();
        }
        stopWatch.stop();
        long sigmaOkvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, sigmaOkvsTime);

        stopWatch.start();
        // Alice computes id_{x_i}^(j)
        Map<ByteBuffer, T> serverPmidMap = generateServerPmidMap();
        stopWatch.stop();
        long pmidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, pmidMapTime);

        stopWatch.start();
        Set<ByteBuffer> pmidSet = union(serverPmidMap);
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, unionTime);

        logPhaseInfo(PtoState.PTO_END);
        return new PmidPartyOutput<>(pmidByteLength, pmidSet, serverPmidMap);
    }

    private void initVariables() {
        pmidByteLength = PmidUtils.getPmidByteLength(serverSetSize, serverU, clientSetSize, clientU);
        pmidMap = HashFactory.createInstance(envType, pmidByteLength);
        sigmaOkvsValueByteLength = Zcl22PmidUtils.getSigmaOkvsValueByteLength(
            serverSetSize, serverU, clientSetSize, clientU
        );
        sigmaOkvsValueMap = HashFactory.createInstance(envType, sigmaOkvsValueByteLength);
        serverElementByteArrays = serverElementArrayList.stream()
            .map(ObjectUtils::objectToByteArray)
            .toArray(byte[][]::new);
    }

    private void oprf() throws MpcAbortException {
        // Alice and Bob invoke the OPRF functionality F_{oprf}.
        // Alice acts as sender and receives a PRF key k_A.
        kaMpOprfKey = mpOprfSender.oprf(clientSetSize);
        // Alice and Bob invoke another OPRF functionality F_{oprf}.
        // Alice acts as receiver with input X and receives {F_{k_B}(x) | x ∈ X}.
        kbMpOprfOutput = mpOprfReceiver.oprf(serverElementByteArrays);
    }

    private void clientEmptySigmaOkvs() {
        // 客户端没有重数，设置dy = 1
        dxArray = new int[serverSetSize];
        Arrays.fill(dxArray, 1);
    }

    private void clientSigmaOkvs() throws MpcAbortException {
        // q_{x_i}^B = H(F_{k_B}(x_i) || 0) for i ∈ [m]
        IntStream serverElementIndexStream = IntStream.range(0, serverSetSize);
        serverElementIndexStream = parallel ? serverElementIndexStream.parallel() : serverElementIndexStream;
        final byte[][] qxkbArray = serverElementIndexStream
            .mapToObj(index -> {
                byte[] fxkb = kbMpOprfOutput.getPrf(index);
                return ByteBuffer.allocate(fxkb.length + Integer.BYTES).put(fxkb).putInt(0).array();
            })
            .map(sigmaOkvsValueMap::digestToBytes)
            .toArray(byte[][]::new);
        // Alice receives σ-OKVS D^B
        DataPacketHeader clientSigmaOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_SIGMA_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientSigmaOkvsPayload = rpc.receive(clientSigmaOkvsHeader).getPayload();
        int clientSigmaOkvsM = Gf2eDokvsFactory.getM(envType, sigmaOkvsType, clientSetSize);
        MpcAbortPreconditions.checkArgument(clientSigmaOkvsPayload.size() == clientSigmaOkvsM);
        // 读取客户端σ的OKVS
        byte[][] clientSigmaOkvsStorage = clientSigmaOkvsPayload.toArray(new byte[0][]);
        Gf2eDokvs<ByteBuffer> clientSigmaOkvs = Gf2eDokvsFactory.createInstance(
            envType, sigmaOkvsType, clientSetSize, sigmaOkvsValueByteLength * Byte.SIZE, clientSigmaOkvsHashKeys
        );
        dxArray = new int[serverSetSize];
        BigInteger clientBigIntegerU = BigInteger.valueOf(clientU);
        serverElementIndexStream = IntStream.range(0, serverSetSize);
        serverElementIndexStream = parallel ? serverElementIndexStream.parallel() : serverElementIndexStream;
        serverElementIndexStream.forEach(index -> {
            // Alice computes d_x = q_{x_i}^B ⊕ Decode_H(D, x_i) for i ∈ [m].
            byte[] x = serverElementByteArrays[index];
            ByteBuffer xi = ByteBuffer.wrap(sigmaOkvsValueMap.digestToBytes(x));
            byte[] dxBytes = clientSigmaOkvs.decode(clientSigmaOkvsStorage, xi);
            BytesUtils.xori(dxBytes, qxkbArray[index]);
            BigInteger dxBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(dxBytes);
            if (BigIntegerUtils.lessOrEqual(dxBigInteger, clientBigIntegerU) && BigIntegerUtils.greater(dxBigInteger, BigInteger.ONE)) {
                // If 1 < d_i ≤ clientU, set d_i = d_i
                dxArray[index] = dxBigInteger.intValue();
            } else {
                // else, set d_i = 1
                dxArray[index] = 1;
            }
        });
    }

    private void serverSigmaOkvs() {
        // q_{x_i}^A = H(F_{k_A}(x_i) || 0) for i ∈ [m]
        Stream<byte[]> serverElementByteArrayStream = Arrays.stream(serverElementByteArrays);
        serverElementByteArrayStream = parallel ? serverElementByteArrayStream.parallel() : serverElementByteArrayStream;
        final byte[][] qxkaArray = serverElementByteArrayStream
            .map(x -> {
                byte[] fxka = kaMpOprfKey.getPrf(x);
                return ByteBuffer.allocate(fxka.length + Integer.BYTES).put(fxka).putInt(0).array();
            })
            .map(sigmaOkvsValueMap::digestToBytes)
            .toArray(byte[][]::new);
        // Alice computes an σ-OKVS D^A
        IntStream serverElementIndexStream = IntStream.range(0, serverSetSize);
        serverElementIndexStream = parallel ? serverElementIndexStream.parallel() : serverElementIndexStream;
        Map<ByteBuffer, byte[]> serverSigmaOkvsKeyValueMap = serverElementIndexStream
            .boxed()
            .collect(Collectors.toMap(
                index -> {
                    byte[] x = serverElementByteArrays[index];
                    return ByteBuffer.wrap(sigmaOkvsValueMap.digestToBytes(x));
                },
                index -> {
                    T x = serverElementArrayList.get(index);
                    int ux = serverElementMap.get(x);
                    byte[] dx;
                    if (ux == 1) {
                        // if u_j = 1, Alice selects a random c_j ← {0, 1}^σ
                        dx = new byte[sigmaOkvsValueByteLength];
                        secureRandom.nextBytes(dx);
                    } else {
                        // else defines c_j = u_j
                        dx = IntUtils.nonNegIntToFixedByteArray(ux, sigmaOkvsValueByteLength);
                    }
                    BytesUtils.xori(dx, qxkaArray[index]);
                    return dx;
                }
            ));
        Gf2eDokvs<ByteBuffer> serverSigmaOkvs = Gf2eDokvsFactory.createInstance(
            envType, sigmaOkvsType, serverSetSize, sigmaOkvsValueByteLength * Byte.SIZE, serverSigmaOkvsHashKeys
        );
        // σ的OKVS编码可以并行处理
        serverSigmaOkvs.setParallelEncode(parallel);
        byte[][] serverSigmaOkvsStorage = serverSigmaOkvs.encode(serverSigmaOkvsKeyValueMap, false);
        List<byte[]> serverSigmaOkvsPayload =  Arrays.stream(serverSigmaOkvsStorage).collect(Collectors.toList());
        DataPacketHeader serverSigmaOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SIGMA_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverSigmaOkvsHeader, serverSigmaOkvsPayload));
    }

    private Map<ByteBuffer, T> generateServerPmidMap() {
        Map<ByteBuffer, T> serverPmidMap = new ConcurrentHashMap<>(serverSetSize * serverU * clientU);
        IntStream serverElementIndexStream = IntStream.range(0, serverSetSize);
        serverElementIndexStream = parallel ? serverElementIndexStream.parallel() : serverElementIndexStream;
        serverElementIndexStream.forEach(index -> {
            T x = serverElementArrayList.get(index);
            byte[] fxka = kaMpOprfKey.getPrf(serverElementByteArrays[index]);
            byte[] fxkb = kbMpOprfOutput.getPrf(index);
            for (int j = 1; j <= serverElementMap.get(x) * dxArray[index]; j++) {
                byte[] extendPmid0 = ByteBuffer.allocate(fxka.length + Integer.BYTES)
                    .put(fxka)
                    .put(IntUtils.intToByteArray(j))
                    .array();
                extendPmid0 = pmidMap.digestToBytes(extendPmid0);
                byte[] extendPmid1 = ByteBuffer.allocate(fxkb.length + Integer.BYTES)
                    .put(fxkb)
                    .put(IntUtils.intToByteArray(j))
                    .array();
                extendPmid1 = pmidMap.digestToBytes(extendPmid1);
                BytesUtils.xori(extendPmid0, extendPmid1);
                serverPmidMap.put(ByteBuffer.wrap(extendPmid0), x);
            }
        });
        serverElementByteArrays = null;
        kaMpOprfKey = null;
        kbMpOprfOutput = null;
        dxArray = null;
        return serverPmidMap;
    }

    private Set<ByteBuffer> union(Map<ByteBuffer, T> serverPmidMap) throws MpcAbortException {
        // 双方同步对方PSU的元素数量
        List<byte[]> serverPsuSetSizePayload = new LinkedList<>();
        serverPsuSetSizePayload.add(IntUtils.intToByteArray(serverPmidMap.size()));
        DataPacketHeader serverPsuSetSizeHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Zcl22MpPmidPtoDesc.PtoStep.SERVER_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPsuSetSizeHeader, serverPsuSetSizePayload));

        DataPacketHeader clientPsuSetSizeHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Zcl22MpPmidPtoDesc.PtoStep.CLIENT_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> clientPsuSetSizePayload = rpc.receive(clientPsuSetSizeHeader).getPayload();
        MpcAbortPreconditions.checkArgument(clientPsuSetSizePayload.size() == 1);
        int clientPsuSetSize = IntUtils.byteArrayToInt(clientPsuSetSizePayload.remove(0));
        // Alice and Bob invoke the PSU functionality F_{psu}. Alice acts as sender with input ID_x.
        psuServer.psu(serverPmidMap.keySet(), clientPsuSetSize, pmidByteLength);
        // Alice receives union
        DataPacketHeader unionHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Zcl22MpPmidPtoDesc.PtoStep.CLIENT_SEND_UNION.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> unionPayload = rpc.receive(unionHeader).getPayload();
        MpcAbortPreconditions.checkArgument(unionPayload.size() >= serverSetSize);
        return unionPayload.stream()
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
    }
}
