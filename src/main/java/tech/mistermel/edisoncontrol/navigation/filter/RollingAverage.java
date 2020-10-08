package tech.mistermel.edisoncontrol.navigation.filter;

public class RollingAverage {

    private int index;
    private float total;
    private float[] samples;

    public RollingAverage(int size) {
        this.samples = new float[size];
        for(int i = 0; i < size; i++) {
        	samples[i] = 0;
        }
    }

    public void add(float x) {
        total -= samples[index];
        samples[index] = x;
        total += x;
        
        index++;
        if(index == samples.length) {
        	index = 0;
        }
    }

    public float getAverage() {
        return total / samples.length;
    }   
}