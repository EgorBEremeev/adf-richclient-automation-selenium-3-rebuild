# adf-richclient-automation-selenium-3-rebuild
This repository contains Eclipse Java project for rebuilding `adf-richclient-automation-11.jar` with selenium-java-3-3-1 libs

Sources of adf-richclient-automation-11.jar produced by decompiling `adf-richclient-automation-11.jar` distributed with JDeveloper.

This is workaround solution for https://stackoverflow.com/questions/52042307/how-to-get-adf-richclient-automation-11-jar-compatible-with-the-latest-selenium
until official version of `adf-richclient-automation-11.jar` will be released.
 
If you have faced with such exception then rebuilt library will fix it:
 
~~~~
org.openqa.selenium.WebDriverException: java.lang.NoSuchMethodError: org.openqa.selenium.support.ui.**WebDriverWait.until(Lcom/google/common/base/Function;)Ljava/lang/Object;** Build info: version: 'unknown', revision: '5234b32', time: '2017-03-10 09:00:17 -0800' System info: host: 'EE-LATITUDE-749', ip: '10.10.207.64', os.name: 'Windows 10', os.arch: 'amd64', os.version: '10.0', java.version: '1.8.0_172' Driver info: driver.version: unknown
  com.redheap.selenium.junit.PageProvider.createPage(PageProvider.java:49)
  com.redheap.selenium.junit.PageProvider.goHome(PageProvider.java:36)
  ru.russvet.selenium.tests.P6_ProcessPageTest.(P6_ProcessPageTest.java:38)
  java.lang.reflect.Constructor.newInstance(Constructor.java:423)
Caused by: java.lang.NoSuchMethodError: org.openqa.selenium.support.ui.WebDriverWait.until(Lcom/google/common/base/Function;)Ljava/lang/Object;
  oracle.adf.view.rich.automation.selenium.RichWebDrivers.waitForServer(RichWebDrivers.java:112)
  oracle.adf.view.rich.automation.selenium.RichWebDrivers.waitForRichPageToLoad(RichWebDrivers.java:175)
  oracle.adf.view.rich.automation.selenium.RichWebDrivers.waitForRichPageToLoad(RichWebDrivers.java:158)
  com.redheap.selenium.page.Page.(Page.java:53)
  com.redheap.selenium.page.Page.(Page.java:45)
  ru.russvet.selenium.pages.BPMWorkspaceLoginPage.(BPMWorkspaceLoginPage.java:19)
  com.redheap.selenium.junit.PageProvider.createPage(PageProvider.java:47)
~~~~

# Full Steps to manualy rebuilt adf-richclient-automation-11.jar:

## Environment
1. Install Eclipse
2. Install Decompiler pluging
```
	Help -> Marketplace -> Enhanced Class Decompiler
	Windows -> Preferences -> Java -> Decompiler -> Default Class Decompiler: CFR -> Applay and Close	
```
3. Set User Libraries
```
	Windows -> Preferences -> Java -> Build Path -> User Libraries
		 New->
			Name -> selenium-java-3.3.1
			Add External JARs... ->
				path\to\selenium-java-3.3.1\
					client-combined-3.3.1-nodeps.jar
					lib\*.jar
		 ->Finish
		 
		 New->
			Name -> adf-richclient-automation-11.jar
			Add External JARs... ->
				path\to\Oracle_Home\oracle_common\modules\oracle.adf.view\
					adf-richclient-automation-11.jar
		 ->Finish
	 -> Applay and Close
```
## Steps	
1. Create Java Project
```
Eclipse -> New -> Java Project
	Name -> project_name
	JDK -> 1.8
	Build Path -> Libraries -> Add Library -> User Library -> Next
		User Libraries ...
			selenium-java-3.3.1
			adf-richclient-automation-11.jar
```
2. Decompile adf-richclient-automation-11.jar
```
Project Explorer -> adf-richclient-automation-11.jar -> Context Menu -> Export Sources
	path\to\project_name\src\
		adf-richclient-automation-11-src.zip
	Project Explorer -> Refresh
		src -> adf-richclient-automation-11-src.zip
```
3. Extract decompiled sources into path\to\project_name\src\
4. Check the src
```
Project Explorer -> Refresh
		src -> adf-richclient-automation-11-src.zip
				* oracle.adf.view.rich.automation.selenium
				* oracle.adf.view.rich.automation.test
				   oracle.adf.view.rich.automation.test.browserfactory
				* oracle.adf.view.rich.automation.test.component
				* oracle.adf.view.rich.automation.test.selenium
				   org.openqa.selenium
				   org.openqa.selenium.firefox
```
5.1 Delete classes used for and with Selenium RC: 
```
		path/to/project_name/src/oracle/adf/view/rich/automation/selenium/RichSelenium.java -> Delete
```
5.2 Delete packages oracle.adf.view.rich.automation.test.* -> Delete
```
			oracle.adf.view.rich.automation.test
			oracle.adf.view.rich.automation.test.browserfactory
			oracle.adf.view.rich.automation.test.component
			oracle.adf.view.rich.automation.test.selenium
```
6. Fix errors:
   - path/to/project_name/src/oracle/adf/view/rich/automation/selenium/RichWebDrivers.java
     - [] 241 Type mismatch: cannot convert from element type Object to String ->
       ```java
       fix 239 -> List<String> logs = (List) jsExecutor.executeScript(_GET_AND_CLEAR_LOG_MESSAGES_JS,
		= 
		List<String> logs = (List) jsExecutor.executeScript(_GET_AND_CLEAR_LOG_MESSAGES_JS,
				new Object[]{logLevel.toString().toUpperCase()});
		for (String s : logs) {
			sbf.append(s).append(_NEW_LINE);
		}
       ```
     - [] 321 Type mismatch: cannot convert from element type Object to String ->
       ```java
       fix 320 -> Set<String> handles = webDriver.getWindowHandles();
		=
		public String apply(WebDriver webDriver) {
			Set<String> handles = webDriver.getWindowHandles();
			for (String handle : handles) {
				if (openWindowHandles.contains(handle))
					continue;
				return handle;
			}
			return null;
		}
       ```
