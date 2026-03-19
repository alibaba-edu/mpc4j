package edu.alibaba.mpc4j.work.db.sketch.utils.gk;

import java.math.BigInteger;
import java.util.PriorityQueue;
import java.util.TreeMap;

public class GKNaiveImpl extends AbstractGK implements GK {
    private final TreeMap<BigInteger, Representative> representatives;
    private final PriorityQueue<Pair> queue;

    public GKNaiveImpl(float epsilon) {
        super(epsilon);
        representatives = new TreeMap<>();
        queue = new PriorityQueue<>();
    }

    @Override
    public void input(BigInteger... elements) {
        for (BigInteger element : elements) {
            input(element);
        }
    }

    @Override
    public void input(BigInteger element) {
        Representative rep = new Representative(element, t);
        t += 1;
        representatives.put(element, rep);
        Representative left = representatives.lowerEntry(element).getValue();
        Representative right = representatives.higherEntry(element).getValue();
        rep.setDelta1(right != null ? right.getDelta1().add(right.getG1()) : BigInteger.ZERO);
        rep.setDelta2(left != null ? left.getDelta2().add(left.getG2()) : BigInteger.ZERO);

        if (left != null) {
            queue.add(new Pair(left, rep));
        }
        if (right != null) {
            queue.add(new Pair(rep, right));
        }
        if (left != null && right != null) {
            queue.remove(new Pair(left, right));
        }
        compress();
    }

    @Override
    public BigInteger query(BigInteger element) {
        Representative e1 = representatives.ceilingEntry(element).getValue();
        BigInteger rMin=BigInteger.ZERO;
        if (e1 != null) {
            rMin = representatives.headMap(e1.getKey(), true).values()
                    .stream().map(rep -> rep.getG1().add(rep.getG2()).add(BigInteger.ONE))
                    .reduce(e1.getKey().subtract(e1.getG2()).subtract(e1.getDelta2()),BigInteger::add);
        }
        Representative e2 = representatives.higherEntry(element).getValue();
        BigInteger rMax = BigInteger.ZERO;
        if (e2 != null) {
            rMax = representatives.headMap(e2.getKey(), true).values()
                    .stream().map(rep -> rep.getG1().add(rep.getG2()).add(BigInteger.ONE))
                    .reduce(e2.getKey().add(e2.getDelta1()).subtract(e2.getG2()),BigInteger::add);
        }
        return rMin.add(rMax).subtract(BigInteger.ONE);
    }

    private void compress() {
        if (!queue.isEmpty() && queue.peek().getD().compareTo(BigInteger.valueOf((long)epsilon * t))<=0) {
            Pair topPair = queue.poll();
            Representative e1 = topPair.e1;
            Representative e2 = topPair.e2;
            if (testMergeable(e1, e2)) {
                Representative newRep = merging(e1, e2);
                //e2 is removed
                if (newRep.equals(e1)) {
                    representatives.remove(e2.getKey());
                    Representative newRight = representatives.higherEntry(newRep.getKey()).getValue();
                    if (newRight != null) {
                        queue.remove(new Pair(e2, newRight));
                        queue.add(new Pair(newRep, newRight));
                    }
                    Representative left = representatives.lowerEntry(newRep.getKey()).getValue();
                    if (left != null) {
                        queue.remove(new Pair(left, newRep));
                        queue.add(new Pair(left, newRep));
                    }
                } else if (newRep.equals(e2)) {
                    representatives.remove(e1.getKey());
                    Representative newLeft = representatives.lowerEntry(newRep.getKey()).getValue();
                    if (newLeft != null) {
                        queue.remove(new Pair(newLeft, e1));
                        queue.add(new Pair(newLeft, newRep));
                    }
                    Representative right = representatives.higherEntry(newRep.getKey()).getValue();
                    if (right != null) {
                        queue.remove(new Pair(newRep, right));
                        queue.add(new Pair(newRep, right));
                    }
                }
            }
        }
    }

}

