package com.isga.translator.model;

public class TranslationResponse {

    private String translation;
    private String latin;

    public TranslationResponse() {}

    public TranslationResponse(String translation, String latin) {
        this.translation = translation;
        this.latin = latin;
    }

    public String getTranslation() { return translation; }
    public void setTranslation(String translation) { this.translation = translation; }

    public String getLatin() { return latin; }
    public void setLatin(String latin) { this.latin = latin; }
}
