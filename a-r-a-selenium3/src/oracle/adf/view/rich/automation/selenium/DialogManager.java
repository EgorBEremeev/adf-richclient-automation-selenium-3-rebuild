package oracle.adf.view.rich.automation.selenium;

import com.google.common.base.Function;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import oracle.adf.view.rich.automation.selenium.ByRich;
import oracle.adf.view.rich.automation.selenium.Dialog;
import oracle.adf.view.rich.automation.selenium.DialogHandle;
import oracle.adf.view.rich.automation.selenium.RichWebDrivers;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

public final class DialogManager {
	private final DialogHandle _mainWindowHandle;
	private DialogHandle _currentDialogHandle;
	private long _timeout;
	private boolean _useAutomaticDialogDetection;
	private DialogHandle _closedDialogHandle;
	private DialogHandle _launchedDialogHandle;
	private boolean _isNestedDialog;
	private static ThreadLocal<DialogManager> _threadLocal = new ThreadLocal();
	private static final String _GET_STABLE_WINDOW_NAME_JS = "return AdfDhtmlRichDialogService.getWindowNameByReturnId(arguments[0]);";
	private static final String _GET_RICH_DIALOG_IDS_JS = "return AdfRichAutomation.getAllRichDialogIds();";
	private static final String _WAIT_FOR_DIALOG_TITLE_JS = "return window.parent.document.title === window.document.title;";
	private static final String _GET_RICH_DIALOG_INFO_FOR_RET_ID_JS = "return AdfRichAutomation.getRichDialogInfo(arguments[0]);";
	private static final String _INLINE_DIALOG_TITLE_LOCATOR = "rich={0}#title";
	private static final String _INLINE_DIALOG_FRAME_LOCATOR = "rich={0}#targetFrame";
	private static final String _INLINE_DIALOG_DISMISS_JS = "return AdfRichAutomation.cancelPopup(arguments[0]) ";
	private static final Logger _LOG = Logger.getLogger(DialogManager.class.getName());

	public static DialogManager getInstance() {
		if (_threadLocal == null) {
			throw new IllegalStateException("DialogManager is not initialized");
		}
		return _threadLocal.get();
	}

	public void setUseAutomaticDialogDetection(boolean use) {
		this._useAutomaticDialogDetection = use;
	}

	@Deprecated
	public static void init(WebDriver webDriver, long timeout) {
		_threadLocal.set(new DialogManager(webDriver, timeout, false));
	}

	public static void init(WebDriver webDriver, long timeout, boolean useAutomaticDialogDetection) {
		_threadLocal.set(new DialogManager(webDriver, timeout, useAutomaticDialogDetection));
	}

	public static void destroy() {
		_threadLocal.remove();
	}

	public Dialog getCurrentDialog() {
		if (this._currentDialogHandle.getType() == DialogHandle.DialogType.MAIN_WINDOW) {
			return null;
		}
		return new Dialog(this._currentDialogHandle);
	}

	public int totalNumberOfDialogsOpen(WebDriver webDriver) {
		return this._getAllDialogIds(webDriver).size();
	}

	@Deprecated
	public int totalNumberOfDialogsOpen() {
		return 0;
	}

	public void selectMainWindow(WebDriver webDriver) {
		_LOG.fine("selectMainWindow: currentDialog set to mainWindowHandle");
		this._moveTo(webDriver, this._mainWindowHandle);
		this._currentDialogHandle = this._mainWindowHandle;
	}

	public Dialog getDialogBy(WebDriver webDriver, SelectDialogBy dialogBy, String identifier) {
		if (_LOG.isLoggable(Level.FINE)) {
			_LOG.fine(String.format("Attempting getDialogBy '%s' using identifier '%s'",
					new Object[]{dialogBy, identifier}));
		}
		DialogHandle handle = null;
		List<Long> keys = this._getAllDialogIds(webDriver);
		for (Long key : keys) {
			DialogHandle temp = this._generateDialogHandleFromReturnId(webDriver, key);
			if (dialogBy == SelectDialogBy.TITLE && identifier.equals(this.getDialogTitle(webDriver, temp))) {
				handle = temp;
				break;
			}
			if (dialogBy != SelectDialogBy.LAUNCH_SOURCE_ID || !identifier.equals(temp.getLaunchSourceId()))
				continue;
			handle = temp;
			break;
		}
		if (handle == null) {
			if (_LOG.isLoggable(Level.FINE)) {
				_LOG.fine(String.format("Failed to find dialog: getDialogBy '%s' using identifier '%s'",
						new Object[]{dialogBy, identifier}));
			}
			return null;
		}
		if (_LOG.isLoggable(Level.FINE)) {
			_LOG.fine(String.format("Found handle: getDialogBy '%s' using identifier '%s' with returnId '%s' ",
					new Object[]{dialogBy, identifier, handle.getReturnId()}));
		}
		return new Dialog(handle);
	}

