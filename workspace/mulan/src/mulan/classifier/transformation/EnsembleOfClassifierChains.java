/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    EnsembleOfClassifierChains.java
 *    Copyright (C) 2009-2012 Aristotle University of Thessaloniki, Greece
 */
package mulan.classifier.transformation;

import java.util.Arrays;
import java.util.Random;
import mulan.classifier.InvalidDataException;
import mulan.classifier.MultiLabelOutput;
import mulan.data.MultiLabelInstances;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.RemovePercentage;

/**
 *
 <!-- globalinfo-start -->
 * Class implementing the Ensemble of Classifier Chains(ECC) algorithm. For more information, see<br/>
 * <br/>
 * Read, Jesse, Pfahringer, Bernhard, Holmes, Geoff, Frank, Eibe: Classifier Chains for Multi-label Classification. In: , 335--359, 2011.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Read2011,
 *    author = {Read, Jesse and Pfahringer, Bernhard and Holmes, Geoff and Frank, Eibe},
 *    journal = {Machine Learning},
 *    number = {3},
 *    pages = {335--359},
 *    title = {Classifier Chains for Multi-label Classification},
 *    volume = {85},
 *    year = {2011}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 * @author Eleftherios Spyromitros-Xioufis ( espyromi@csd.auth.gr )
 * @author Konstantinos Sechidis (sechidis@csd.auth.gr)
 * @author Grigorios Tsoumakas (greg@csd.auth.gr)
 * @version 2012.02.27
 */
public class EnsembleOfClassifierChains extends TransformationBasedMultiLabelLearner {

    /**
     * The number of classifier chain models
     */
    protected int numOfModels;
    /**
     * An array of ClassifierChain models
     */
    protected ClassifierChain[] ensemble;
    /**
     * Random number generator
     */
    protected Random rand;
    /**
     * Whether the output is computed based on the average votes or on the
     * average confidences
     */
    protected boolean useConfidences;
    /**
     * Whether to use sampling with replacement to create the data of the models
     * of the ensemble
     */
    protected boolean useSamplingWithReplacement = true;
    /**
     * The size of each bag sample, as a percentage of the training size. Used
     * when useSamplingWithReplacement is true
     */
    protected int BagSizePercent = 100;

    /**
     * Returns the size of each bag sample, as a percentage of the training size
     * 
     * @return the size of each bag sample, as a percentage of the training size
     */
    public int getBagSizePercent() {
        return BagSizePercent;
    }

    /**
     * Sets the size of each bag sample, as a percentage of the training size
     * 
     * @param bagSizePercent the size of each bag sample, as a percentage of 
     * the training size
     */
    public void setBagSizePercent(int bagSizePercent) {
        BagSizePercent = bagSizePercent;
    }

    /**
     * Returns the sampling percentage
     * 
     * @return the sampling percentage
     */
    public double getSamplingPercentage() {
        return samplingPercentage;
    }

    /**
     * Sets the sampling percentage
     * 
     * @param samplingPercentage the sampling percentage
     */
    public void setSamplingPercentage(double samplingPercentage) {
        this.samplingPercentage = samplingPercentage;
    }
    
    /**
     * The size of each sample, as a percentage of the training size Used when
     * useSamplingWithReplacement is false
     */
    protected double samplingPercentage = 67;

    
    /**
     * Default constructor
     */
    public EnsembleOfClassifierChains() {
        this(new J48(), 10, true, true);
    }
    
    /**
     * Creates a new object
     *
     * @param classifier the base classifier for each ClassifierChain model
     * @param aNumOfModels the number of models
     * @param doUseConfidences
     * @param doUseSamplingWithReplacement
     */
    public EnsembleOfClassifierChains(Classifier classifier, int aNumOfModels,
            boolean doUseConfidences, boolean doUseSamplingWithReplacement) {
        super(classifier);
        numOfModels = aNumOfModels;
        useConfidences = doUseConfidences;
        useSamplingWithReplacement = doUseSamplingWithReplacement;
        ensemble = new ClassifierChain[aNumOfModels];
        rand = new Random(1);
    }

