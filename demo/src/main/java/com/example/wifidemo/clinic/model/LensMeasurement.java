package com.example.wifidemo.clinic.model;

public class LensMeasurement {
    private double sph;
    private double cyl;
    private double axis;
    private double add;
    private double va;
    private double pd;
    private double prismX;
    private double prismY;
    private double prismR;
    private double prismTheta;

    public LensMeasurement() {
    }

    public LensMeasurement(
            double sph,
            double cyl,
            double axis,
            double add,
            double va,
            double pd,
            double prismX,
            double prismY,
            double prismR,
            double prismTheta
    ) {
        this.sph = sph;
        this.cyl = cyl;
        this.axis = axis;
        this.add = add;
        this.va = va;
        this.pd = pd;
        this.prismX = prismX;
        this.prismY = prismY;
        this.prismR = prismR;
        this.prismTheta = prismTheta;
    }

    public LensMeasurement copy() {
        return new LensMeasurement(sph, cyl, axis, add, va, pd, prismX, prismY, prismR, prismTheta);
    }

    public double getSph() {
        return sph;
    }

    public void setSph(double sph) {
        this.sph = sph;
    }

    public double getCyl() {
        return cyl;
    }

    public void setCyl(double cyl) {
        this.cyl = cyl;
    }

    public double getAxis() {
        return axis;
    }

    public void setAxis(double axis) {
        this.axis = axis;
    }

    public double getAdd() {
        return add;
    }

    public void setAdd(double add) {
        this.add = add;
    }

    public double getVa() {
        return va;
    }

    public void setVa(double va) {
        this.va = va;
    }

    public double getPd() {
        return pd;
    }

    public void setPd(double pd) {
        this.pd = pd;
    }

    public double getPrismX() {
        return prismX;
    }

    public void setPrismX(double prismX) {
        this.prismX = prismX;
    }

    public double getPrismY() {
        return prismY;
    }

    public void setPrismY(double prismY) {
        this.prismY = prismY;
    }

    public double getPrismR() {
        return prismR;
    }

    public void setPrismR(double prismR) {
        this.prismR = prismR;
    }

    public double getPrismTheta() {
        return prismTheta;
    }

    public void setPrismTheta(double prismTheta) {
        this.prismTheta = prismTheta;
    }
}
