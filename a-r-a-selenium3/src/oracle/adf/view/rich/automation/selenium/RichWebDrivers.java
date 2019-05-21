package oracle.adf.view.rich.automation.selenium;

import com.google.common.base.Function;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import oracle.adf.view.rich.automation.selenium.ByRich;
import oracle.adf.view.rich.automation.selenium.Dialog;
import oracle.adf.view.rich.automation.selenium.DialogHandle;
import oracle.adf.view.rich.automation.selenium.DialogInfo;
import oracle.adf.view.rich.automation.selenium.DialogLauncher;
import oracle.adf.view.rich.automation.selenium.DialogManager;
import oracle.adf.view.rich.automation.test.BrowserLogLevel;
import oracle.adf.view.rich.automation.test.component.RichComponent;
import oracle.adf.view.rich.automation.test.component.RichWebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

public class RichWebDrivers {
	static final long _THOUSAND_MILLIS = 1000L;
	static final long _DEFAULT_TIMEOUT = 120000L;
	private static final String _NEW_LINE = "\n";
	private static final String _PAGE_LOAD_WAIT_STRATEGY_JS = "return AdfRichAutomation.getWaitForRichPageFnName();";
	private static final String _WINDOW_DIALOG_LOCATOR = "rich={0}#targetFrame";
	private static final String _GET_INLINE_WINDOW_DOM_ID_JS = "return AdfRichAutomation.getInlineWindowDomId(arguments[0]);";
	private static final String _GET_AND_CLEAR_LOG_MESSAGES_JS = "return AdfRichAutomation.getAndClearLogMessages(arguments[0]);";
	private static final String _WAIT_FOR_RICH_INLINE_DIALOG_JS = "return AdfRichAutomation.waitForRichInlineDialogLaunch(arguments[0]);";
	private static final String _GET_RICH_DIALOG_INFO_JS = "return AdfRichAutomation.getRichDialogInfo();";
	private static final String _WHY_NOT_SYNCHRONIZED_WITH_SERVER_JS = "if (!window.document || window.document.readyState != 'complete')   return { 'reason': 'PAGE_DOCUMENT_NOT_READY',            'message':'Document readyState is not complete.'};if (window.AdfPage && window.AdfPage.PAGE){  if (!window.AdfPage.PAGE.isAutomationEnabled())    return {'reason':'AUTOMATION_NOT_ENABLED', 'message': 'Automation is not enabled.'};  else if (window.AdfPage.PAGE.whyIsNotSynchronizedWithServer)     return window.AdfPage.PAGE.whyIsNotSynchronizedWithServer(arguments[0]);}return { 'reason': 'WAITING_FOR_ADF_PAGE_OR_NOT_ADF_PAGE',          'message': 'ADF page not loaded or on a non-ADF page.'       };";
	private static final String _ADF_PAGE_CHECK_JS = "if (window.document     && window.document.readyState == 'complete'     && window.AdfPage && window.AdfPage.PAGE)   return true; return false;";
	private static final String _DOCUMENT_READY_JS = "if (window.document && window.document.readyState == 'complete')  return true; return false;";
	private static final Logger _LOG = Logger.getLogger(RichWebDrivers.class.getName());

	public static Boolean isADFPage(WebDriver webDriver, long timeoutInMillis) {
		_LOG.finest("Executing isADFPage...");
		RichWebDrivers._waitUntilTrue(webDriver, timeoutInMillis, false, _DOCUMENT_READY_JS, new Object[0]);
		Object result = ((JavascriptExecutor) webDriver).executeScript(_ADF_PAGE_CHECK_JS, new Object[0]);
		return Boolean.TRUE.equals(result);
	}

	public static void waitForServer(WebDriver webDriver, long timeoutInMillis) {
		_LOG.finest("Executing waitForServer(webDriver, timeout)");
		RichWebDrivers.waitForServer(webDriver, timeoutInMillis, false);
	}