	void beforeWaitForServer(WebDriver webDriver) {
		if (DialogHandle.DialogType.MAIN_WINDOW == this._currentDialogHandle.getType()) {
			_LOG.finest("Already on MAIN WINDOW, so no where to step back");
			return;
		}
		DialogHandle parent = this._currentDialogHandle.getParent();
		this._traverseTo(webDriver, parent);
		if (_LOG.isLoggable(Level.FINE)) {
			_LOG.fine(String.format("DialogManager.beforeWaitForServer moved to parent '%s'", new Object[]{parent}));
		}
	}

	void afterWaitForServer(WebDriver webDriver) {
		boolean needToMove = false;
		if (this._closedDialogHandle != null || this._launchedDialogHandle != null) {
			if (_LOG.isLoggable(Level.FINEST)) {
				_LOG.finest(String.format(
						"afterWaitForServer processing dialog launch or dismissal. nested dialog: '%s', launchedDialog: '%s', closedDialog: '%s'",
						new Object[]{this._isNestedDialog, this._launchedDialogHandle, this._closedDialogHandle}));
			}
			if (this._isNestedDialog) {
				this._moveTo(webDriver, this._currentDialogHandle);
			}
			if (this._closedDialogHandle != null) {
				if (_LOG.isLoggable(Level.FINE)) {
					_LOG.fine(String.format("Processing closed dialog '%s'", new Object[]{this._closedDialogHandle}));
				}
				this._currentDialogHandle = this._closedDialogHandle.getParent();
				needToMove = true;
			}
			if (this._launchedDialogHandle != null) {
				if (_LOG.isLoggable(Level.FINE)) {
					_LOG.fine(
							String.format("Processing launched dialog '%s'", new Object[]{this._launchedDialogHandle}));
				}
				this._workAroundUnstableWindowName(webDriver, this._launchedDialogHandle);
				this._currentDialogHandle = this._launchedDialogHandle;
				needToMove = true;
			}
			this._closedDialogHandle = null;
			this._launchedDialogHandle = null;
		}
		if (needToMove || !DialogHandle.DialogType.MAIN_WINDOW.equals((Object) this._currentDialogHandle.getType())) {
			_LOG.fine(String.format("Either dialog was launched or dismissed, or current dialog was not MAIN_WINDOW.",
					new Object[0]));
			this._traverseTo(webDriver, this._currentDialogHandle);
		} else {
			_LOG.finest("No need to move. No dialogs launched or dismissed and already in MAIN_WINDOW");
		}
	}

	void focusCurrentDialogsParent(WebDriver webDriver) {
		this._moveTo(webDriver, this._currentDialogHandle.getParent());
	}

	boolean inDialog() {
		return this._currentDialogHandle.getType() != DialogHandle.DialogType.MAIN_WINDOW;
	}

	void focusDialog(WebDriver webDriver, DialogHandle handle) {
		if (_LOG.isLoggable(Level.FINEST)) {
			_LOG.finest(String.format("Executing focusDialog for '%s'", new Object[]{handle}));
		}
		this._traverseTo(webDriver, handle);
		this._currentDialogHandle = handle;
	}

	void refreshDialog(WebDriver webDriver, DialogHandle handle) {
		if (_LOG.isLoggable(Level.FINEST)) {
			_LOG.finest(String.format("Executing refreshDialog for '%s'", new Object[]{handle}));
		}
		this.focusDialog(webDriver, handle);
		webDriver.navigate().refresh();
		RichWebDrivers.waitForServer((WebDriver) webDriver, (long) this._timeout,
				(boolean) this._useAutomaticDialogDetection);
	}

