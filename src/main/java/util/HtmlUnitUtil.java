package util;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import exception.ConnectionProblem;
import org.w3c.dom.Node;
import se.fishtank.css.selectors.dom.DOMNodeSelector;

public class HtmlUnitUtil {

    public static final Integer JS_WAIT_TIME_CHUNK = 200;

    public static <X extends DomNode> X waitAndReturnElementBySelectorWithTimeout(HtmlPage page, String selector, long maxTime){
        StopWatch stopWatch = new StopWatch();
        X elem = null;
        do {
            elem = page.querySelector(selector);
            if (elem != null) {
                break;
            }
            page.getWebClient().waitForBackgroundJavaScript(JS_WAIT_TIME_CHUNK);
        } while (stopWatch.isTimePassedLessThanMs(maxTime));

        if (elem == null){
            throw new ConnectionProblem("Timeout reached while waiting for:[CSS Selector:" + selector + "]!");
        }

        return elem;
    }

    public static HtmlElement getHtmlElementFromPageWithClassOrNull(HtmlPage page, String className){
        return page.getFirstByXPath("//*[contains(@class, '"+className+"')]");
    }

}
