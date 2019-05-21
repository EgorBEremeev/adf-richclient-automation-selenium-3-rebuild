package oracle.adf.view.rich.automation.selenium;

import java.util.Map;
import java.util.logging.Logger;
import oracle.adf.view.rich.automation.selenium.Dialog;
import oracle.adf.view.rich.automation.selenium.DialogManager;

final class DialogHandle {
	private final DialogType _dialogType;
	private final Long _returnId;
	private String _dialogWindowHandle;
	private String _launchSourceId;
	private String _inlinePopupId;
	private String _inlineDialogId;
	private String _inlineWindowDomId;
	private DialogHandle _parentDialog;
	private static final Logger _LOG = Logger.getLogger(DialogHandle.class.getName());

	static DialogHandle createInlineDialogHandle(Long returnId, String inlinePopupId, String inlineDialogId,
			String inlineWindowDomId, String launchSourceId) {
		assert (returnId != null);
		assert (inlinePopupId != null && !inlinePopupId.isEmpty());
		assert (inlineDialogId != null && !inlineDialogId.isEmpty());
		assert (inlineWindowDomId != null && !inlineWindowDomId.isEmpty());
		assert (launchSourceId != null && !launchSourceId.isEmpty());
		DialogHandle handle = new DialogHandle(returnId, DialogType.INLINE);
		handle._setInlinePopupId(inlinePopupId);
		handle._setInlineDialogId(inlineDialogId);
		handle._setInlineWindowDomId(inlineWindowDomId);
		handle._setLaunchSourceId(launchSourceId);
		return handle;
	}

	static DialogHandle createWindowDialogHandle(Long returnId, String dialogWindowHandle, String launchSourceId) {
		assert (returnId != null);
		assert (dialogWindowHandle != null && !dialogWindowHandle.isEmpty());
		assert (launchSourceId != null && !launchSourceId.isEmpty());
		DialogHandle handle = new DialogHandle(returnId, DialogType.WINDOW);
		handle.setDialogWindowHandle(dialogWindowHandle);
		handle._setLaunchSourceId(launchSourceId);
		return handle;
	}

	static DialogHandle createMainWindowHandle(String dialogWindowHandle) {
		assert (dialogWindowHandle != null && !dialogWindowHandle.isEmpty());
		DialogHandle handle = new DialogHandle(-1000L, DialogType.MAIN_WINDOW);
		handle.setDialogWindowHandle(dialogWindowHandle);
		return handle;
	}

	static DialogHandle createNestedDialogHandle(Map<String, String> infoMap) {
		return DialogHandle._createDialogHandle(infoMap, true);
	}

	static DialogHandle createDialogHandle(Map<String, String> infoMap) {
		return DialogHandle._createDialogHandle(infoMap, false);
	}

	private DialogHandle(Long returnId, DialogType dialogType) {
		assert (returnId != null);
		assert (dialogType != null);
		this._returnId = returnId;
		this._dialogType = dialogType;
	}