	public static void waitForServer(WebDriver webDriver, long timeoutInMillis, boolean useAutomaticDialogDetection) {
		_LOG.finest("Executing waitForServer(webDriver, timeout, useAutomaticDialogDetection)");
		long startTime = System.currentTimeMillis();
		WebDriverWait wait = new WebDriverWait(webDriver, timeoutInMillis / 1000L);
		DialogManager manager = DialogManager.getInstance();
		String returnIdOfCurrentDialog = null;
		if (useAutomaticDialogDetection) {
			if (manager.inDialog()) {
				returnIdOfCurrentDialog = String.valueOf(manager.getCurrentDialog().getHandle().getReturnId());
			}
			manager.beforeWaitForServer(webDriver);
		}
		WaitForServerExpectedCondition wfsExpectedCondition = new WaitForServerExpectedCondition(
				returnIdOfCurrentDialog, useAutomaticDialogDetection);
		wait.ignoring(WebDriverException.class);
		try {
			wait.until((Function) wfsExpectedCondition);
		} catch (TimeoutException tEx) {
			_LOG.fine("Caught timeout, gather message and print it");
			long time = System.currentTimeMillis() - startTime;
			String message = String.format(
					"waitForServer failed. Time taken (ms): (%d). Page returned the following busy status before timing out:\n %s",
					time, wfsExpectedCondition.getReasons());
			String msg = message.toString();
			_LOG.severe(msg);
			throw new TimeoutException(msg, (Throwable) tEx);
		} catch (DialogChangedException dex) {
			_LOG.fine("DialogChangedException caught.. proceeding with dialog launch or dismissal");
			manager.afterWaitForServer(webDriver);
			RichWebDrivers.waitForServer(webDriver, timeoutInMillis, useAutomaticDialogDetection);
		}
		if (useAutomaticDialogDetection) {
			manager.afterWaitForServer(webDriver);
		}
		if (_LOG.isLoggable(Level.FINEST)) {
			_LOG.finest(String.format(
					"Exiting waitForServer(webDriver, timeout, useAutomaticDialogDetection). Time taken (ms): %d",
					System.currentTimeMillis() - startTime));
		}
	}

	public static void waitForRichPageToLoad(WebDriver webDriver, long timeoutInMillis) {
		RichWebDrivers.waitForRichPageToLoad(webDriver, timeoutInMillis, false);
	}

	public static void waitForRichPageToLoad(WebDriver webDriver, long timeoutInMillis,
			boolean useAutomaticDialogDetection) {
		RichWebDrivers.waitForServer(webDriver, timeoutInMillis, useAutomaticDialogDetection);
		String functionName = ((JavascriptExecutor) webDriver).executeScript(_PAGE_LOAD_WAIT_STRATEGY_JS, new Object[0])
				.toString();
		RichWebDrivers._waitUntilTrue(webDriver, timeoutInMillis, true, functionName, new Object[0]);
	}

	@Deprecated
	public static void waitForRichPopupVisible(WebDriver webDriver, String locator, long timeoutInMillis) {
	}

	@Deprecated
	public static void waitForRichPopupHidden(WebDriver webDriver, String locator, long timeoutInMillis) {
	}

	@Deprecated
	public static DialogInfo waitForRichDialog(WebDriver webDriver, DialogLauncher launcher, long timeoutInMillis) {
		return RichWebDrivers.waitForRichChildDialog(webDriver, launcher, null, timeoutInMillis);
	}

	@Deprecated
	public static DialogInfo waitForRichChildDialog(WebDriver webDriver, DialogLauncher launcher,
			DialogInfo parentDialogInfo, long timeoutInMillis) {
		DialogInfo dialogInfo = RichWebDrivers.waitForRichChildDialogLaunch(webDriver, launcher, parentDialogInfo,
				timeoutInMillis);
		return RichWebDrivers.selectRichDialog(webDriver, dialogInfo, timeoutInMillis);
	}

	@Deprecated
	public static DialogInfo waitForRichDialogLaunch(WebDriver webDriver, DialogLauncher launcher,
			long timeoutInMillis) {
		Set handles = webDriver.getWindowHandles();
		launcher.launch(webDriver);
		return RichWebDrivers._waitForRichChildDialogLaunch(webDriver, handles, null, timeoutInMillis);
	}

	@Deprecated
	public static DialogInfo waitForRichChildDialogLaunch(WebDriver webDriver, DialogLauncher launcher,
			DialogInfo parentDialogInfo, long timeoutInMillis) {
		Set handles = webDriver.getWindowHandles();
		launcher.launch(webDriver);
		return RichWebDrivers._waitForRichChildDialogLaunch(webDriver, handles, parentDialogInfo, timeoutInMillis);
	}

	@Deprecated
	public static void waitForRichDialogReturn(WebDriver webDriver, DialogInfo dialogInfo, long timeoutInMillis) {
		if (DialogInfo.DialogType.WINDOW.equals((Object) dialogInfo.getDialogType())) {
			final String dialogWindowHandle = dialogInfo.getDialogWindowHandle();
			WebDriverWait wait = new WebDriverWait(webDriver, timeoutInMillis / 1000L);
			wait.ignoring(WebDriverException.class);
			wait.until((Function) new ExpectedCondition<Boolean>() {

				public Boolean apply(WebDriver webDriver) {
					return !webDriver.getWindowHandles().contains(dialogWindowHandle);
				}
			});
		}
		if (dialogInfo.getParentInlineDialogId() != null) {
			RichWebDrivers._selectInlineDialog(webDriver, dialogInfo.getParentDialogType(),
					dialogInfo.getParentWindowHandle(), dialogInfo.getParentInlineDialogId(), timeoutInMillis);
		} else {
			webDriver.switchTo().window(dialogInfo.getParentWindowHandle());
			if (DialogInfo.DialogType.WINDOW.equals((Object) dialogInfo.getParentDialogType())) {
				webDriver.switchTo().frame(0);
			}
		}
	}

