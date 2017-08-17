package scrapper;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;
import sun.security.pkcs.ParsingException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import util.HtmlUnitUtil;
import util.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class IPKOScrapper {

    private static final String URL = "https://www.ipko.pl";
    private static final int MIN_BACKGROUND_JS_TASKS_ON_LOGIN_PAGE = 3;
    private static final int MIN_BACKGROUND_JS_TASKS_ON_PASSWORD_PAGE = 4;
    private static final int MIN_BACKGROUND_JS_TASKS_ON_HOME_PAGE = 3;
    private static final int MIN_BACKGROUND_JS_TASKS_ON_HOME_WITH_MENU = 4;
    private static final int MIN_JS_LOADING_WAIT_INTERVAL = 2000;
    private static final int BLOCKS_IN_ACCOUNT_NUMBER = 7;
    private boolean isAuthenticated = false;
    private WebClient webClient;
    private HtmlPage currentPage;

    private Logger logger;

    public IPKOScrapper(Logger logger){
        this.logger = logger;
        setupWebClientConfiguration();
    }

    private HtmlSelect getSelectMenu(String name) throws ParsingException {
        HtmlSelect select;
        while ((select = currentPage.getFirstByXPath("//select[@name=\"" + name + "\"]")) == null) {
            waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL);
        }
        return select;
    }

    public List<Account> fetchAccountList() {
        if (!isAuthenticated) throw new ScrapperNotAuthorisedException("Scrapper is not authorised! Please use authorise(account, password) method.");
        //maps account title to Account object to simplify data retrieval from multiple sources
        Map<String, Account> accountMap = new HashMap<>();

        try {
            HtmlSelect selectMenu = getSelectMenu("account");
            getDataFromUpperMenu(accountMap);
            getDataFromSelectMenu(accountMap, selectMenu);
        } catch (ParsingException cause){
            throw new FailedToFetchDataException("Parsing exception occurred during fetching.", cause);
        }

        return accountMap
                .entrySet()
                .stream()
                .map(x -> x.getValue())
                .collect(Collectors.toList());
    }

    private void getDataFromSelectMenu(Map<String, Account> accountMap, HtmlSelect selectMenu) throws ParsingException {
        List<HtmlOption> selectListOptions = selectMenu.getOptions();
        for(HtmlOption option : selectListOptions){
            String optionText = option.getTextContent();
            String[] splittedText = optionText.split(" ");
            Account currentAccount = accountMap.get(buildAccountTitle(splittedText));
            currentAccount.setIBAN(buildAccountNumber(splittedText));
        }
    }

    private String buildAccountTitle(String[] accInfoSplitLine){
        StringBuilder accNameBuilder = new StringBuilder();
        for(int i = 0; i < accInfoSplitLine.length - 1 - BLOCKS_IN_ACCOUNT_NUMBER; i++){
            accNameBuilder.append(accInfoSplitLine[i] + " ");
        }
        accNameBuilder.append(accInfoSplitLine[accInfoSplitLine.length - 1 - BLOCKS_IN_ACCOUNT_NUMBER]);
        return accNameBuilder.toString();
    }

    private String buildAccountNumber(String[] accInfoSplitLine){
        StringBuilder accountNumBuilder = new StringBuilder();
        for(int i = accInfoSplitLine.length - BLOCKS_IN_ACCOUNT_NUMBER; i < accInfoSplitLine.length; i++){
            accountNumBuilder.append(accInfoSplitLine[i]);
        }
        return "PL" + accountNumBuilder.toString();
    }

    private void waitBeforeJSTaskCountLessThanTargetWithMinTime(int targetJSJobCount, Integer minTime){
        long currentTime = System.currentTimeMillis();
        long targetTime = currentTime + minTime;
        while (getJavascriptJobCount() > targetJSJobCount
                || targetTime > System.currentTimeMillis() ) {
            waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL);
        }
        waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL);
    }

    private void getDataFromUpperMenu(Map<String, Account> accountMap) throws ParsingException {
        HtmlAnchor link = currentPage.getFirstByXPath("//a[@href=\"#accounts\"]");
        try {
            currentPage = link.click();
        } catch (IOException e) {
            ParsingException ex = new ParsingException();
            ex.initCause(e);
            throw ex;
        }
        logger.log("Waiting for menu...");
        waitBeforeJSTaskCountLessThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_HOME_WITH_MENU, 5000);
        HtmlElement accountDataBlock = currentPage.getFirstByXPath("//div[@class=\"x-submenu submenu-region\"]");
        DomElement inter = accountDataBlock.getFirstElementChild();
        HtmlUnorderedList unorderedList = (HtmlUnorderedList) inter.getFirstElementChild();
        Iterator<DomElement> elemIterator = unorderedList.getChildElements().iterator();
            while (elemIterator.hasNext()){
            extractMenuCard(accountMap, elemIterator.next());
        }
    }

    private void extractMenuCard(Map<String, Account> accountMap, DomElement current) {
        if (current.getChildElementCount() == 0){
            return;
        }
        DomElement anchor = current.getFirstElementChild();
        if (anchor.getChildElementCount() != 3){
            return;
        }
        Iterable<DomElement> cardElements = anchor.getChildElements();

        List<DomElement> cardElementList = StreamSupport.stream(cardElements.spliterator(), false)
                .collect(Collectors.toList());
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

    public void loadStartingPage(){
        try{
            currentPage = webClient.getPage(URL);
        } catch (IOException cause){
            throw new FailedToLoginException("Failed to load a page!", cause);
        }
    }


    public void authorise(String login, String password) {

        loadStartingPage();


        logger.log("Opening login page...");
        waitBeforeJSTaskCountLessThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_LOGIN_PAGE, 7000);
        insertStringIntoInput(login);
        tryProceedWithLogin();
        waitBeforeJSTaskCountLessThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_PASSWORD_PAGE, 5000);
        insertStringIntoInput(password);
        proceedWithPassword();

        logger.log("Loading home page...");
        waitBeforeJSTaskCountLessThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_HOME_PAGE, 10000);
        isAuthenticated = true;
    }


    private void tryProceedWithLogin()  {


        HtmlElement buttonsDivParent1 = currentPage.getFirstByXPath("//div[@class=\"ui-inplace-dialog-buttonset\"]");



        HtmlButton loginButton = (HtmlButton) buttonsDivParent1.getFirstElementChild();

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

        waitBeforeJSTaskCountLessThanTargetWithMinTime(MIN_BACKGROUND_JS_TASKS_ON_PASSWORD_PAGE, 10000);
    }

    private boolean isSecurityImageLabelPresent(){
        return null != HtmlUnitUtil.getHtmlElementFromPageWithClassOrNull(currentPage, "label-password-image");
    }

    private void proceedWithPassword() {
        //Refactoring notes.

        //check if element is present
        //get element
        //get data from element or interact with the element


        HtmlElement buttonsDivParent = currentPage
                .getFirstByXPath("//div[@class=\"ui-inplace-dialog-buttonset\"]");
        HtmlButton button = (HtmlButton) buttonsDivParent.getFirstElementChild();
        WebWindow window = currentPage.getEnclosingWindow();

        try {
            button.click();
        } catch (IOException cause) {
            throw new FailedToLoginException("IOException thrown by a HtmlButton during authorisation!", cause);
        }

        logger.log("Waiting for redirect...");

        while(window.getEnclosedPage() == currentPage ) {
            try {
                Thread.sleep(MIN_JS_LOADING_WAIT_INTERVAL);
            } catch (InterruptedException cause) {
                throw new FailedToLoginException("InterruptedException in System.sleep() during redirection waiting!", cause);
            }

            if (isInvalidCredentialsMessagePresent()){
                throw new WrongPasswordException("Password was incorrect!");
            }
        }

        waitForJsToLoadInMs(MIN_JS_LOADING_WAIT_INTERVAL);
        currentPage = (HtmlPage) window.getEnclosedPage();
    }

    private int getJavascriptJobCount() {
        final JavaScriptJobManager tmpJobManager = currentPage.getEnclosingWindow().getJobManager();
        return tmpJobManager.getJobCount();
    }

    private void insertStringIntoInput(String string)  {
        HtmlElement span = currentPage.getFirstByXPath("//span[@class=\"input\"]");
        HtmlInput inputBox = (HtmlInput)  span.getFirstElementChild();
        inputBox.setValueAttribute(string);
    }


}
