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
 *    GeometricMeanAveragePrecision.java
 *    Copyright (C) 2009-2012 Aristotle University of Thessaloniki, Greece
 */
package mulan.evaluation.measure;

import java.util.Collections;

/**
 * Implementation of GMAP (Geometric Mean Average Precision)
 *
 * @author Eleftherios Spyromitros Xioufis
 * @version 2010.12.10
 */
public class GeometricMeanAveragePrecision extends LabelBasedAveragePrecision {

    private double[] AveragePrecision;

    /**
     * Creates a new instance of this class
     *
     * @param numOfLabels the number of labels
     */
    public GeometricMeanAveragePrecision(int numOfLabels) {
        super(numOfLabels);
    }

    @Override
    public String getName() {
        return "Geometric Mean Average Precision";
    }

    @Override
    public double getValue() {
        AveragePrecision = new double[numOfLabels];
        double productAvgPre = 1;
        for (int labelIndex = 0; labelIndex < numOfLabels; labelIndex++) {
            AveragePrecision[labelIndex] = 0;
            Collections.sort(confact[labelIndex], Collections.reverseOrder());
            double retrievedCounter = 0;
            double relevantCounter = 0;

            for (int i = 0; i < confact[labelIndex].size(); i++) {
                retrievedCounter++;
                Boolean actual = confact[labelIndex].get(i).getActual();
                if (actual) {
                    relevantCounter++;
                    AveragePrecision[labelIndex] += relevantCounter / retrievedCounter;
                }
            }
            AveragePrecision[labelIndex] /= relevantCounter;
            productAvgPre = productAvgPre * AveragePrecision[labelIndex];
        }

        return Math.pow(productAvgPre, 1.0 / numOfLabels);
    }

    @Override
    public double getIdealValue() {
        return 1;
    }
}