	@Deprecated
	public static DialogInfo selectRichDialog(WebDriver webDriver, DialogInfo dialogInfo, long timeoutInMillis) {
		if (dialogInfo == null) {
			return null;
		}
		if (DialogInfo.DialogType.INLINE.equals((Object) dialogInfo.getDialogType())) {
			RichWebDrivers._selectInlineDialog(webDriver, dialogInfo.getParentDialogType(),
					dialogInfo.getParentWindowHandle(), dialogInfo.getInlineDialogId(), timeoutInMillis);
		} else if (DialogInfo.DialogType.WINDOW.equals((Object) dialogInfo.getDialogType())) {
			String dialogHandle = dialogInfo.getDialogWindowHandle();
			webDriver.switchTo().window(dialogInfo.getDialogWindowHandle());
			RichWebDrivers.waitForElementPresent(webDriver, "xpath=//frame", timeoutInMillis);
			webDriver.switchTo().frame(0);
		}
		RichWebDrivers.waitForRichPageToLoad(webDriver, timeoutInMillis);
		return dialogInfo;
	}

	@Deprecated
	public static <T> T getRichComponentProperty(WebDriver webDriver, String richLocator, String propertyName) {
		RichWebElement el = RichWebDrivers._waitForRichWebElement(webDriver, richLocator, 120000L);
		return (T) el.getRichComponent().getProperty(propertyName);
	}

	@Deprecated
	public static Map<String, Object> getRichComponentProperties(WebDriver webDriver, String richLocator,
			Set<String> propertyNames) {
		if (propertyNames == null || propertyNames.size() == 0) {
			throw new IllegalArgumentException("propertyNames is null or empty.");
		}
		RichWebElement el = RichWebDrivers._waitForRichWebElement(webDriver, richLocator, 120000L);
		return el.getRichComponent().getProperties(propertyNames);
	}

	@Deprecated
	public static <T> T setRichComponentProperty(WebDriver webDriver, String richLocator, String propertyName,
			Object value) {
		RichWebElement el = RichWebDrivers._waitForRichWebElement(webDriver, richLocator, 120000L);
		return (T) el.getRichComponent().setProperty(propertyName, value);
	}

	@Deprecated
	public static <T> T callRichComponentFunction(WebDriver webDriver, String richLocator, String functionName,
			Object... args) {
		RichWebElement el = RichWebDrivers._waitForRichWebElement(webDriver, richLocator, 120000L);
		return (T) el.getRichComponent().invokeFunction(functionName, args);
	}

	@Deprecated
	public static <T> T callRichComponentFunctionChain(WebDriver webDriver, String richLocator, String[] functionNames,
			Object[][] args) {
		RichWebElement el = RichWebDrivers._waitForRichWebElement(webDriver, richLocator, 120000L);
		return (T) el.getRichComponent().invokeFunctionChain(functionNames, args);
	}

	public static String getAndClearBrowserLog(WebDriver webDriver, BrowserLogLevel logLevel) {
		if (webDriver == null) {
			return "";
		}
		StringBuilder sbf = new StringBuilder();
		try {
			JavascriptExecutor jsExecutor = (JavascriptExecutor) webDriver;
			List<String> logs = (List) jsExecutor.executeScript(_GET_AND_CLEAR_LOG_MESSAGES_JS,
					new Object[]{logLevel.toString().toUpperCase()});
			for (String s : logs) {
				sbf.append(s).append(_NEW_LINE);
			}
		} catch (WebDriverException e) {
			String pageSource = webDriver.getPageSource();
			sbf.append("Browser log capture failed.").append(_NEW_LINE).append("Page Title: ")
					.append(webDriver.getTitle()).append(_NEW_LINE).append("Page source (First 1000 characters): ")
					.append(_NEW_LINE).append(pageSource.substring(0, Math.min(pageSource.length(), 1000)))
					.append(_NEW_LINE);
		}
		return sbf.toString();
	}

	public static String getAndClearBrowserLog(WebDriver webDriver) {
		return RichWebDrivers.getAndClearBrowserLog(webDriver, BrowserLogLevel.INFO);
	}

	public static By locatorStringToBy(String locator) {
		if (_LOG.isLoggable(Level.FINEST)) {
			_LOG.finest(String.format("Executing locatorStringToBy for '%s'", locator));
		}
		if (locator == null || locator.trim().length() == 0) {
			return null;
		}
		if (locator.startsWith("/")) {
			return By.xpath((String) locator);
		}
		if (locator.startsWith("rich=")) {
			return ByRich.locator((String) locator);
		}
		if (locator.startsWith("id=")) {
			return By.id((String) locator.substring(3));
		}
		if (locator.startsWith("xpath=")) {
			return By.xpath((String) locator.substring(6));
		}
		if (locator.startsWith("css=")) {
			return By.cssSelector((String) locator.substring(4));
		}
		if (locator.startsWith("name=")) {
			return By.name((String) locator.substring(5));
		}
		if (locator.startsWith("tagName=")) {
			return By.tagName((String) locator.substring(8));
		}
		if (locator.startsWith("link=")) {
			return By.linkText((String) locator.substring(5));
		}
		return ByRich.locator((String) locator, (boolean) true);
	}

