package schoolbot.commands.school;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import schoolbot.Constants;
import schoolbot.Schoolbot;
import schoolbot.objects.command.Command;
import schoolbot.objects.command.CommandEvent;
import schoolbot.objects.command.CommandFlag;
import schoolbot.objects.school.School;
import schoolbot.util.Checks;
import schoolbot.util.EmbedUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SchoolAdd extends Command
{
      private static final String BACKUP_API_URL = "http://universities.hipolabs.com/search?name=";

      public SchoolAdd(Command parent)
      {
            super(parent, "Adds a school to the server", "[school name]", 1);
            addUsageExample("school add \"University of Pittsburgh\"");
            addPermissions(Permission.ADMINISTRATOR);
            addSelfPermissions(Permission.MANAGE_ROLES, Permission.MANAGE_CHANNEL);
            addFlags(CommandFlag.DATABASE);

      }

      private static boolean addToDbAndCreateRole(MessageEmbed embed, Schoolbot schoolbot, CommandEvent event)
      {
            String schoolName = embed.getTitle();

            if (event.schoolExist(schoolName)) return false;

            String url = embed.getFields().get(0).getValue();
            String suffix = embed.getFields().get(1).getValue();
            Guild guild = event.getGuild();


            guild.createRole()
                    .setName(schoolName)
                    .setColor(new Random().nextInt(0xFFFFFF))
                    .queue(role ->
                            event.addSchool(new School(
                                    schoolName,
                                    suffix,
                                    role.getIdLong(),
                                    guild.getIdLong(),
                                    url.contains(",") ? url.split(",")[0] : url
                            )));
            return true;
      }

      @Override
      public void run(@NotNull CommandEvent event, @NotNull List<String> args)
      {
            String firstArg = args.get(0);


            var channel = event.getChannel();
            var user = event.getUser();
            var guild = event.getGuild();
            var schoolbot = event.getSchoolbot();

            if (Checks.isNumber(firstArg))
            {
                  EmbedUtils.error(event, "Your school name cannot contain numbers!");
                  return;
            }
            Document doc;
            try
            {
                  doc = Jsoup.connect(BACKUP_API_URL + firstArg)
                          .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                          .referrer("https://www.google.com")
                          .ignoreContentType(true)
                          .get();
            }
            catch (Exception e)
            {
                  channel.sendMessage("Cannot connect to API... exiting..").queue();
                  return;
            }


            String parseAbleJson =
                    Jsoup.parse(doc.outerHtml())
                            .body()
                            .text();


            JSONArray json = new JSONArray(parseAbleJson);

            if (json.length() > 40)
            {
                  EmbedUtils.error(event, "Your search for ```" + firstArg + "``` contained ***" + json.length() + "*** elements.. Please narrow down your search");
                  return;
            }

            Map<Integer, MessageEmbed> schools = evalSchools(json);

            if (schools.isEmpty())
            {
                  EmbedUtils.error(event, "No schools matching " + firstArg + " found");
            }
            else if (schools.size() == 1)
            {
                  MessageEmbed embed = schools.get(1);
                  if (addToDbAndCreateRole(embed, schoolbot, event))
                  {
                        event.sendMessage("School Created");
                        event.sendMessage(embed);
                  }
                  else
                  {
                        EmbedUtils.error(event, " ** %s ** already exist", firstArg);
                  }
            }
            else
            {
                  event.sendMessage("More than one school that has been found with your search.. Type the number that matches your desired result");
                  event.sendAsPaginator(List.copyOf(schools.values()));

                  event.getJDA().addEventListener(new SchoolStateMachine(event, schools));
            }
      }


      private Map<Integer, MessageEmbed> evalSchools(JSONArray jsonArray)
      {
            HashMap<Integer, MessageEmbed> em = new HashMap<>();

            for (int i = 0; i < jsonArray.length(); i++)
            {

                  JSONObject elementsWithinArray = jsonArray.getJSONObject(i);

                  String schoolName = elementsWithinArray.getString("name");
                  String country = elementsWithinArray.getString("country");

                  JSONArray webPagesArray = elementsWithinArray.getJSONArray("web_pages");
                  StringBuilder webPagesUrls = new StringBuilder();
                  for (int j = 0; j < webPagesArray.length(); j++)
                  {
                        webPagesUrls.append(webPagesArray.getString(j)).append("\n");
                  }
                  JSONArray schoolDomainsElements = elementsWithinArray.getJSONArray("domains");
                  StringBuilder schoolDomains = new StringBuilder();
                  for (int k = 0; k < schoolDomainsElements.length(); k++)
                  {

                        String schoolDomain = schoolDomainsElements.getString(k);
                        if (schoolDomain.contains("edu"))
                        {
                              String[] splitDomain = schoolDomain.split("\\.");
                              for (int s = 0; s < splitDomain.length; s++)
                              {
                                    if (splitDomain[s].equals("edu"))
                                    {
                                          schoolDomains.append("@").append(splitDomain[s - 1]).append(".edu").append("\n");
                                          break;
                                    }

                              }
                        }
                  }


                  em.put(i + 1, new EmbedBuilder()
                          .setTitle(schoolName)
                          .addField("Web pages", webPagesUrls.toString(), false)
                          .addField("Email Suffix", schoolDomains.toString(), false)
                          .addField("Country", country, false)
                          .addField("School #", String.valueOf(i + 1), false)
                          .setColor(Constants.DEFAULT_EMBED_COLOR)
                          .setFooter("Big thanks to https://github.com/Hipo/university-domains-list-api")
                          .setTimestamp(Instant.now())
                          .build()
                  );
            }

            return em;
      }

      public static class SchoolStateMachine extends ListenerAdapter
      {
            private final long channelID, authorID;
            private final Map<Integer, MessageEmbed> schools;
            private final Schoolbot schoolbot;
            private final CommandEvent cmdEvent;

            private final int state = 0;


            public SchoolStateMachine(@NotNull CommandEvent event, @NotNull Map<Integer, MessageEmbed> schools)
            {
                  this.cmdEvent = event;
                  this.authorID = event.getUser().getIdLong();
                  this.channelID = event.getChannel().getIdLong();
                  this.schools = schools;
                  this.schoolbot = event.getSchoolbot();
            }

            @Override
            public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event)
            {
                  if (event.getAuthor().getIdLong() != authorID) return;
                  if (event.getChannel().getIdLong() != channelID) return;
                  if (!event.getMessage().getContentRaw().chars().allMatch(Character::isDigit)) return;
                  int pageNum = Integer.parseInt(event.getMessage().getContentRaw());
                  if (!Checks.between(pageNum, schools.size())) return;

                  int schoolChoice = Integer.parseInt(event.getMessage().getContentRaw());
                  MessageEmbed embed = schools.get(schoolChoice);

                  var guild = event.getGuild();


                  if (event.getMessage().getContentRaw().equalsIgnoreCase("stop"))
                  {
                        event.getChannel().sendMessage("Okay aborting..").queue();
                        event.getJDA().removeEventListener(this);
                        return;
                  }


                  if (addToDbAndCreateRole(embed, schoolbot, cmdEvent))
                  {
                        event.getChannel().sendMessage("School Created").queue();
                        event.getChannel().sendMessageEmbeds(embed).queue();
                  }
                  else
                  {
                        EmbedUtils.error(event, "School already exist");
                  }

                  event.getJDA().removeEventListener(this);
            }
      } // end
}

