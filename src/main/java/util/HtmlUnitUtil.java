package util;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import exception.ConnectionProblem;
import org.w3c.dom.Node;
import se.fishtank.css.selectors.dom.DOMNodeSelector;

public class HtmlUnitUtil {

    public static final Integer JS_WAIT_TIME_CHUNK = 200;

    public static HtmlElement waitAndReturnElementChildWithTimeout(DomElement parent, long maxTime){
        DomElement child = null;
        StopWatch stopWatch = new StopWatch();
        do {
            child = parent.getFirstElementChild();
            if (child != null){
                break;
            }
            parent.getPage().getWebClient().waitForBackgroundJavaScript(JS_WAIT_TIME_CHUNK);
        } while (stopWatch.isTimePassedLessThanMs(maxTime));
        if ( (child instanceof HtmlElement) == false){
            throw new ConnectionProblem("Dom element wasn't HtmlElement!");
        }
        return (HtmlElement) child;
    }

    public static void foo(Node node){
        DOMNodeSelector domNodeSelector = new DOMNodeSelector(node);

    }

    public static <X> X waitAndReturnElementByXPathWithTimeout(HtmlPage page, String xpath, long maxTime){
        StopWatch stopWatch = new StopWatch();
        X elem = null;
        do {
            elem = page.getFirstByXPath(xpath);
            if (elem != null) {
                break;
            }
            page.getWebClient().waitForBackgroundJavaScript(JS_WAIT_TIME_CHUNK);
        } while (stopWatch.isTimePassedLessThanMs(maxTime));

        if (elem == null){
            throw new ConnectionProblem("Timeout reached while waiting for:[XPATH:" + xpath + "]!");
        }
        return elem;
    }

    public static HtmlElement getHtmlElementFromPageWithClassOrNull(HtmlPage page, String className){
        return page.getFirstByXPath("//*[contains(@class, '"+className+"')]");
    }

}