	public static WebElement waitForElementPresent(WebDriver webDriver, String locator, long timeoutInMillis) {
		WebDriverWait wait = new WebDriverWait(webDriver, timeoutInMillis / 1000L);
		wait.ignoring(WebDriverException.class);
		return (WebElement) wait.until(
				(Function) ExpectedConditions.presenceOfElementLocated((By) RichWebDrivers.locatorStringToBy(locator)));
	}

	private static DialogInfo _waitForRichChildDialogLaunch(WebDriver webDriver, final Set<String> openWindowHandles,
			DialogInfo parentDialogInfo, long timeoutInMillis) {
		RichWebDrivers.waitForServer(webDriver, timeoutInMillis);
		List dialogInfo = (List) ((JavascriptExecutor) webDriver).executeScript(_GET_RICH_DIALOG_INFO_JS,
				new Object[0]);
		String type = (String) dialogInfo.get(0);
		if ("inline".equals(type)) {
			RichWebDrivers._waitUntilTrue(webDriver, timeoutInMillis, true, _WAIT_FOR_RICH_INLINE_DIALOG_JS,
					dialogInfo.get(1));
			String inlineWindowId = (String) ((JavascriptExecutor) webDriver)
					.executeScript(_GET_INLINE_WINDOW_DOM_ID_JS, new Object[]{dialogInfo.get(1)});
			return DialogInfo.createInlineDialogInfo((String) ((String) dialogInfo.get(2)), (String) inlineWindowId,
					(String) webDriver.getWindowHandle(), (DialogInfo) parentDialogInfo);
		}
		if ("window".equals(type)) {
			String parentWindowHandle = webDriver.getWindowHandle();
			WebDriverWait wait = new WebDriverWait(webDriver, timeoutInMillis / 1000L);
			wait.ignoring(WebDriverException.class);
			String dialogWindowHandle = (String) wait.until((Function) new ExpectedCondition<String>() {

				public String apply(WebDriver webDriver) {
					Set<String> handles = webDriver.getWindowHandles();
					for (String handle : handles) {
						if (openWindowHandles.contains(handle))
							continue;
						return handle;
					}
					return null;
				}
			});
			if (dialogWindowHandle == null) {
				throw new IllegalStateException("Popup window expected to be opened.");
			}
			RichWebDrivers.waitForServer(webDriver, timeoutInMillis);
			webDriver.switchTo().window(dialogWindowHandle);
			RichWebDrivers.waitForElementPresent(webDriver, "xpath=//frame", timeoutInMillis);
			webDriver.switchTo().frame(0);
			RichWebDrivers.waitForRichPageToLoad(webDriver, timeoutInMillis);
			RichWebDrivers.selectRichDialog(webDriver, parentDialogInfo, timeoutInMillis);
			return DialogInfo.createWindowDialogInfo((String) dialogWindowHandle, (String) parentWindowHandle,
					(DialogInfo) parentDialogInfo);
		}
		String message = "Unknown dialog type. Dialog type has to be either 'inline' or 'window'";
		throw new IllegalStateException(message);
	}

	private static void _selectInlineDialog(WebDriver webDriver, DialogInfo.DialogType parentDialogType,
			String parentWindowHandle, String inlineDialogId, long timeoutInMillis) {
		String currentWindowHandle = webDriver.getWindowHandle();
		if (!currentWindowHandle.equals(parentWindowHandle)) {
			webDriver.switchTo().window(parentWindowHandle);
			if (DialogInfo.DialogType.WINDOW.equals((Object) parentDialogType)) {
				webDriver.switchTo().frame(0);
			}
		}
		String locator = MessageFormat.format(_WINDOW_DIALOG_LOCATOR, inlineDialogId);
		WebElement el = RichWebDrivers.waitForElementPresent(webDriver, locator, timeoutInMillis);
		webDriver.switchTo().frame(el.getAttribute("id"));
	}

