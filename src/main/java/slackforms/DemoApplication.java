package slackforms;

import static com.slack.api.model.block.Blocks.asBlocks;
import static com.slack.api.model.block.Blocks.input;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.asOptions;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.option;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.datetimePicker;
import static com.slack.api.model.block.element.BlockElements.plainTextInput;
import static com.slack.api.model.block.element.BlockElements.staticSelect;
import static com.slack.api.model.view.Views.view;
import static com.slack.api.model.view.Views.viewClose;
import static com.slack.api.model.view.Views.viewSubmit;
import static com.slack.api.model.view.Views.viewTitle;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.model.view.View;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ServletComponentScan
public class DemoApplication {
  private static Logger LOG = LogManager.getLogger();

  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

  @Bean
  App app(
      @Value("${slack-bot-token}") String slackBotToken,
      @Value("${slack-signing-secret}") String slackSigningSecret) {

    App app =
        new App(
            AppConfig.builder()
                .singleTeamBotToken(slackBotToken)
                .signingSecret(slackSigningSecret)
                .build());

    app.globalShortcut(
        "bug-form",
        (req, ctx) -> {
          ViewsOpenResponse viewsOpenRes =
              ctx.client().viewsOpen(r -> r.triggerId(ctx.getTriggerId()).view(buildView()));

          if (viewsOpenRes.isOk()) return ctx.ack();
          else return Response.builder().statusCode(500).body(viewsOpenRes.getError()).build();
        });

    app.viewClosed(
        "bug-form",
        (req, ctx) -> {
          return ctx.ack();
        });

    app.blockAction(
        "bug-status-selection-action",
        (req, ctx) -> {
          return ctx.ack();
        });

    app.blockAction(
        "bug-priority-selection-action",
        (req, ctx) -> {
          return ctx.ack();
        });

    app.blockAction(
        "bug-found-datetime-action",
        (req, ctx) -> {
          return ctx.ack();
        });

    app.viewSubmission(
        "bug-form",
        (req, ctx) -> {
          var payloadValues = req.getPayload().getView().getState().getValues();

          LOG.info(
              """
              {}
              Bug Request Form:
              Bug Found on: {}
              Bug Status: {}
              Bug Priority: {}
              Description: {}
              Reproduction Steps: {}
              """,
              System.lineSeparator(),
              Instant.ofEpochSecond(
                  Long.valueOf(
                      payloadValues
                          .get("bug-found-datetime-block")
                          .get("bug-found-datetime-action")
                          .getSelectedDateTime())),
              payloadValues
                  .get("bug-status-block")
                  .get("bug-status-selection-action")
                  .getSelectedOption()
                  .getText()
                  .getText(),
              payloadValues
                  .get("bug-priority-block")
                  .get("bug-priority-selection-action")
                  .getSelectedOption()
                  .getText()
                  .getText(),
              payloadValues.get("bug-desc-block").get("bug-desc-action").getValue(),
              payloadValues.get("bug-repr-steps-block").get("bug-repr-steps-action").getValue());

          return ctx.ack();
        });

    return app;
  }

  private static View buildView() {
    return view(
        view ->
            view.callbackId("bug-form")
                .type("modal")
                .notifyOnClose(true)
                .title(
                    viewTitle(
                        title -> title.type("plain_text").text("Bug Submission Form").emoji(true)))
                .submit(viewSubmit(submit -> submit.type("plain_text").text("Submit").emoji(true)))
                .close(viewClose(close -> close.type("plain_text").text("Cancel").emoji(true)))
                .blocks(
                    asBlocks(
                        input(
                            input ->
                                input
                                    .blockId("bug-found-datetime-block")
                                    .label(plainText(pt -> pt.text("Buf Found On")))
                                    .element(
                                        datetimePicker(
                                            datePicker ->
                                                datePicker.actionId("bug-found-datetime-action")))),
                        section(
                            section ->
                                section
                                    .blockId("bug-status-block")
                                    .text(markdownText("Status of the Bug Fixing"))
                                    .accessory(
                                        staticSelect(
                                            staticSelect ->
                                                staticSelect
                                                    .actionId("bug-status-selection-action")
                                                    .placeholder(plainText("Status"))
                                                    .initialOption(
                                                        option(plainText("To Fix"), "to-fix"))
                                                    .options(
                                                        asOptions(
                                                            option(plainText("To Fix"), "to-fix"),
                                                            option(
                                                                plainText("In Progress"),
                                                                "in-progress"),
                                                            option(
                                                                plainText("Completed"),
                                                                "completed")))))),
                        section(
                            section ->
                                section
                                    .blockId("bug-priority-block")
                                    .text(markdownText("Bug Priority"))
                                    .accessory(
                                        staticSelect(
                                            staticSelect ->
                                                staticSelect
                                                    .actionId("bug-priority-selection-action")
                                                    .placeholder(plainText("Priority"))
                                                    .initialOption(option(plainText("Low"), "low"))
                                                    .options(
                                                        asOptions(
                                                            option(plainText("Low"), "low"),
                                                            option(plainText("Medium"), "medium"),
                                                            option(plainText("High"), "high")))))),
                        input(
                            input ->
                                input
                                    .blockId("bug-desc-block")
                                    .label(plainText(pt -> pt.text("Describe the Bug").emoji(true)))
                                    .element(
                                        plainTextInput(
                                            pti ->
                                                pti.actionId("bug-desc-action").multiline(true)))),
                        input(
                            input ->
                                input
                                    .blockId("bug-repr-steps-block")
                                    .label(plainText(pt -> pt.text("Bug Reproduction Steps")))
                                    .element(
                                        plainTextInput(
                                            pti ->
                                                pti.actionId("bug-repr-steps-action")
                                                    .multiline(true)))))));
  }
}
