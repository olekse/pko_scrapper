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
import util.Timer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class IPKOScrapper {
    private static final String URL = "https://www.ipko.pl";
    private static final int MIN_JS_LOAD_TIME_MS = 10000;
    private static final int MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS = 20000;
    private static final int MIN_BACKGROUND_JS_TASKS_ON_LOGIN_PAGE = 3;
    private static final int MIN_BACKGROUND_JS_TASKS_ON_PASSWORD_PAGE = 4;
    private static final int MIN_BACKGROUND_JS_TASKS_ON_HOME_PAGE = 3;
    private static final int MIN_BACKGROUND_JS_TASKS_ON_HOME_WITH_MENU = 4;
    private static final int MIN_JS_LOADING_WAIT_INTERVAL_MS = 2000;
    private static final int BLOCKS_IN_ACCOUNT_NUMBER = 7;
    public static final int NUM_OF_ELEMENTS_IN_UPPER_MENU_CARD = 3;
    private boolean isLoggedIn = false;
    private WebClient webClient;
    private HtmlPage currentPage;
    private Map<String, Account> accountMap = null;
    private Logger logger;

    public IPKOScrapper(Logger logger){
        this.logger = logger;
        accountMap = new HashMap<>();
        setupWebClientConfiguration();
    }

    private HtmlSelect getAccountSelectMenu() {
        String xPath = "//select[@name=\"account\"]";
        return HtmlUnitUtil.waitAndReturnElementByXPathWithTimeout(currentPage, xPath, MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS);
    }

    public Collection<Account> fetchAccountList() {
        if (!isLoggedIn) throw new ScrapperNotAuthenticatedException("Scrapper is not authenticated! Please use authenticate(account, password) method.");
        try {
            HtmlSelect selectMenu = getAccountSelectMenu();
            saveDataFromUpperMenu();
            saveDataFromAccountSelectMenu(selectMenu);
        } catch (FailedToParseException cause){
            throw new FailedToFetchDataException("Parsing exception occurred during fetching.", cause);
        }
        return accountMap.values();
    }

    private void saveDataFromAccountSelectMenu(HtmlSelect selectMenu) {
        List<HtmlOption> selectListOptions = selectMenu.getOptions();
        for(HtmlOption option : selectListOptions){
            saveDataFromSelectMenuElement(option);
        }
    }

    private void saveDataFromSelectMenuElement(HtmlOption option) {
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
            waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL_MS);
        }
        waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL_MS);
    }

    private HtmlAnchor getUpperMenuAnchor(){
        String accountLinkXpath = "//a[@href=\"#accounts\"]";
        return HtmlUnitUtil.waitAndReturnElementByXPathWithTimeout(currentPage, accountLinkXpath, MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS);
    }

    private HtmlDivision getUpperMenuDivision(){
        String upperMenuDivisionXpath = "//div[@class=\"x-submenu submenu-region\"]";
        return HtmlUnitUtil.waitAndReturnElementByXPathWithTimeout(currentPage, upperMenuDivisionXpath, MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS);
    }

    private void saveDataFromUpperMenu() {
        clickOnAnchor(getUpperMenuAnchor());
        logger.log("Waiting for menu...");
        waitBeforeLessJSTasksThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_HOME_WITH_MENU, MIN_JS_LOAD_TIME_MS);
        HtmlUnorderedList listOfAccountCards = getUnorderedListOfAccountCards();
        Iterator<DomElement> elemIterator = listOfAccountCards.getChildElements().iterator();
        while (elemIterator.hasNext()){
            saveDataFromMenuCard(elemIterator.next());
        }
    }

    private HtmlUnorderedList getUnorderedListOfAccountCards() {
        HtmlDivision upperMenuDiv = getUpperMenuDivision();
        DomElement upperMenuDivChild = getUpperMenuDivChild(upperMenuDiv);
        return getListOfAccountCards(upperMenuDivChild);
    }

    private void clickOnAnchor(HtmlAnchor link) {
        try {
            currentPage = link.click();
        } catch (IOException e) {
            throw new FailedToParseException("IOException when opening the upper menu.", e);
        }
    }

    private DomElement getUpperMenuDivChild(HtmlDivision upperMenuDiv) {
        return HtmlUnitUtil.waitAndReturnElementChildWithTimeout(upperMenuDiv, MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS);
    }

    private HtmlUnorderedList getListOfAccountCards(DomElement upperMenuDivChild) {
        return (HtmlUnorderedList) HtmlUnitUtil
                .waitAndReturnElementChildWithTimeout(upperMenuDivChild, MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS);
    }

    private void saveDataFromMenuCard(DomElement cardContainer) {
        if (cardContainer.getChildElementCount() == 0) {
            return;
        }
        DomElement menuCardAnchor = HtmlUnitUtil.waitAndReturnElementChildWithTimeout(cardContainer, MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS);
        if (menuCardAnchor.getChildElementCount() != NUM_OF_ELEMENTS_IN_UPPER_MENU_CARD) {
            return;
        }
        List<DomElement> cardElementList = getMenuElementsFromItsAnchor(menuCardAnchor);
        putFetchedDataIntoMap(cardElementList);
    }

    private List<DomElement> getMenuElementsFromItsAnchor(DomElement menuCardAnchor){
        Iterable<DomElement> cardElements = menuCardAnchor.getChildElements();
        return StreamSupport.stream(cardElements.spliterator(), false).collect(Collectors.toList());
    }

    private void putFetchedDataIntoMap(List<DomElement> cardElementList) {
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
        waitBeforeLessJSTasksThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_LOGIN_PAGE, MIN_JS_LOAD_TIME_MS);
        insertIntoInputField(login);
        enterWithLogin();
        waitBeforeLessJSTasksThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_PASSWORD_PAGE, MIN_JS_LOAD_TIME_MS);
        insertIntoInputField(password);
        enterWithPassword();
        logger.log("Loading home page...");
        waitBeforeLessJSTasksThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_HOME_PAGE, MIN_JS_LOAD_TIME_MS);
        isLoggedIn = true;
    }

    private void enterWithLogin()  {
        clickLoginButton();
        while ( (isSecurityImageLabelPresent()
                || isInvalidCredentialsMessagePresent()) == false){
            waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL_MS * 2);
            logger.log("Waiting for password field to come up!");
        }
        if (isInvalidCredentialsMessagePresent()){
            throw new WrongAccountNumberException("Invalid credentials message was detected on the site.");
        }
        waitBeforeLessJSTasksThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_PASSWORD_PAGE, MIN_JS_LOAD_TIME_MS);
    }

    private boolean isSecurityImageLabelPresent(){
        return null != HtmlUnitUtil.getHtmlElementFromPageWithClassOrNull(currentPage, "label-password-image");
    }

    private HtmlButton getLoginButton(){
        String buttonsDivParentXPath = "//div[@class=\"ui-inplace-dialog-buttonset\"]";
        HtmlElement buttonsDivParent = HtmlUnitUtil.waitAndReturnElementByXPathWithTimeout(currentPage, buttonsDivParentXPath, MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS);
        return (HtmlButton) HtmlUnitUtil.waitAndReturnElementChildWithTimeout(buttonsDivParent, MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS);
    }

    private void enterWithPassword() {
        WebWindow window = currentPage.getEnclosingWindow();
        clickLoginButton();
        logger.log("Waiting for redirect...");
        while(currentPage == window.getEnclosedPage()) {
            try {
                Thread.sleep(MIN_JS_LOADING_WAIT_INTERVAL_MS);
            } catch (InterruptedException cause) {
                throw new FailedToLoginException("InterruptedException in System.sleep() while waiting for redirect!", cause);
            }

            if (isInvalidCredentialsMessagePresent()){
                throw new WrongPasswordException("Password was incorrect!");
            }
        }
        waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL_MS);
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
        HtmlElement span = HtmlUnitUtil.waitAndReturnElementByXPathWithTimeout(currentPage, xPathInput, MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS);
        HtmlInput inputBox = (HtmlInput)  HtmlUnitUtil.waitAndReturnElementChildWithTimeout(span, MAX_WAITING_TIME_FOR_HTML_ELEM_TO_LOAD_MS);
        inputBox.setValueAttribute(string);
    }
}
