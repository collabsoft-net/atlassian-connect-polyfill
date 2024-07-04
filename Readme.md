# Atlassian Connect Polyfill

The Atlassian Connect Polyfill companion app aims to provide support for Connect apps on the Atlassian Server/Data Center platform.

## What it does

The purpose of this app is to help Atlassian app developers to remove any friction when developing apps for both
Server/Data Center as well as Cloud (using Atlassian Connect).

One of the key architectural features of Atlassian Connect is that apps run within a sandboxed iframe and communicate
with the host application using the Atlassian Javascript API (AP).

Running the app in an iframe also brings a lot of benefits to the Atlassian Server/Data Center platform. It avoids
common issues with 3rd party dependency collision, where the host application or other app developers introduce
popular libraries such as React or jQuery that are available on the global variable scope. This can override 
other versions which may introduce issues. When the app runs in an iframe, there is no shared code between the app
and the host application.

In addition, with the Atlassian Connect Polyfill, front-end developers can use the same AP methods in Cloud as well
as Server/Data Center environment, removing the requirement to detect platform and write platform specific code.

This should make developing apps for both Cloud and Server/Data Center easier, more maintainable and hopefully
reduce errors.

This package is the "back-end" part and relies heavily on the "front-end" part that is in the `@collabsoft-net/connect` package. See https://github.com/collabsoft-net/iapetus/tree/main/packages/connect

## How it works

When installed, the app registers a servlet that listens to `/plugins/servlet/atlassian-connect/{appKey}/{moduleKey}`.
This is similar to the Atlassian servlet that runs on Cloud (`/plugins/servlet/ac`).

It will use the provided `{appKey}` and `{moduleKey}` to retrieve the module parameters from the `atlassian-plugin.xml`.
These parameters are used to generate an iframe that will load the app front-end code.

In addition, it also provides a `all.js` similar to that of Atlassian Connect, which is an Atlassian Javascript API (AP) polyfill. This will interact with the host application in the same way that you can interact with the host product in Cloud.

## Getting started

In order to use this companion app, there are a lot of different steps that you will need to take:

1. Switch to publishing an OBR instead of a single JAR
2. Generate a WRM context with the client-side "host" application
3. Create a servlet that will serve your HTML
4. Adjust your `atlassian-plugin.xml` to start using the polyfill
5. Add platform specific code (optional)

In addition to this guide, there are also example apps in this repository (in the `examples` directory) for each of the supported Atlassian host products.

### Switch to publishing an OBR instead of a single JAR

If you're not familiar with OBR, you can [read more about it here](https://developer.atlassian.com/server/framework/atlassian-sdk/bundling-extra-dependencies-in-an-obr/).

Here are the specific steps to add the Atlassian Connect polyfill as a dependency to your app:

#### Add the Collabsoft Maven repository to your POM

The Atlassian Connect Polyfill is not published on the Maven Central repository.
Instead it is pushed to our own public Maven repository. In order to use it,
you will need to add our repository to your `pom.xml`.

```
<repositories>
  ...
  <repository>
    <id>CollabSoft Maven Repository</id>
    <name>CollabSoft Maven Repository</name>
    <releases>
        <enabled>true</enabled>
    </releases>
    <snapshots>
        <enabled>false</enabled>
    </snapshots>
    <url>http://repository.collabsoft.net/releases</url>
  </repository>
  ...
</repositories>
```

#### Add the package as a provided dependency

```
<dependencies>
  ...
  <dependency>
    <groupId>fyi.iapetus.plugins</groupId>
    <artifactId>acpolyfill</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
  </dependency>
  ...
</dependencies>
```

#### Add Atlassian Maven Plugin Suite (AMPS) specific instructions

In order for the plugin to be included in the OBR, we need to
add the following to the `jira-maven-plugin` or `confluence-maven-plugin`:

```
<plugins>
  ...
  <plugin>
    <groupId>com.atlassian.maven.plugins</groupId>
    ...
    <configuration>
      ...
      <pluginDependencies>
          <pluginDependency>
              <groupId>fyi.iapetus.plugins</groupId>
              <artifactId>acpolyfill</artifactId>
          </pluginDependency>
      </pluginDependencies>
      ...
      <instructions>
        ...
        <Require-Bundle>
          ...
          fyi.iapetus.plugins.acpolyfill;resolution:="optional",
          ...
        </Require-Bundle>
        <Import-Package>
          ...
          fyi.iapetus.plugins.acpolyfill;resolution:="optional",
          ...
        </Import-Package>
        ...
      </instructions>
      ...
    </configuration>
    ...
  </plugin>
  ...
</plugins>
```

### Generate a WRM context with the client-side "host" application

Whenever the Atlassian Connect Polyfill generates the iframe, it also needs to load a client-side "host" application.

