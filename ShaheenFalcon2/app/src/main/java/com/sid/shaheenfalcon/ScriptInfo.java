package com.sid.shaheenfalcon;

public class ScriptInfo {
    private String scriptName = "", host = "", matchUrl = "", scriptLocation = "", firstRun = "", scriptSource = "";

    public ScriptInfo(String scriptName, String host, String matchUrl, String scriptLocation, String firstRun) {
        this.scriptName = scriptName;
        this.host = host;
        this.matchUrl = matchUrl;
        this.scriptLocation = scriptLocation;
        this.firstRun = firstRun;
    }

    public ScriptInfo(String scriptName, String host, String matchUrl, String scriptLocation, String firstRun, String scriptSource) {
        this.scriptName = scriptName;
        this.host = host;
        this.matchUrl = matchUrl;
        this.scriptLocation = scriptLocation;
        this.firstRun = firstRun;
        this.scriptSource = scriptSource;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getMatchUrl() {
        return matchUrl;
    }

    public void setMatchUrl(String matchUrl) {
        this.matchUrl = matchUrl;
    }

    public String getScriptLocation() {
        return scriptLocation;
    }

    public void setScriptLocation(String scriptLocation) {
        this.scriptLocation = scriptLocation;
    }

    public void setFirstRun(String firstRun){
        this.firstRun = firstRun;
    }

    public String getFirstRun(){
        return this.firstRun;
    }

    public String getScriptSource() {
        return scriptSource;
    }

    public void setScriptSource(String scriptSource) {
        this.scriptSource = scriptSource;
    }
}
