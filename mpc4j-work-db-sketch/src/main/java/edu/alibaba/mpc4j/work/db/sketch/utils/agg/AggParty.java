package edu.alibaba.mpc4j.work.db.sketch.utils.agg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.ThreePartyOpfPto;
import edu.alibaba.mpc4j.work.db.sketch.utils.agg.AggFnParam.AggOp;
import org.apache.commons.lang3.tuple.Pair;

public interface AggParty extends ThreePartyOpfPto {
    /**
     * set the usage of the protocol
     *
     * @param params the input parameters
     * @return the required tuple number
     */
    long[] setUsage(AggFnParam... params);

    /**
     * aggregation
     *
     * @param input     input table
     * @param validFlag valid flag
     * @param aggOp     aggregation type
     */
    TripletLongVector agg(TripletLongVector input, TripletLongVector validFlag, AggOp aggOp) throws MpcAbortException;

    /**
     * aggregation
     *
     * @param input     input table
     * @param validFlag valid flag
     * @param aggOp     aggregation type
     */
    TripletZ2Vector[] agg(TripletZ2Vector[] input, TripletZ2Vector validFlag, AggOp aggOp) throws MpcAbortException;

    /**
     * obtain <the extreme value, the index of the extreme value>
     * if no valid data, return the first data
     *
     * @param input     input table
     * @param validFlag valid flag, if no valid flag, all data are valid
     * @param aggOp     aggregation type
     */
    Pair<TripletZ2Vector[], TripletZ2Vector[]> extremeIndex(TripletZ2Vector[] input, AggOp aggOp, TripletZ2Vector... validFlag) throws MpcAbortException;

    /**
     * obtain <the extreme value, the Indicator vector of the extreme value(1 for extreme value, 0 for other values)>
     * if no valid data, return the first data
     *
     * @param input     input table
     * @param validFlag valid flag, if no valid flag, all data are valid
     * @param aggOp     aggregation type
     */
    Pair<TripletZ2Vector[], TripletZ2Vector> extremeIndicator(TripletZ2Vector[] input, AggOp aggOp, TripletZ2Vector... validFlag) throws MpcAbortException;
}
