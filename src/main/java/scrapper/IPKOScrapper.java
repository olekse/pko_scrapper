package scrapper;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import exception.*;
import model.Account;
import util.HtmlUnitUtil;
import util.Logger;
import util.StopWatch;

public class IPKOScrapper {
    private static final String URL = "https://www.ipko.pl";
    private static final int MIN_JS_LOAD_TIME_MS = 500;
    private static final int MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS = 20000;
    private static final int MIN_BACKGROUND_JS_TASKS_ON_PASSWORD_PAGE = 4;
    private static final int MIN_JS_LOADING_WAIT_INTERVAL_MS = 500;
    private static final int BLOCKS_IN_ACCOUNT_NUMBER = 7;
    private Logger logger;
    private WebClient webClient;
    private HtmlPage currentPage;
    private Map<String, Account> accountMap = new HashMap<>();
    private String login;
    private String password;

    public IPKOScrapper(String login, String password, Logger logger) {
        if (login == null || login.equals(""))
            throw new IllegalArgumentException("Login cannot be null or empty!");
        if (password == null || password.equals(""))
            throw new IllegalArgumentException("Password cannot be null or empty!");
        this.login = login;
        this.password = password;
        this.logger = logger;
        setupWebClientConfiguration();
    }

    private void setupWebClientConfiguration() {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
        webClient = new WebClient();
        webClient.getOptions().setRedirectEnabled(true);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setHistoryPageCacheLimit(0);
        webClient.getCookieManager().setCookiesEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(true);
    }

    public Collection<Account> fetchAccounts() {
        authenticate();
        saveAccountNumberFromAccountSelectMenu(getAccountSelectMenu());
        saveAccountValueFromUpperMenu();
        return accountMap.values();
    }

    private void authenticate() {
        logger.log("Opening login page...");
        loadStartingPage();
        insertIntoInputField(login);
        enterWithLogin();
        insertIntoInputField(password);
        enterWithPassword();
        logger.log("Redirected...");
    }

    private void loadStartingPage() {
        try{
            currentPage = webClient.getPage(URL);
        } catch (IOException cause) {
            throw new ConnectionProblem("Failed to load a page!", cause);
        }
    }

    private void insertIntoInputField(String string) {
        HtmlInput inputField = getInputField();
        inputField.setValueAttribute(string);
    }

    private HtmlInput getInputField(){
        String cssSelector = "input[type]";
        return HtmlUnitUtil.waitAndReturnElementBySelectorWithTimeout(currentPage, cssSelector, MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS);
    }

