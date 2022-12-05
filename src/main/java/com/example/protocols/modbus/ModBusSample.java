package com.example.protocols.modbus;

/**
 * @author chenhaiming
 */
public class ModBusSample {

    private int avgTemp;
    private int avgHumid;
    private int avgPm10;
    private int avgCo2;
    private int inModBus;
    private int inTemp;
    private int inPm10;
    private int inCo2;
    private int outTemp;
    private int outPm10;
    private int outCo2;
    private int warnData;

    public ModBusSample(int avgTemp, int avgHumid, int avgPm10, int avgCo2, int inModBus, int inTemp, int inPm10, int inCo2, int outTemp, int outPm10, int outCo2) {
        this.avgTemp = avgTemp;
        this.avgHumid = avgHumid;
        this.avgPm10 = avgPm10;
        this.avgCo2 = avgCo2;
        this.inModBus = inModBus;
        this.inTemp = inTemp;
        this.inPm10 = inPm10;
        this.inCo2 = inCo2;
        this.outTemp = outTemp;
        this.outPm10 = outPm10;
        this.outCo2 = outCo2;
    }

    public int getAvgTemp() {
        return avgTemp;
    }

    public void setAvgTemp(int avgTemp) {
        this.avgTemp = avgTemp;
    }

    public int getAvgHumid() {
        return avgHumid;
    }

    public void setAvgHumid(int avgHumid) {
        this.avgHumid = avgHumid;
    }

    public int getAvgPm10() {
        return avgPm10;
    }

    public void setAvgPm10(int avgPm10) {
        this.avgPm10 = avgPm10;
    }

    public int getAvgCo2() {
        return avgCo2;
    }

    public void setAvgCo2(int avgCo2) {
        this.avgCo2 = avgCo2;
    }

    public int getInModBus() {
        return inModBus;
    }

    public void setInModBus(int inModBus) {
        this.inModBus = inModBus;
    }

    public int getInTemp() {
        return inTemp;
    }

    public void setInTemp(int inTemp) {
        this.inTemp = inTemp;
    }

    public int getInPm10() {
        return inPm10;
    }

    public void setInPm10(int inPm10) {
        this.inPm10 = inPm10;
    }

    public int getInCo2() {
        return inCo2;
    }

    public void setInCo2(int inCo2) {
        this.inCo2 = inCo2;
    }

    public int getOutTemp() {
        return outTemp;
    }

    public void setOutTemp(int outTemp) {
        this.outTemp = outTemp;
    }

    public int getOutPm10() {
        return outPm10;
    }

    public void setOutPm10(int outPm10) {
        this.outPm10 = outPm10;
    }

    public int getOutCo2() {
        return outCo2;
    }

    public void setOutCo2(int outCo2) {
        this.outCo2 = outCo2;
    }

    public int getWarnData() {
        return warnData;
    }

    public void setWarnData(int warnData) {
        this.warnData = warnData;
    }
}