7. Build and Export into jar
   ```
    remove -> path\to\project_name\src\adf-richclient-automation-11-src.zip
    Project Explorer -> Export -> Java -> JAR file -> Next
	    select src folder only
	    check Export generated classes and resources
	    uncheck .classpath, .project
		    -> Finish -> Ok in warning dialog  
   ```
8. Optional fix error in classes from oracle.adf.view.rich.automation.test.* packages.
   - path/to/project_name/src/oracle/adf/view/rich/automation/test/selenium/WebDriverManager.java
     - [] 87 Type mismatch: cannot convert from element type Object to String ->
       ```java
		fix 85 Set<String> windowHandles = webDriver.getWindowHandles();
		=
		try {
			Set<String> windowHandles = webDriver.getWindowHandles();
			_LOG.fine("try to close all windows... ");
			for (String handle : windowHandles) {	
       ```
   - path/to/project_name/src/oracle/adf/view/rich/automation/test/selenium/RichWebDriverTest.java
     - [] 953 Syntax error on token "finally", delete this token ->
       ```java        
       fix -> delete  956,952,949, 941
		=
		protected void refresh() {
			_LOG.fine("Executing refresh()");
			this.getWebDriver().navigate().refresh();
			try {
				Alert alert = this.getWebDriver().switchTo().alert();
				if (alert != null) {
					alert.accept();
				};
			}
			catch (WebDriverException alert) {}
			finally {
				this.waitForPage();
			}
		}
       ```
     - [] 1026 Unreachable catch block for Exception. It is already handled by the catch block for Throwable ->
       ```
		fix -> replace whole method by variant of Jad Decompiler-> 
			-> Windows -> Preferences -> Java -> Decompiler -> Default Class Decompiler: Jad -> Applay and Close
			-> fix 1020, 1028 Duplicate local variable cachingEnabled ->
				fix-> delete
				    -> 1019 String msg;
					-> 1018 boolean cachingEnabled;
		=
		protected void onShutdownBrowser() {
			_LOG.finest("Shutting down browser");
			try {
				_logSeleniumBrowserLogAndResetLoggerLevel();
			} catch (Exception e) {
				boolean cachingEnabled;
				String msg;
					_LOG.warning("The page did not generate any logs.");
			} finally {
				boolean cachingEnabled = isBrowserCachingEnabled();
				try {
					if (cachingEnabled) {
						getWebDriverManager().releaseInstance();
					} else {
						getWebDriverManager().destroyInstance();
					}
				} catch (Throwable t) {
					String msg = cachingEnabled
							? "Failed to release the browser. Error message: %s"
							: "Failed to shutdown the browser. Error message: %s";
					_LOG.severe(String.format(msg, new Object[]{t.getMessage()}));
				}
			}
		}
       ```		
     - [] 1047 Type mismatch: cannot convert from element type Object to WebElement ->
       ```
		fix 1046 List<WebElement> allOptions = element.findElements(By.xpath((String) builder.toString()));
		=
		List<WebElement> allOptions = element.findElements(By.xpath((String) builder.toString()));
		for (WebElement option : allOptions) {
       ```			
   - path/to/project_name/src/oracle/adf/view/rich/automation/test/UrlFactories.java
     - [] 34 Type mismatch: cannot convert from UrlFactory to UrlFactories.UrlFactoryImpl ->
       ```
		fix Add cast to 'UrlFactoryImpl'
		=
		factory = (UrlFactoryImpl) urlFactoryIterator.next();
       ``` 
     - [] 52 Type mismatch: cannot convert from UrlFactory to UrlFactories.UrlFactoryImpl
       ```
		fix Add cast to 'UrlFactoryImpl'
		=
		UrlFactoryImpl urlFactoryImpl = (UrlFactoryImpl) (_INSTANCE = factory != null ? factory : new UrlFactoryImpl());
       ```
