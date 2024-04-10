package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.file;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.FileUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.FilePtoWorkType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.AbstractRpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env.RpZ2EnvParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.RpZ2Mtg;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.RpZ2MtgFactory;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * replicated 3p sharing z2 mt provider in file mode
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public class RpZ2FileMtp extends AbstractRpZ2Mtp implements RpZ2Mtp {
    /**
     * work type
     */
    private final FilePtoWorkType workType;
    /**
     * mtg config
     */
    private final String fileDir;
    /**
     * replicated Z2 multiplication tuple generator
     */
    private final RpZ2Mtg rpZ2Mtg;
    /**
     * storing the tuple files
     */
    List<String> allFileName;

    public RpZ2FileMtp(Rpc rpc, RpZ2FileMtpConfig config, S3pcCrProvider crProvider) {
        super(RpZ2FileMtpPtoDesc.getInstance(), rpc, config);
        workType = config.getPtoWorkType();
        fileDir = config.getFileDir();
        RpZ2EnvParty envParty = new RpZ2EnvParty(rpc, config.getRpZ2EnvConfig(), crProvider);
        rpZ2Mtg = RpZ2MtgFactory.createParty(rpc, config.getRpZ2MtgConfig(), envParty);
        addSubPto(rpZ2Mtg);
    }

    @Override
    public void init(long totalBit) {
        if (partyState.equals(PartyState.INITIALIZED)) {
            return;
        }
        rpZ2Mtg.init(totalBit);
        initState();
    }

    @Override
    public RpZ2EnvParty getEnv() {
        return rpZ2Mtg.getEnv();
    }

    @Override
    protected void fillBuffer() throws MpcAbortException {
        if (allFileName == null) {
            updateFileList();
        }
        if (allFileName.isEmpty()) {
            throw new MpcAbortException("no enough z2mt files in the directory");
        }
        BitVector[] tmp = FileUtils.readFileIntoBitVectors(allFileName.get(0), !workType.equals(FilePtoWorkType.TEST));
        if (!workType.equals(FilePtoWorkType.TEST)) {
            allFileName.remove(0);
        }
        assert (tmp.length & 1) == 0;
        buffer = IntStream.range(0, tmp.length >> 1).mapToObj(each ->
                TripletRpZ2Vector.create(Arrays.copyOfRange(tmp, each << 1, (each << 1) + 2)))
            .toArray(TripletRpZ2Vector[]::new);
    }

    /**
     * list the z2 mt files in the current directory
     */
    public void updateFileList() throws MpcAbortException {
        File folder = new File(fileDir);
        File[] files = folder.listFiles();
        if (files == null) {
            throw new MpcAbortException("no files in z2mt directory");
        } else {
            allFileName = Arrays.stream(files)
                .filter(file -> file.isFile() && file.getName().endsWith(".txt") && file.getName().startsWith(rpc.ownParty().getPartyId() + "_rpZ2mt_"))
                .map(File::getPath).collect(Collectors.toList());
        }
        Collections.sort(allFileName);
    }

    /**
     * generate the tuples online and write them into the files
     *
     * @param fileStartIndexes the start index of files
     */
    public void writeFiles(int fileStartIndexes) throws MpcAbortException {
        if (workType.equals(FilePtoWorkType.ONLY_READ)) {
            throw new MpcAbortException("current mode only support read tuples");
        }
        int rounds = 1 << rpZ2Mtg.getLogOfRound();
        for (int tmp = 0; tmp < rounds; tmp++, fileStartIndexes++) {
            String fileName = fileDir + File.separator + rpc.ownParty().getPartyId() + "_rpZ2mt_" + fileStartIndexes + ".txt";
            TripletRpZ2Vector[] data = Arrays.stream(rpZ2Mtg.genMtOnline())
                .map(TripletRpZ2Vector::mergeWithPadding).toArray(TripletRpZ2Vector[]::new);
            BitVector[] bitVec = new BitVector[6];
            for (int i = 0; i < data.length; i++) {
                bitVec[i << 1] = data[i].getBitVectors()[0];
                bitVec[(i << 1) + 1] = data[i].getBitVectors()[1];
            }
            FileUtils.writeFile(bitVec, fileName);
        }
        updateFileList();
    }
}
