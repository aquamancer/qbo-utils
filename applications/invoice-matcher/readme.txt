Requirements:
preferences.json in .jar directory

preferences.json:
bankDepositDescriptionFilter: String[] = bank deposits description to include. Follows OR logic

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

Example preferences.json:
{
  "rootDirName": "Input-files",

  "absolutePath": "absolute/path/to/folder/containing/the/folders/below",
  "bankDepositDir": "bank-deposits",
  "myInvoiceDir": "myinvoice",
  "invoicesUnpaidDir": "invoices-unpaid",

  "bankDepositDescriptionFilter": [
    "filter1",
    "OR filter2",
    "OR filter3"
  ]
}
