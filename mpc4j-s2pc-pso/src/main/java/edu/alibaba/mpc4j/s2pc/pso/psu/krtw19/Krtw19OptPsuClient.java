package edu.alibaba.mpc4j.s2pc.pso.psu.krtw19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.EmptyPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePoly;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuClient;
import edu.alibaba.mpc4j.s2pc.pso.oprf.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * KRTW19-OPT-PSU协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/02/20
 */
public class Krtw19OptPsuClient extends AbstractPsuClient {
    /**
     * RMPT协议的OPRF发送方
     */
    private final OprfSender rpmtOprfSender;
    /**
     * PEQT协议的OPRF接收方
     */
    private final OprfReceiver peqtOprfReceiver;
    /**
     * 核COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * 流水线执行数量
     */
    private final int pipeSize;
    /**
     * 抗关联哈希函数
     */
    private final Crhf crhf;
    /**
     * 桶哈希函数密钥
     */
    private byte[][] hashBinKeys;
    /**
     * 桶数量（β）
     */
    private int binNum;
    /**
     * 最大桶大小（m）
     */
    private int maxBinSize;
    /**
     * 服务端元素哈希桶
     */
    private EmptyPadHashBin<ByteBuffer> hashBin;
    /**
     * 多项式插值服务
     */
    private Gf2ePoly gf2ePoly;
    /**
     * 有限域字节长度
     */
    private int fieldByteLength;
    /**
     * 有限域哈希函数
     */
    private Hash finiteFieldHash;
    /**
     * PEQT输出哈希函数
     */
    private Hash peqtHash;
    /**
     * 加密伪随机数生成器
     */
    private Prg encPrg;

