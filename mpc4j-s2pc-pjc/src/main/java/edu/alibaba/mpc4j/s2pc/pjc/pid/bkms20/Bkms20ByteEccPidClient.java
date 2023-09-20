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
 * Facebook的字节椭圆曲线PID方案客户端，对应论文中的参与方P。
 *
 * @author Weiran Liu
 * @date 2022/9/13
 */
public class Bkms20ByteEccPidClient<T> extends AbstractPidParty<T> {
    /**
     * 字节椭圆曲线
     */
    private final ByteMulEcc byteMulEcc;
    /**
     * PID映射函数
     */
    private Hash pidMap;
    /**
     * k_p
     */
    private byte[] kp;
    /**
     * r_p
     */
    private byte[] rp;
    /**
     * 逆置换映射
     */
    private Map<Integer, Integer> reShuffleMap;
    /**
     * E_c，用于计算差集，本质上是一个打乱后的集合
     */
    private Set<ByteBuffer> ecSet;
    /**
     * 客户端PID映射
     */
    private Map<ByteBuffer, T> clientPidMap;
    /**
     * 客户端PID集合
     */
    private Set<ByteBuffer> clientPidSet;

    public Bkms20ByteEccPidClient(Rpc clientRpc, Party serverParty, Bkms20ByteEccPidConfig config) {
        super(Bkms20EccPidPtoDesc.getInstance(), clientRpc, serverParty, config);
        byteMulEcc = ByteEccFactory.createMulInstance(envType);
    }

    @Override
    public void init(int maxOwnElementSetSize, int maxOtherElementSetSize) throws MpcAbortException {
        setInitInput(maxOwnElementSetSize, maxOtherElementSetSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // Let k_c, r_c ←_R Z_q
        kp = byteMulEcc.randomScalar(secureRandom);
        rp = byteMulEcc.randomScalar(secureRandom);
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
        // 生成置乱映射，计算并发送U_p
        List<byte[]> upPayload = generateUpPayload();
        DataPacketHeader upHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_UP.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(upHeader, upPayload));
        stopWatch.stop();
        long upGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, upGenTime);