#### Add the @collabsoft-net/connect package

The first step is to add the `@collabsoft-net/connect` package to your `package.json`.
You can read how to install the package in [the package readme](https://github.com/collabsoft-net/iapetus/blob/main/packages/connect/README.md#im-feeling-lucky)

#### Add an entrypoint specifically for Atlassian Connect

```
try {
  // Uncomment this line if you want a "general pages" experience (it removes the footer)
  // document.documentElement.classList.add('page-type-connect');

  // We need to make sure we only initialize AC polyfill once
  // After initialisation, the AC object is placed on the window object
  // If it's already there, don't bother, otherwise go for it!
  // Make sure to use a app-specific name, to avoid collision with other apps
  if (!window.MyAppHost) {
    window.MyAppHost = new Host({
      // appKey:
      // product: 'jira'|'confluence
      // baseUrl: The URL to your servlet, see below
      // xdm_e: the base URL of the jira instance (incl. contextpath)
      // contextPath: the contextpath of the host application
      // license: the state of the license. This is also available as meta tag (name="${appKey}-lic")
      // navigator: a list of paths for the `addonModule` option in AP.navigator.go()
      // dialogs: a list of paths & options for dialogs based on dialog key
      // editors: a list of paths & options for confluence macro editors
      // verbose: true|false (for debugging)
    });
    window.MyAppHost.init();
  }
} catch (err) {
  console.error('[AC] Failed to initialize Atlassian Connect polyfill', err);
}
```

This code will have to be added to a `<web-resource />` [module](https://developer.atlassian.com/server/confluence/web-resource-module/) 
which can be included in the page using a specific `<context />`. An example would be:


```
<atlassian-plugin ...>
  ...
  <web-resource key="atlassian-connect-polyfill" ...>
    <context>atlassian-connect-polyfill</context>
    ...
  </web-resource>
  ...
</atlassian-plugin>
```

In addition, you will need to set the `ac.context` parameter in the `plugin-info` section of the `atlassian-plugin.xml`:

```
<atlassian-plugin ...>
  ...
  <plugin-info>
    ...
    <!-- Atlassian Connect -->
    <param name="ac.context">atlassian-connect-polyfill</param>
    ...
  </plugin-info>
  ...
</atlassian-plugin>
```

### Create a servlet that will serve your HTML

In an Atlassian Connect descriptor for Cloud apps, there is a `baseUrl` property that needs to be set.
For Atlassian Connect Polyfill, this is equally required. But instead of an external URL, it needs to be
served from the Atlassian host application.

In this example, we will be adding a Servlet which will serve HTML and will include a web resource through a context.
This is basically the setup for a Single Page Application type of architecture, in which the logic is all done client-side.

You can of course change the Servlet and use server-side rendering, for instance using velocity templates. That's all up to you.

In the `atlassian-plugin.xml` add:

```
<atlassian-plugin ...>
  ...
  <plugin-info>
    ...
    <!-- Atlassian Connect -->
    <param name="ac.baseurl">/plugins/servlet/path/to/servlet/</param>
    ...
  </plugin-info>
  ...
</atlassian-plugin>
```

Next we need to register the URL pattern for our servlet in the same `atlassian-plugin.xml`:

```
<atlassian-plugin ...>
  ...
  <servlet class="path.to.ACServlet" ...>
      <url-pattern>/path/to/servlet/*</url-pattern>
  </servlet>
  ...
</atlassian-plugin>
```

And finally we add `<root>/src/main/path/to/ACServlet.java` with the following code:

```
public class ACServlet extends HttpServlet {

  private final WebResourceAssemblerFactory webResourceAssemblerFactory;

  ACServlet(@ComponentImport WebResourceAssemblerFactory webResourceAssemblerFactory) {
      this.webResourceAssemblerFactory = webResourceAssemblerFactory;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      resp.setContentType("text/html; charset=utf-8");

      try (PrintWriter writer = resp.getWriter()) {
          String response = renderIframeContent(req);
          writer.write(response);
      } catch (Exception exp) {
          resp.sendError(500, exp.toString());
      }
  }

  public String renderIframeContent(HttpServletRequest req)  throws IOException {
      HtmlFragment resources = this.getResources();

      StringBuilder sb = new StringBuilder();
      sb.append("<!doctype html>");
      sb.append("<html lang=\"en\">");
      sb.append("<head>");
      sb.append("<meta charset=\"UTF-8\">");
      sb.append(new HtmlFragment(resources.toString()));
      sb.append("</head>");
      sb.append("<body>");
      sb.append("</body>");
      sb.append("</html>");
      return sb.toString();
  }

  private HtmlFragment getResources() throws IOException {
    Writer writer = new StringWriter();
    WebResourceAssembler assembler = this.webResourceAssemblerFactory.create().includeSyncbatchResources(false).includeSuperbatchResources(false).autoIncludeFrontendRuntime(false).build();
    assembler.resources().requireContext(ResourcePhase.DEFER, "my-front-end-app-context");
    WebResourceSet resources = assembler.assembled().drainIncludedResources();
    resources.writeHtmlTags(writer, UrlMode.AUTO);

    writer.flush();
    return new HtmlFragment(writer.toString());
  }
}
```

### Adjust your `atlassian-plugin.xml` to start using the polyfill

The next step is to start directing traffic to the Atlassian Connect Polyfill by adjusting your
`atlassian-plugin.xml` and using the polyfill in modules.

The easiest implementation it the web-item module ([jira](https://developer.atlassian.com/server/jira/platform/web-item/)|[Confluence](https://developer.atlassian.com/server/confluence/web-item-plugin-module/))

Take for instance the following web item module:

```
<web-item key="my-web-item" ...>
  <label>My Web Item</label>
  <link>/plugins/servlet/atlassian-connect/${pluginKey}/my-web-item</link>
  <param name="decorator" value="atl.admin" />
  <param name="url" value="/my-web-item" />
</web-item>
```

This will add a link to the specified location which will redirect the user to the Atlassian Connect Polyfill.
The Atlassian Connect Polyfill will apply the decorator (incl. a WebSudo check for admin pages).

The `url` parameter is appended to the `ac.baseurl` which we provided earlier in the `plugin-info` section.
This allows us to differentiate between modules when we choose for server-side rendering.

Please refer to the examples to see more implementations of modules using Atlassian Connect Polyfill.

### Add platform specific code (optional)

The Atlassian Connect Polyfill also comes with platform specific helpers. These are libraries that you can
use in your application just like any other 3rd party dependency.

#### Atlassian Jira

For Atlassian Jira, you can add the following dependency:

```
<dependency>
    <groupId>fyi.iapetus.plugins</groupId>
    <artifactId>acpolyfill</artifactId>
    <version>1.0.0</version>
    <classifier>jira</classifier>
</dependency>
```

This exposes the `AbstractACWebPanel` and the `AbstractACContextProvider` classes.
As can be derived from the name, the `AbstractWebPanel` provides support for 
the Web Panel module. Together with the `AbstractACContextProvider` this
makes sure that the iframe is also generated properly in those locations.

Example:

```
<web-panel key="my-issue-panel" location="atl.jira.view.issue.left.context" class="my.app.ACWebPanel">
    <label key="Issue Panel" />
    <context-provider class="my.app.ACContextProvider">
        <param name="ac.plugin.key" value="${pluginKey}" />
        <param name="ac.moduleKey" value="my-issue-panel" />
        <param name="url" value="/my-issue-panel" />
    </context-provider>
</web-panel>
```

The `my.app.ACWebPanel` can be a very simple implementation:

```
public class ACWebPanel extends AbstractACWebPanel {

    ACWebPanel(@ComponentImport ApplicationProperties applicationProperties, @ComponentImport PageBuilderService pageBuilderService, @ComponentImport UserManager userManger) {
        super(applicationProperties, pageBuilderService, userManger);
    }
}
```

Same goes for the `my.app.ACContextProvider`:

```
public class ACContextProvider extends AbstractACContextProvider {

    ACContextProvider(@ComponentImport PluginAccessor pluginAccessor, @ComponentImport ApplicationProperties applicationProperties, @ComponentImport PluginLicenseManager pluginLicenseManager) {
        super(pluginAccessor, applicationProperties, pluginLicenseManager);
    }

}
```

This will render the issue panel correctly with an iframe and the client-side host application.

## Disclaimer

It is important to note that this is specifically created to help our (Collabsoft) own app development goals. 

We do not provide any warranty as to the stability of features. Collabsoft retains the sole right to make architectural choices without any regard to how this may impact other projects.

We also provide minimal to no documentation. If you want to know what a certain class and/or method is doing, you'd best look at the source code.

It is what it is.

## Contributions

Obviously we're open to any contribution in the form of issue reports and/or pull requests (PR), but we do not give any guarantees as to whether these will be fixed and/or merged.

It is recommended to file an issue before starting work on a PR to make sure that we agree to the proposed fix before you start doing any work.

## License & other legal stuff

Licensed under the Apache License, Version 2.0 (the "License"); You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

### Important usage notice

The code in this project rely heavily on other 3rd party Open Source projects, installed through Maven. These projects all have their own licenses. Although commercial use of Collabsoft packages is permitted under the Apache 2.0 license, this right is limited to the "original content" created as part of this project. Please make sure you check the licenses of all 3rd party components. Collabsoft cannot be held responsible for non-compliance with 3rd party licenses when using the packages or source code. The use of 3rd party projects is listed in the dependency section of the package.json or inline in the code (when applicable).
