package org.keywords4bytecodes.firstclass.baseline;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;

import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class WekaBaseline0 {

    private static final int TOP_TERMS = 30;
    private static final int FOLDS = 3;
    private static final int SEQ_LENGTH = 28;

    public static void main(String[] args) throws Exception {

        // first pass, determine vocabulary
        BufferedReader br = new BufferedReader(new FileReader(args[0]));

        Map<String, AtomicInteger> verbCounts = new HashMap<String, AtomicInteger>();
        Set<String> vocab = new HashSet<>();
        vocab.add("PADDING");
        String line = br.readLine();
        int useful = 0;
        while (line != null) {
            if (line.indexOf(' ') > 0) {
                String[] parts = line.split(" ");
                if (parts.length > 1) {
                    boolean first = true;
                    useful++;
                    for (String part : parts) {
                        if (first) {
                            part = part.replaceFirst("\\_.*", "").replaceFirst("[A-Z].*", "");
                            if (!part.startsWith("<") && part.matches(".*[a-z].*")) {
                                if (!verbCounts.containsKey(part))
                                    verbCounts.put(part, new AtomicInteger(0));
                                verbCounts.get(part).incrementAndGet();
                            }
                            first = false;
                        } else {
                            if (part.length() > 0)
                                vocab.add(part);
                        }
                    }
                }
            }

            line = br.readLine();
        }
        br.close();

        System.out.println("Useful lines: " + useful);
        System.out.println("Vocabulary: " + vocab.size());

        List<Pair<String, Integer>> toSort = new ArrayList<>(verbCounts.size());
        for (Map.Entry<String, AtomicInteger> e : verbCounts.entrySet())
            toSort.add(Pair.of(e.getKey(), e.getValue().get()));
        Collections.sort(toSort, new Comparator<Pair<String, Integer>>() {

            @Override
            public int compare(Pair<String, Integer> p1, Pair<String, Integer> p2) {
                int rec = p2.getRight().compareTo(p1.getRight());
                if (rec == 0)
                    return p2.getLeft().compareTo(p1.getLeft());
                else
                    return rec;
            }
        });

        Set<String> topTerms = new HashSet<String>();
        int topTermsOcc = 0;
        for (int i = 0; i < TOP_TERMS - 1; i++) {
            topTerms.add(toSort.get(i).getLeft());
            topTermsOcc += toSort.get(i).getRight();
        }

        System.out.println("Top terms(" + topTerms.size() + " / " + topTermsOcc + " / " + (topTermsOcc * 1.0 / useful)
                + "%): " + topTerms);

        String[] posToVocab = new String[vocab.size()];
        Map<String, Integer> vocabToPos = new HashMap<>();

        int pos = 0;
        for (String v : vocab) {
            posToVocab[pos] = v;
            vocabToPos.put(v, (int) pos);
            pos++;
        }
        int vocab_size = vocab.size();
        int paddingPos = vocabToPos.get("PADDING");

        List<String> posToTerm = new ArrayList<>();
        Map<String, Integer> termToPos = new HashMap<>();
        pos = 0;
        for (String t : topTerms) {
            posToTerm.add(t);
            termToPos.put(t, pos);
            pos++;
        }
        posToTerm.add("OTHER");
        termToPos.put("OTHER", pos);
        // int otherPos = pos;

        // second pass, read folds
        List<List<Pair<String, int[]>>> folds = new ArrayList<>(FOLDS);
        for (int i = 0; i < FOLDS; i++)
            folds.add(new ArrayList<Pair<String, int[]>>());

        Random foldDistributor = new Random(1993);

        br = new BufferedReader(new FileReader(args[0]));

        line = br.readLine();
        while (line != null) {
            if (line.indexOf(' ') > 0) {
                String[] parts = line.split(" ");
                if (parts.length > 1) {
                    List<Pair<String, int[]>> fold = folds.get(foldDistributor.nextInt(FOLDS));
                    String name = "OTHER";
                    int[] seq = new int[SEQ_LENGTH];
                    Arrays.fill(seq, (int) paddingPos);

                    pos = 0;
                    boolean first = true;
                    for (String part : parts) {
                        if (first) {
                            part = part.replaceFirst("\\_.*", "").replaceFirst("[A-Z].*", "");
                            if (termToPos.containsKey(part))
                                name = part;
                            first = false;
                        } else {
                            if (part.length() > 0) {
                                seq[pos] = vocabToPos.get(part);
                                pos++;
                            }
                        }
                        if (pos >= SEQ_LENGTH)
                            break;
                    }
                    fold.add(Pair.of(name, seq));
                }
            }

            line = br.readLine();
        }
        br.close();

        // cross validation

        ArrayList<Attribute> attInfo = new ArrayList<>();
        for (int s = 0; s < SEQ_LENGTH; s++)
            for (int i = 0; i < vocab_size; i++)
                attInfo.add(new Attribute("S" + s + "_" + posToVocab[i]));
        attInfo.add(new Attribute("Verb", posToTerm));
        System.out.println("Row size: " + attInfo.size());

        int correctOnTop3 = 0;
        for (int f = 0; f < FOLDS; f++) {
            // assemble train set
            Instances trainset = new Instances("verb", attInfo, (int) (useful * (FOLDS - 1.0) / FOLDS + 1));
            trainset.setClassIndex(attInfo.size() - 1);
            Instances testset = new Instances("verb", attInfo, (int) (useful * 1.0 / FOLDS + 1));
            testset.setClassIndex(attInfo.size() - 1);
            for (int t = 0; t < FOLDS; t++) {
                boolean isTest = t == f;

                for (Pair<String, int[]> p : folds.get(t)) {
                    if(!isTest && foldDistributor.nextDouble() < 0.9)
                        continue;
                    
                    pos = 0;
                    double[] instV = new double[attInfo.size()];
                    int[] bs = p.getRight();
                    for (pos = 0; pos < bs.length; pos++){
                        instV[pos * vocab_size + bs[pos]] = 1.0;
                    }

                    instV[instV.length - 1] = (double)termToPos.get(p.getLeft());
                    Instance inst = new DenseInstance(1.0, instV);
                    inst.setDataset(isTest ? testset : trainset);
                    //inst.setClassValue(p.getLeft());
                    if (isTest)
                        testset.add(inst);
                    else
                        trainset.add(inst);
                }
            }
            
            /*
            ArffSaver saver = new ArffSaver();
            saver.setFile(new File("/tmp/fold" + f + "_train.arff"));
            saver.setInstances(trainset);
            saver.writeBatch();
            
            saver = new ArffSaver();
            saver.setFile(new File("/tmp/fold" + f + "_test.arff"));
            saver.setInstances(testset);
            saver.writeBatch();
            */
            
            System.out.println(new Date() + " Fold " + f + " about to build classifier for " + trainset.size() + " instances...");
            RandomForest rf = new RandomForest();
            rf.setSeed(1993);
            //rf.setNumExecutionSlots(1);
            rf.buildClassifier(trainset);

            System.out.println(new Date() + " Fold " + f + " about to test classifier on " + testset.size() + " instances...");
            for (int i = 0; i < testset.size(); i++) {
                Instance inst = testset.get(i);
                int correct = (int) inst.classValue();
                //System.out.println(correct);
                inst.setClassMissing();
                double[] dist = rf.distributionForInstance(inst);
                //System.out.println(Arrays.asList(dist));
                double correctValue = dist[correct];
                int biggerThanCorrect = 0;
                for (int p = 0; p < dist.length; p++) {
                    if (p != correct && dist[p] >= correctValue)
                        biggerThanCorrect++;
                }
                //System.out.println(biggerThanCorrect);
                if (biggerThanCorrect < 3)
                    correctOnTop3++;
            }
            System.out.println(new Date() + " Correct on top 3 so far: " + correctOnTop3); 
        }
        System.out.println("Correct on top 3: " + correctOnTop3 + " / " + (correctOnTop3 * 1.0 / useful));
    }
}
