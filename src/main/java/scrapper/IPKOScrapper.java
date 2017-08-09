package scrapper;

/**
 * Created by OleksandrSerediuk on 08.08.2017.
 */
import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;
import sun.security.pkcs.ParsingException;

import javax.naming.AuthenticationException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by OleksandrSerediuk on 08.08.2017.
 */
public class IPKOScrapper {

    static final String URL = "https://www.ipko.pl";
    static final int LOGIN_PAGE_JS_ACTIVITY = 3;
    static final int PASS_PAGE_JS_ACTIVITY = 4;
    static final int HOME_PAGE_JS_ACTIVITY = 4;
    static final int ACC_PAGE_JS_ACTIVITY = 4;
    static final int WAIT_TIME_Q = 500;
    static final int blocksInAccountNumber = 7;
    boolean isAuthenticated = false;
    WebClient webClient;
    HtmlPage currentPage;

    public IPKOScrapper(){
        setUpConfiguration();
    }


    public List<Account> listAccounts() throws ParsingException {

        if (!isAuthenticated) throw new IllegalStateException("Scrapper is not logged in! Please use authenticate(account, passowrd) method.");

        Map<String, Account> accountMap = new HashMap<String, Account>();

        getDataFromUpperMenu(accountMap);
        getDataFromSelectMenu(accountMap);

        List<Account> accountList = accountMap
                .entrySet()
                .stream()
                .map(x -> x.getValue())
                .collect(Collectors.toList());

        return accountList;
    }

    private void getDataFromSelectMenu(Map<String, Account> accountMap) {

        HtmlSelect select = currentPage.getFirstByXPath("//select[@name=\"account\"]");

        List<HtmlOption> selectListOptions = select.getOptions();

        for(HtmlOption option : selectListOptions){

            String optionText = option.getTextContent();
            String[] splittedText = optionText.split(" ");

            Account currentAccount = accountMap.get(buildAccountName(splittedText));
            currentAccount.setIBAN(buildAccountNumber(splittedText));
        }

    }

    private String buildAccountName(String[] accInfoSplitLine){
        StringBuilder accNameBuilder = new StringBuilder();

        for(int i = 0; i < accInfoSplitLine.length - 1 - blocksInAccountNumber; i++){
            accNameBuilder.append(accInfoSplitLine[i] + " ");
        }

        accNameBuilder.append(accInfoSplitLine[accInfoSplitLine.length - 1 - blocksInAccountNumber]);

        return accNameBuilder.toString();
    }

    private String buildAccountNumber(String[] slicedText){

        StringBuilder accountNumBuilder = new StringBuilder();

        for(int i = slicedText.length - blocksInAccountNumber; i < slicedText.length; i++){
            accountNumBuilder.append(slicedText[i]);
        }

        return "PL" + accountNumBuilder.toString();
    }