        stopWatch.start();
        // 接收U_c，根据U_c计算E_c和V_c，存储E_c，发送V_c
        DataPacketHeader ucHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_UC.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> ucPayload = rpc.receive(ucHeader).getPayload();
        List<byte[]> vcPayload = handleUcPayload(ucPayload);
        DataPacketHeader vcHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_VC.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(vcHeader, vcPayload));
        stopWatch.stop();
        long vcGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, vcGenTime);

        stopWatch.start();
        // 接收V_p，计算自己ID对应的PID
        DataPacketHeader vpHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_VP.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> vpPayload = rpc.receive(vpHeader).getPayload();
        handleVpPayload(vpPayload);
        // 发送E_c
        List<byte[]> ecPayload = generateEcPayload();
        DataPacketHeader ecHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_EC.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(ecHeader, ecPayload));
        stopWatch.stop();
        long ecGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, ecGenTime);

        stopWatch.start();
        // 接收S_p，计算并发送S_p'
        DataPacketHeader spHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SP.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> spPayload = rpc.receive(spHeader).getPayload();
        List<byte[]> sppPayload = handleSpPayload(spPayload);
        DataPacketHeader sppHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_SPP.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sppHeader, sppPayload));
        stopWatch.stop();
        long sppGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, sppGenTime);

        stopWatch.start();
        // 接收S_c'，得到非自己ID对应的PID
        DataPacketHeader scpHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SCP.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> scpPayload = rpc.receive(scpHeader).getPayload();
        handleScpPayload(scpPayload);
        stopWatch.stop();
        long scpHandleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, scpHandleTime);

        logPhaseInfo(PtoState.PTO_END);
        return new PidPartyOutput<>(pidByteLength, new HashSet<>(clientPidSet), new HashMap<>(clientPidMap));
    }

    private List<byte[]> generateUpPayload() {
        // For each p_i ∈ P, compute u_p^i = H(p_i)^{k_p}
        Stream<T> pStream = ownElementArrayList.stream();
        pStream = parallel ? pStream.parallel() : pStream;
        byte[][] up = pStream
            .map(piElement -> byteMulEcc.hashToCurve(ObjectUtils.objectToByteArray(piElement)))
            .map(pi -> byteMulEcc.mul(pi, kp))
            .toArray(byte[][]::new);
        // Randomly shuffle the elements in U_p using a permutation π_p
        ArrayList<Integer> shuffleMap = IntStream.range(0, ownElementSetSize)
            .boxed()
            .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(shuffleMap, secureRandom);
        byte[][] shuffleUp = new byte[ownElementSetSize][];
        // For example, shuffleMap = [2, 0, 1, 3], input = [a_0, a_1, a_2, a_3], output = [a_2, a_0, a_1. a_3]
        for (int i = 0; i < ownElementSetSize; i++) {
            shuffleUp[i] = up[shuffleMap.get(i)];
        }
        // Given shuffleMap = [2, 0, 1, 3], reShuffleMap = [2 -> 0, 0 -> 1, 1 -> 2, 3 -> 3]
        reShuffleMap = IntStream.range(0, ownElementSetSize)
            .boxed()
            .collect(Collectors.toMap(shuffleMap::get, Function.identity()));
        // send to P
        return Arrays.stream(shuffleUp).collect(Collectors.toList());
    }

    private List<byte[]> handleUcPayload(List<byte[]> ucPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(ucPayload.size() == otherElementSetSize);
        // For each u_c^i ∈ U_c, Compute e_c^i = (u_c^i)^{k_p}
        Stream<byte[]> ucStream = ucPayload.stream();
        ucStream = parallel ? ucStream.parallel() : ucStream;
        List<ByteBuffer> ecList = ucStream
            .map(uci -> byteMulEcc.mul(uci, kp))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
        // Compute v_p^i = (e_p^i)^{r_c}
        Stream<ByteBuffer> ecStream = ecList.stream();
        ecStream = parallel ? ecStream.parallel() : ecStream;
        List<byte[]> vcPayload = ecStream
            .map(ByteBuffer::array)
            .map(eci -> byteMulEcc.mul(eci, rp))
            .collect(Collectors.toList());
        ecSet = new HashSet<>(ecList);

        return vcPayload;
    }

    private void handleVpPayload(List<byte[]> vpPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(vpPayload.size() == ownElementSetSize);
        // Shuffle back the elements of V_p using π^{−1}_p.
        byte[][] shuffleVp = vpPayload.toArray(new byte[0][]);
        byte[][] vp = new byte[ownElementSetSize][];
        for (int i = 0; i < ownElementSetSize; i++) {
            vp[i] = shuffleVp[reShuffleMap.get(i)];
        }
        reShuffleMap = null;
        // For every v_p^i ∈ V_p, let w_p^i = (v_p^i)^{r_p} and M_p[(v_p^i)^{r_p}] = p_i
        Stream<byte[]> vpStream = Arrays.stream(vp);
        vpStream = parallel ? vpStream.parallel() : vpStream;
        ByteBuffer[] wp = vpStream
            .map(vpi -> byteMulEcc.mul(vpi, rp))
            .map(pidMap::digestToBytes)
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        clientPidMap = IntStream.range(0, ownElementSetSize)
            .boxed()
            .collect(Collectors.toMap(index -> wp[index], index -> ownElementArrayList.get(index)));
        clientPidSet = Arrays.stream(wp).collect(Collectors.toSet());
    }

    private List<byte[]> generateEcPayload() {
        List<byte[]> ecPayload = ecSet.stream()
            .map(ByteBuffer::array)
            .collect(Collectors.toList());
        ecSet = null;
        // Randomly shuffle the elements in E_c
        Collections.shuffle(ecPayload, secureRandom);
        return ecPayload;
    }

    private List<byte[]> handleSpPayload(List<byte[]> spPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(spPayload.size() <= ownElementSetSize);
        // For each s_p^i ∈ S_p, s_p^i' = (s_p^i)^{r_p}
        Stream<byte[]> spStream = spPayload.stream();
        spStream = parallel ? spStream.parallel() : spStream;
        return spStream
            .map(spi -> byteMulEcc.mul(spi, rp))
            .collect(Collectors.toList());
    }

    private void handleScpPayload(List<byte[]> scpPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(scpPayload.size() <= otherElementSetSize);
        // For every s_c^i ∈ S_c', let s_c^i'' = s_c^i^{r_p} and M_p[(s_p^i)^{r_p}] = ⊥
        Stream<byte[]> scpStream = scpPayload.stream();
        scpStream = parallel ? scpStream.parallel() : scpStream;
        List<ByteBuffer> dc = scpStream
            .map(scpi -> byteMulEcc.mul(scpi, rp))
            .map(pidMap::digestToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
        clientPidSet.addAll(dc);
    }
}