	private static void _waitUntilTrue(WebDriver webDriver, long timeoutInMillis, boolean ignoreWebDriverException,
			final String functionName, final Object... args) {
		WebDriverWait wait = new WebDriverWait(webDriver, timeoutInMillis / 1000L);
		if (ignoreWebDriverException) {
			wait.ignoring(WebDriverException.class);
		}
		wait.until((Function) new ExpectedCondition<Boolean>() {

			public Boolean apply(WebDriver d) {
				Object functionResult = null;
				try {
					functionResult = ((JavascriptExecutor) d).executeScript(functionName, args);
				} catch (WebDriverException wEx) {
					if (_LOG.isLoggable(Level.FINEST)) {
						_LOG.finest(String.format("Exception while trying to execute '%s'('%s'). Error is: %s",
								functionName, Arrays.asList(args), wEx.getMessage()));
					}
					if (wEx.getMessage().contains("waiting for doc.body failed")) {
						_LOG.info(
								"Hitting webdriver bug http://code.google.com/p/selenium/issues/detail?id=1157. Retrying...");
						functionResult = Boolean.FALSE;
					}
					wEx.printStackTrace();
					throw wEx;
				}
				return Boolean.TRUE.equals(functionResult);
			}
		});
	}

	private static RichWebElement _waitForRichWebElement(WebDriver webDriver, String richLocator,
			long timeoutInMillis) {
		if (_LOG.isLoggable(Level.FINEST)) {
			_LOG.finest(String.format("Executing _waitForRichWebElement() on locator '%s'", richLocator));
		}
		WebDriverWait wait = new WebDriverWait(webDriver, timeoutInMillis / 1000L);
		wait.ignoring(WebDriverException.class);
		WebElement el = (WebElement) wait.until((Function) ExpectedConditions.presenceOfElementLocated((By) ByRich
				.locator((String) richLocator, (ByRich.ReturnType) ByRich.ReturnType.RICH_WEB_ELEMENT_WRAPPED)));
		return (RichWebElement) el;
	}

	private RichWebDrivers() {
	}

	private static class DialogChangedException extends RuntimeException {
		private DialogChangedException() {
		}
	}

	private static class WaitForServerExpectedCondition implements ExpectedCondition<Boolean> {
		private final boolean _useAutomaticDialogDetection;
		private final String _returnIdOfCurrentDialog;
		private final DialogManager _dialogManager;
		private final Deque<Reason> _reasonStack = new ArrayDeque<Reason>();

		WaitForServerExpectedCondition(String returnIdOfCurrentDialog, boolean useAutomaticDialogDetection) {
			this._useAutomaticDialogDetection = useAutomaticDialogDetection;
			this._returnIdOfCurrentDialog = returnIdOfCurrentDialog;
			this._dialogManager = DialogManager.getInstance();
			if (_LOG.isLoggable(Level.FINER)) {
				_LOG.finer(String.format("Creating WaitForServerExpectedCondition(%s, %s)",
						this._returnIdOfCurrentDialog, this._useAutomaticDialogDetection));
			}
		}

		public Boolean apply(WebDriver webDriver) {
			try {
				Object whyIsNotSynchronizedWithServer = ((JavascriptExecutor) webDriver).executeScript(
						RichWebDrivers._WHY_NOT_SYNCHRONIZED_WITH_SERVER_JS,
						new Object[]{this._returnIdOfCurrentDialog});
				if (_LOG.isLoggable(Level.FINEST)) {
					_LOG.finest(
							String.format("whyIsNotSynchronizedWithServer is: '%s'", whyIsNotSynchronizedWithServer));
				}
				if (whyIsNotSynchronizedWithServer == null) {
					return Boolean.TRUE;
				}
				if (!(whyIsNotSynchronizedWithServer instanceof Map)) {
					throw new WebDriverException(
							String.format("Unexpected result from whyIsNotSynchronizedWithServer '%s'",
									whyIsNotSynchronizedWithServer.toString()));
				}
				Reason reason = new Reason(webDriver, (Map) whyIsNotSynchronizedWithServer);
				if (_LOG.isLoggable(Level.FINEST)) {
					_LOG.finest(String.format("Created Reason: '%s'", reason));
				}
				if (reason.automationNotEnabled()) {
					throw new WebDriverException(
							"Your test application is not running in AUTOMATION mode. Make sure that test application web.xml has 'oracle.adf.view.rich.automation.ENABLED' set to 'true', and 'javax.faces.PROJECT_STAGE' set to either 'SystemTest' or 'UnitTest' as appropriate.");
				}
				this._pushToReasonStack(reason);
				if (this._useAutomaticDialogDetection) {
					Reason nestedReason;
					boolean dialogChanged = false;
					if (reason.wasDialogServiceBusy()) {
						_LOG.fine("Dialog launched or dismissed: reason.wasDialogServiceBusy()");
						dialogChanged = this._processDialogDetails(reason);
					} else if (reason.dialogNotSyced()
							&& (nestedReason = reason.getNestedReason()).wasDialogServiceBusy()) {
						_LOG.fine("Nested dialog launched or dismissed: nestedReason.wasDialogServiceBusy()");
						dialogChanged = this._processDialogDetails(nestedReason);
					}
					if (dialogChanged) {
						_LOG.fine("Dialog launched or closed. Throwing DialogChangeException");
						throw new DialogChangedException();
					}
				}
				if (_LOG.isLoggable(Level.FINEST)) {
					_LOG.finest(String.format("Page returned busy status: '%s'. Retrying...", reason));
				}
				this._workAroundWebDriverFocusIssue(webDriver, reason);
				return Boolean.FALSE;
			} catch (UnhandledAlertException uae) {
				String message = "Unhandled alert detected on the page. Test to handle the alert by accepting or dismissing it. This can be done by invoking getAlert().accept() or getAlert().reject().";
				_LOG.warning(message);
				this._pushToReasonStack(new Reason("UnexpectedAlertException", message));
				return Boolean.TRUE;
			} catch (WebDriverException wde) {
				_LOG.finest("Caught WebDriverException");
				String exMsg = wde.getMessage();
				String message = "Exception while executing waitForServer: " + exMsg;
				this._pushToReasonStack(new Reason("WebDriverException", message));
				if (exMsg.contains("waiting for doc.body failed")) {
					_LOG.info(
							"Hitting webdriver bug http://code.google.com/p/selenium/issues/detail?id=1157. Retrying...");
					return Boolean.FALSE;
				}
				if (_LOG.isLoggable(Level.WARNING)) {
					_LOG.warning(message);
				}
				throw wde;
			}
		}

