package com.aquamancer.invoicematcher;

import com.aquamancer.invoicematcher.uploader.UploaderEntry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class FilePathParser {
    private String jarPath;
    private String rootDirName;
    private String absolutePath, bankDepositDir, fragmentsDir, invoicesUnpaidDir;
    private String rootDirPath;
    public FilePathParser(String jarPath) {
        this.jarPath = jarPath;
        try (FileReader reader = new FileReader(jarPath + "/preferences.json")) {
            JsonObject preferences = JsonParser.parseReader(reader).getAsJsonObject();
            this.rootDirName = preferences.get("rootDirName").getAsString();
            this.absolutePath = preferences.get("absolutePath").getAsString();
            this.bankDepositDir = preferences.get("bankDepositDir").getAsString();
            this.fragmentsDir = preferences.get("myInvoiceDir").getAsString();
            this.invoicesUnpaidDir = preferences.get("invoicesUnpaidDir").getAsString();
            this.rootDirPath = absolutePath + '/' + this.rootDirName + '/';
        } catch (IOException ex) {
            throw new RuntimeException("Could not find or parse preferences.json. Verify preferences.json is in same directory as .jar");
        }
    }
    public List<String> getAllFilePathsInDir(String dirName) {
        String dirPath = this.rootDirPath + '/' + dirName + '/';
        File dir = new File(dirPath);
        List<String> result = new ArrayList<>();
        if (dir.isDirectory()) {
            String[] fileNames = dir.list();
            for (String fileName : fileNames) {
                result.add(this.rootDirPath + '/' + dirName + '/' + fileName);
            }
            if (fileNames == null || fileNames.length == 0) throw new RuntimeException("No files exist in the directory " + dirPath);
        } else {
            throw new RuntimeException(dirPath + " is not a directory!");
        }
        return result;
    }
    public List<String> getAllBankDepositFileNames() {
        return getAllFilePathsInDir(this.bankDepositDir);
    }
    public List<String> getAllFragmentFilePaths() {
        return getAllFilePathsInDir(this.fragmentsDir);
    }
    public String getInvoicesUnpaidFileName() {
        List<String> allFileNamesInDir = getAllFilePathsInDir(this.invoicesUnpaidDir);
        if (allFileNamesInDir.size() != 1) {
            throw new RuntimeException("There are multiple files or zero files in " + this.rootDirPath + '/' + this.invoicesUnpaidDir + '/');
        }
        return allFileNamesInDir.getFirst();
    }
    public static String dateAppend() {
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        return "_" + date + '_' + UploaderEntry.digitPlaceholders(time.getHour(), 2) + '-' + UploaderEntry.digitPlaceholders(time.getMinute(), 2) + '-' + UploaderEntry.digitPlaceholders(time.getSecond(), 2);
    }
    public String getPaymentExportFilePath() {
        return this.rootDirPath + "/paymentsuploader" + dateAppend() + ".csv";
    }
    public String getDepositExportFilePath() {
        return this.rootDirPath + "/depositsuploader" + dateAppend() + ".csv";
    }
    public String getSummaryExportFilePath() {
        return this.rootDirPath + "/summary" + dateAppend() + ".log";
    }
}
