package com.aquamancer.invoicematcher.fragment;

import org.apache.commons.csv.CSVRecord;

import java.util.List;

public class Match {
    private List<CSVRecord> fragments;
    private CSVRecord bankDeposit;
    private MatchMethod method;
    public Match(List<CSVRecord> fragments, CSVRecord bankDeposit, MatchMethod method) {
        this.fragments = fragments;
        this.bankDeposit = bankDeposit;
        this.method = method;
    }
    public Match(CSVRecord bankDeposit, MatchMethod method) {
        this.bankDeposit = bankDeposit;
        this.method = method;
    }
    public List<CSVRecord> fragments() {
        return this.fragments;
    }
    public CSVRecord bankDeposit() {
        return this.bankDeposit;
    }
    public MatchMethod method() {
        return this.method;
    }
}