		private void _workAroundWebDriverFocusIssue(WebDriver webDriver, Reason reason) {
			if (reason.nonAdfPage() && this._dialogManager.inDialog()) {
				this._dialogManager.focusCurrentDialogsParent(webDriver);
				_LOG.finest(String.format(
						"Hitting webDriver focus workaround in WaitForServerExpectedCondition. Refocusing current dialog",
						new Object[0]));
			}
		}

		private boolean _processDialogDetails(Reason reason) {
			boolean dialogChanged = false;
			List<DialogDetail> dialogDetails = reason.getDialogDetails();
			if (dialogDetails != null && dialogDetails.size() > 0) {
				for (DialogDetail detail : dialogDetails) {
					detail.updateDialogManager();
				}
				dialogChanged = true;
			}
			return dialogChanged;
		}

		private void _pushToReasonStack(Reason reason) {
			Reason head = this._reasonStack.peek();
			if (head == null || !head.equals(reason)) {
				this._reasonStack.push(reason);
			}
		}

		String getReasons() {
			StringBuilder reasons = new StringBuilder();
			for (Reason reason : this._reasonStack) {
				reasons.append(reason.toString());
			}
			return reasons.toString();
		}
	}

	private static class TimerDetail {
		private String _timerId;
		private String _callback;

		TimerDetail(WebDriver webDriver, Map<String, String> details) {
			_LOG.finest("Creating TimerDetail");
			this._timerId = details.get("timerId");
			this._callback = details.get("callback");
		}

		public String toString() {
			StringBuilder toString = new StringBuilder();
			toString.append("{ 'timerId': ").append(this._timerId).append(", 'callback': ").append(this._callback)
					.append("}");
			return toString.toString();
		}
	}

	private static class NestedDialogDetail extends DialogDetail {
		NestedDialogDetail(WebDriver webDriver, Map dialogDetails) {
			super(webDriver, dialogDetails);
			_LOG.finest("NestedDialogDetail constructor");
		}

		@Override
		void updateDialogManager() {
			if (_LOG.isLoggable(Level.FINE)) {
				_LOG.fine(String.format("updateDialogManager for NestedDialogDetail Closed: '%s', Launched: '%s'",
						this._dialogClosed, this._dialogLaunched));
			}
			if (this._dialogLaunched) {
				if (_LOG.isLoggable(Level.FINE)) {
					_LOG.fine(String.format("Update Nested Dialog Launch with DialogManager '%s'",
							new Object[]{this._dialog}));
				}
				DialogManager.getInstance().notifyNestedDialogLaunched(this._dialog);
			}
			if (this._dialogClosed) {
				if (_LOG.isLoggable(Level.FINE)) {
					_LOG.fine(String.format("Update Nested Dialog Close with DialogManager '%s'",
							new Object[]{this._dialog}));
				}
				DialogManager.getInstance().notifyNestedDialogClosed(this._dialog);
			}
		}

		@Override
		protected DialogHandle createDialogHandle(WebDriver webDriver, Map<String, String> dialogInfo) {
			_LOG.finest("createDialogHandle for NestedDialogDetail");
			return DialogHandle.createNestedDialogHandle(dialogInfo);
		}
	}

	private static class DialogDetail {
		protected final DialogHandle _dialog;
		protected final boolean _dialogLaunched;
		protected final boolean _dialogClosed;
		private final String _op;
		private static final String _LAUNCHING_INLINE_DIALOG = "LAUNCHING_INLINE_DIALOG";
		private static final String _LAUNCHING_WINDOW_DIALOG = "LAUNCHING_WINDOW_DIALOG";
		private static final String _CLOSING_INLINE_DIALOG = "CLOSING_INLINE_DIALOG";
		private static final String _CLOSING_WINDOW_DIALOG = "CLOSING_WINDOW_DIALOG";

