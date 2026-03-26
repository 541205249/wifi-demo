package com.example.wifidemo.clinic.model;

public class FunctionalTestState {
    private double npc;
    private double npa;
    private double nra;
    private double pra;
    private double acaBi;
    private double acaTarget;
    private double ampRight;
    private double ampLeft;
    private boolean nearLampOn;
    private String nraNote;
    private String praNote;
    private String acaNote;
    private String ampNote;

    public FunctionalTestState() {
    }

    public FunctionalTestState copy() {
        FunctionalTestState copy = new FunctionalTestState();
        copy.npc = npc;
        copy.npa = npa;
        copy.nra = nra;
        copy.pra = pra;
        copy.acaBi = acaBi;
        copy.acaTarget = acaTarget;
        copy.ampRight = ampRight;
        copy.ampLeft = ampLeft;
        copy.nearLampOn = nearLampOn;
        copy.nraNote = nraNote;
        copy.praNote = praNote;
        copy.acaNote = acaNote;
        copy.ampNote = ampNote;
        return copy;
    }

    public double getNpc() {
        return npc;
    }

    public void setNpc(double npc) {
        this.npc = npc;
    }

    public double getNpa() {
        return npa;
    }

    public void setNpa(double npa) {
        this.npa = npa;
    }

    public double getNra() {
        return nra;
    }

    public void setNra(double nra) {
        this.nra = nra;
    }

    public double getPra() {
        return pra;
    }

    public void setPra(double pra) {
        this.pra = pra;
    }

    public double getAcaBi() {
        return acaBi;
    }

    public void setAcaBi(double acaBi) {
        this.acaBi = acaBi;
    }

    public double getAcaTarget() {
        return acaTarget;
    }

    public void setAcaTarget(double acaTarget) {
        this.acaTarget = acaTarget;
    }

    public double getAmpRight() {
        return ampRight;
    }

    public void setAmpRight(double ampRight) {
        this.ampRight = ampRight;
    }

    public double getAmpLeft() {
        return ampLeft;
    }

    public void setAmpLeft(double ampLeft) {
        this.ampLeft = ampLeft;
    }

    public boolean isNearLampOn() {
        return nearLampOn;
    }

    public void setNearLampOn(boolean nearLampOn) {
        this.nearLampOn = nearLampOn;
    }

    public String getNraNote() {
        return nraNote;
    }

    public void setNraNote(String nraNote) {
        this.nraNote = nraNote;
    }

    public String getPraNote() {
        return praNote;
    }

    public void setPraNote(String praNote) {
        this.praNote = praNote;
    }

    public String getAcaNote() {
        return acaNote;
    }

    public void setAcaNote(String acaNote) {
        this.acaNote = acaNote;
    }

    public String getAmpNote() {
        return ampNote;
    }

    public void setAmpNote(String ampNote) {
        this.ampNote = ampNote;
    }
}