	boolean isDialogAlive(WebDriver webDriver, DialogHandle handle) {
		boolean returnValue;
		if (_LOG.isLoggable(Level.FINEST)) {
			_LOG.finest(String.format("Executing isDialogAlive for '%s'", new Object[]{handle}));
		}
		if (handle == null) {
			return false;
		}
		if (handle.equals((Object) this._currentDialogHandle)) {
			return true;
		}
		try {
			this._traverseTo(webDriver, handle);
			returnValue = true;
		} catch (Exception e) {
			returnValue = false;
		}
		this._traverseTo(webDriver, this._currentDialogHandle);
		return returnValue;
	}

	void closeDialog(WebDriver webDriver, DialogHandle handle) {
		boolean dismissedSuccessfully;
		if (_LOG.isLoggable(Level.FINEST)) {
			_LOG.finest(String.format("Executing closeDialog for '%s'", new Object[]{handle}));
		}
		if (handle == null || webDriver == null) {
			return;
		}
		if (handle.getType() == DialogHandle.DialogType.MAIN_WINDOW) {
			throw new UnsupportedOperationException(
					"DialogManager#closeDialog: Cannot close the main window using DialogManager");
		}
		if (DialogHandle.DialogType.WINDOW == handle.getType()) {
			this._traverseTo(webDriver, handle);
			webDriver.close();
			dismissedSuccessfully = true;
		} else {
			this._traverseTo(webDriver, handle.getParent());
			dismissedSuccessfully = (Boolean) ((JavascriptExecutor) webDriver).executeScript(_INLINE_DIALOG_DISMISS_JS,
					new Object[]{handle.getInlinePopupId()});
		}
		if (dismissedSuccessfully) {
			if (_LOG.isLoggable(Level.FINEST)) {
				_LOG.finest(String.format("Dialog dismissed successfully '%s'. Proceed with waitForServer.",
						new Object[]{handle}));
			}
		} else {
			String message = String.format("Failed to dismiss dialog '%s'", new Object[]{handle});
			if (_LOG.isLoggable(Level.SEVERE)) {
				_LOG.severe(message);
			}
			throw new WebDriverException(message);
		}
		RichWebDrivers.waitForServer((WebDriver) webDriver, (long) this._timeout,
				(boolean) this._useAutomaticDialogDetection);
	}

	String getDialogTitle(WebDriver webDriver, DialogHandle handle) {
		boolean needTraversal;
		if (handle == null || webDriver == null) {
			return null;
		}
		boolean bl = needTraversal = !handle.equals((Object) this._currentDialogHandle);
		if (needTraversal) {
			this._traverseTo(webDriver, handle);
		}
		String title = null;
		DialogHandle.DialogType currDialogType = handle.getType();
		String currInlineWindowDomId = handle.getInlineWindowDomId();
		DialogHandle parent = handle.getParent();
		if (DialogHandle.DialogType.INLINE == currDialogType && currInlineWindowDomId != null) {
			this._traverseTo(webDriver, parent);
			String locator = MessageFormat.format(_INLINE_DIALOG_TITLE_LOCATOR, currInlineWindowDomId);
			title = webDriver.findElement(ByRich.locator((String) locator)).getText();
			this._traverseTo(webDriver, handle);
		} else if (DialogHandle.DialogType.WINDOW == currDialogType) {
			WebDriverWait wait = new WebDriverWait(webDriver, this._timeout);
			wait.ignoring(WebDriverException.class);
			wait.until((Function) new ExpectedCondition<Boolean>() {

				public Boolean apply(WebDriver webDriver) {
					return (Boolean) ((JavascriptExecutor) webDriver)
							.executeScript(DialogManager._WAIT_FOR_DIALOG_TITLE_JS, new Object[0]);
				}
			});
			title = webDriver.getTitle();
		} else if (DialogHandle.DialogType.MAIN_WINDOW == currDialogType) {
			title = webDriver.getTitle();
		}
		if (needTraversal) {
			this._traverseTo(webDriver, this._currentDialogHandle);
		}
		return title;
	}

