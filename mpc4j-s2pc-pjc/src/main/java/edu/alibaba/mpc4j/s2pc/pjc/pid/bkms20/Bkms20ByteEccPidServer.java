package edu.alibaba.mpc4j.s2pc.pjc.pid.bkms20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteMulEcc;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pid.AbstractPidParty;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pid.bkms20.Bkms20ByteEccPidPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Facebook的字节椭圆曲线PID方案服务端，对应论文中的参与方C。
 *
 * @author Weiran Liu
 * @date 2022/9/13
 */
public class Bkms20ByteEccPidServer<T> extends AbstractPidParty<T> {
    /**
     * 字节椭圆曲线
     */
    private final ByteMulEcc byteMulEcc;
    /**
     * PID映射函数
     */
    private Hash pidMap;
    /**
     * k_c
     */
    private byte[] kc;
    /**
     * r_c
     */
    private byte[] rc;
    /**
     * 逆置换映射
     */
    private Map<Integer, Integer> reShuffleMap;
    /**
     * E_c，用于计算差集，本质上是一个打乱后的集合
     */
    private Set<ByteBuffer> ecSet;
    /**
     * E_p，用于计算差集，本质上是一个打乱后的集合
     */
    private Set<ByteBuffer> epSet;
    /**
     * S_p集合大小，用于数据包大小验证
     */
    private int spSize;
    /**
     * 服务端PID映射
     */
    private Map<ByteBuffer, T> serverPidMap;
    /**
     * S_c = E_c \ E_p，本质上是一个打乱后的集合
     */
    private Set<ByteBuffer> scSet;
    /**
     * PID集合
     */
    private Set<ByteBuffer> serverPidSet;

    public Bkms20ByteEccPidServer(Rpc serverRpc, Party clientParty, Bkms20ByteEccPidConfig config) {
        super(Bkms20EccPidPtoDesc.getInstance(), serverRpc, clientParty, config);
        byteMulEcc = ByteEccFactory.createMulInstance(envType);
    }

    @Override
    public void init(int maxOwnElementSetSize, int maxOtherElementSetSize) {
        setInitInput(maxOwnElementSetSize, maxOtherElementSetSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // Let k_c, r_c ←_R Z_q
        kc = byteMulEcc.randomScalar(secureRandom);
        rc = byteMulEcc.randomScalar(secureRandom);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PidPartyOutput<T> pid(Set<T> ownElementSet, int otherElementSetSize) throws MpcAbortException {
        setPtoInput(ownElementSet, otherElementSetSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int pidByteLength = PidUtils.GLOBAL_PID_BYTE_LENGTH;
        pidMap = HashFactory.createInstance(envType, pidByteLength);
        // 生成置乱映射，计算并发送U_c
        List<byte[]> ucPayload = generateUcPayload();
        DataPacketHeader ucHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_UC.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(ucHeader, ucPayload));
        stopWatch.stop();
        long ucGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, ucGenTime);

        stopWatch.start();
        // 接收U_p，根据U_p计算E_p和V_p，存储E_p并发送V_p
        DataPacketHeader upHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_UP.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> upPayload = rpc.receive(upHeader).getPayload();
        List<byte[]> vpPayload = handleUpPayload(upPayload);
        DataPacketHeader vpHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_VP.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(vpHeader, vpPayload));
        stopWatch.stop();
        long vpGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, vpGenTime);

