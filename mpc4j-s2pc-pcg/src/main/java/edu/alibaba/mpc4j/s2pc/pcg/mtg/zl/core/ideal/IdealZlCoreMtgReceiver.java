package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ideal;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.AbstractZlCoreMtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ideal.IdealZlCoreMtgPtoDesc.PtoStep;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 理想核l比特三元组生成协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public class IdealZlCoreMtgReceiver extends AbstractZlCoreMtgParty {
    /**
     * 生成a0的伪随机函数
     */
    private Prf a0Prf;
    /**
     * 生成b0的伪随机函数
     */
    private Prf b0Prf;
    /**
     * 生成c0的伪随机函数
     */
    private Prf c0Prf;
    /**
     * 生成a1的伪随机函数
     */
    private Prf a1Prf;
    /**
     * 生成b1的伪随机函数
     */
    private Prf b1Prf;

    public IdealZlCoreMtgReceiver(Rpc receiverRpc, Party senderParty, IdealZlCoreMtgConfig config) {
        super(IdealZlCoreMtgPtoDesc.getInstance(), receiverRpc, senderParty, config);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        DataPacketHeader rootKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ROOT_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> rootKeyPayload = rpc.receive(rootKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(rootKeyPayload.size() == 1);
        byte[] rootKey = rootKeyPayload.remove(0);
        stopWatch.stop();
        long rootKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), rootKeyTime);

        stopWatch.start();
        Kdf kdf = KdfFactory.createInstance(envType);
        byte[] key = kdf.deriveKey(rootKey);
        a0Prf = PrfFactory.createInstance(envType, byteL);
        a0Prf.setKey(key);
        key = kdf.deriveKey(key);
        b0Prf = PrfFactory.createInstance(envType, byteL);
        b0Prf.setKey(key);
        key = kdf.deriveKey(key);
        c0Prf = PrfFactory.createInstance(envType, byteL);
        c0Prf.setKey(key);
        key = kdf.deriveKey(key);
        a1Prf = PrfFactory.createInstance(envType, byteL);
        a1Prf.setKey(key);
        key = kdf.deriveKey(key);
        b1Prf = PrfFactory.createInstance(envType, byteL);
        b1Prf.setKey(key);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public ZlTriple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        ZlTriple receiverOutput = generateZlTriple();
        stopWatch.stop();
        long generateTripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), generateTripleTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private ZlTriple generateZlTriple() {
        BigInteger[] a1s = new BigInteger[num];
        BigInteger[] b1s = new BigInteger[num];
        BigInteger[] c1s = new BigInteger[num];
        IntStream numIntStream = IntStream.range(0, num);
        numIntStream = parallel ? numIntStream.parallel() : numIntStream;
        numIntStream.forEach(index -> {
            byte[] seed = ByteBuffer.allocate(Long.BYTES + Integer.BYTES)
                .putLong(extraInfo)
                .putInt(index)
                .array();
            // 生成a0，b0，c0
            byte[] a0Bytes = a0Prf.getBytes(seed);
            BigInteger a0 = BigIntegerUtils.byteArrayToBigInteger(a0Bytes).and(mask);
            byte[] b0Bytes = b0Prf.getBytes(seed);
            BigInteger b0 = BigIntegerUtils.byteArrayToBigInteger(b0Bytes).and(mask);
            byte[] c0Bytes = c0Prf.getBytes(seed);
            BigInteger c0 = BigIntegerUtils.byteArrayToBigInteger(c0Bytes).and(mask);
            // 生成a1，b1
            byte[] a1Bytes = a1Prf.getBytes(seed);
            a1s[index] = BigIntegerUtils.byteArrayToBigInteger(a1Bytes).and(mask);
            byte[] b1Bytes = b1Prf.getBytes(seed);
            b1s[index] = BigIntegerUtils.byteArrayToBigInteger(b1Bytes).and(mask);
            // 计算c1
            c1s[index] = a0.add(a1s[index]).multiply(b0.add(b1s[index])).and(mask).subtract(c0).and(mask);
        });

        return ZlTriple.create(l, num, a1s, b1s, c1s);
    }
}
