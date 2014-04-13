dumbster-http-mod
=================

A simple extension of the Dumbster SMTP testing library that adds a basic HTML front-end (and built-in HTTP server to host it).  This allows e-mail generated during a web/integration test session to be checked and verified using the web/integration test client itself.


### Building

Just run 'ant' to build, test, and assemble the JAR file.  

### Usage

To use this utility, perform the following steps:

1.  Include the project on your testing classpath either by referencing the assembled JAR file or by including the source files directly.
2.  `import com.dumbster.smtp.extensions.SmtpServerWithHttp;`.
3.  Invoke something like `SmtpServerWithHttp.start(25, 81);` at the start of your testing session.
4.  Write your integration test as you normally would.
5.  Point your webtest client (i.e. WebDriver, Windmill, WebTest, HtmlUnit, etc.) at `http://localhost:81` when you need to verify your e-mails.

### Limitations

Dumbster is very old code, and this extension is not much younger.  That said, both generally work as intended.  

The HTML interface is very basic, and the server driving it is extremely barebones.  It only supports HTTP 1.0, and only the bare minumum feature-set necessary to meet its needs.   

Despite these imperfections, in practical terms there are no known/serious functional limitations when this code is used for its intended purpose.  

### FAQ

**_Why does this project build with Ant?_**<br />
Because that's what Dumbster builds with (or built with, when this extension was written), and it didn't seem necessary or beneficial to change it.

**_Didn't all the cool kids stop using Ant like, years ago?_**<br />
Probably.  Dumbster hasn't seen any material updates since 2005.  The code added in this project was implemented around 2008.

**_Why should I use this library?_**<br />
Use this library if you need to verify email contents as part of a web/integration test.  For instance to test a registration flow by receiving the welcome e-mail, verifying that it contains the expected text and/or graphics, and clicking the activation link to confirm that it really does activate the newly registered account.

**_Why should I NOT use this library?_**<br />
Don't use this library if you don't test your code and think TDD is for losers.

**_What are your license terms?_**<br />
Dumbster is distributed under the terms of the Apache license.  So let's just stick with that.
