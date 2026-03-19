package edu.alibaba.mpc4j.work.db.sketch.utils.gk;


import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.TreeMap;

public class GKBatchImpl extends AbstractGK implements GK {

    private int tableSize;
    private ArrayList<Representative> table;
    private ArrayList<Representative> buffer;
    private TreeMap<BigInteger, Integer> histogram=new TreeMap<>();

    public GKBatchImpl(float epsilon) {
        super(epsilon);
        table = new ArrayList<>();
        buffer = new ArrayList<>();
        this.tableSize = (int) Math.ceil(4/epsilon);
    }
    public GKBatchImpl(float epsilon, int threshold) {
        super(epsilon);
        table = new ArrayList<>();
        buffer = new ArrayList<>();
        this.tableSize = threshold;
    }

    private void resize(){
        int newSize = (int) (3 * (Math.log(epsilon*this.t) / Math.log(2))/ epsilon);
        this.tableSize = Math.max(newSize, this.tableSize);
    }

    @Override
    public void input(BigInteger... elements) {
        for (BigInteger element : elements) {
            input(element);
        }
    }

    @Override
    public void input(BigInteger element){
        Representative newRep=new Representative(element, t);
        t++;
        buffer.add(newRep);
        histogram.put(element, histogram.getOrDefault(element, 0)+1);
        if(buffer.size()>=this.tableSize){
            merge();
        }
    }

    private void merge(){
        if(buffer.isEmpty()){
            return;
        }
        buffer.addAll(table);
        Representative[] tmpTable = buffer.toArray(new Representative[0]);
        buffer.clear();
        Arrays.sort(tmpTable);
        int length=tmpTable.length;
        BigInteger T1=null;
        BigInteger T2=null;
        for(int i=length-1;i>=0;i--){
            Representative rep=tmpTable[i];
            if(rep.getDelta1()!=null){
                T1=rep.getDelta1().add(rep.getG1());
            }
            else if(rep.getDelta1()==null){
                tmpTable[i].setDelta1(T1==null ? BigInteger.ZERO:T1);
            }
        }
        for(int i=0;i<length;i++){
            Representative rep=tmpTable[i];
            if(rep.getDelta2()!=null){
                T2=rep.getDelta2().add(rep.getG2());
            }
            else if(rep.getDelta2()==null){
                tmpTable[i].setDelta2(T2==null? BigInteger.ZERO:T2);
            }
        }
        compress(tmpTable);
        resize();
        Representative[] compressedTable=new Representative[this.tableSize];
        int newLength= Math.min(tableSize, tmpTable.length);
        System.arraycopy(tmpTable, 0, compressedTable, 0, newLength);
        ArrayList<Representative> newTable=new ArrayList<>();
        Arrays.stream(compressedTable).filter(Objects::nonNull).forEach(newTable::add);
        this.table= newTable;
//        assertProperty();
    }

    void assertProperty(){
        Representative lastRep=null;
        BigInteger T=BigInteger.ZERO;
        for(Representative rep:table){
            BigInteger threshold=BigInteger.valueOf((long) (this.epsilon*this.t));
            assert (rep.getDelta1().add(rep.getG1()).compareTo(threshold)<=0);
            assert (rep.getDelta2().add(rep.getG2()).compareTo(threshold)<=0);
            T=T.add(rep.getG1()).add(rep.getG2()).add(BigInteger.ONE);

            BigInteger rMin=T.subtract(rep.getDelta2()).subtract(rep.getG2());
            BigInteger rMax=T.add(rep.getDelta1()).subtract(rep.getG2());
            int trueRank=getTrueRank(rep.getKey());
            assert(rMin.compareTo(BigInteger.valueOf(trueRank))<=0): "rMin: "+rMin+ " true rank: "+trueRank;
            assert(rMax.compareTo(BigInteger.valueOf(trueRank))>=0): "rMax: "+rMax+ " true rank: "+trueRank;
            if(lastRep!=null){
                assert (lastRep.getKey().compareTo(rep.getKey())<=0);
            }
            lastRep=rep;
        }
    }

