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
 *    SubsetAccuracy.java
 *    Copyright (C) 2009-2010 Aristotle University of Thessaloniki, Thessaloniki, Greece
 */
package mulan.evaluation.measure;

import mulan.classifier.MultiLabelOutput;
import mulan.core.ArgumentNullException;

/**
 * Implementation of the subset accuracy measure. This measure is the opposite
 * of the zero-one loss for multi-label classification.
 * 
 * @author Grigorios Tsoumakas
 */
public class SubsetAccuracy extends ExampleBasedMeasure {

    public String getName() {
        return "Subset Accuracy";
    }

    public double getIdealValue() {
        return 1;
    }

    public double updateInternal(MultiLabelOutput prediction, boolean[] truth) {
        boolean[] bipartition = prediction.getBipartition();
        if (bipartition == null) {
            throw new ArgumentNullException("Bipartition is null");
        }
        if (bipartition.length != truth.length) {
            throw new IllegalArgumentException("The dimensions of the " +
                    "bipartition and the ground truth array do not match");
        }

        double value = 1;
        for (int i = 0; i < truth.length; i++) {
            if (bipartition[i] != truth[i]) {
                value = 0;
                break;
            }
        }

        sum += value;
        count++;

        return value;
    }

    public double getValue() {
        return sum / count;
    }
}
