package com.aquamancer.invoicematcher.uploader;

import com.aquamancer.invoicematcher.fragment.Fragment;
import com.aquamancer.invoicematcher.fragment.FragmentMatcher;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Uploader {
    private static final Logger LOGGER = LogManager.getLogger(Uploader.class);
    private static final CSVFormat PAYMENTS_FORMAT = CSVFormat.Builder
            .create()
            .setHeader("RefNumber", "TxnDate", "PaymentRefNumber", "Customer", "PaymentMethod", "PrivateNote", "DepositToAccount", "InvoiceApplyTo", "LineAmount", "LineDesc", "Currency", "ExchangeRate")
            .build();
    private static final CSVFormat DEPOSITS_FORMAT = CSVFormat.Builder
            .create()
            .setHeader(
                    "RefNumber",
                    "TxnDate",
                    "PrivateNote",
                    "Currency",
                    "ExchangeRate",
                    "DepositToAccount",
                    "Location",
                    "Entity",
                    "LineDesc",
                    "LineAmount",
                    "Account",
                    "PaymentMethod",
                    "PaymentRefNumber",
                    "Class",
                    "LinkedTxnType",
                    "LinkedTxnNumber",
                    "CashBackAccount",
                    "CashBackAmount",
                    "CashBackMemo"
            )
            .build();

    private static final String BLANK_LINE = "";

    private Appendable paymentsWriter, depositsWriter, paymentsErrorWriter, depositsErrorWriter;
    // todo this is a bandaid fix for fragment mutability (as high level change as possible)
    private List<List<Fragment>> matchedFragments;
    private Map<String, List<CSVRecord>> invoicesUnpaid;
    public Uploader(Appendable paymentsWriter, Appendable depositsWriter, Appendable paymentsErrorWriter, Appendable depositsErrorWriter, Map<String, List<CSVRecord>> invoicesUnpaid) {
        this.paymentsWriter = paymentsWriter;
        this.depositsWriter = depositsWriter;
        this.paymentsErrorWriter = paymentsErrorWriter;
        this.depositsErrorWriter = depositsErrorWriter;
        this.invoicesUnpaid = invoicesUnpaid;
        matchedFragments = new ArrayList<>();
    }

    public void append(List<CSVRecord> fragmentMatchGroup) {
        // todo this is a bandaid fix for fragment mutability
        List<Fragment> matchGroup = new ArrayList<>();
        for (CSVRecord fragment : fragmentMatchGroup)
            matchGroup.add(new Fragment(fragment));
        this.matchedFragments.add(matchGroup);
    }

    /**
     * @updates matchGroup
     */
    private void mergeFragmentsWithSameInvoiceNumber(List<Fragment> matchGroup) {
        Function<Fragment, List<String>> merge = fragment -> List.of(fragment.getInvoiceNumber(), fragment.getEftTraceNumber());
        // Map<[Invoice number, eft trace], fragments with same key>
        Map<List<String>, List<Fragment>> grouped = matchGroup.stream().collect(Collectors.groupingBy(merge));

        // merge every List<Fragment> into one Fragment
        // values() returns a view(aliases)
        for (List<Fragment> mergeGroup : grouped.values()) {
            if (mergeGroup.size() > 1) {
                // merge all fragment amounts into result
                Fragment result = mergeGroup.getFirst();
                for (int i = 1; i < mergeGroup.size(); i++) {
                    result.mergeAmounts(mergeGroup.get(i));
                }
                // replace mergeGroup with result
                mergeGroup.clear();
                mergeGroup.add(result);
            }
        }
        // clear matchGroup and replace it with the merged result
        matchGroup.clear();
        for (List<Fragment> group : grouped.values()) {
            // there should only be one element per group
            matchGroup.add(group.getFirst());
        }
    }
    // Must handle negative adjustment for same eft trace group, not entire match
    private void handleNegativeAdjustmentsByEftTrace(List<Fragment> matchGroup) {
        Map<String, List<Fragment>> eftTraceGroups = matchGroup.stream().collect(Collectors.groupingBy(fragment -> fragment.getEftTraceNumber()));
        for (List<Fragment> eftTraceGroup : eftTraceGroups.values()) {
            handleNegativeAdjustment(eftTraceGroup);
        }
        // handleNegativeAdjustment removes elements from eftTraceGroups.values(), which does not
        // get reflected in matchGroup, so you must rebuild matchGroup.
        matchGroup.clear();
        for (List<Fragment> eftTraceGroup : eftTraceGroups.values()) {
            matchGroup.addAll(eftTraceGroup);
        }
    }
    private void handleNegativeAdjustment(List<Fragment> group) {
        // gather negative fragments
        List<Integer> indexesOfNegativeFragments = new ArrayList<>();
        for (int i = 0; i < group.size(); i++) {
            if (group.get(i).getEftAmount().compareTo(BigDecimal.ZERO) < 0)
                indexesOfNegativeFragments.add(i);
        }

        if (!indexesOfNegativeFragments.isEmpty()) {
            Fragment fragmentWithGreatestAmount = group.get(getIndexOfGreatestAmount(group));
            List<Fragment> toRemove = new ArrayList<>();
            for (Integer indexOfNegativeFragment : indexesOfNegativeFragments) {
                if (fragmentWithGreatestAmount.getEftAmount().compareTo(group.get(indexOfNegativeFragment).getEftAmount().abs()) >= 0) {
                    fragmentWithGreatestAmount.mergeCrossInvoiceAdjustment(group.get(indexOfNegativeFragment));
                    toRemove.add(group.get(indexOfNegativeFragment));
                }
            }
            group.removeAll(toRemove);
        }
    }
    private int getIndexOfGreatestAmount(List<Fragment> fragments) {
        if (fragments.size() == 1)
            return 0;
        int greatestIndex = 0;
        BigDecimal greatestAmount = fragments.getFirst().getEftAmount();
        for (int i = 1; i < fragments.size(); i++) {
            if (fragments.get(i).getEftAmount().compareTo(greatestAmount) > 0) {
                greatestIndex = i;
                greatestAmount = fragments.get(i).getEftAmount();
            }
        }
        return greatestIndex;
    }

    /**
     * @requires eftAmount for all fragments to be length >= 4. Fragments.size() < 1000;
     */
    public void export() {
        if (FragmentMatcher.extractAll(this.matchedFragments).size() > 999) {
            throw new RuntimeException("Amount of fragments exceeds 3 digit constraint of paymentRefNumber.");
        }
        try (
                CSVPrinter paymentsPrinter = new CSVPrinter(paymentsWriter, PAYMENTS_FORMAT);
                CSVPrinter depositsPrinter = new CSVPrinter(depositsWriter, DEPOSITS_FORMAT);
                CSVPrinter paymentsErrorPrinter = new CSVPrinter(paymentsErrorWriter, PAYMENTS_FORMAT);
                CSVPrinter depositsErrorPrinter = new CSVPrinter(depositsErrorWriter, DEPOSITS_FORMAT);
        ) {
            int paymentRefNumber = 0;
            int depositRefNumber = 0;
            for (List<Fragment> matchedFragmentGroup : matchedFragments) {
                mergeFragmentsWithSameInvoiceNumber(matchedFragmentGroup);
                handleNegativeAdjustmentsByEftTrace(matchedFragmentGroup);
                List<UploaderEntry> uploaderEntryGroup = new ArrayList<>();
                boolean groupHasError = false;
                for (Fragment fragment : matchedFragmentGroup) {
                    UploaderEntry uploaderEntry = new UploaderEntry(fragment, paymentRefNumber, depositRefNumber, invoicesUnpaid);
                    if (uploaderEntry.hasError()) {
                        groupHasError = true;
                    }
                    uploaderEntryGroup.add(uploaderEntry);
                }
                for (UploaderEntry uploaderEntry : uploaderEntryGroup) {
                    if (groupHasError) {
                        uploaderEntry.printPaymentRecord(paymentsErrorPrinter);
                        uploaderEntry.printDepositRecord(depositsErrorPrinter);
                    } else {
                        uploaderEntry.printPaymentRecord(paymentsPrinter);
                        uploaderEntry.printDepositRecord(depositsPrinter);
                    }
                    paymentRefNumber++;
                }
                depositRefNumber++;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