        stopWatch.start();
        // 接收V_c，计算自己ID对应的PID
        DataPacketHeader vcHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_VC.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> vcPayload = rpc.receive(vcHeader).getPayload();
        handleVcPayload(vcPayload);
        stopWatch.stop();
        long idMapGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, idMapGenTime);

        stopWatch.start();
        // 接收E_c，先计算S_p、S_c并发送S_p
        DataPacketHeader ecHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_EC.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> ecPayload = rpc.receive(ecHeader).getPayload();
        List<byte[]> spPayload = handleEcPayload(ecPayload);
        DataPacketHeader spHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SP.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(spHeader, spPayload));
        // 再计算并发送S_c'
        List<byte[]> scpPayload = generateScpPayload();
        DataPacketHeader scpHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SCP.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(scpHeader, scpPayload));
        stopWatch.stop();
        long scpGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, scpGenTime);

        stopWatch.start();
        // 接收S_p'，得到非自己ID对应的PID
        DataPacketHeader sppHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_SPP.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> sppPayload = rpc.receive(sppHeader).getPayload();
        handleSppPayload(sppPayload);
        stopWatch.stop();
        long sppHandleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, sppHandleTime);

        logPhaseInfo(PtoState.PTO_END);
        return new PidPartyOutput<>(pidByteLength, new HashSet<>(serverPidSet), new HashMap<>(serverPidMap));
    }

    private List<byte[]> generateUcPayload() {
        // For each c_i ∈ C, compute u_c^i = H(c_i)^{k_c}
        Stream<T> cStream = ownElementArrayList.stream();
        cStream = parallel ? cStream.parallel() : cStream;
        byte[][] uc = cStream
            .map(ciElement -> byteMulEcc.hashToCurve(ObjectUtils.objectToByteArray(ciElement)))
            .map(ci -> byteMulEcc.mul(ci, kc))
            .toArray(byte[][]::new);
        // Randomly shuffle the elements in U_c using a permutation π_c
        ArrayList<Integer> shuffleMap = IntStream.range(0, ownElementSetSize)
            .boxed()
            .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(shuffleMap, secureRandom);
        byte[][] shuffleUc = new byte[ownElementSetSize][];
        // For example, shuffleMap = [2, 0, 1, 3], input = [a_0, a_1, a_2, a_3], output = [a_2, a_0, a_1. a_3]
        for (int i = 0; i < ownElementSetSize; i++) {
            shuffleUc[i] = uc[shuffleMap.get(i)];
        }
        // Given shuffleMap = [2, 0, 1, 3], reShuffleMap = [2 -> 0, 0 -> 1, 1 -> 2, 3 -> 3]
        reShuffleMap = IntStream.range(0, ownElementSetSize)
            .boxed()
            .collect(Collectors.toMap(shuffleMap::get, Function.identity()));
        // send to P
        return Arrays.stream(shuffleUc).collect(Collectors.toList());
    }

    private List<byte[]> handleUpPayload(List<byte[]> upPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(upPayload.size() == otherElementSetSize);
        // For each u_p^i ∈ U_p, Compute e_p^i = (u_p^i)^{k_c}
        Stream<byte[]> upStream = upPayload.stream();
        upStream = parallel ? upStream.parallel() : upStream;
        List<ByteBuffer> epList = upStream
            .map(upi -> byteMulEcc.mul(upi, kc))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
        // Compute v_p^i = (e_p^i)^{r_c}
        Stream<ByteBuffer> epStream = epList.stream();
        epStream = parallel ? epStream.parallel() : epStream;
        List<byte[]> vpPayload = epStream
            .map(ByteBuffer::array)
            .map(epi -> byteMulEcc.mul(epi, rc))
            .collect(Collectors.toList());
        epSet = new HashSet<>(epList);

        return vpPayload;
    }

    private void handleVcPayload(List<byte[]> vcPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(vcPayload.size() == ownElementSetSize);
        // Shuffle back the elements of V_c using π^{−1}_c.
        byte[][] shuffleVc = vcPayload.toArray(new byte[0][]);
        byte[][] vc = new byte[ownElementSetSize][];
        for (int i = 0; i < ownElementSetSize; i++) {
            vc[i] = shuffleVc[reShuffleMap.get(i)];
        }
        reShuffleMap = null;
        // For every v_c^i ∈ V_c, let w_c^i = (v_c^i)^{r_c} and M_c[(v_c^i)^{r_c}] = c_i
        Stream<byte[]> vcStream = Arrays.stream(vc);
        vcStream = parallel ? vcStream.parallel() : vcStream;
        ByteBuffer[] wc = vcStream
            .map(vci -> byteMulEcc.mul(vci, rc))
            .map(pidMap::digestToBytes)
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        serverPidMap = IntStream.range(0, ownElementSetSize)
            .boxed()
            .collect(Collectors.toMap(index -> wc[index], index -> ownElementArrayList.get(index)));
        serverPidSet = Arrays.stream(wc).collect(Collectors.toSet());
    }

    private List<byte[]> handleEcPayload(List<byte[]> ecPayload) {
        ecSet = ecPayload.stream()
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        // Let S_p = E_p \ E_c and S_c = E_c \ E_p
        scSet = ecSet.stream()
            .filter(eci -> !epSet.contains(eci))
            .collect(Collectors.toSet());
        List<byte[]> spPayload = epSet.stream()
            .filter(epi -> !ecSet.contains(epi))
            .map(ByteBuffer::array)
            .collect(Collectors.toList());
        // 记录S_p的集合大小
        spSize = spPayload.size();
        epSet = null;
        return spPayload;
    }

    private List<byte[]> generateScpPayload() {
        // For each s_c^i ∈ S_c, s_c^i' = (s_c^i)^{r_c}
        Stream<ByteBuffer> scStream = scSet.stream();
        scStream = parallel ? scStream.parallel() : scStream;
        return scStream
            .map(ByteBuffer::array)
            .map(sci -> byteMulEcc.mul(sci, rc))
            .collect(Collectors.toList());
    }

    private void handleSppPayload(List<byte[]> sppPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(sppPayload.size() == spSize);
        // For every s_p^i ∈ S_p', let s_p^i'' = s_p^i^{r_c} and M_c[(s_p^i)^{r_c}] = ⊥
        Stream<byte[]> sppStream = sppPayload.stream();
        sppStream = parallel ? sppStream.parallel() : sppStream;
        List<ByteBuffer> dp = sppStream
            .map(sppi -> byteMulEcc.mul(sppi, rc))
            .map(pidMap::digestToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
        serverPidSet.addAll(dp);
    }
}
