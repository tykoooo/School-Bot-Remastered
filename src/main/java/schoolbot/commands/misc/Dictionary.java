package schoolbot.commands.misc;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import schoolbot.natives.objects.command.Command;
import schoolbot.natives.objects.command.CommandEvent;
import schoolbot.natives.util.Checks;
import schoolbot.natives.util.Embed;

import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class Dictionary extends Command
{
      public Dictionary()
      {
            super("", "", 1);
            addCalls("dictionary", "dict", "define");

      }


      @Override
      public void run(@NotNull CommandEvent event, @NotNull List<String> args)
      {
            String dictURL = "https://api.dictionaryapi.dev/api/v2/entries/en_US/";
            String word = args.get(0);

            if (Checks.isNumber(word))
            {
                  Embed.error(event, "Please input a word not numbers :)");
                  return;
            }

            Document document = null;

            try
            {
                  document = Jsoup.connect(dictURL + word)
                          .ignoreContentType(true)
                          .get();
            }
            catch (Exception e)
            {
                  Embed.error(event, "Can not connect to the API or this is not a valid english word");
                  return;
            }


            event.sendMessage(parseJson(args, document));

      }


      private MessageEmbed parseJson(List<String> args, Document document)
      {

            String parseAbleJson =
                    Jsoup.parse(document.outerHtml())
                            .body()
                            .text();


            JSONArray jsonArray = new JSONArray(parseAbleJson);

            String name = args.get(0);

            JSONObject jsonObject = jsonArray.getJSONObject(0);
            JSONArray phoneticsArray = (JSONArray) jsonObject.get("phonetics");
            JSONObject phonetics = phoneticsArray.getJSONObject(0);
            String pronounce = phonetics.getString("text");
            String audioPronounce = phonetics.getString("audio");


            JSONArray meaningsArray = (JSONArray) jsonObject.get("meanings");


            JSONObject meanings = meaningsArray.getJSONObject(0);

            String partOfSpeech = meanings.getString("partOfSpeech");

            JSONArray definitionArray = meanings.getJSONArray("definitions");
            JSONObject _definition = definitionArray.getJSONObject(0);
            String definition = _definition.getString("definition");

            return new EmbedBuilder()
                    .setTitle(name, audioPronounce)
                    .setDescription(pronounce)
                    .addField("Part of Speech", partOfSpeech, false)
                    .addField("Definition", definition, false)
                    .setColor(Color.ORANGE)
                    .setFooter("Definition generated at " + LocalDate.now())
                    .build();
      }
}