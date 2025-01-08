package com.aquamancer.qbo;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

public class QboActions {
    /**
     * Private constructor to prevent instantiation.
     */
    private QboActions(){};
    public static void login(String email, String password, WebDriver driver, WebDriverWait wait) {
        //email
        wait.until(d -> driver.findElement(By.name("Email")).isDisplayed());
        driver.findElement(By.name("Email")).sendKeys(email);
        driver.findElement(By.xpath("//*[@id=\"app\"]/div/div[1]/div/div/div[1]/div/div/div/div[2]/div/div[1]/div[2]/form/button")).click();
        //                                          //*[@id="app"]/div/div[1]/div/div/div[1]/div/div/div/div[2]/div/div[1]/div[2]/form/button
        //password
        wait.until(d -> driver.findElement(By.name("Password")).isDisplayed());
        driver.findElement(By.name("Password")).sendKeys(password);
        driver.findElement(By.xpath("//*[@id=\"app\"]/div/div[1]/div/div/div[1]/div/div/div/div[2]/div/div[1]/div[2]/form/button[2]")).click();
    }
    public static void selectCompany(String companyName, WebDriver driver, WebDriverWait wait) {
        System.out.println("waiting for company list to be displayed");
        wait.until(d -> driver.findElement(By.className("account-list"))).isDisplayed();
        // div that contains the companies
        WebElement divElement = driver.findElement(By.xpath("//*[@id=\"account-list-container\"]/div[1]/div/section/div[2]"));
        // put the children into a list
        List<WebElement> companyList = divElement.findElements(By.xpath("./li"));
        // loop through the children
        for (int i = 0; i < companyList.size(); i++) {
            String companyLabel = companyList.get(i).findElement(By.className("account-name")).getText();
            // click the button with the name passed in from the main method
            if (companyLabel.equals(companyName)) {
                //companyList.get(i).findElement(By.xpath("./button")).click();
                companyList.get(i).findElement(By.className("account-btn")).click(); //may break -> use xpath
            }
        }
    }
    public static void logInAndSelectCompany(String email, String password, String companyName, WebDriver driver, WebDriverWait wait) {
        login(email, password, driver, wait);
        selectCompany(companyName, driver, wait);
    }
    public static void pressControlA(WebElement element, String platform) {
        if (platform.contains("Windows")) {
            element.sendKeys(Keys.CONTROL + "a");
        } else {
            element.sendKeys(Keys.COMMAND + "a");
        }
    }
    public static void goToTransactionsFromHome(ChromeDriver driver, WebDriverWait wait) {
        wait.until(d -> driver.findElement(By.className("QBOLogoComponent__LogoLink-sc-11exkc1-0")));
        driver.navigate().to("https://app.qbo.intuit.com/app/banking");
    }
    public static void goToInvoicesFromHome(ChromeDriver driver, WebDriverWait wait) {
        wait.until(d -> driver.findElement(By.className("QBOLogoComponent__LogoLink-sc-11exkc1-0")));
        driver.navigate().to("https://app.qbo.intuit.com/app/invoices");
    }
    public static void destroyNewLookBanner(ChromeDriver driver, WebDriverWait wait) {
        wait.until(d -> driver.findElement(By.xpath("//*[@id=\"app\"]/div/div[1]/div/div/div[1]/div/div/div/div[1]/button")));
        driver.findElement(By.xpath("//*[@id=\"app\"]/div/div[1]/div/div/div[1]/div/div/div/div[1]/button")).click();
        System.out.println("destroyed banner");
    }
}
