package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.alsz13;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.AbstractZ2CoreMtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotSender;

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
        addSubPto(ncCotSender);
        ncCotReceiver = NcCotFactory.createReceiver(senderRpc, receiverParty, config.getNcCotConfig());
        addSubPto(ncCotReceiver);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

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
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Z2Triple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        initParam();
        stopWatch.stop();
        long initParamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, initParamTime);

        stopWatch.start();
        firstRound();
        stopWatch.stop();
        long firstRoundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, firstRoundTime);

        stopWatch.start();
        secondRound();
        stopWatch.stop();
        long secondRoundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, secondRoundTime);

        stopWatch.start();
        // Finally, each Pi sets ci = (ai ⊙ bi) ⊕ ui ⊕ vi. This is the remaining part.
        byte[] temp = BytesUtils.and(a0, b0);
        BytesUtils.xori(c0, temp);
        stopWatch.stop();
        long generateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, generateTime);

        logPhaseInfo(PtoState.PTO_END);
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
        RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfType.MMO, cotSenderOutput);
        byte[][] r0Array = rotSenderOutput.getR0Array();
        byte[][] r1Array = rotSenderOutput.getR1Array();
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
        RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfType.MMO, cotReceiverOutput);
        a0 = BinaryUtils.binaryToRoundByteArray(cotReceiverOutput.getChoices());
        byte[][] rbArray = rotReceiverOutput.getRbArray();
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