	void notifyDialogLaunched(DialogHandle dialog) {
		this._assignParentIfMissing(dialog);
		this._launchedDialogHandle = dialog;
		this._isNestedDialog = false;
		if (_LOG.isLoggable(Level.FINEST)) {
			_LOG.finest(String.format("Registered launched dialog '%s' ", new Object[]{dialog}));
		}
	}

	void notifyDialogClosed(DialogHandle dialog) {
		this._assignParentIfMissing(dialog);
		this._closedDialogHandle = dialog;
		this._isNestedDialog = false;
		if (_LOG.isLoggable(Level.FINEST)) {
			_LOG.finest(String.format("Registered closed dialog '%s'", new Object[]{dialog}));
		}
	}

	void notifyNestedDialogLaunched(DialogHandle dialog) {
		this._launchedDialogHandle = dialog;
		this._isNestedDialog = true;
		_LOG.finest(String.format("Registered launched nested dialog '%s'", new Object[]{dialog}));
	}

	void notifyNestedDialogClosed(DialogHandle dialog) {
		this._closedDialogHandle = dialog;
		this._isNestedDialog = true;
		if (_LOG.isLoggable(Level.FINEST)) {
			_LOG.finest(String.format("Registered closed nested dialog '%s'", new Object[]{dialog}));
		}
	}

	private void _assignParentIfMissing(DialogHandle handle) {
		if (handle.getParent() == null) {
			handle.assignParent(this._mainWindowHandle);
		}
	}

	private void _workAroundUnstableWindowName(WebDriver webDriver, DialogHandle handle) {
		if (_LOG.isLoggable(Level.FINEST)) {
			_LOG.finest(String.format("Executing _workAroundUnstableWindowName for '%s'", new Object[]{handle}));
		}
		if (handle == null || handle.getType() != DialogHandle.DialogType.WINDOW) {
			return;
		}
		final String rtnId = String.valueOf(handle.getReturnId());
		final ArrayList stableWindowNameHolder = new ArrayList(1);
		WebDriverWait wait = new WebDriverWait(webDriver, this._timeout);
		wait.until((Function) new ExpectedCondition<Boolean>() {

			public Boolean apply(WebDriver webDriver) {
				String windowName = (String) ((JavascriptExecutor) webDriver)
						.executeScript(DialogManager._GET_STABLE_WINDOW_NAME_JS, new Object[]{rtnId});
				if (windowName == null || windowName.contains("$")) {
					stableWindowNameHolder.add(windowName);
					return true;
				}
				return false;
			}
		});
		String stableWindowName = (String) stableWindowNameHolder.get(0);
		if (stableWindowName == null) {
			throw new WebDriverException(String.format(
					"Error while launching dialog. Cannot find a window name for dialog with return id: '%s'. It may have closed.",
					rtnId));
		}
		handle.setDialogWindowHandle(stableWindowName);
	}

	private DialogHandle _generateDialogHandleFromReturnId(WebDriver webDriver, Long handleReturnId) {
		List dialogInfo = (List) ((JavascriptExecutor) webDriver).executeScript(_GET_RICH_DIALOG_INFO_FOR_RET_ID_JS,
				new Object[]{handleReturnId});
		if (_LOG.isLoggable(Level.FINEST)) {
			_LOG.finest(String.format("Dialog info '%s' found for returnId '%s'", dialogInfo, handleReturnId));
		}
		String type = (String) dialogInfo.get(0);
		DialogHandle retHandle = null;
		if ("inline".equals(type)) {
			retHandle = DialogHandle.createInlineDialogHandle((Long) handleReturnId,
					(String) ((String) dialogInfo.get(1)), (String) ((String) dialogInfo.get(2)),
					(String) ((String) dialogInfo.get(3)), (String) ((String) dialogInfo.get(4)));
		} else if ("window".equals(type)) {
			retHandle = DialogHandle.createWindowDialogHandle((Long) handleReturnId,
					(String) ((String) dialogInfo.get(1)), (String) ((String) dialogInfo.get(2)));
		} else {
			String message = "Unknown dialog type. Dialog type has to be either 'inline' or 'window'";
			throw new IllegalStateException(message);
		}
		retHandle.assignParent(this._currentDialogHandle);
		this._workAroundUnstableWindowName(webDriver, retHandle);
		return retHandle;
	}

