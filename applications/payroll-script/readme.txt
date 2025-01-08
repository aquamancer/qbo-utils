Payroll Script

Requirements:
    Required files in the same directory as the payroll script jar:
        column-references.json
        payroll-script-preferences.json
        element-config.json
    Java Development Kit or Java Runtime Environment 21+

How to run:
    1. cd to jar directory
    2. java -jar "payroll jar name.jar"

column-references.json
    This file creates a universal naming convention for column headers in quickbooks and spreadsheet.
    The program recognizes quickbooks column names through ".contains()" which is why "Name (" works.
    Update this file if the column header for a column defined in this sheet is changed in quickbooks or spreadsheet.
payroll-script-preferences.json
    absolutePathToSpreadsheetFolder
        Absolute path to the directory containing the spreadsheet named with the previous pay period.
        Used to locate the spreadsheet using the name auto-generator.
    absoluteManualPathToSpreadsheet
        Fill this field to manually override the spreadsheet target.
        Leave blank to use absolutePathToSpreadsheetFolder and the auto-generated spreadsheet name.
element-config.json
    Should not be modified.