		DialogDetail(WebDriver webDriver, Map<String, ?> dialogDetails) {
			this._op = (String) dialogDetails.get("operation");
			Map dialogInfo = (Map) dialogDetails.get("dialogInfo");
			this._dialog = this.createDialogHandle(webDriver, dialogInfo);
			if (_LOG.isLoggable(Level.FINEST)) {
				_LOG.finest(String.format("createDialogHandle return value is '%s'", new Object[]{this._dialog}));
			}
			if (_LAUNCHING_INLINE_DIALOG.equals(this._op) || _LAUNCHING_WINDOW_DIALOG.equals(this._op)) {
				this._dialogLaunched = true;
				this._dialogClosed = false;
			} else if (_CLOSING_INLINE_DIALOG.equals(this._op) || _CLOSING_WINDOW_DIALOG.equals(this._op)) {
				this._dialogClosed = true;
				this._dialogLaunched = false;
			} else {
				this._dialogLaunched = false;
				this._dialogClosed = false;
			}
		}

		public String toString() {
			StringBuilder toString = new StringBuilder();
			toString.append("{ 'operation': ").append(this._op).append(", 'dialog': ").append((Object) this._dialog)
					.append("}");
			return toString.toString();
		}

		protected DialogHandle createDialogHandle(WebDriver webDriver, Map<String, String> dialogInfo) {
			return DialogHandle.createDialogHandle(dialogInfo);
		}

		void updateDialogManager() {
			if (this._dialogLaunched) {
				if (_LOG.isLoggable(Level.FINE)) {
					_LOG.fine(
							String.format("Update Dialog Launch with DialogManager '%s'", new Object[]{this._dialog}));
				}
				DialogManager.getInstance().notifyDialogLaunched(this._dialog);
			}
			if (this._dialogClosed) {
				if (_LOG.isLoggable(Level.FINE)) {
					_LOG.fine(String.format("Update Dialog Close with DialogManager '%s'", new Object[]{this._dialog}));
				}
				DialogManager.getInstance().notifyDialogClosed(this._dialog);
			}
		}
	}

	private static class NestedReason extends Reason {
		NestedReason(WebDriver webDriver, Map<String, ?> reasonMap) {
			super(webDriver, reasonMap);
			_LOG.finest("NestedReason constructor");
		}

		protected DialogDetail createDialogDetail(WebDriver webDriver, Map detail) {
			_LOG.finest("createDialogDetail for NestedDialogDetail");
			return new NestedDialogDetail(webDriver, detail);
		}
	}

	private static class NullReason extends Reason {
		static NullReason INSTANCE = new NullReason();

		private NullReason() {
			super("NULL", "Null reason");
		}
	}

	private static class Reason {
		private final String _reasonCode;
		private final String _message;
		private final List<?> _rawDetails;
		private final List<DialogDetail> _dialogDetails;
		private final List<TimerDetail> _timerDetails;
		private final Reason _nestedReason;

		Reason(String reason, String message) {
			this._reasonCode = reason;
			this._message = message;
			this._rawDetails = Collections.EMPTY_LIST;
			this._timerDetails = Collections.EMPTY_LIST;
			this._dialogDetails = Collections.EMPTY_LIST;
			this._nestedReason = NullReason.INSTANCE;
		}

		Reason(WebDriver webDriver, Map<String, ?> reasonMap) {
			this._reasonCode = ((String) reasonMap.get("reason")).trim();
			this._message = (String) reasonMap.get("message");
			Object details = reasonMap.get("details");
			Object nestedReason = reasonMap.get("nested");
			if (details == null) {
				this._rawDetails = Collections.EMPTY_LIST;
				this._dialogDetails = Collections.EMPTY_LIST;
				this._timerDetails = Collections.EMPTY_LIST;
			} else {
				if (!(details instanceof List)) {
					throw new ClassCastException("'details' is expected to be a List");
				}
				this._rawDetails = (List) details;
				if (this._rawDetails.size() == 0) {
					this._dialogDetails = Collections.EMPTY_LIST;
					this._timerDetails = Collections.EMPTY_LIST;
				} else if ("DIALOG_SERVICE_IS_BUSY".equals(this._reasonCode)) {
					this._dialogDetails = new ArrayList<DialogDetail>();
					for (Object detail : this._rawDetails) {
						if (!(detail instanceof Map)) {
							throw new ClassCastException("Each element of 'details' is expected to be a Map");
						}
						this._dialogDetails.add(this.createDialogDetail(webDriver, (Map) detail));
					}
					this._timerDetails = Collections.EMPTY_LIST;
				} else if ("WAITING_FOR_SYNCHRONOUS_TIMERS_TO_CLEAR".equals(this._reasonCode)) {
					this._timerDetails = new ArrayList<TimerDetail>();
					for (Object detail : this._rawDetails) {
						if (!(detail instanceof Map)) {
							throw new ClassCastException("Each element of 'details' is expected to be a Map");
						}
						this._timerDetails.add(new TimerDetail(webDriver, (Map) detail));
					}
					this._dialogDetails = Collections.EMPTY_LIST;
				} else {
					this._dialogDetails = Collections.EMPTY_LIST;
					this._timerDetails = Collections.EMPTY_LIST;
				}
			}
			this._nestedReason = nestedReason == null
					? NullReason.INSTANCE
					: new NestedReason(webDriver, (Map) nestedReason);
		}