	private void _traverseTo(WebDriver wd, DialogHandle handle) {
		if (handle == null) {
			return;
		}
		if (handle.getType() == DialogHandle.DialogType.MAIN_WINDOW) {
			this._moveTo(wd, this._mainWindowHandle);
			return;
		}
		ArrayDeque<DialogHandle> parents = new ArrayDeque<DialogHandle>();
		_LOG.finest("_traverseTo: collect parents");
		for (DialogHandle currParent = handle.getParent(); currParent != null; currParent = currParent.getParent()) {
			parents.push(currParent);
		}
		DialogHandle ancestor = (DialogHandle) parents.peek();
		if (ancestor != this._mainWindowHandle) {
			throw new IllegalStateException("The parent hierarchy has to start with MAIN_WINDOW");
		}
		while (parents.peek() != null) {
			this._moveTo(wd, (DialogHandle) parents.pop());
		}
		_LOG.finest("_traverseTo: finally move to the dialog req.");
		this._moveTo(wd, handle);
	}

	private void _moveTo(WebDriver webDriver, DialogHandle handle) {
		if (handle.getType() == DialogHandle.DialogType.MAIN_WINDOW) {
			if (handle != this._mainWindowHandle) {
				throw new IllegalStateException(
						"Found a different MAIN_WINDOW handle than expected. There should be only one MAIN_WINDOW handle.");
			}
			webDriver.switchTo().window(this._mainWindowHandle.getWindowHandle());
		} else if (DialogHandle.DialogType.INLINE.equals((Object) handle.getType())) {
			String locator = MessageFormat.format(_INLINE_DIALOG_FRAME_LOCATOR, handle.getInlineDialogId());
			WebElement el = webDriver.findElement(ByRich.locator((String) locator));
			webDriver.switchTo().frame(el.getAttribute("id"));
		} else if (DialogHandle.DialogType.WINDOW.equals((Object) handle.getType())) {
			webDriver.switchTo().window(handle.getWindowHandle());
			webDriver.switchTo().frame(0);
		}
	}

	private List<Long> _getAllDialogIds(WebDriver webDriver) {
		List dialogIds = (List) ((JavascriptExecutor) webDriver).executeScript(_GET_RICH_DIALOG_IDS_JS, new Object[0]);
		if (dialogIds == null) {
			return Collections.emptyList();
		}
		return dialogIds;
	}

	private DialogManager(WebDriver wd, long timeout, boolean useAutomaticDialogDetection) {
		this._currentDialogHandle = this._mainWindowHandle = DialogHandle
				.createMainWindowHandle((String) wd.getWindowHandle());
		this._timeout = timeout;
		this._useAutomaticDialogDetection = useAutomaticDialogDetection;
	}

	public static enum SelectDialogBy {
		TITLE, LAUNCH_SOURCE_ID;

		private SelectDialogBy() {
		}
	}

}

/*
	DECOMPILATION REPORT

	Decompiled from: C:\Oracle\Middleware_12c_bpmqs\Oracle_Home\oracle_common\modules\oracle.adf.view\adf-richclient-automation-11.jar
	Total time: 85 ms
	
	Decompiled with CFR 0_130.
	Could not load the following classes:
	com.google.common.base.Function
	oracle.adf.view.rich.automation.selenium.ByRich
	oracle.adf.view.rich.automation.selenium.Dialog
	oracle.adf.view.rich.automation.selenium.DialogHandle
	oracle.adf.view.rich.automation.selenium.DialogHandle$DialogType
	oracle.adf.view.rich.automation.selenium.RichWebDrivers
	org.openqa.selenium.By
	org.openqa.selenium.JavascriptExecutor
	org.openqa.selenium.WebDriver
	org.openqa.selenium.WebDriver$Navigation
	org.openqa.selenium.WebDriver$TargetLocator
	org.openqa.selenium.WebDriverException
	org.openqa.selenium.WebElement
	org.openqa.selenium.support.ui.ExpectedCondition
	org.openqa.selenium.support.ui.FluentWait
	org.openqa.selenium.support.ui.WebDriverWait
	
*/