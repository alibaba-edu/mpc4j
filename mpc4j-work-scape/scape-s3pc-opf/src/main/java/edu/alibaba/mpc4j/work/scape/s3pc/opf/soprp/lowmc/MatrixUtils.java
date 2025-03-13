package edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Arrays;

/**
 * compute the rank and inverse matrix for a bit matrix
 *
 * @author Feng Han
 * @date 2025/2/14
 */
public class MatrixUtils {
    /**
     * get the rank for a matrix
     */
    public static int rankOfMatrix(boolean[][] mat) {
        int size = mat[0].length;
        boolean[][] matrix = new boolean[mat.length][];
        for(int i = 0;i < mat.length;i++){
            matrix[i] = Arrays.copyOf(mat[i], size);
        }
        int row = 0;
        for(int col = 1;col <= size;++col){
            if(!matrix[row][size-col]){
                int r = row;
                while (r < matrix.length && !matrix[r][size-col]){
                    ++r;
                }
                if(r >= matrix.length){
                    continue;
                }else {
                    boolean[] temp = matrix[row];
                    matrix[row] = matrix[r];
                    matrix[r] = temp;
                }
            }
            for(int i = row + 1;i < matrix.length;++i){
                if(matrix[i][size-col]){
                    for(int j = 0;j < size;j++){
                        matrix[i][j] ^= matrix[row][j];
                    }
                }
            }
            ++row;
            if(row == Math.min(size, matrix.length)){
                break;
            }
        }
        return row;
    }

    /**
     * get the inverse matrix of input bit matrix
     */
    public static boolean[][] invertMatrix(boolean[][] mat) {
        int size = mat[0].length;
        boolean[][] matrix = new boolean[mat.length][mat[0].length];
        for(int i = 0;i < mat.length;i++){
            System.arraycopy(mat[i], 0, matrix[i], 0, size);
        }
        boolean[][] invMat = new boolean[size][size];
        if(size != matrix.length){
            throw new IllegalArgumentException("not a square matrix");
        }else{
            for(int i = 0;i < size;i++){
                invMat[i][i] = true;
            }
            int row = 0;
            for(int col = 0;col < size;++col){
                if(!matrix[row][col]){
                    int r = row + 1;
                    while (r < matrix.length && !matrix[r][col]){
                        ++r;
                    }
                    if(r >= matrix.length){
                        continue;
                    }else {
                        boolean[] temp = matrix[row];
                        matrix[row] = matrix[r];
                        matrix[r] = temp;
                        temp = invMat[row];
                        invMat[row] = invMat[r];
                        invMat[r] = temp;
                    }
                }
                for (int i = row+1; i < matrix.length;++i){
                    if(matrix[i][col]){
                        for(int j = 0;j < size;j++){
                            matrix[i][j] ^= matrix[row][j];
                            invMat[i][j] ^= invMat[row][j];
                        }
                    }
                }
                ++row;
            }
            for(int col = size;col > 0;--col){
                for(int r = 0;r < col-1;++r){
                    if(matrix[r][col-1]){
                        for(int j = 0;j < size;j++){
                            matrix[r][j] ^= matrix[col-1][j];
                            invMat[r][j] ^= invMat[col-1][j];
                        }
                    }
                }
            }
        }
        return invMat;
    }

    /**
     * if the input matrix are not inverse matrix throw MpcAbortException
     */
    public static void testInvMatrix(boolean[][] invLinMatrix, boolean[][] linMatrix) throws MpcAbortException {
        boolean flag = true;
        boolean[][] invMat = MatrixUtils.invertMatrix(invLinMatrix);
        for(int i = 0;i < invMat.length;i++){
            if (!Arrays.equals(invMat[i], linMatrix[i])) {
                flag = false;
                break;
            }
        }
        if(!flag){
            throw new MpcAbortException("the input matrix are not inverse matrix");
        }
    }
}