    /**
     * Returns a string describing classifier.
     *
     * @return a description suitable for displaying in the
     * explorer/experimenter gui
     */
    @Override
    public String globalInfo() {
        return "Class implementing the Ensemble of Classifier Chains"
                + "(ECC) algorithm. For more information, see\n\n"
                + getTechnicalInformation().toString();
    }

    @Override
    public TechnicalInformation getTechnicalInformation() {
        TechnicalInformation result;
        result = new TechnicalInformation(Type.INPROCEEDINGS);
        result.setValue(Field.AUTHOR, "Read, Jesse and Pfahringer, Bernhard and Holmes, Geoff and Frank, Eibe");
        result.setValue(Field.TITLE, "Classifier Chains for Multi-label Classification");
        result.setValue(Field.VOLUME, "85");
        result.setValue(Field.NUMBER, "3");
        result.setValue(Field.YEAR, "2011");
        result.setValue(Field.PAGES, "335--359");
        result.setValue(Field.JOURNAL, "Machine Learning");
        return result;
    }

    @Override
    protected void buildInternal(MultiLabelInstances trainingSet) throws Exception {

        Instances dataSet = new Instances(trainingSet.getDataSet());

        for (int i = 0; i < numOfModels; i++) {
            debug("ECC Building Model:" + (i + 1) + "/" + numOfModels);
            Instances sampledDataSet;
            dataSet.randomize(rand);
            if (useSamplingWithReplacement) {
                int bagSize = dataSet.numInstances() * BagSizePercent / 100;
                // create the in-bag dataset
                sampledDataSet = dataSet.resampleWithWeights(new Random(1));
                if (bagSize < dataSet.numInstances()) {
                    sampledDataSet = new Instances(sampledDataSet, 0, bagSize);
                }
            } else {
                RemovePercentage rmvp = new RemovePercentage();
                rmvp.setInvertSelection(true);
                rmvp.setPercentage(samplingPercentage);
                rmvp.setInputFormat(dataSet);
                sampledDataSet = Filter.useFilter(dataSet, rmvp);
            }
            MultiLabelInstances train = new MultiLabelInstances(sampledDataSet, trainingSet.getLabelsMetaData());

            int[] chain = new int[numLabels];
            for (int j = 0; j < numLabels; j++) {
                chain[j] = j;
            }
            for (int j = 0; j < chain.length; j++) {
                int randomPosition = rand.nextInt(chain.length);
                int temp = chain[j];
                chain[j] = chain[randomPosition];
                chain[randomPosition] = temp;
            }
            debug(Arrays.toString(chain));

            // MAYBE WE SHOULD CHECK NOT TO PRODUCE THE SAME VECTOR FOR THE
            // INDICES
            // BUT IN THE PAPER IT DID NOT MENTION SOMETHING LIKE THAT
            // IT JUST SIMPLY SAY A RANDOM CHAIN ORDERING OF L

            ensemble[i] = new ClassifierChain(baseClassifier, chain);
            ensemble[i].build(train);
        }

    }

    @Override
    protected MultiLabelOutput makePredictionInternal(Instance instance) throws Exception,
            InvalidDataException {

        int[] sumVotes = new int[numLabels];
        double[] sumConf = new double[numLabels];

        Arrays.fill(sumVotes, 0);
        Arrays.fill(sumConf, 0);

        for (int i = 0; i < numOfModels; i++) {
            MultiLabelOutput ensembleMLO = ensemble[i].makePrediction(instance);
            boolean[] bip = ensembleMLO.getBipartition();
            double[] conf = ensembleMLO.getConfidences();

            for (int j = 0; j < numLabels; j++) {
                sumVotes[j] += bip[j] == true ? 1 : 0;
                sumConf[j] += conf[j];
            }
        }

        double[] confidence = new double[numLabels];
        for (int j = 0; j < numLabels; j++) {
            if (useConfidences) {
                confidence[j] = sumConf[j] / numOfModels;
            } else {
                confidence[j] = sumVotes[j] / (double) numOfModels;
            }
        }

        MultiLabelOutput mlo = new MultiLabelOutput(confidence, 0.5);
        return mlo;
    }
}