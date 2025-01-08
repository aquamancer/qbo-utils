Requirements:
preferences.json in .jar directory

preferences.json:
File Structure:
absolutePath
    rootDirName
        {IMPORTS}
        bankDepositDir
            <any number of .csv files for bank deposits with any name>
        myInvoiceDir
            <any number of .csv files for myInvoice (fragments) with any name>
        invoicesUnpaidDir
            <one .csv file for invoices unpaid with any name>

        {EXPORTS}
        summary.log
        paymentsuploader.csv
        depositsuploader.csv
