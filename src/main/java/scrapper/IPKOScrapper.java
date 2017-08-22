package scrapper;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import exception.*;
import util.HtmlUnitUtil;
import util.Logger;
import util.SystemOutLogger;
import util.Timer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class IPKOScrapper {

    private static final String URL = "https://www.ipko.pl";
    private static final int MAX_WAITING_TIME_FOR_ELEM_TO_LOAD = 20000;
    private static final int MIN_BACKGROUND_JS_TASKS_ON_LOGIN_PAGE = 3;
    private static final int MIN_BACKGROUND_JS_TASKS_ON_PASSWORD_PAGE = 4;
    private static final int MIN_BACKGROUND_JS_TASKS_ON_HOME_PAGE = 3;
    private static final int MIN_BACKGROUND_JS_TASKS_ON_HOME_WITH_MENU = 4;
    private static final int MIN_JS_LOADING_WAIT_INTERVAL = 2000;
    private static final int BLOCKS_IN_ACCOUNT_NUMBER = 7;
    public static final int NUM_OF_ELEMENTS_IN_UPPER_MENU_CARD = 3;

    private boolean isLoggedIn = false;
    private WebClient webClient;
    private HtmlPage currentPage;
    private Map<String, Account> accountMap = null;

    private Logger logger;

    public IPKOScrapper(){
        this.logger = new SystemOutLogger();
        accountMap = new HashMap<>();
        setupWebClientConfiguration();
    }

    private HtmlSelect getSelectMenu() {
        String xPath = "//select[@name=\"account\"]";
        return HtmlUnitUtil.waitForFirstByXPathWithTimeout(currentPage, xPath, MAX_WAITING_TIME_FOR_ELEM_TO_LOAD);
    }

    public Collection<Account> fetchAccountList() {
        if (!isLoggedIn) throw new ScrapperNotAuthenticatedException("Scrapper is not authenticated! Please use authenticate(account, password) method.");

        try {
            HtmlSelect selectMenu = getSelectMenu();
            fetchDataFromUpperMenu();
            fetchDataFromSelectMenu(selectMenu);
        } catch (FailedToParseException cause){
            throw new FailedToFetchDataException("Parsing exception occurred during fetching.", cause);
        }

        return accountMap.values();
    }

    private void fetchDataFromSelectMenu(HtmlSelect selectMenu) {
        List<HtmlOption> selectListOptions = selectMenu.getOptions();
        for(HtmlOption option : selectListOptions){
            fetchDataFromSelectMenuElement(option);
        }
    }

    private void fetchDataFromSelectMenuElement(HtmlOption option) {
        String optionText = option.getTextContent();
        String[] splittedOptionText = optionText.split(" ");
        String accountTitle = buildAccountTitle(splittedOptionText);
        String accountNumber = buildAccountNumber(splittedOptionText);
        Account currentAccount = accountMap.get(accountTitle);
        currentAccount.setIBAN(accountNumber);
    }

    private String buildAccountTitle(String[] splittedSelectListLine){
        StringBuilder accNameBuilder = new StringBuilder();

        for(int i = 0; i < splittedSelectListLine.length - 1 - BLOCKS_IN_ACCOUNT_NUMBER; i++){
            accNameBuilder.append(splittedSelectListLine[i] + " ");
        }
        accNameBuilder.append(splittedSelectListLine[splittedSelectListLine.length - 1 - BLOCKS_IN_ACCOUNT_NUMBER]);
        return accNameBuilder.toString();
    }

    private String buildAccountNumber(String[] accInfoSplitLine){
        StringBuilder accountNumBuilder = new StringBuilder();
        for(int i = accInfoSplitLine.length - BLOCKS_IN_ACCOUNT_NUMBER; i < accInfoSplitLine.length; i++){
            accountNumBuilder.append(accInfoSplitLine[i]);
        }
        return "PL" + accountNumBuilder.toString();
    }

    private void waitBeforeLessJSTasksThanTargetWithMinTime(int targetJSJobCount, Integer minTime){
        Timer timer = new Timer();
        timer.reset();

        while (getJavascriptJobCount() > targetJSJobCount
                || timer.timePassedLessThanMs(minTime) ) {
            waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL);
        }
        waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL);
    }

    private HtmlAnchor getUpperMenuAnchor(){
        String accountLinkXpath = "//a[@href=\"#accounts\"]";
        return HtmlUnitUtil.waitForFirstByXPathWithTimeout(currentPage, accountLinkXpath, MAX_WAITING_TIME_FOR_ELEM_TO_LOAD);
    }

    private HtmlDivision getUpperMenuDivision(){
        String upperMenuDivisionXpath = "//div[@class=\"x-submenu submenu-region\"]";
        return HtmlUnitUtil.waitForFirstByXPathWithTimeout(currentPage, upperMenuDivisionXpath, MAX_WAITING_TIME_FOR_ELEM_TO_LOAD);
    }

    private void fetchDataFromUpperMenu() {
        HtmlAnchor link = getUpperMenuAnchor();

        try {
            currentPage = link.click();
        } catch (IOException e) {
            throw new FailedToParseException("IOException when opening the upper menu.", e);
        }

        logger.log("Waiting for menu...");
        waitBeforeLessJSTasksThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_HOME_WITH_MENU, 5000);
        HtmlDivision upperMenuDiv = getUpperMenuDivision();
        DomElement upperMenuDivChild = HtmlUnitUtil.waitForFirstElementChildWithTimeout(upperMenuDiv, MAX_WAITING_TIME_FOR_ELEM_TO_LOAD);
        HtmlUnorderedList listOfAccountCards = (HtmlUnorderedList) HtmlUnitUtil
                .waitForFirstElementChildWithTimeout(upperMenuDivChild, MAX_WAITING_TIME_FOR_ELEM_TO_LOAD);
        Iterator<DomElement> elemIterator = listOfAccountCards.getChildElements().iterator();
        while (elemIterator.hasNext()){
            fetchDataFromUpperMenuCard(elemIterator.next());
        }
    }

    private void fetchDataFromUpperMenuCard(DomElement cardContainer) {

        if (cardContainer.getChildElementCount() == 0){
            return;
        }

        DomElement menuCardAnchor = HtmlUnitUtil.waitForFirstElementChildWithTimeout(cardContainer, MAX_WAITING_TIME_FOR_ELEM_TO_LOAD);

        if (menuCardAnchor.getChildElementCount() != NUM_OF_ELEMENTS_IN_UPPER_MENU_CARD){
            return;
        }
        Iterable<DomElement> cardElements = menuCardAnchor.getChildElements();
        List<DomElement> cardElementList = StreamSupport.stream(cardElements.spliterator(), false).collect(Collectors.toList());
        String accountName = cardElementList.get(0).getAttribute("data-text");
        String amount = cardElementList.get(2).getFirstElementChild().getTextContent();
        accountMap.put(accountName, new Account(accountName, null, amount));
    }


    private void setupWebClientConfiguration(){
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
        webClient = new WebClient();
        webClient.getOptions().setRedirectEnabled(true);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setHistoryPageCacheLimit(0);
        webClient.getCookieManager().setCookiesEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(true);
    }

    private boolean isInvalidCredentialsMessagePresent(){
        return null != currentPage.getFirstByXPath("//div[@class=\"ui-error-message x-invalid-credentials\"]");
    }

    private int waitForJsToLoadInMs(long time){
        return webClient.waitForBackgroundJavaScript(time);
    }

    private void loadStartingPage(){
        try{
            currentPage = webClient.getPage(URL);
        } catch (IOException cause){
            throw new FailedToLoginException("Failed to load a page!", cause);
        }
    }

    public void authenticate(String login, String password) {
        logger.log("Opening login page...");
        loadStartingPage();
        waitBeforeLessJSTasksThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_LOGIN_PAGE, 7000);
        insertIntoInputField(login);
        enterWithLogin();
        waitBeforeLessJSTasksThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_PASSWORD_PAGE, 5000);
        insertIntoInputField(password);
        enterWithPassword();
        logger.log("Loading home page...");
        waitBeforeLessJSTasksThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_HOME_PAGE, 10000);
        isLoggedIn = true;
    }





    private void enterWithLogin()  {
        HtmlButton loginButton = getLoginButton();
        try {
            currentPage = loginButton.click();
        } catch (IOException cause) {
            throw new FailedToLoginException("IOException after a login button click!", cause);
        }

        while ( ! (isSecurityImageLabelPresent()
                || isInvalidCredentialsMessagePresent())){
            waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL * 2);
            logger.log("Waiting for password field to come up!");
        }

        if (isInvalidCredentialsMessagePresent()){
            throw new WrongAccountNumberException("Invalid credentials message was detected on the site.");
        }

        waitBeforeLessJSTasksThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_PASSWORD_PAGE, 10000);
    }

    private boolean isSecurityImageLabelPresent(){
        return null != HtmlUnitUtil.getHtmlElementFromPageWithClassOrNull(currentPage, "label-password-image");
    }

    private HtmlButton getLoginButton(){
        String buttonsDivParentXPath = "//div[@class=\"ui-inplace-dialog-buttonset\"]";
        HtmlElement buttonsDivParent = HtmlUnitUtil.waitForFirstByXPathWithTimeout(currentPage, buttonsDivParentXPath, MAX_WAITING_TIME_FOR_ELEM_TO_LOAD);
        return (HtmlButton) HtmlUnitUtil.waitForFirstElementChildWithTimeout(buttonsDivParent, MAX_WAITING_TIME_FOR_ELEM_TO_LOAD);
    }

    private void enterWithPassword() {

        WebWindow window = currentPage.getEnclosingWindow();

        clickLoginButton();

        logger.log("Waiting for redirect...");

        while(currentPage == window.getEnclosedPage()) {

            try {
                Thread.sleep(MIN_JS_LOADING_WAIT_INTERVAL);
            } catch (InterruptedException cause) {
                throw new FailedToLoginException("InterruptedException in System.sleep() while waiting for redirect!", cause);
            }

            if (isInvalidCredentialsMessagePresent()){
                throw new WrongPasswordException("Password was incorrect!");
            }
        }

        waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL);
        currentPage = (HtmlPage) window.getEnclosedPage();
    }

    private void clickLoginButton() {
        HtmlButton button = getLoginButton();
        try {
            button.click();
        } catch (IOException cause) {
            throw new FailedToLoginException("IOException thrown by the HtmlButton during authorisation!", cause);
        }
    }

    private int getJavascriptJobCount() {
        final JavaScriptJobManager tmpJobManager = currentPage.getEnclosingWindow().getJobManager();
        return tmpJobManager.getJobCount();
    }

    private void insertIntoInputField(String string)  {
        String xPathInput = "//span[@class=\"input\"]";
        HtmlElement span = HtmlUnitUtil.waitForFirstByXPathWithTimeout(currentPage, xPathInput, MAX_WAITING_TIME_FOR_ELEM_TO_LOAD);
        HtmlInput inputBox = (HtmlInput)  HtmlUnitUtil.waitForFirstElementChildWithTimeout(span, MAX_WAITING_TIME_FOR_ELEM_TO_LOAD);
        inputBox.setValueAttribute(string);
    }
}
