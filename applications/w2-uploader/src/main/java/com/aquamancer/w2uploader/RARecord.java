package com.aquamancer.w2uploader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;

public class RARecord {
    private static String RECORD_IDENTIFIER = "RA";
    private String submitterEIN;
    private String submitterUserID;
    private String softwareVendorCode;
    private static String BLANKS_5 = "     ";
    private String resubmissionIndicator;
    private String resubmissionWageFileIdentifier;
    private static String SOFTWARE_CODE = "98";
    private String companyName, companyLocationAddress, companyDeliveryAddress, companyCity, companyStateAbbreviation, companyZipCode, companyZipCodeExtension;
    private String companyForeignState, companyForeignPostalCode, companyCountryCode;
    private String submitterName, submitterLocationAddress, submitterDeliveryAddress, submitterCity, submitterStateAbbreviation, submitterZipCode, submitterZipCodeExtension;
    private String submitterForeignState, submitterForeignPostalCode, submitterCountryCode, submitterContactName, submitterContactPhoneNumber, submitterContactPhoneExtension;
    private String submitterContactEmail, submitterContactFax, submitterPreparerCode;

    public RARecord(String jsonFilePath) {
        try {
            JsonObject map = JsonParser.parseReader(new FileReader(jsonFilePath)).getAsJsonObject();
            submitterEIN = map.get("submitterEIN").getAsString();
            submitterUserID = map.get("submitterUserID").getAsString();
            softwareVendorCode = map.get("softwareVendorCode").getAsString();
            resubmissionIndicator = map.get("resubmissionIndicator").getAsString();
            resubmissionWageFileIdentifier = map.get("resubmissionWageFileIdentifier").getAsString();
            companyName = map.get("companyName").getAsString();
            companyLocationAddress = map.get("companyLocationAddress").getAsString();
            companyDeliveryAddress = map.get("companyDeliveryAddress").getAsString();
            companyCity = map.get("companyCity").getAsString();
            companyStateAbbreviation = map.get("companyStateAbbreviation").getAsString();
            companyZipCode = map.get("companyZipCode").getAsString();
            companyZipCodeExtension = map.get("companyZipCodeExtension").getAsString();
            companyForeignState = map.get("companyForeignState").getAsString();
            companyForeignPostalCode = map.get("companyForeignPostalCode").getAsString();
            companyCountryCode = map.get("companyCountryCode").getAsString();
            submitterName = map.get("submitterName").getAsString();
            submitterLocationAddress = map.get("submitterLocationAddress").getAsString();
            submitterDeliveryAddress = map.get("submitterDeliveryAddress").getAsString();
            submitterCity = map.get("submitterCity").getAsString();
            submitterStateAbbreviation = map.get("submitterStateAbbreviation").getAsString();
            submitterZipCode = map.get("submitterZipCode").getAsString();
            submitterZipCodeExtension = map.get("submitterZipCodeExtension").getAsString();
            submitterForeignState = map.get("submitterForeignState").getAsString();
            submitterForeignPostalCode = map.get("submitterForeignPostalCode").getAsString();
            submitterCountryCode = map.get("submitterCountryCode").getAsString();
            submitterContactName = map.get("submitterContactName").getAsString();
            submitterContactPhoneNumber = map.get("submitterContactPhoneNumber").getAsString();
            submitterContactPhoneExtension = map.get("submitterContactPhoneExtension").getAsString();
            submitterContactEmail = map.get("submitterContactEmail").getAsString();
            submitterContactFax = map.get("submitterContactFax").getAsString();
            submitterPreparerCode = map.get("submitterPreparerCode").getAsString();
        } catch (IOException ex) {
            throw new RuntimeException("Could not parse RA json file.");
        }
    }
}
