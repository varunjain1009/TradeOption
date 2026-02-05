package com.tradeoption.domain;

public class ConePoint {
    private double timeInYears;
    private double upper1Sigma;
    private double lower1Sigma;
    private double upper2Sigma;
    private double lower2Sigma;

    public ConePoint(double timeInYears, double upper1Sigma, double lower1Sigma, double upper2Sigma,
            double lower2Sigma) {
        this.timeInYears = timeInYears;
        this.upper1Sigma = upper1Sigma;
        this.lower1Sigma = lower1Sigma;
        this.upper2Sigma = upper2Sigma;
        this.lower2Sigma = lower2Sigma;
    }

    public double getTimeInYears() {
        return timeInYears;
    }

    public double getUpper1Sigma() {
        return upper1Sigma;
    }

    public double getLower1Sigma() {
        return lower1Sigma;
    }

    public double getUpper2Sigma() {
        return upper2Sigma;
    }

    public double getLower2Sigma() {
        return lower2Sigma;
    }

    @Override
    public String toString() {
        return "ConePoint{" +
                "t=" + timeInYears +
                ", 1σ=[" + lower1Sigma + ", " + upper1Sigma + "]" +
                ", 2σ=[" + lower2Sigma + ", " + upper2Sigma + "]" +
                '}';
    }
}
