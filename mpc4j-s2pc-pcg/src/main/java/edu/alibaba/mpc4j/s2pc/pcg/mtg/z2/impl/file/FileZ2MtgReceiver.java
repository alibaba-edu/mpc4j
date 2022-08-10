package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.file;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.AbstractZ2MtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;
import org.bouncycastle.util.encoders.Hex;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 文件布尔三元组生成协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/5/22
 */
public class FileZ2MtgReceiver extends AbstractZ2MtgParty {
    /**
     * 文件名分隔符
     */
    private static final String FILE_NAME_SEPARATOR = "_";
    /**
     * 文件路径
     */
    private final String filePath;
    /**
     * 布尔三元组
     */
    private Z2Triple z2Triple;

    public FileZ2MtgReceiver(Rpc receiverRpc, Party senderParty, FileZ2MtgConfig config) {
        super(FileZ2MtgPtoDesc.getInstance(), receiverRpc, senderParty, config);
        filePath = config.getFilePath();
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(maxRoundNum, updateNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        String recvZ2MtgFileName = getFileName();
        File recvZ2MtgFile = new File(filePath + File.separator + recvZ2MtgFileName);
        if (!recvZ2MtgFile.exists()) {
            // 如果没有文件，则生成
            int maxUpdateByteNum = CommonUtils.getByteLength(updateNum);
            Kdf kdf = KdfFactory.createInstance(envType);
            Prg prg = PrgFactory.createInstance(envType, maxUpdateByteNum);
            // 生成三元组
            byte[] a0Key = kdf.deriveKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
            byte[] a1Key = kdf.deriveKey(a0Key);
            byte[] b0Key = kdf.deriveKey(a1Key);
            byte[] b1Key = kdf.deriveKey(b0Key);
            byte[] c0Key = kdf.deriveKey(b1Key);
            // 生成a0、b0、c0
            byte[] a0 = prg.extendToBytes(a0Key);
            BytesUtils.reduceByteArray(a0, maxRoundNum);
            byte[] b0 = prg.extendToBytes(b0Key);
            BytesUtils.reduceByteArray(b0, maxRoundNum);
            byte[] c0 = prg.extendToBytes(c0Key);
            BytesUtils.reduceByteArray(c0, maxRoundNum);
            // 生成a1、b1
            byte[] a1 = prg.extendToBytes(a1Key);
            BytesUtils.reduceByteArray(a1, maxRoundNum);
            byte[] b1 = prg.extendToBytes(b1Key);
            BytesUtils.reduceByteArray(b1, maxRoundNum);
            // 计算c1
            byte[] c1 = BytesUtils.xor(a0, a1);
            byte[] b = BytesUtils.xor(b0, b1);
            BytesUtils.andi(c1, b);
            BytesUtils.xori(c1, c0);
            try {
                FileWriter fileWriter = new FileWriter(recvZ2MtgFile);
                PrintWriter printWriter = new PrintWriter(fileWriter, true);
                String a1String = Hex.toHexString(a1);
                printWriter.println(a1String);
                String b1String = Hex.toHexString(b1);
                printWriter.println(b1String);
                String c1String = Hex.toHexString(c1);
                printWriter.println(c1String);
                printWriter.close();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalStateException("Cannot write data into file " + recvZ2MtgFile.getAbsolutePath());
            }
        }
        stopWatch.stop();
        long generateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 0/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), generateTime);

        try {
            stopWatch.start();
            // 开始读取
            InputStreamReader inputStreamReader = new InputStreamReader(
                new FileInputStream(recvZ2MtgFile), StandardCharsets.UTF_8
            );
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String a1String = bufferedReader.readLine();
            byte[] a1 = Hex.decode(a1String);
            String b1String = bufferedReader.readLine();
            byte[] b1 = Hex.decode(b1String);
            String c1String = bufferedReader.readLine();
            byte[] c1 = Hex.decode(c1String);
            z2Triple = Z2Triple.create(updateNum, a1, b1, c1);
            bufferedReader.close();
            inputStreamReader.close();
            stopWatch.stop();
            long readTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), readTime);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Cannot read data from file " + recvZ2MtgFile.getAbsolutePath());
        }
        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Z2Triple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        Z2Triple receiverOutput = z2Triple.split(num);
        stopWatch.stop();
        long tripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), tripleTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private String getFileName() {
        return "Z2_MTG"
            + FILE_NAME_SEPARATOR + maxRoundNum
            + FILE_NAME_SEPARATOR + updateNum
            + FILE_NAME_SEPARATOR + ownParty().getPartyId() + ".txt";
    }
}
