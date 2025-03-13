package edu.alibaba.mpc4j.work.scape.s3pc.opf.agg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggFnParam.AggOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.ThreePartyOpfPto;

/**
 * @author Feng Han
 * @date 2025/2/26
 */
public interface AggParty extends ThreePartyOpfPto {
    /**
     * set the usage of the protocol
     *
     * @param params the input parameters
     * @return the required tuple number
     */
    long[] setUsage(AggFnParam... params);

    /**
     * 得到aggregation的结果
     *
     * @param input     输入的表格数据
     * @param validFlag 有效标识
     * @param aggOp   aggregation类型
     */
    TripletLongVector agg(TripletLongVector input, TripletLongVector validFlag, AggOp aggOp) throws MpcAbortException;

    /**
     * 得到aggregation的结果
     *
     * @param input     输入的表格数据
     * @param validFlag 有效标识
     * @param aggOp   aggregation类型
     */
    TripletZ2Vector[] agg(TripletZ2Vector[] input, TripletZ2Vector validFlag, AggOp aggOp) throws MpcAbortException;
}