    private void jsThreadsLessThan(int targetJSJobCount){
        int i = 0;
        webClient.waitForBackgroundJavaScriptStartingBefore(WAIT_TIME_Q);
        while ((i = getJavascriptJobCount()) > targetJSJobCount){
            System.out.println(i + " out of " + targetJSJobCount);
            webClient.waitForBackgroundJavaScriptStartingBefore(WAIT_TIME_Q);
        }
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

        System.out.println("Opening menu...");
        webClient.waitForBackgroundJavaScriptStartingBefore(WAIT_TIME_Q*4);
        jsThreadsLessThan(ACC_PAGE_JS_ACTIVITY);
        webClient.waitForBackgroundJavaScriptStartingBefore(WAIT_TIME_Q*4);

        HtmlElement accountDataBlock = currentPage.getFirstByXPath("//div[@class=\"x-submenu submenu-region\"]");

        HtmlUnorderedList unorderedList = (HtmlUnorderedList) accountDataBlock.getFirstElementChild().getFirstElementChild();

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

    private void setUpConfiguration(){

        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

        webClient = new WebClient();
        webClient.getOptions().setRedirectEnabled(true);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setHistoryPageCacheLimit(0);
        webClient.getCookieManager().setCookiesEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(true);
    }

    boolean isInvalidCredentialsMessagePresent(){
        return null != currentPage.getFirstByXPath("//div[@class=\"ui-error-message x-invalid-credentials\"]");
    }

    public void authenticate(String login, String password) throws AuthenticationException {

        try {
            currentPage = webClient.getPage(URL);
        } catch (IOException cause) {
            AuthenticationException ex = new AuthenticationException("IOException in WebClient::getPage!");
            ex.initCause(cause);
            throw ex;
        }

        System.out.println("Opening login page...");
        jsThreadsLessThan(LOGIN_PAGE_JS_ACTIVITY);

        insertStringIntoInput(login);
        proceedWithLogin();

        insertStringIntoInput(password);
        proceedWithPassword();


        System.out.println("Opening home page...");
        jsThreadsLessThan(HOME_PAGE_JS_ACTIVITY);
        isAuthenticated = true;
    }

    private void proceedWithLogin() throws AuthenticationException {

        HtmlElement buttonsDivParent1 = currentPage.getFirstByXPath("//div[@class=\"ui-inplace-dialog-buttonset\"]");
        HtmlButton loginButton = (HtmlButton) buttonsDivParent1.getFirstElementChild();

        try {
            currentPage = loginButton.click();
        } catch (IOException cause) {
            AuthenticationException ex = new AuthenticationException("IOException in HtmlButton!");
            ex.initCause(cause);
            throw ex;
        }

        webClient.waitForBackgroundJavaScript(WAIT_TIME_Q * 2);

        if (isInvalidCredentialsMessagePresent()){
            throw new AuthenticationException("Login number was incorrect!");
        }

        System.out.println("Opening password page...");
        jsThreadsLessThan(PASS_PAGE_JS_ACTIVITY);

        webClient.waitForBackgroundJavaScript(WAIT_TIME_Q*2);
    }

    private void proceedWithPassword() throws AuthenticationException {

        HtmlElement buttonsDivParent = currentPage
                .getFirstByXPath("//div[@class=\"ui-inplace-dialog-buttonset\"]");

        HtmlButton button = (HtmlButton) buttonsDivParent.getFirstElementChild();

        WebWindow window = currentPage.getEnclosingWindow();

        try {
            button.click();
        } catch (IOException cause) {
            AuthenticationException ex = new AuthenticationException("IOException in HtmlButton!");
            ex.initCause(cause);
            throw ex;
        }

        webClient.waitForBackgroundJavaScript(WAIT_TIME_Q * 2);
        if (isInvalidCredentialsMessagePresent()){
            throw new AuthenticationException("Password was incorrect!");
        }

        System.out.println("Waiting for redirect...");
        while(window.getEnclosedPage() == currentPage) {
            try {
                Thread.sleep(WAIT_TIME_Q);
            } catch (InterruptedException cause) {
                AuthenticationException ex = new AuthenticationException("InterruptedException in Thread::sleep");
                ex.initCause(cause);
                throw ex;
            }
        }

        webClient.waitForBackgroundJavaScript(WAIT_TIME_Q * 4);

        currentPage = (HtmlPage) window.getEnclosedPage();
    }

    int getJavascriptJobCount() {
        final JavaScriptJobManager tmpJobManager = currentPage.getEnclosingWindow().getJobManager();
        return tmpJobManager.getJobCount();
    }

    private void insertStringIntoInput(String string)  {
        HtmlElement span = currentPage.getFirstByXPath("//span[@class=\"input\"]");
        HtmlElement inputElement = (HtmlElement) span.getFirstElementChild();
        HtmlInput intputBox = (HtmlInput)inputElement;
        intputBox.setValueAttribute(string);
    }


}
