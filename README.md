In order to run the application you need two environment variables:

    SLACK_BOT_TOKEN
    SLACK_SIGNING_SECRET

This application creates a bug submission form that consists of several fields and prints them out as a log message with output 


    Ticket Progress Form
    Fix ui on the front page : In Progress
    Develop implementation of the scheduler : In Progress
    Fix deployment issues : Blocked 
    What holds up? : The tasks are difficult


/src/main/java/slack_forms/DemoApplication.java contains an App bean that defines all the form-management logic. It has a scheduleForms methods that will will be called at 18:00 (06:00 pm) to send messages to all users in the workspace that have confirmed their email address. After the user submits the form, the results of the form will be logged and the message in chat between the user and the bot will be edited confirming the submission and removing the form itself.

/src/main/java/slack_forms/SlackAppController.java
Responsible for handling requests from the Slack Server. It opens a "/slack/events" callback url and sends requests to App bean defined in DemoApplication class in order to handle them.

In order to received the callback a request url should be set for the bot on slack with form: "https://{your app's public URL domain}/slack/events" 

The callback url should be accessible from the internet in order for Slack to send requests to it. I used [ngrok](https://dashboard.ngrok.com/get-started/setup/linux) to implement it.




https://github.com/user-attachments/assets/b8aefe6e-2b2d-4c58-8044-c4dc61d28ff5