    private int getTrueRank(BigInteger key){
        int accurateRes=histogram.headMap(key)
                .values().stream().reduce(0,Integer::sum)
                +histogram.get(key);
        return accurateRes;
    }

    private void compress(Representative[] tmpTable){
        int length=tmpTable.length;
        int round=LongUtils.ceilLog2(this.tableSize);
        for(int j=0;j<round;j++){
            for(int i=0;i+1<length;i+=2){
                if(testMergeable(tmpTable[i],tmpTable[i+1])){
                    Representative newRep=merging(tmpTable[i],tmpTable[i+1]);
                    if(newRep.equals(tmpTable[i])){
                        tmpTable[i+1]=null;
                    }
                    else{
                        tmpTable[i]=null;
                    }
                }
            }
            for(int i=1;i+1<length;i+=2){
                if(testMergeable(tmpTable[i],tmpTable[i+1])){
                    Representative newRep=merging(tmpTable[i],tmpTable[i+1]);
                    if(newRep.equals(tmpTable[i])){
                        tmpTable[i+1]=null;
                    }
                    else{
                        tmpTable[i]=null;
                    }
                }
            }
            compact(tmpTable);
        }
    }

    private void compact(Representative[] table){
        int length=table.length;
        for(int i=0,k=0;i<length;i++){
            if(table[i]!=null){
                while(k<=i){
                    if(table[k]==null){
                        table[k]=table[i];
                        table[i]=null;
                        k++;
                        break;
                    }
                    else{
                        k++;
                    }
                }
            }
        }
    }

    @Override
    public BigInteger query(BigInteger element){
        merge();
        BigInteger T=this.table.stream()
                .filter(e->e.getKey().compareTo(element)<=0)
                .map(e->e.getG1().add(e.getG2()).add(BigInteger.ONE))
                .reduce(BigInteger.ZERO, BigInteger::add);
        int i=binarySearch(this.table.toArray(new Representative[0]),element);
        BigInteger Tmin;
        BigInteger Tmax;
        if(i==-1){
            Tmin=T;
            Tmax=BigInteger.ONE.add(this.table.get(0).getG1().add(table.get(0).getDelta1()));
        }
        else if(i==this.table.size()-1){
            Tmin=T.subtract(this.table.get(i).getDelta2().subtract(this.table.get(i).getG2()));
            Tmax=BigInteger.valueOf(this.t);
        }
        else{
            Representative repI=this.table.get(i);
            Representative repI1=this.table.get(i+1);
            Tmin=T.subtract(repI.getDelta2()).subtract(repI.getG2());
            Tmax=T.add(repI1.getDelta1()).add(repI1.getG1()).add(BigInteger.ONE);
        }
        int trueRank=getTrueRank(element);
//        assert(Tmin.compareTo(BigInteger.valueOf(trueRank))<=0): "rMin: "+Tmin+ " true rank: "+trueRank;
//        assert(Tmax.compareTo(BigInteger.valueOf(trueRank))>=0): "rMax: "+Tmax+ " true rank: "+trueRank;
        BigInteger res=Tmax.add(Tmin).subtract(BigInteger.ONE).shiftRight(1);
        return res;
    }

    private int binarySearch(Representative[] table, BigInteger element){
        int l=0,r=table.length-1;
        if(element.compareTo(table[l].getKey())<0){
            return -1;
        }
        if(element.compareTo(table[table.length-1].getKey())>=0){
            return table.length-1;
        }
        while(l<=r){
            int mid=(l+r)/2;
            if(table[mid].getKey().compareTo(element)<=0&&table[mid+1].getKey().compareTo(element)>0){
                return mid;
            }
            else if(table[mid].getKey().compareTo(element)>0){
                r=mid-1;
            }
            else {
                l=mid+1;
            }
        }
        return -1;
    }
    public ArrayList<Representative> getTable(){
        return this.table;
    }
}