    private void enterWithLogin() {
        clickLoginButton();
        logger.log("Waiting for password field to come up!");
        StopWatch timeoutStopWatch = new StopWatch();
        while (timeoutStopWatch.isTimePassedLessThanMs(MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS)) {
            if (isInvalidCredentialsMessagePresent()) {
                throw new WrongLoginFormat("Invalid credentials message was detected on the site.");
            }
            if (isSecurityImageLabelPresent()) {
                break;
            }
            waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL_MS);
        }
        waitBeforeLessJSTasksThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_PASSWORD_PAGE, MIN_JS_LOAD_TIME_MS);
    }

    private void enterWithPassword() {
        WebWindow window = currentPage.getEnclosingWindow();
        clickLoginButton();
        logger.log("Waiting for redirect...");
        while(currentPage == window.getEnclosedPage()) {
            try {
                Thread.sleep(MIN_JS_LOADING_WAIT_INTERVAL_MS);
            } catch (InterruptedException cause) {
                logger.log("Gamma ray detected! Must comply.");
                Thread.currentThread().interrupt();
                throw new RuntimeException(cause);
            }

            if (isInvalidCredentialsMessagePresent()){
                throw new WrongPassword("Password was incorrect!");
            }
        }
        waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL_MS);
        currentPage = (HtmlPage) window.getEnclosedPage();
    }

    private HtmlSelect getAccountSelectMenu() {
        String cssSelector = "select[name=\"account\"]";
        return HtmlUnitUtil.waitAndReturnElementBySelectorWithTimeout(currentPage, cssSelector, MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS);
    }

    private void saveAccountNumberFromAccountSelectMenu(HtmlSelect selectMenu) {
        List<HtmlOption> selectListOptions = selectMenu.getOptions();
        for (HtmlOption option : selectListOptions) {
            saveDataFromSelectMenuElement(option);
        }
    }

    private void saveDataFromSelectMenuElement(HtmlOption option) {
        String optionText = option.getTextContent().replace("\n", "");
        String[] splittedOptionText = optionText.split(" ");
        String accountTitle = buildAccountTitle(splittedOptionText).trim();
        String accountNumber = buildAccountNumber(splittedOptionText);
        Account currentAccount = accountMap.get(accountTitle);
        if (currentAccount == null) {
            accountMap.put(accountTitle, new Account(accountTitle, accountNumber, "?"));
        }
    }

    private String buildAccountTitle(String[] splittedSelectListLine) {
        StringBuilder accNameBuilder = new StringBuilder();
        for (int i = 0; i < splittedSelectListLine.length - 1 - BLOCKS_IN_ACCOUNT_NUMBER; i++) {
            accNameBuilder.append(splittedSelectListLine[i] + " ");
        }
        accNameBuilder.append(splittedSelectListLine[splittedSelectListLine.length - 1 - BLOCKS_IN_ACCOUNT_NUMBER]);
        return accNameBuilder.toString();
    }

    private String buildAccountNumber(String[] accInfoSplitLine) {
        StringBuilder accountNumBuilder = new StringBuilder();
        for(int i = accInfoSplitLine.length - BLOCKS_IN_ACCOUNT_NUMBER; i < accInfoSplitLine.length; i++){
            accountNumBuilder.append(accInfoSplitLine[i]);
        }
        return "PL" + accountNumBuilder.toString();
    }

    private void waitBeforeLessJSTasksThanTargetWithMinTime(int targetJSJobCount, Integer minTime) {
        StopWatch stopWatch = new StopWatch();
        while (getJavascriptJobCount() > targetJSJobCount
                || stopWatch.isTimePassedLessThanMs(minTime) ) {
            waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL_MS);
        }
        waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL_MS);
    }

    private HtmlAnchor getUpperMenuLink() {
        String cssSelector = "a[href=\"#accounts\"]";
        HtmlAnchor anchor = HtmlUnitUtil.waitAndReturnElementBySelectorWithTimeout(currentPage, cssSelector, MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS);
        return anchor;
    }

    private void saveAccountValueFromUpperMenu() {
        clickOnLink(getUpperMenuLink());
        logger.log("Waiting for menu...");
        HtmlUnorderedList listOfAccountCards = getUnorderedListOfAccountCards();
        Iterator<DomElement> elemIterator = listOfAccountCards.getChildElements().iterator();
        while (elemIterator.hasNext()) {
            saveAccountValueFromMenuCard(elemIterator.next());
        }
    }

    private HtmlUnorderedList getUnorderedListOfAccountCards() {
        String cssSelector = "ul.submenu";
        return (HtmlUnorderedList) HtmlUnitUtil.waitAndReturnElementBySelectorWithTimeout(currentPage, cssSelector, MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS);
    }

    private void clickOnLink(HtmlAnchor link) {
        try {
            currentPage = link.click();
        } catch (IOException e) {
            throw new ConnectionProblem("IOException when opening the upper menu.", e);
        }
    }

    private void saveAccountValueFromMenuCard(DomElement cardContainer) {
        HtmlDivision nameDivison = cardContainer.querySelector("div.account-title");
        if (nameDivison == null) {
            return;
        }
        String accountName = nameDivison.getTextContent();
        HtmlSpan span = cardContainer.querySelector("a.x-element > div > span");
        String value = span.getTextContent();
        Account current = accountMap.get(accountName);
        current.setBalance(value);
    }

    private boolean isInvalidCredentialsMessagePresent() {
        return null != currentPage.getFirstByXPath("//div[@class=\"ui-error-message x-invalid-credentials\"]");
    }

    private int waitForJsToLoadInMs(long time) {
        return webClient.waitForBackgroundJavaScript(time);
    }

    private boolean isSecurityImageLabelPresent() {
        return null != HtmlUnitUtil.getHtmlElementFromPageWithClassOrNull(currentPage, "label-password-image");
    }

    private void clickLoginButton() {
        HtmlButton button = getLoginButton();
        try {
            button.click();
        } catch (IOException cause) {
            throw new ConnectionProblem("IOException thrown by the HtmlButton during authorisation!", cause);
        }
    }

    private HtmlButton getLoginButton() {
        return HtmlUnitUtil.waitAndReturnElementBySelectorWithTimeout(currentPage, "button[role=\"button\"]", MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS);
    }

    private int getJavascriptJobCount() {
        final JavaScriptJobManager tmpJobManager = currentPage.getEnclosingWindow().getJobManager();
        return tmpJobManager.getJobCount();
    }
}
