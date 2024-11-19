package slackforms;

import static com.slack.api.model.block.Blocks.input;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.asOptions;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.option;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.button;
import static com.slack.api.model.block.element.BlockElements.plainTextInput;
import static com.slack.api.model.block.element.BlockElements.staticSelect;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.request.builtin.BlockActionRequest;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.users.UsersListRequest;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.User;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.StaticSelectElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;

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
      @Value("${slack-signing-secret}") String slackSigningSecret)
      throws IOException, SlackApiException {

    App app =
        new App(
            AppConfig.builder()
                .singleTeamBotToken(slackBotToken)
                .signingSecret(slackSigningSecret)
                .build());

    app.blockAction(
        Pattern.compile("ticket-progress-selection-action-\\d{1,5}"),
        (req, ctx) -> {
          return ctx.ack();
        });

    app.blockAction(
        "form-submit-action",
        (req, ctx) -> {
          logForm(req);

          var channel = req.getPayload().getContainer().getChannelId();
          var ts = req.getPayload().getMessage().getTs();

          app.getClient()
              .chatUpdate(
                  chatUpdate ->
                      chatUpdate.channel(channel).ts(ts).text("Thanks for submitting the form!"));

          return ctx.ack();
        });

    scheduleForms(app);

    return app;
  }

  @Scheduled(cron = "0 18 * * *")
  private void scheduleForms(App app) throws IOException, SlackApiException {
    var client = app.getClient();

    UsersListResponse usersList = client.usersList(UsersListRequest.builder().build());
    var members = usersList.getMembers();

    for (User user : members) {
      if (!user.isEmailConfirmed()) {
        continue;
      }
      var resp =
          client.chatPostMessage(
              chat ->
                  chat.channel(user.getId())
                      .text("Ticket Progress")
                      .blocks(bildTicketBlocks(getMockTickets())));
      LOG.info(
          """
          Sent a Message
          Status IsOk: {}
          Errors: {}
          Error Response: {}
          To Channel: {}
          """,
          resp.isOk(),
          resp.getError(),
          resp.getResponseMetadata(),
          resp.getChannel());
    }
  }

  private List<String> getMockTickets() {
    return List.of(
        "Fix ui on the front page",
        "Develop implementation of the scheduler",
        "Fix deployment issues");
  }

  private static List<LayoutBlock> bildTicketBlocks(List<String> tickets) {

    var header =
        Blocks.header(
            h -> h.blockId("header-block").text(plainText(pt -> pt.text("Tickets Progress Form"))));

    var sections = new ArrayList<SectionBlock>();
    for (String ticket : tickets) {

      sections.add(
          section(
              s ->
                  s.blockId("ticket-progress-block-" + sections.size())
                      .text(markdownText(ticket))
                      .accessory(
                          staticSelect(
                              staticSelect ->
                                  staticSelect
                                      .actionId(
                                          "ticket-progress-selection-action-" + sections.size())
                                      .placeholder(plainText("Status"))
                                      .initialOption(
                                          option(plainText("In Progress"), "in-progress"))
                                      .options(
                                          asOptions(
                                              option(plainText("Blocked"), "blocked"),
                                              option(plainText("In Progress"), "in-progress"),
                                              option(plainText("Completed"), "completed")))))));
    }

    var input =
        input(
            i ->
                i.blockId("ticket-issues-block")
                    .label(
                        plainText(
                            pt -> pt.text("Is there anything preventing you from moving forward?")))
                    .element(
                        plainTextInput(
                            pti -> pti.actionId("ticket-issues-action").multiline(true))));
    var submit =
        Blocks.actions(
            List.of(
                button(
                    button ->
                        button
                            .actionId("form-submit-action")
                            .text(plainText(pt -> pt.text("Submit"))))));

    List<LayoutBlock> lay = new ArrayList<>();
    lay.add(header);
    lay.addAll(sections);
    lay.add(input);
    lay.add(submit);

    return lay;
  }

  private void logForm(BlockActionRequest req) {
    var values = req.getPayload().getState().getValues();
    var blocks = req.getPayload().getMessage().getBlocks();

    var sb = new StringBuilder();
    sb.append(System.lineSeparator())
        .append(System.lineSeparator())
        .append("Ticket Progress Form")
        .append(System.lineSeparator());

    for (LayoutBlock layoutBlock : blocks) {
      if (layoutBlock.getType().equals("section")) {
        var sectionBlock = (SectionBlock) layoutBlock;
        var secId =
            values
                .get(sectionBlock.getBlockId())
                .get(((StaticSelectElement) sectionBlock.getAccessory()).getActionId())
                .getSelectedOption()
                .getText()
                .getText();

        sb.append(sectionBlock.getText().getText()).append(" : ");
        sb.append(secId).append(System.lineSeparator());
      }
    }

    sb.append("What holds up? : ");
    sb.append(values.get("ticket-issues-block").get("ticket-issues-action").getValue())
        .append(System.lineSeparator());
    LOG.info(sb.toString());
  }
}
