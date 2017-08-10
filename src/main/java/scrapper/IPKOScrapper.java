package scrapper;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;
import sun.security.pkcs.ParsingException;

import javax.security.auth.login.FailedLoginException;
import java.io.IOException;
import java.rmi.AccessException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class IPKOScrapper {

    static final String URL = "https://www.ipko.pl";
    static final int LOGIN_PAGE_JS_ACTIVITY = 3;
    static final int PASS_PAGE_JS_ACTIVITY = 4;
    static final int HOME_PAGE_JS_ACTIVITY = 3;
    static final int ACC_PAGE_JS_ACTIVITY = 4;
    static final int WAIT_TIME_Q = 2000;
    static final int blocksInAccountNumber = 7;
    private boolean isAuthenticated = false;
    private WebClient webClient;
    private HtmlPage currentPage;

    public IPKOScrapper(){
        setUpConfiguration();
    }

    /**
     * This method returns select menu item from the current page
     * @param name the name parameter of the select menu
     * @return select menu element
     * @exception ParsingException on unexpected parsing error (site change?)
     */
    private HtmlSelect getSelectMenu(String name) throws ParsingException {
        HtmlSelect select = null;

        while ( (select = currentPage.getFirstByXPath("//select[@name=\""+ name +"\"]")) == null){
            jsWait(WAIT_TIME_Q);
        }

        return select;
    }

    /**
     * This method returns current user bank accounts in a List.
     * @return list of accounts.
     * @exception ParsingException on unexpected parsing error (site change?)
     * @exception AccessException user tried to retrieve account list before loggin in
     */
    public List<Account> listAccounts() throws ParsingException, AccessException {

        if (!isAuthenticated) throw new AccessException("Scrapper is not logged in! Please use authorise(account, passowrd) method.");

        //map that maps from account title to Account object to simplify data retrieval from multiple sources
        Map<String, Account> accountMap = new HashMap<String, Account>();

        HtmlSelect selectMenu = getSelectMenu("account");
        getDataFromUpperMenu(accountMap);
        getDataFromSelectMenu(accountMap, selectMenu);

        List<Account> accountList = accountMap
                .entrySet()
                .stream()
                .map(x -> x.getValue())
                .collect(Collectors.toList());

        return accountList;
    }


    /**
     * This fills up account map from upper horizontal menu. Data such as account name and account value is retrieved
     * @param accountMap map that will be filled with data
     * @param selectMenu
     * @exception ParsingException on unexpected parsing error (site change?)
     */
    private void getDataFromSelectMenu(Map<String, Account> accountMap, HtmlSelect selectMenu) throws ParsingException {

        List<HtmlOption> selectListOptions = selectMenu.getOptions();

        for(HtmlOption option : selectListOptions){

            String optionText = option.getTextContent();
            String[] splittedText = optionText.split(" ");

            Account currentAccount = accountMap.get(buildAccountTitle(splittedText));
            currentAccount.setIBAN(buildAccountNumber(splittedText));
        }

    }

    /**
     * This method builds an accounts title from string extracted from account selection element
     * @param accInfoSplitLine splitted line that contains account name
     * @return the title of an account
     */
    private String buildAccountTitle(String[] accInfoSplitLine){
        StringBuilder accNameBuilder = new StringBuilder();

        for(int i = 0; i < accInfoSplitLine.length - 1 - blocksInAccountNumber; i++){
            accNameBuilder.append(accInfoSplitLine[i] + " ");
        }

        accNameBuilder.append(accInfoSplitLine[accInfoSplitLine.length - 1 - blocksInAccountNumber]);

        return accNameBuilder.toString();
    }

    /**
     * This method builds an accounts number from string extracted from account selection element
     * @param accInfoSplitLine splitted line that contains account number
     * @return the number of an account
     */
    private String buildAccountNumber(String[] accInfoSplitLine){

        StringBuilder accountNumBuilder = new StringBuilder();

        for(int i = accInfoSplitLine.length - blocksInAccountNumber; i < accInfoSplitLine.length; i++){
            accountNumBuilder.append(accInfoSplitLine[i]);
        }

        return "PL" + accountNumBuilder.toString();
    }

    /**
     * This method waits until the amount of JS tasks will get lower than or equal to the target number.
     * Used to approximately determine when page is loaded
     * @param targetJSJobCount target JS task count
     * @param minTime the minimum amount of time method waits for JS
     */
    private void jsTasksLessThan(int targetJSJobCount, Integer minTime){

        long currentTime = System.currentTimeMillis();
        long targetTime = currentTime + minTime;

        while (getJavascriptJobCount() > targetJSJobCount || targetTime > System.currentTimeMillis() ) {
            jsWait(WAIT_TIME_Q);

        }

        jsWait(WAIT_TIME_Q);
    }

    /**
     * This fills up account map from the selector. Data such as account name and account number is retrieved
     * @param accountMap map that will be filled with data
     * @exception ParsingException on unexpected parsing error (site change?)
     */
    private void getDataFromUpperMenu(Map<String, Account> accountMap) throws ParsingException {

        HtmlAnchor link = currentPage.getFirstByXPath("//a[@href=\"#accounts\"]");

        try {
            currentPage = link.click();
        } catch (IOException e) {
            ParsingException ex = new ParsingException();
            ex.initCause(e);
            throw ex;
        }

        System.out.println("Waiting for menu...");
        jsTasksLessThan(ACC_PAGE_JS_ACTIVITY, 5000);

        HtmlElement accountDataBlock = currentPage.getFirstByXPath("//div[@class=\"x-submenu submenu-region\"]");
        DomElement inter = accountDataBlock.getFirstElementChild();
        HtmlUnorderedList unorderedList = (HtmlUnorderedList) inter.getFirstElementChild();
        Iterator<DomElement> elemIterator = unorderedList.getChildElements().iterator();

        while (elemIterator.hasNext()){
            extractMenuCard(accountMap, elemIterator.next());
        }

    }

    /**
     * This method processes single menu card
     * @param accountMap map that will be filled with data
     * @param current current menu card DOM element
     */
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


    private void setUpConfiguration(){

        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

        webClient = new WebClient();
        webClient.getOptions().setRedirectEnabled(true);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setHistoryPageCacheLimit(0);
        webClient.getCookieManager().setCookiesEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(true);
    }

    /**
     * This method checks for particular block that appears after wrong credentials were entered
     * @return  boolean answer
     */
    boolean isInvalidCredentialsMessagePresent(){
        return null != currentPage.getFirstByXPath("//div[@class=\"ui-error-message x-invalid-credentials\"]");
    }

    /**
     * Small wrapper method for waitForBackgroundJavaScript(long)
     * @param time the maximum amount of time to wait (in milliseconds)
     * @return  the number of background JavaScript jobs still executing or waiting to be executed when this method returns;
     * will be 0 if there are no jobs left to execute
     */
    private int jsWait(long time){
        return webClient.waitForBackgroundJavaScript(time);
    }

    /**
     * Method logs in the scrapper into the bank website. Should be invoked before any other method.
     * @param login login used in authentication
     * @param password password used in authentication
     * @exception FailedLoginException is thrown when the login or password is wrong
     * @exception IOException is thrown when there is a problem with the network
     */
    public void authorise(String login, String password) throws FailedLoginException, IOException {

        currentPage = webClient.getPage(URL);

        try {
            System.out.println("Opening login page...");
            jsTasksLessThan(LOGIN_PAGE_JS_ACTIVITY, 7000);

            insertStringIntoInput(login);
            tryProceedWithLogin();

            jsTasksLessThan(PASS_PAGE_JS_ACTIVITY, 5000);

            insertStringIntoInput(password);
            proceedWithPassword();

        } catch (ParsingException pe){
            FailedLoginException ex = new FailedLoginException("ParsingException in authorise()");
            ex.initCause(pe);
            throw ex;
        }

        System.out.println("Loading home page...");
        jsTasksLessThan(HOME_PAGE_JS_ACTIVITY, 10000);

        isAuthenticated = true;
    }

    /**
     * Method proceeds with the login part of the authentication
     * @exception FailedLoginException is thrown when the login is wrong
     */
    private void tryProceedWithLogin() throws FailedLoginException {

        HtmlElement buttonsDivParent1 = currentPage.getFirstByXPath("//div[@class=\"ui-inplace-dialog-buttonset\"]");

        HtmlButton loginButton = (HtmlButton) buttonsDivParent1.getFirstElementChild();

        try {
            currentPage = loginButton.click();
        } catch (IOException cause) {
            FailedLoginException ex = new FailedLoginException("IOException in HtmlButton!");
            ex.initCause(cause);
            throw ex;
        }

        while ( ! (isSecurityImageLabelPresent()
                || isInvalidCredentialsMessagePresent())){
            jsWait(WAIT_TIME_Q * 2);
            System.out.println("Waiting for password field to come up!");
        }

        if (isInvalidCredentialsMessagePresent()){
            throw new FailedLoginException("Login was incorrect!");
        }

        jsTasksLessThan(PASS_PAGE_JS_ACTIVITY, 10000);
    }

    /**
     * This method checks for particular block that appears after login was accepted
     * @return  boolean answer
     */
    private boolean isSecurityImageLabelPresent(){
        return null != currentPage.getFirstByXPath("//label[@class=\"label push-30 label-password-image\"]");
    }

    /**
     * Method proceeds with the password part of the authentication
     * @exception FailedLoginException is thrown when the password is wrong
     */
    private void proceedWithPassword() throws FailedLoginException {

        HtmlElement buttonsDivParent = currentPage
                .getFirstByXPath("//div[@class=\"ui-inplace-dialog-buttonset\"]");

        HtmlButton button = (HtmlButton) buttonsDivParent.getFirstElementChild();

        WebWindow window = currentPage.getEnclosingWindow();

        try {
            button.click();
        } catch (IOException cause) {
            FailedLoginException ex = new FailedLoginException("IOException in HtmlButton!");
            ex.initCause(cause);
            throw ex;
        }


        System.out.println("Waiting for redirect...");

        while(window.getEnclosedPage() == currentPage ) {
            try {
                Thread.sleep(WAIT_TIME_Q);
            } catch (InterruptedException cause) {
                FailedLoginException ex = new FailedLoginException("InterruptedException in Thread::sleep");
                ex.initCause(cause);
                throw ex;
            }

            if (isInvalidCredentialsMessagePresent()){
                throw new FailedLoginException("Password was incorrect!");
            }
        }

        jsWait(WAIT_TIME_Q);

        currentPage = (HtmlPage) window.getEnclosedPage();
    }

    /**
     * Small wrapper method for JavaScriptJobManager.getJobCount()
     * @return  the number of background JavaScript tasks executing or waiting to be executed when this method returns;
     */
    int getJavascriptJobCount() {
        final JavaScriptJobManager tmpJobManager = currentPage.getEnclosingWindow().getJobManager();
        return tmpJobManager.getJobCount();
    }

    /**
     * Method inserts the string into a particular input.
     * @param string the string that will be inserted
     * @exception ParsingException on unexpected parsing error (site change?)
     */
    private void insertStringIntoInput(String string) throws ParsingException {
        HtmlElement span = currentPage.getFirstByXPath("//span[@class=\"input\"]");
        HtmlElement inputElement = (HtmlElement) span.getFirstElementChild();
        HtmlInput inputBox = (HtmlInput) inputElement;
        inputBox.setValueAttribute(string);
    }


}
