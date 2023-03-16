package eu.wdaqua.qanary.utils;

public class NeuralUtilities {
    public static float computeCosSimilarity(float[] a, float[] b) {
        //todo: you might want to check they are the same size before proceeding

        float dotProduct = 0;
        float normASum = 0;
        float normBSum = 0;

        for(int i = 0; i < a.length; i ++) {
            dotProduct += a[i] * b[i];
            normASum += a[i] * a[i];
            normBSum += b[i] * b[i];
        }

        float eucledianDist = (float) (Math.sqrt(normASum) * Math.sqrt(normBSum));
        return dotProduct / eucledianDist;
    }
}
