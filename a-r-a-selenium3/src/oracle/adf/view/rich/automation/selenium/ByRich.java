package oracle.adf.view.rich.automation.selenium;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import oracle.adf.view.rich.automation.test.component.RichWebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

public final class ByRich extends By {
	private final String _richLocator;
	private final boolean _fallBackOnIdSearch;
	private final ReturnType _returnType;
	private static final Logger _LOG = Logger.getLogger(ByRich.class.getName());
	private static final String _RICH_STR = "rich=";
	private static final String _LOCATE_ELEMENT_BY_RICH_JS = "var locator = arguments[0];var fallBackOnId = arguments[1];var elem = null;if (typeof AdfRichAutomation !== \"undefined\"){  elem = AdfRichAutomation.locateElementByRich(locator);}if (!elem && fallBackOnId == true){  elem = document.getElementById(locator);}return elem;";

	public static By locator(String richLocator, ReturnType type) {
		if (richLocator == null) {
			throw new IllegalArgumentException(
					"Locator string is null. Cannot find elements with a null rich locator. Please provide a locator string in the format 'rich=scope#subid'");
		}
		return new ByRich(richLocator, false, type);
	}

	public static By locator(String richLocator) {
		return ByRich.locator(richLocator, false);
	}

	static By locator(String richLocator, boolean fallBackToIdSearch) {
		if (richLocator == null) {
			throw new IllegalArgumentException(
					"Locator string is null. Cannot find elements with a null rich locator. Please provide a locator string in the format 'rich=scope#subid'");
		}
		return new ByRich(richLocator, fallBackToIdSearch, ReturnType.DEFAULT);
	}

	public List<WebElement> findElements(SearchContext searchContext) {
		Object result;
		try {
			result = ((JavascriptExecutor) searchContext).executeScript(_LOCATE_ELEMENT_BY_RICH_JS,
					new Object[]{this._richLocator, this._fallBackOnIdSearch});
		} catch (WebDriverException wEx) {
			if (_LOG.isLoggable(Level.FINE)) {
				_LOG.log(Level.FINE, String.format("Exception while trying to locate element by rich for locator '%s'",
						this._richLocator), (Throwable) wEx);
			}
			throw wEx;
		}
		return this._processResult((WebDriver) searchContext, result);
	}

	private List<WebElement> _processResult(WebDriver webDriver, Object result) {
		List elements = Collections.emptyList();
		if (result == null) {
			return elements;
		}
		if (!(result instanceof WebElement) && !(result instanceof List)) {
			throw new WebDriverException(String.format(
					"Unexpected return type '%s' encountered for locator '%s'.Expected types are WebElement or List<WebElement>.",
					result.getClass(), this._richLocator));
		}
		if (result instanceof WebElement) {
			if (_LOG.isLoggable(Level.FINEST)) {
				_LOG.finest(String.format("Found single WebElement for locator '%s'", this._richLocator));
			}
			elements = Collections.singletonList((WebElement) result);
		} else if (result instanceof List) {
			if (_LOG.isLoggable(Level.FINEST)) {
				_LOG.finest(String.format("Found multiple WebElements for locator '%s'", this._richLocator));
			}
			elements = (List) result;
		}
		return this._wrapWithRichWebElementIfNeeded(webDriver, elements);
	}

	private List<WebElement> _wrapWithRichWebElementIfNeeded(WebDriver webDriver, List<WebElement> elements) {
		if (this._returnType == ReturnType.DEFAULT || elements.isEmpty()) {
			return elements;
		}
		if (elements.size() == 1) {
			RichWebElement el = RichWebElement.create((WebDriver) webDriver, (WebElement) elements.get(0));
			return Collections.singletonList(el);
		}
		ArrayList<RichWebElement> convertedEls = new ArrayList<RichWebElement>(elements.size());
		for (WebElement el : elements) {
			convertedEls.add(RichWebElement.create((WebDriver) webDriver, (WebElement) el));
		}
		return Collections.unmodifiableList(convertedEls);
	}

	public String toString() {
		return "By.rich: " + this._richLocator;
	}

	private ByRich(String richLocator, boolean fallBackToIdSearch, ReturnType returnType) {
		this._richLocator = richLocator.startsWith(_RICH_STR) ? richLocator.substring(5) : richLocator;
		this._fallBackOnIdSearch = fallBackToIdSearch;
		this._returnType = returnType;
	}

	public static enum ReturnType {
		DEFAULT, RICH_WEB_ELEMENT_WRAPPED;

		private ReturnType() {
		}
	}

}

/*
	DECOMPILATION REPORT

	Decompiled from: C:\Oracle\Middleware_12c_bpmqs\Oracle_Home\oracle_common\modules\oracle.adf.view\adf-richclient-automation-11.jar
	Total time: 66 ms
	
	Decompiled with CFR 0_130.
	Could not load the following classes:
	oracle.adf.view.rich.automation.test.component.RichWebElement
	org.openqa.selenium.By
	org.openqa.selenium.JavascriptExecutor
	org.openqa.selenium.SearchContext
	org.openqa.selenium.WebDriver
	org.openqa.selenium.WebDriverException
	org.openqa.selenium.WebElement
	
*/