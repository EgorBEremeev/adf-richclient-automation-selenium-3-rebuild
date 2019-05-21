package oracle.adf.view.rich.automation.selenium;

import oracle.adf.view.rich.automation.selenium.DialogHandle;
import oracle.adf.view.rich.automation.selenium.DialogManager;
import org.openqa.selenium.WebDriver;

public final class Dialog {
	private DialogHandle _handle;

	Dialog(DialogHandle handle) {
		this._handle = handle;
	}

	public String getTitle(WebDriver webDriver) {
		return DialogManager.getInstance().getDialogTitle(webDriver, this._handle);
	}

	public boolean isAlive(WebDriver webDriver) {
		return DialogManager.getInstance().isDialogAlive(webDriver, this._handle);
	}

	@Deprecated
	public boolean isAlive() {
		return false;
	}

	public void close(WebDriver webDriver) {
		DialogManager.getInstance().closeDialog(webDriver, this._handle);
	}

	public void focus(WebDriver webDriver) {
		DialogManager.getInstance().focusDialog(webDriver, this._handle);
	}

	public void refresh(WebDriver webDriver) {
		DialogManager.getInstance().refreshDialog(webDriver, this._handle);
	}

	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof Dialog)) {
			return false;
		}
		Dialog other = (Dialog) object;
		if (!(this._handle != null ? this._handle.equals((Object) other._handle) : other._handle == null)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		return this._handle.hashCode();
	}

	public String toString() {
		return this._handle.toString();
	}

	DialogHandle getHandle() {
		return this._handle;
	}
}

/*
	DECOMPILATION REPORT

	Decompiled from: C:\Oracle\Middleware_12c_bpmqs\Oracle_Home\oracle_common\modules\oracle.adf.view\adf-richclient-automation-11.jar
	Total time: 35 ms
	
	Decompiled with CFR 0_130.
	Could not load the following classes:
	oracle.adf.view.rich.automation.selenium.DialogHandle
	oracle.adf.view.rich.automation.selenium.DialogManager
	org.openqa.selenium.WebDriver
	
*/