		public String toString() {
			StringBuilder toString = new StringBuilder();
			toString.append("{ 'reason': ").append(this._reasonCode).append(", 'message': ").append(this._message);
			if (!this._dialogDetails.isEmpty()) {
				toString.append(", 'dialogDetails': ").append(this._dialogDetails);
			} else if (!this._timerDetails.isEmpty()) {
				toString.append(", 'timerDetails': ").append(this._timerDetails);
			} else if (!this._rawDetails.isEmpty()) {
				toString.append(", 'details': ").append(this._rawDetails);
			} else if (this._nestedReason != NullReason.INSTANCE) {
				toString.append(", 'nestedReason': ").append(this._nestedReason);
			}
			toString.append("}");
			return toString.toString();
		}

		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof Reason)) {
				return false;
			}
			Reason other = (Reason) object;
			if (!this._reasonCode.equals(other._reasonCode)) {
				return false;
			}
			if (!this._message.equals(other._message)) {
				return false;
			}
			if (!this._rawDetails.equals(other._rawDetails)) {
				return false;
			}
			if (!this._nestedReason.equals(other._nestedReason)) {
				return false;
			}
			return true;
		}

		public int hashCode() {
			int PRIME = 37;
			int result = 1;
			result = 37 * result + this._reasonCode.hashCode();
			result = 37 * result + this._message.hashCode();
			result = 37 * result + this._rawDetails.hashCode();
			result = 37 * result + this._nestedReason.hashCode();
			return result;
		}

		protected DialogDetail createDialogDetail(WebDriver webDriver, Map<String, ?> detail) {
			return new DialogDetail(webDriver, detail);
		}

		boolean wasDialogServiceBusy() {
			return "DIALOG_SERVICE_IS_BUSY".equals(this._reasonCode);
		}

		boolean dialogNotSyced() {
			return "DIALOG_NOT_SYNCED".equals(this._reasonCode);
		}

		boolean nonAdfPage() {
			return "WAITING_FOR_ADF_PAGE_OR_NOT_ADF_PAGE".equals(this._reasonCode);
		}

		boolean automationNotEnabled() {
			return "AUTOMATION_NOT_ENABLED".equals(this._reasonCode);
		}

		Reason getNestedReason() {
			return this._nestedReason;
		}

		List<DialogDetail> getDialogDetails() {
			return this._dialogDetails;
		}

		List<TimerDetail> getTimerDetails() {
			return this._timerDetails;
		}
	}

}

/*
	DECOMPILATION REPORT

	Decompiled from: C:\Oracle\Middleware_12c_bpmqs\Oracle_Home\oracle_common\modules\oracle.adf.view\adf-richclient-automation-11.jar
	Total time: 115 ms
	
	Decompiled with CFR 0_130.
	Could not load the following classes:
	com.google.common.base.Function
	oracle.adf.view.rich.automation.selenium.ByRich
	oracle.adf.view.rich.automation.selenium.ByRich$ReturnType
	oracle.adf.view.rich.automation.selenium.Dialog
	oracle.adf.view.rich.automation.selenium.DialogHandle
	oracle.adf.view.rich.automation.selenium.DialogInfo
	oracle.adf.view.rich.automation.selenium.DialogInfo$DialogType
	oracle.adf.view.rich.automation.selenium.DialogLauncher
	oracle.adf.view.rich.automation.selenium.DialogManager
	oracle.adf.view.rich.automation.test.BrowserLogLevel
	oracle.adf.view.rich.automation.test.component.RichComponent
	oracle.adf.view.rich.automation.test.component.RichWebElement
	org.openqa.selenium.By
	org.openqa.selenium.JavascriptExecutor
	org.openqa.selenium.TimeoutException
	org.openqa.selenium.UnhandledAlertException
	org.openqa.selenium.WebDriver
	org.openqa.selenium.WebDriver$TargetLocator
	org.openqa.selenium.WebDriverException
	org.openqa.selenium.WebElement
	org.openqa.selenium.support.ui.ExpectedCondition
	org.openqa.selenium.support.ui.ExpectedConditions
	org.openqa.selenium.support.ui.FluentWait
	org.openqa.selenium.support.ui.WebDriverWait
	varargs 
	varargs 
*/