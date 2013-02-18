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
 *    Meta.java
 *    Copyright (C) 2009-2012 Aristotle University of Thessaloniki, Greece
 */
package mulan.classifier.meta.thresholding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import mulan.classifier.InvalidDataException;
import mulan.classifier.ModelInitializationException;
import mulan.classifier.MultiLabelLearner;
import mulan.classifier.MultiLabelOutput;
import mulan.classifier.meta.MultiLabelMetaLearner;
import mulan.data.DataUtils;
import mulan.data.MultiLabelInstances;
import mulan.transformations.RemoveAllLabels;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Base class for instance-based prediction of a bipartition from the labels'
 * scores
 *
 * @author Marios Ioannou
 * @author George Sakkas
 * @author Grigorios Tsoumakas
 * @version 2010.12.14
 */
public abstract class Meta extends MultiLabelMetaLearner {

    /**
     * the classifier to learn the number of top labels or the threshold
     */
    protected Classifier classifier;
    /**
     * the training instances for the single-label model
     */
    protected Instances classifierInstances;
    /**
     * the type for constructing the meta dataset
     */
    protected String metaDatasetChoice;
    /**
     * the number of folds for cross validation
     */
    protected int kFoldsCV;
    /**
     * clean multi-label learner for cross-validation
     */
    protected MultiLabelLearner foldLearner;

    /**
     * Constructor that initializes the learner
     *
     * @param baseLearner the MultiLabelLearner
     * @param aClassifier the learner that will predict the number of relevant
     * labels or a threshold
     * @param aMetaDatasetChoice what features to use for predicting the number
     * of relevant labels or a threshold
     */
    public Meta(MultiLabelLearner baseLearner, Classifier aClassifier, String aMetaDatasetChoice) {
        super(baseLearner);
        metaDatasetChoice = aMetaDatasetChoice;
        classifier = aClassifier;
    }

    /**
     * Returns the classifier used to predict the number of labels/threshold
     *
     * @return the classifier used to predict the number of labels/threshold
     */
    public Classifier getClassifier() {
        return classifier;
    }

    /**
     * abstract method that transforms the training data to meta data
     *
     * @param trainingData the training data set
     * @return the meta data for training the predictor of labels/threshold
     * @throws Exception
     */
    protected abstract Instances transformData(MultiLabelInstances trainingData) throws Exception;

    /**
     * A method that modify an instance
     *
     * @param instance to modified
     * @param xBased the type for constructing the meta dataset
     * @return a transformed instance for the predictor of labels/threshold
     */
    protected Instance modifiedInstanceX(Instance instance, String xBased) {
        Instance modifiedIns = null;
        MultiLabelOutput mlo = null;
        if (xBased.compareTo("Content-Based") == 0) {
            Instance tempInstance = RemoveAllLabels.transformInstance(instance, labelIndices);
            modifiedIns = DataUtils.createInstance(tempInstance, tempInstance.weight(), tempInstance.toDoubleArray());
        } else if (xBased.compareTo("Score-Based") == 0) {
            try {
                mlo = baseLearner.makePrediction(instance);
            } catch (InvalidDataException ex) {
                Logger.getLogger(Meta.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ModelInitializationException ex) {
                Logger.getLogger(Meta.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(Meta.class.getName()).log(Level.SEVERE, null, ex);
            }
            double[] arrayOfScores = mlo.getConfidences();
            modifiedIns = DataUtils.createInstance(instance, numLabels);
            for (int i = 0; i < numLabels; i++) {
                modifiedIns.setValue(i, arrayOfScores[i]);
            }
        } else {       //Rank-Based
            try {
                //Rank-Based
                mlo = baseLearner.makePrediction(instance);
                double[] arrayOfScores = mlo.getConfidences();
                ArrayList<Double> list = new ArrayList();
                for (int i = 0; i < numLabels; i++) {
                    list.add(arrayOfScores[i]);
                }
                Collections.sort(list);
                modifiedIns = DataUtils.createInstance(instance, numLabels);
                int j = numLabels - 1;
                for (Double x : list) {
                    modifiedIns.setValue(j, x);
                    j--;
                }
            } catch (InvalidDataException ex) {
                Logger.getLogger(Meta.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ModelInitializationException ex) {
                Logger.getLogger(Meta.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(Meta.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return modifiedIns;
    }

    /**
     * Prepares the instances for the predictor of labels/threshold
     *
     * @param data the training data
     * @return the prepared instances
     */
    protected Instances prepareClassifierInstances(MultiLabelInstances data) {
        Instances temp = null;
        if (metaDatasetChoice.compareTo("Content-Based") == 0) {
            try {
                temp = RemoveAllLabels.transformInstances(data);
                temp = new Instances(temp, 0);
            } catch (Exception ex) {
                Logger.getLogger(Meta.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            ArrayList<Attribute> atts = new ArrayList<Attribute>();
            for (int i = 0; i < numLabels; i++) {
                atts.add(new Attribute("Label" + i));
            }
            temp = new Instances("threshold", atts, 0);
        }
        return temp;
    }

    /**
     * A method that fill the array "newValues"
     *
     * @param learner the multi-label learner
     * @param instance the training instances
     * @param newValues the array to fill
     * @param xBased the type for constructing the meta dataset
     * @throws Exception
     */
    protected void valuesX(MultiLabelLearner learner, Instance instance, double[] newValues, String xBased) throws Exception {
        MultiLabelOutput mlo;
        if (metaDatasetChoice.compareTo("Content-Based") == 0) {
            double[] values = instance.toDoubleArray();
            for (int i = 0; i < featureIndices.length; i++) {
                newValues[i] = values[featureIndices[i]];
            }
        } else if (metaDatasetChoice.compareTo("Score-Based") == 0) {
            mlo = learner.makePrediction(instance);
            double[] values = mlo.getConfidences();
            System.arraycopy(values, 0, newValues, 0, values.length);
        } else if (metaDatasetChoice.compareTo("Rank-Based") == 0) {
            mlo = learner.makePrediction(instance);
            double[] values = mlo.getConfidences();
            ArrayList<Double> list = new ArrayList();
            for (int i = 0; i < numLabels; i++) {
                list.add(values[i]);
            }
            Collections.sort(list);
            int j = numLabels - 1;
            for (Double x : list) {
                newValues[j] = x;
                j--;
            }
        }
    }

    @Override
    protected void buildInternal(MultiLabelInstances trainingData) throws Exception {
        debug("building meta-model");
        classifierInstances = transformData(trainingData);
        classifier.buildClassifier(classifierInstances);
        // keep just the header information
        classifierInstances = new Instances(classifierInstances, 0);

        debug("building the multi-label classifier");
        baseLearner.setDebug(getDebug());
        baseLearner.build(trainingData);
    }
}