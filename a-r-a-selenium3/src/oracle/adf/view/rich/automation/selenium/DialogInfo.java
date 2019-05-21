package oracle.adf.view.rich.automation.selenium;

import com.google.common.base.Function;
import java.io.PrintStream;
import java.text.MessageFormat;
import oracle.adf.view.rich.automation.selenium.ByRich;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

@Deprecated
public class DialogInfo {
	private DialogType _dialogType;
	private String _dialogWindowHandle;
	private String _parentWindowHandle;
	private DialogType _parentDialogType;
	private String _inlineDialogId;
	private String _inlineWindowDomId;
	private String _parentInlineDialogId;
	private static final String _INLINE_DIALOG_TITLE_LOCATOR = "rich={0}#title";
	private static final String _INLINE_DIALOG_FRAME_LOCATOR = "rich={0}#targetFrame";

	public DialogType getDialogType() {
		return this._dialogType;
	}

	public String getDialogWindowHandle() {
		return this._dialogWindowHandle;
	}

	public String getParentWindowHandle() {
		return this._parentWindowHandle;
	}

	public String getInlineDialogId() {
		return this._inlineDialogId;
	}

	public String getParentInlineDialogId() {
		return this._parentInlineDialogId;
	}

	public DialogType getParentDialogType() {
		return this._parentDialogType;
	}

	public String getTitle(WebDriver webDriver) {
		String title = null;
		if (this._dialogType == DialogType.INLINE && this._inlineWindowDomId != null) {
			webDriver.switchTo().window(this._parentWindowHandle);
			if (DialogType.WINDOW.equals((Object) this._parentDialogType)) {
				webDriver.switchTo().frame(0);
			}
			String locator = MessageFormat.format(_INLINE_DIALOG_TITLE_LOCATOR, this._inlineWindowDomId);
			title = webDriver.findElement(ByRich.locator((String) locator)).getText();
			locator = MessageFormat.format(_INLINE_DIALOG_FRAME_LOCATOR, this._inlineDialogId);
			WebElement el = webDriver.findElement(ByRich.locator((String) locator));
			if (el != null) {
				webDriver.switchTo().frame(el.getAttribute("id"));
			} else {
				System.out.println("Could not find the iframe to return to with locator: " + locator);
			}
		} else {
			String dialogHandle = this.getDialogWindowHandle();
			if (dialogHandle == null) {
				throw new IllegalStateException(
						"getTitle() invoked on DialogInfo with null window handle. Please ensure DialogInfo is properly initialized before invoking getTitle().");
			}
			String currentWindowHandle = webDriver.getWindowHandle();
			boolean notInDialogContext = true;
			if (!dialogHandle.equals(currentWindowHandle)) {
				notInDialogContext = true;
				webDriver.switchTo().window(dialogHandle);
				webDriver.switchTo().frame(0);
			} else {
				notInDialogContext = false;
			}
			WebDriverWait wait = new WebDriverWait(webDriver, 120000L);
			wait.ignoring(WebDriverException.class);
			wait.until((Function) new ExpectedCondition<Boolean>() {

				public Boolean apply(WebDriver webDriver) {
					return (Boolean) ((JavascriptExecutor) webDriver).executeScript(
							"return window.parent.document.title === window.document.title;", new Object[0]);
				}
			});
			title = webDriver.getTitle();
			if (notInDialogContext) {
				webDriver.switchTo().window(currentWindowHandle);
			}
		}
		return title;
	}

	static DialogInfo createInlineDialogInfo(String inlineDialogId, String inlineWindowDomId, String parentWindowHandle,
			DialogInfo parentDialogInfo) {
		DialogInfo info = new DialogInfo(DialogType.INLINE);
		info._setInlineDialogId(inlineDialogId);
		info._setInlineWindowDomId(inlineWindowDomId);
		DialogInfo._popupateParentInfo(info, parentWindowHandle, parentDialogInfo);
		return info;
	}

	static DialogInfo createWindowDialogInfo(String dialogWindowHandle, String parentWindowHandle,
			DialogInfo parentDialogInfo) {
		DialogInfo info = new DialogInfo(DialogType.WINDOW);
		info._setDialogWindowHandle(dialogWindowHandle);
		DialogInfo._popupateParentInfo(info, parentWindowHandle, parentDialogInfo);
		return info;
	}

	private DialogInfo(DialogType dialogType) {
		this._dialogType = dialogType;
	}

	private void _setDialogWindowHandle(String dialogWindowHandle) {
		this._dialogWindowHandle = dialogWindowHandle;
	}

	private void _setInlineDialogId(String inlineDialogId) {
		this._inlineDialogId = inlineDialogId;
	}

	private void _setInlineWindowDomId(String inlineWindowDomId) {
		this._inlineWindowDomId = inlineWindowDomId;
	}

	private void _setParentInlineDialogId(String parentInlineDialogId) {
		this._parentInlineDialogId = parentInlineDialogId;
	}

	private void _setParentWindowHandle(String parentWindowHandle) {
		this._parentWindowHandle = parentWindowHandle;
	}

	private void _setParentDialogType(DialogType parentDialogType) {
		this._parentDialogType = parentDialogType;
	}

	private static void _popupateParentInfo(DialogInfo info, String parentWindowHandle, DialogInfo parentDialogInfo) {
		info._setParentWindowHandle(parentWindowHandle);
		if (parentDialogInfo != null) {
			info._setParentDialogType(parentDialogInfo.getDialogType());
			if (parentDialogInfo.getInlineDialogId() != null) {
				info._setParentInlineDialogId(parentDialogInfo.getInlineDialogId());
			}
		}
	}

	public static enum DialogType {
		INLINE, WINDOW;

		private DialogType() {
		}
	}

}

/*
	DECOMPILATION REPORT

	Decompiled from: C:\Oracle\Middleware_12c_bpmqs\Oracle_Home\oracle_common\modules\oracle.adf.view\adf-richclient-automation-11.jar
	Total time: 80 ms
	
	Decompiled with CFR 0_130.
	Could not load the following classes:
	com.google.common.base.Function
	oracle.adf.view.rich.automation.selenium.ByRich
	org.openqa.selenium.By
	org.openqa.selenium.JavascriptExecutor
	org.openqa.selenium.WebDriver
	org.openqa.selenium.WebDriver$TargetLocator
	org.openqa.selenium.WebDriverException
	org.openqa.selenium.WebElement
	org.openqa.selenium.support.ui.ExpectedCondition
	org.openqa.selenium.support.ui.FluentWait
	org.openqa.selenium.support.ui.WebDriverWait
	
*/