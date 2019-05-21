package oracle.adf.view.rich.automation.selenium;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import oracle.adf.view.rich.automation.selenium.RichWebDrivers;
import org.openqa.selenium.Alert;
import org.openqa.selenium.Beta;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.security.Credentials;

public class RichAlert implements Alert {
	private WebDriver _wrappedDriver;
	private Alert _wrappedAlert;
	private boolean _useAutomaticDialogDetection;
	private static final Logger _LOG = Logger.getLogger(RichAlert.class.getName());

	public RichAlert(WebDriver driver, Alert alert, boolean useAutomaticDialogDetection) {
		this._wrappedDriver = driver;
		this._wrappedAlert = alert;
		this._useAutomaticDialogDetection = useAutomaticDialogDetection;
	}

	public void dismiss() {
		int attempt = 0;
		do {
			try {
				++attempt;
				this._wrappedAlert.dismiss();
			} catch (WebDriverException wde) {
				if (_LOG.isLoggable(Level.INFO)) {
					_LOG.info("Attempt[" + attempt + " of 10] RichAlert.dismiss() failed with the exception "
							+ wde.getMessage());
				}
				try {
					Thread.sleep(250L);
					continue;
				} catch (InterruptedException interruptedException) {
					// empty catch block
				}
				if (attempt <= 10)
					continue;
			}
			break;
		} while (true);
		RichWebDrivers.waitForServer((WebDriver) this._wrappedDriver, (long) 120000L,
				(boolean) this._useAutomaticDialogDetection);
	}

	public void accept() {
		int attempt = 0;
		do {
			try {
				++attempt;
				this._wrappedAlert.accept();
			} catch (WebDriverException wde) {
				if (_LOG.isLoggable(Level.INFO)) {
					_LOG.info("Attempt[" + attempt + " of 10] RichAlert.accept() failed with the exception "
							+ wde.getMessage());
				}
				try {
					Thread.sleep(250L);
					continue;
				} catch (InterruptedException interruptedException) {
					// empty catch block
				}
				if (attempt <= 10)
					continue;
			}
			break;
		} while (true);
		RichWebDrivers.waitForServer((WebDriver) this._wrappedDriver, (long) 120000L,
				(boolean) this._useAutomaticDialogDetection);
	}

	public String getText() {
		return this._wrappedAlert.getText();
	}

	public void sendKeys(String keys) {
		this._wrappedAlert.sendKeys(keys);
	}

	public void authenticateUsing(Credentials credentials) {
		this._wrappedAlert.authenticateUsing(credentials);
	}

	@Beta
	public void setCredentials(Credentials credentials) {
		try {
			Method setCredentialsMethod = this._wrappedAlert.getClass().getMethod("setCredentials", Credentials.class);
			setCredentialsMethod.invoke((Object) this._wrappedAlert, new Object[]{credentials});
		} catch (NoSuchMethodException e) {
			return;
		} catch (InvocationTargetException e) {
			return;
		} catch (IllegalAccessException e) {
			return;
		}
	}
}

/*
	DECOMPILATION REPORT

	Decompiled from: C:\Oracle\Middleware_12c_bpmqs\Oracle_Home\oracle_common\modules\oracle.adf.view\adf-richclient-automation-11.jar
	Total time: 42 ms
	
	Decompiled with CFR 0_130.
	Could not load the following classes:
	oracle.adf.view.rich.automation.selenium.RichWebDrivers
	org.openqa.selenium.Alert
	org.openqa.selenium.Beta
	org.openqa.selenium.WebDriver
	org.openqa.selenium.WebDriverException
	org.openqa.selenium.security.Credentials
	
*/