    public Krtw19OptPsuClient(Rpc clientRpc, Party serverParty, Krtw19OptPsuConfig config) {
        super(Krtw19OptPsuPtoDesc.getInstance(), clientRpc, serverParty, config);
        rpmtOprfSender = OprfFactory.createOprfSender(clientRpc, serverParty, config.getRpmtOprfConfig());
        rpmtOprfSender.addLogLevel();
        peqtOprfReceiver = OprfFactory.createOprfReceiver(clientRpc, serverParty, config.getPeqtOprfConfig());
        peqtOprfReceiver.addLogLevel();
        coreCotReceiver = CoreCotFactory.createReceiver(clientRpc, serverParty, config.getCoreCotConfig());
        coreCotReceiver.addLogLevel();
        pipeSize = config.getPipeSize();
        crhf = CrhfFactory.createInstance(getEnvType(), CrhfType.MMO);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        rpmtOprfSender.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        peqtOprfReceiver.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
        coreCotReceiver.setTaskId(taskIdPrf.getLong(2, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        rpmtOprfSender.setParallel(parallel);
        peqtOprfReceiver.setParallel(parallel);
        coreCotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        rpmtOprfSender.addLogLevel();
        peqtOprfReceiver.addLogLevel();
        coreCotReceiver.addLogLevel();
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 初始化各个子协议
        rpmtOprfSender.init(Krtw19PsuUtils.MAX_BIN_NUM);
        peqtOprfReceiver.init(Krtw19PsuUtils.MAX_BIN_NUM);
        coreCotReceiver.init(Krtw19PsuUtils.MAX_BIN_NUM);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        DataPacketHeader keysHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Krtw19OptPsuPtoDesc.PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> keysPayload = rpc.receive(keysHeader).getPayload();
        MpcAbortPreconditions.checkArgument(keysPayload.size() == 1);
        // 初始化哈希桶密钥
        hashBinKeys = new byte[1][];
        hashBinKeys[0] = keysPayload.remove(0);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyTime);

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Set<ByteBuffer> psu(Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize, elementByteLength);
        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        initParams();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        Set<ByteBuffer> union = new HashSet<>(serverElementSize + clientElementSize);
        for (int binColumnIndex = 0; binColumnIndex < maxBinSize; binColumnIndex++) {
            info("{}{} Client Step 2/2 ({}/{}) begin", ptoStepLogPrefix, getPtoDesc().getPtoName(),
                (binColumnIndex + 1), maxBinSize
            );
            union.addAll(handleBinColumn());
            info("{}{} Client Step 2/2 ({}/{}) end", ptoStepLogPrefix, getPtoDesc().getPtoName(),
                (binColumnIndex + 1), maxBinSize
            );
        }
        union.addAll(clientElementSet);
        union.remove(botElementByteBuffer);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return union;
    }

    private void initParams() {
        // 取得服务单和客户端元素数量的最大值
        int n = Math.max(serverElementSize, clientElementSize);
        // 设置桶参数
        binNum = Krtw19PsuUtils.getBinNum(n);
        maxBinSize = Krtw19PsuUtils.getMaxBinSize(n);
        hashBin = new EmptyPadHashBin<>(envType, binNum, maxBinSize, clientElementSize, hashBinKeys);
        // 向桶插入元素
        hashBin.insertItems(clientElementArrayList);
        // 放置特殊的元素\bot，并进行随机置乱
        hashBin.insertPaddingItems(botElementByteBuffer);
        // 设置有限域比特长度σ = λ + log(β * (m + 1)^2)
        int fieldBitLength = Krtw19PsuUtils.getFiniteFieldBitLength(binNum, maxBinSize);
        fieldByteLength = fieldBitLength / Byte.SIZE;
        // 设置有限域哈希
        finiteFieldHash = HashFactory.createInstance(envType, fieldByteLength);
        // 设置多项式运算服务
        gf2ePoly = Gf2ePolyFactory.createInstance(envType, fieldBitLength);
        // 初始化PEQT哈希
        int peqtByteLength = Krtw19PsuUtils.getPeqtByteLength(binNum, maxBinSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        // 设置加密伪随机数生成器
        encPrg = PrgFactory.createInstance(envType, elementByteLength);
    }

    private Set<ByteBuffer> handleBinColumn() throws MpcAbortException {
        stopWatch.start();
        // 调用OPRF得到密钥
        OprfSenderOutput rpmtOprfSenderOutput = rpmtOprfSender.oprf(binNum);
        stopWatch.stop();
        long qTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2.1/2.4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), qTime);

        stopWatch.start();
        // 初始化s
        byte[][] ss = IntStream.range(0, binNum)
            .mapToObj(binIndex -> {
                byte[] s = new byte[fieldByteLength];
                secureRandom.nextBytes(s);
                return s;
            })
            .toArray(byte[][]::new);
        // Pipeline过程，先执行整除倍，最后再循环一遍
        int pipeTime = binNum / pipeSize;
        int round;
        for (round = 0; round < pipeTime; round++) {
            // 构造多项式
            byte[][][] polys = generatePolys(rpmtOprfSenderOutput, ss, round * pipeSize, (round + 1) * pipeSize);
            List<byte[]> polyPayload = Arrays.stream(polys)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
            // 发送多项式
            DataPacketHeader polyHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Krtw19OptPsuPtoDesc.PtoStep.CLIENT_SEND_POLYS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(polyHeader, polyPayload));
            extraInfo++;
        }
        int remain = binNum - round * pipeSize;
        if (remain > 0) {
            // 构造多项式
            byte[][][] polys = generatePolys(rpmtOprfSenderOutput, ss, round * pipeSize, binNum);
            List<byte[]> polyPayload = Arrays.stream(polys)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
            // 发送多项式
            DataPacketHeader polyHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Krtw19OptPsuPtoDesc.PtoStep.CLIENT_SEND_POLYS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(polyHeader, polyPayload));
            extraInfo++;
        }
        stopWatch.stop();
        long polyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2.2/2.4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), polyTime);

        stopWatch.start();
        // 以s为输入调用OPRF
        OprfReceiverOutput peqtOprfReceiverOutput = peqtOprfReceiver.oprf(ss);
        IntStream sIntStream = IntStream.range(0, binNum);
        sIntStream = parallel ? sIntStream.parallel() : sIntStream;
        ByteBuffer[] sOprfs = sIntStream
            .mapToObj(peqtOprfReceiverOutput::getPrf)
            .map(peqtHash::digestToBytes)
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        // 接收sStarsOprf
        DataPacketHeader sStarOprfHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Krtw19OptPsuPtoDesc.PtoStep.SERVER_SEND_S_STAR_OPRFS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> sStarOprfPayload = rpc.receive(sStarOprfHeader).getPayload();
        MpcAbortPreconditions.checkArgument(sStarOprfPayload.size() == binNum);
        ByteBuffer[] sStarOprfs = sStarOprfPayload.stream()
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        // 对比并得到结果
        boolean[] choiceArray = new boolean[binNum];
        for (int binIndex = 0; binIndex < binNum; binIndex++) {
            choiceArray[binIndex] = sOprfs[binIndex].equals(sStarOprfs[binIndex]);
        }
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2.3/2.4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), peqtTime);

        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(choiceArray);
        DataPacketHeader encHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Krtw19OptPsuPtoDesc.PtoStep.SERVER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> encPayload = rpc.receive(encHeader).getPayload();
        MpcAbortPreconditions.checkArgument(encPayload.size() == binNum);
        ArrayList<byte[]> encArrayList = new ArrayList<>(encPayload);
        // Y \cup Z
        IntStream decIntStream = IntStream.range(0, binNum);
        decIntStream = parallel ? decIntStream.parallel() : decIntStream;
        Set<ByteBuffer> binColumnUnion = decIntStream
            .mapToObj(binIndex -> {
                if (choiceArray[binIndex]) {
                    return botElementByteBuffer;
                } else {
                    // 密钥处理
                    byte[] key = crhf.hash(cotReceiverOutput.getRb(binIndex));
                    byte[] message = encPrg.extendToBytes(key);
                    BytesUtils.xori(message, encArrayList.get(binIndex));
                    return ByteBuffer.wrap(message);
                }
            })
            .collect(Collectors.toSet());
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2.4/2.4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), unionTime);

        return binColumnUnion;
    }

    private byte[][][] generatePolys(OprfSenderOutput rpmtOprfSenderOutput, byte[][] ss, int start, int end) {
        byte[][][] polys = new byte[end - start][][];
        IntStream binIndexStream = IntStream.range(start, end);
        binIndexStream = parallel ? binIndexStream.parallel() : binIndexStream;
        binIndexStream.forEach(binIndex -> {
            // q_i
            byte[][] qs = hashBin.getBin(binIndex).stream()
                // 从桶中取出元素
                .map(HashBinEntry::getItem)
                .map(ByteBuffer::array)
                .distinct()
                // q_i = F_k(x_i)
                .map(x -> rpmtOprfSenderOutput.getPrf(binIndex, x))
                .map(q -> finiteFieldHash.digestToBytes(q))
                .toArray(byte[][]::new);
            // 构造多项式
            polys[binIndex - start] = gf2ePoly.rootInterpolate(maxBinSize - 1, qs, ss[binIndex]);
        });

        return polys;
    }
}
