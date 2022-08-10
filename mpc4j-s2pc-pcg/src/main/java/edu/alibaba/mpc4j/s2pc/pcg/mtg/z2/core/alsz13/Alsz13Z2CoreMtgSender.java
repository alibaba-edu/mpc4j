package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.alsz13;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.AbstractZ2CoreMtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotSender;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * ALSZ13-核布尔三元组生成协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/4/6
 */
public class Alsz13Z2CoreMtgSender extends AbstractZ2CoreMtgParty {
    /**
     * NC-COT协议发送方
     */
    private final NcCotSender ncCotSender;
    /**
     * NC-COT协议接收方
     */
    private final NcCotReceiver ncCotReceiver;
    /**
     * 抗关联哈希函数
     */
    private final Crhf crhf;
    /**
     * 偏置量
     */
    private int offset;
    /**
     * a0
     */
    private byte[] a0;
    /**
     * b0
     */
    private byte[] b0;
    /**
     * c0
     */
    private byte[] c0;

    public Alsz13Z2CoreMtgSender(Rpc senderRpc, Party receiverParty, Alsz13Z2CoreMtgConfig config) {
        super(Alsz13Z2CoreMtgPtoDesc.getInstance(), senderRpc, receiverParty, config);
        ncCotSender = NcCotFactory.createSender(senderRpc, receiverParty, config.getNcCotConfig());
        ncCotSender.addLogLevel();
        ncCotReceiver = NcCotFactory.createReceiver(senderRpc, receiverParty, config.getNcCotConfig());
        ncCotReceiver.addLogLevel();
        crhf = CrhfFactory.createInstance(getEnvType(), CrhfFactory.CrhfType.MMO);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        ncCotSender.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        ncCotReceiver.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        ncCotSender.setParallel(parallel);
        ncCotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        ncCotSender.addLogLevel();
        ncCotReceiver.addLogLevel();
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 初始化COT发送方
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        ncCotSender.init(delta, maxNum);
        // 初始化COT接收方
        ncCotReceiver.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Z2Triple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        initParam();
        stopWatch.stop();
        long initParamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initParamTime);

        stopWatch.start();
        firstRound();
        stopWatch.stop();
        long firstRoundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), firstRoundTime);

        stopWatch.start();
        secondRound();
        stopWatch.stop();
        long secondRoundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 3/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), secondRoundTime);

        stopWatch.start();
        // Finally, each Pi sets ci = (ai ⊙ bi) ⊕ ui ⊕ vi. This is the remaining part.
        byte[] temp = BytesUtils.and(a0, b0);
        BytesUtils.xori(c0, temp);
        stopWatch.stop();
        long generateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 4/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), generateTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return Z2Triple.create(num, a0, b0, c0);
    }

    private void initParam() {
        // 不需要初始化a0，协议执行过程会初始化
        b0 = new byte[byteNum];
        c0 = new byte[byteNum];
        // 计算偏置量
        offset = byteNum * Byte.SIZE - num;
    }

    private void firstRound() throws MpcAbortException {
        // S and R perform a silent R-OT. S obtains bits x0, x1.
        CotSenderOutput cotSenderOutput = ncCotSender.send();
        cotSenderOutput.reduce(num);
        IntStream tripleIndexIntStream = IntStream.range(0, num);
        byte[][] r0Array = new byte[num][];
        byte[][] r1Array = new byte[num][];
        tripleIndexIntStream = parallel ? tripleIndexIntStream.parallel() : tripleIndexIntStream;
        tripleIndexIntStream.forEach(tripleIndex -> {
            // S sets b = x0 ⊕ x1 and v = x0.
            r0Array[tripleIndex] = crhf.hash(cotSenderOutput.getR0(tripleIndex));
            r1Array[tripleIndex] = crhf.hash(cotSenderOutput.getR1(tripleIndex));
        });
        byte[] x1 = new byte[byteNum];
        IntStream.range(0, num).forEach(tripleIndex -> {
            // 只取每一组ROT的最高位1比特
            BinaryUtils.setBoolean(c0, offset + tripleIndex, r0Array[tripleIndex][0] % 2 == 1);
            BinaryUtils.setBoolean(b0, offset + tripleIndex, r0Array[tripleIndex][0] % 2 == 1);
            BinaryUtils.setBoolean(x1, offset + tripleIndex, r1Array[tripleIndex][0] % 2 == 1);
        });
        BytesUtils.xori(b0, x1);
    }

    private void secondRound() throws MpcAbortException {
        // S and R perform a silent R-OT. R obtains bits a and xa as output.
        CotReceiverOutput cotReceiverOutput = ncCotReceiver.receive();
        cotReceiverOutput.reduce(num);
        a0 = BinaryUtils.binaryToRoundByteArray(cotReceiverOutput.getChoices());

        IntStream tripleIndexIntStream = IntStream.range(0, num);
        tripleIndexIntStream = parallel ? tripleIndexIntStream.parallel() : tripleIndexIntStream;
        byte[][] rbArray = tripleIndexIntStream
                .mapToObj(tripleIndex -> crhf.hash(cotReceiverOutput.getRb(tripleIndex)))
                .toArray(byte[][]::new);
        // R sets u = xa
        byte[] cb = new byte[byteNum];
        IntStream.range(0, num).forEach(tripleIndex -> {
            // 只取每个ROT的最高位1比特
            BinaryUtils.setBoolean(cb, offset + tripleIndex, rbArray[tripleIndex][0] % 2 == 1);
        });
        // Finally, each Pi sets ci = (ai ⊙ bi) ⊕ ui ⊕ vi. This is the ui ⊕ vi part.
        BytesUtils.xori(c0, cb);
    }
}