	public String toString() {
		StringBuilder toString = new StringBuilder();
		toString.append("ReturnId: ").append(this._returnId).append(", DialogType: ").append((Object) this._dialogType);
		if (this._dialogWindowHandle != null) {
			toString.append(", DialogwindowHandle: ").append(this._dialogWindowHandle);
		}
		if (this._inlineDialogId != null) {
			toString.append(", InlineDialogId: ").append(this._inlineDialogId);
		}
		if (this._inlineWindowDomId != null) {
			toString.append(", InlineWindowDomId: ").append(this._inlineWindowDomId);
		}
		toString.append(", { ParentDialog: ").append(this._parentDialog).append(" }");
		return toString.toString();
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DialogHandle)) {
			return false;
		}
		DialogHandle handle = (DialogHandle) o;
		if (this._dialogType != handle._dialogType) {
			return false;
		}
		if (!this._returnId.equals(handle._returnId)) {
			return false;
		}
		if (this._dialogWindowHandle != null
				? !this._dialogWindowHandle.equals(handle._dialogWindowHandle)
				: handle._dialogWindowHandle != null) {
			return false;
		}
		if (this._inlineDialogId != null
				? !this._inlineDialogId.equals(handle._inlineDialogId)
				: handle._inlineDialogId != null) {
			return false;
		}
		if (this._inlinePopupId != null
				? !this._inlinePopupId.equals(handle._inlinePopupId)
				: handle._inlinePopupId != null) {
			return false;
		}
		if (this._inlineWindowDomId != null
				? !this._inlineWindowDomId.equals(handle._inlineWindowDomId)
				: handle._inlineWindowDomId != null) {
			return false;
		}
		if (this._parentDialog != null
				? !this._parentDialog.equals(handle._parentDialog)
				: handle._parentDialog != null) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int result = this._dialogType.hashCode();
		result = 31 * result + this._returnId.hashCode();
		result = 31 * result + (this._dialogWindowHandle != null ? this._dialogWindowHandle.hashCode() : 0);
		result = 31 * result + (this._inlineDialogId != null ? this._inlineDialogId.hashCode() : 0);
		result = 31 * result + (this._inlinePopupId != null ? this._inlinePopupId.hashCode() : 0);
		result = 31 * result + (this._inlineWindowDomId != null ? this._inlineWindowDomId.hashCode() : 0);
		result = 31 * result + (this._parentDialog != null ? this._parentDialog.hashCode() : 0);
		return result;
	}

	void assignParent(DialogHandle parent) {
		this._parentDialog = parent;
	}

	DialogHandle getParent() {
		return this._parentDialog;
	}

	Long getReturnId() {
		return this._returnId;
	}

	String getInlineWindowDomId() {
		return this._inlineWindowDomId;
	}

	String getLaunchSourceId() {
		return this._launchSourceId;
	}

	DialogType getType() {
		return this._dialogType;
	}

	String getWindowHandle() {
		return this._dialogWindowHandle;
	}

	String getInlineDialogId() {
		return this._inlineDialogId;
	}

	String getInlinePopupId() {
		return this._inlinePopupId;
	}

	void setDialogWindowHandle(String dialogWindowHandle) {
		this._dialogWindowHandle = dialogWindowHandle;
	}

	private static DialogHandle _createDialogHandle(Map<String, String> infoMap, boolean nested) {
		DialogHandle handle;
		assert (infoMap != null);
		assert (infoMap.get("rtnId") != null);
		Long rtnId = Long.parseLong(infoMap.get("rtnId"));
		String type = infoMap.get("type");
		String launchSourceId = infoMap.get("launchSourceId");
		if ("INLINE".equals(type)) {
			handle = DialogHandle.createInlineDialogHandle(rtnId, infoMap.get("popupId"), infoMap.get("frameName"),
					infoMap.get("panelWindowId"), launchSourceId);
		} else {
			String windowNameFromClient = infoMap.get("windowName");
			handle = DialogHandle.createWindowDialogHandle(rtnId, windowNameFromClient, launchSourceId);
		}
		Dialog currentDialog = DialogManager.getInstance().getCurrentDialog();
		if (currentDialog != null) {
			DialogHandle currentDialogHandle = currentDialog.getHandle();
			handle.assignParent(nested ? currentDialogHandle : currentDialogHandle.getParent());
		}
		return handle;
	}

	private void _setInlineDialogId(String inlineDialogId) {
		this._inlineDialogId = inlineDialogId;
	}

	private void _setInlinePopupId(String inlinePopupId) {
		this._inlinePopupId = inlinePopupId;
	}

	private void _setInlineWindowDomId(String inlineWindowDomId) {
		this._inlineWindowDomId = inlineWindowDomId;
	}

	private void _setLaunchSourceId(String launchSourceId) {
		this._launchSourceId = launchSourceId;
	}

	static enum DialogType {
		INLINE, WINDOW, MAIN_WINDOW;

		private DialogType() {
		}
	}

}

/*
	DECOMPILATION REPORT

	Decompiled from: C:\Oracle\Middleware_12c_bpmqs\Oracle_Home\oracle_common\modules\oracle.adf.view\adf-richclient-automation-11.jar
	Total time: 94 ms
	
	Decompiled with CFR 0_130.
	Could not load the following classes:
	oracle.adf.view.rich.automation.selenium.Dialog
	oracle.adf.view.rich.automation.selenium.DialogManager
	
*/