package schoolbot.commands.school;

import org.jetbrains.annotations.NotNull;
import schoolbot.objects.command.Command;
import schoolbot.objects.command.CommandEvent;
import schoolbot.objects.command.CommandFlag;
import schoolbot.objects.school.School;
import schoolbot.util.Embed;

import java.util.List;

public class ListSchools extends Command
{
      public ListSchools()
      {
            super("List schools all schools in database", "[none]", 0);
            addCalls("schools", "school-list");
            addFlags(CommandFlag.DATABASE);
      }


      @Override
      public void run(@NotNull CommandEvent event, @NotNull List<String> args)
      {
            List<School> schools = event.getGuildSchools();


            if (schools.isEmpty())
            {
                  Embed.error(event, "No schools!");
            }
            // No need for a paginator call if theres only one school tbh..
            else if (schools.size() == 1)
            {
                  event.sendMessage(schools.get(0).getAsEmbed(event.getSchoolbot()));
            }
            else
            {
                  event.getAsPaginatorWithPageNumbers(schools);
            }
      }
}
