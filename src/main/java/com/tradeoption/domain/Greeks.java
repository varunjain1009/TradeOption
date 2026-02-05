package com.tradeoption.domain;

public class Greeks {
    private double delta;
    private double gamma;
    private double theta;
    private double vega;
    private double rho;

    public Greeks() {
    }

    public Greeks(double delta, double gamma, double theta, double vega, double rho) {
        this.delta = delta;
        this.gamma = gamma;
        this.theta = theta;
        this.vega = vega;
        this.rho = rho;
    }

    public void add(Greeks other) {
        this.delta += other.delta;
        this.gamma += other.gamma;
        this.theta += other.theta;
        this.vega += other.vega;
        this.rho += other.rho;
    }

    public double getDelta() {
        return delta;
    }

    public void setDelta(double delta) {
        this.delta = delta;
    }

    public double getGamma() {
        return gamma;
    }

    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    public double getTheta() {
        return theta;
    }

    public void setTheta(double theta) {
        this.theta = theta;
    }

    public double getVega() {
        return vega;
    }

    public void setVega(double vega) {
        this.vega = vega;
    }

    public double getRho() {
        return rho;
    }

    public void setRho(double rho) {
        this.rho = rho;
    }

    @Override
    public String toString() {
        return "Greeks{" +
                "delta=" + delta +
                ", gamma=" + gamma +
                ", theta=" + theta +
                ", vega=" + vega +
                ", rho=" + rho +
                '}';
    }
}
