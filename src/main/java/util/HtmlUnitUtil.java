package util;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import scrapper.FailedToParseException;

public class HtmlUnitUtil {

    public static final Integer JS_WAIT_TIME_CHUNK = 200;
    private static Timer timer = new Timer();

    public static HtmlElement waitForFirstElementChildWithTimeout(DomElement parent, long maxTime){

        DomElement child = null;

        timer.reset();

        do {
            child = parent.getFirstElementChild();

            if (child != null){
                break;
            }

            parent.getPage().getWebClient().waitForBackgroundJavaScript(JS_WAIT_TIME_CHUNK);

        } while (timer.timePassedLessThanMs(maxTime));


        if ( (child instanceof HtmlElement) == false){
            throw new FailedToParseException("Dom element wasn't HtmlElement!");
        }

        return (HtmlElement) child;
    }


    public static <X> X waitForFirstByXPathWithTimeout(HtmlPage page, String xpath, long maxTime){

        timer.reset();


        X elem = null;

        do {
            elem = page.getFirstByXPath(xpath);

            if (elem != null){
                break;
            }

            page.getWebClient().waitForBackgroundJavaScript(JS_WAIT_TIME_CHUNK);

        } while (timer.timePassedLessThanMs(maxTime));

        if (elem == null){
            throw new FailedToParseException("Timeout reached while waiting for:[XPATH:" + xpath + "]!");
        }
        return elem;
    }


    public static HtmlElement getHtmlElementFromPageWithClassOrNull(HtmlPage page, String className){
        return page.getFirstByXPath("//*[contains(@class, '"+className+"')]");
    }

}
