package org.esa.beam.snowradiance.util;

import org.esa.beam.framework.gpf.OperatorException;

/**
 * Utility class for snow radiance algorithms
 *
 * @author Olaf Danne
 * @version $Revision: 8267 $ $Date: 2010-02-05 16:39:24 +0100 (Fr, 05 Feb 2010) $
 */
public class SnowRadianceUtils {


/**
        * This method computed the index of the nearest higher value in a float array
        * compared to a given input float value
        *
        * @param x - input value
        * @param array - the float array
        * @return int
        */
       public static int getNearestHigherValueIndexInDoubleArray(double x, double[] array) {
           int nearestValueIndex = -1;
           double big = Double.MAX_VALUE;

           for (int i = 0; i < array.length; i++) {
               if (x < array[i]) {
                   if (array[i] - x < big) {
                       big = array[i] - x;
                       nearestValueIndex = i;
                   }
               }
           }
        // special boundary case:
        if (x >= array[array.length-1] && nearestValueIndex == -1) {
            nearestValueIndex = array.length-1;
        }

        return nearestValueIndex;
       }

    public static int getNearestLowerValueIndexInDoubleArray(double x, double[] array) {
        int nearestValueIndex = -1;
        double big = Double.MAX_VALUE;

        for (int i = 0; i < array.length; i++) {
            if (x > array[i]) {
                if (x - array[i] < big) {
                    big = x - array[i];
                    nearestValueIndex = i;
                }
            }
        }
        // special boundary case:
        if (x <= array[0] && nearestValueIndex == -1) {
            nearestValueIndex = 0;
        }

        return nearestValueIndex;
    }


}
