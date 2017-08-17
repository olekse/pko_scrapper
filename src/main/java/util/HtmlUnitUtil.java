package util;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class HtmlUnitUtil {
    public static HtmlElement getHtmlElementFromPageWithClassOrNull(HtmlPage page, String className){
        return page.getFirstByXPath("//*[contains(@class, '"+className+"')]");
    }
}
