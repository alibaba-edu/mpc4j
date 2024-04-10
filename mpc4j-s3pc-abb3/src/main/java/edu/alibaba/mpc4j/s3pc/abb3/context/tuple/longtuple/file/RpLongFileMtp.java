package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.file;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.FileUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.FilePtoWorkType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.AbstractRpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env.RpLongEnvParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.RpLongMtg;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.RpLongMtgFactory;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * replicated 3p sharing zl64 mt provider in file mode
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public class RpLongFileMtp extends AbstractRpLongMtp implements RpLongMtp {
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
    private final RpLongMtg rpLongMtg;
    /**
     * storing the tuple files
     */
    List<String> allFileName;

    public RpLongFileMtp(Rpc rpc, RpLongFileMtpConfig config, S3pcCrProvider crProvider) {
        super(RpLongFileMtpPtoDesc.getInstance(), rpc, config);
        workType = config.getPtoWorkType();
        fileDir = config.getFileDir();
        RpLongEnvParty envParty = new RpLongEnvParty(rpc, config.getRpZl64EnvConfig(), crProvider);
        rpLongMtg = RpLongMtgFactory.createParty(rpc, config.getRpZl64MtgConfig(), envParty);
        addSubPto(rpLongMtg);
    }

    @Override
    public void init(long totalData) {
        if (partyState.equals(PartyState.INITIALIZED)) {
            return;
        }
        rpLongMtg.init(totalData);
        initState();
    }

    @Override
    public RpLongEnvParty getEnv() {
        return rpLongMtg.getEnv();
    }

    @Override
    protected void fillBuffer() throws MpcAbortException {
        if (allFileName == null) {
            updateFileList();
        }
        if (allFileName.isEmpty()) {
            throw new MpcAbortException("no enough zl64 mt files in the directory");
        }
        long[][] tmp = FileUtils.readFileIntoLongMatrix(allFileName.get(0), !workType.equals(FilePtoWorkType.TEST));
        if (!workType.equals(FilePtoWorkType.TEST)) {
            allFileName.remove(0);
        }
        assert (tmp.length & 1) == 0;
        buffer = IntStream.range(0, tmp.length >> 1).mapToObj(each ->
                TripletRpLongVector.create(Arrays.copyOfRange(tmp, each << 1, (each << 1) + 2)))
            .toArray(TripletRpLongVector[]::new);
    }

    /**
     * list the z2 mt files in the current directory
     */
    public void updateFileList() throws MpcAbortException {
        File folder = new File(fileDir);
        File[] files = folder.listFiles();
        if (files == null) {
            throw new MpcAbortException("no files in zl64 mt directory");
        } else {
            allFileName = Arrays.stream(files)
                .filter(file -> file.isFile() && file.getName().endsWith(".txt") && file.getName().startsWith(rpc.ownParty().getPartyId() + "_rpZl64mt_"))
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
        // the file name: rpZl64mt_[index].txt
        int rounds = 1 << rpLongMtg.getLogOfRound();
        for (int tmp = 0; tmp < rounds; tmp++, fileStartIndexes++) {
            String fileName = fileDir + File.separator + rpc.ownParty().getPartyId() + "_rpZl64mt_" + fileStartIndexes + ".txt";
            TripletRpLongVector[] data = Arrays.stream(rpLongMtg.genMtOnline())
                .map(TripletRpLongVector::mergeWithPadding).toArray(TripletRpLongVector[]::new);
            long[][] vec = new long[6][];
            for (int i = 0; i < data.length; i++) {
                vec[i << 1] = data[i].getVectors()[0].getElements();
                vec[(i << 1) + 1] = data[i].getVectors()[1].getElements();
            }
            FileUtils.writeFile(vec, fileName);
        }
        updateFileList();
    }
}
