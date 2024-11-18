In order to run the application you need two environment variables:

    SLACK_BOT_TOKEN
    SLACK_SIGNING_SECRET

This application creates a bug submission form that consists of several fields and prints them out as a log message with output 


    2024-11-18T18:13:41.365+04:00  INFO 15951 --- [nio-8080-exec-4] slack_forms.DemoApplication              : 

    Bug Request Form:
    Bug Found on: 2024-11-17T00:00:00Z
    Bug Status: In Progress
    Bug Priority: Medium
    Description: description
    Reproduction Steps: reproduction steps


/src/main/java/slack_forms/DemoApplication.java contains an App bean that defines all the form-management logic. It also has a buildView() method that returns the View of the form.

This application supports an Interactive Global Shortcut with callback id "bug-form" that allows you to create a Bug Submission Form and send its information to the server.

So, you need to create an Interactive Global Shortcut with Location: Global, CallbackId: "bug-form" and Request URL: "https://{your app's public URL domain}/slack/events" 

/src/main/java/slack_forms/SlackAppController.java
Responsible for handling requests from the Slack Server. It opens a "/slack/events" callback url and sends requests to App bean defined in DemoApplication class in order to handle them.

The callback url should be accessible from the internet in order for Slack to send requests to it. I used [ngrok](https://dashboard.ngrok.com/get-started/setup/linux) to implement it.


