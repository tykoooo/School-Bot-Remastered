package schoolbot.commands.school;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import schoolbot.natives.objects.command.Command;
import schoolbot.natives.objects.command.CommandEvent;
import schoolbot.natives.objects.command.CommandFlag;
import schoolbot.natives.objects.school.School;
import schoolbot.natives.util.Checks;
import schoolbot.natives.util.Embed;

import java.util.List;
import java.util.stream.Collectors;

public class SchoolRemove extends Command
{
      public SchoolRemove(Command parent)
      {
            super(parent, "Removes a school given the name", "[school name]", 0);
            addPermissions(Permission.ADMINISTRATOR);
            addFlags(CommandFlag.DATABASE);
      }


      @Override
      public void run(@NotNull CommandEvent event, @NotNull List<String> args)
      {
            List<School> schools = event.getGuildSchools()
                    .stream()
                    .filter(school -> school.getClassroomList().size() == 0)
                    .filter(school -> school.getProfessorList().size() == 0)
                    .collect(Collectors.toList());


            if (schools.isEmpty())
            {
                  Embed.error(event, "There is no valid schools you can delete.. Schools can only be deleted if they have no **classes** and **professors** assigned to it");
                  return;
            }
            else if (schools.size() == 1)
            {
                  event.sendMessage(schools.get(0).getAsEmbed(event.getSchoolbot()));
                  event.sendMessage("This is the only school available to delete would you like to delete it?");
                  event.getJDA().addEventListener(new SchoolRemoveStateMachine(event, schools, 2));
                  return;
            }


            event.sendMessage("Please tell me the school you want to remove by telling me the page number!");
            event.getAsPaginatorWithPageNumbers(schools);
            event.getJDA().addEventListener(new SchoolRemoveStateMachine(event, schools, 1));
      }

      public static class SchoolRemoveStateMachine extends ListenerAdapter
      {
            private final long channelId, authorId;
            private final List<School> schools;
            private int state = 1;
            private CommandEvent commandEvent;
            private School schoolRemoving;

            public SchoolRemoveStateMachine(CommandEvent event, List<School> school, int stateToSwitch)
            {
                  this.authorId = event.getUser().getIdLong();
                  this.channelId = event.getChannel().getIdLong();
                  this.schools = school;
                  this.state = stateToSwitch;
                  this.commandEvent = event;
            }


            @Override
            public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event)
            {
                  if (event.getAuthor().getIdLong() != authorId) return;
                  if (event.getChannel().getIdLong() != channelId) return;

                  String content = event.getMessage().getContentRaw();
                  MessageChannel channel = event.getChannel();

                  switch (state)
                  {
                        case 1 -> {
                              if (!Checks.isNumber(content))
                              {
                                    Embed.error(event, "[ ** %s ** ] is not a number", content);
                                    return;
                              }


                              int index = Integer.parseInt(content) - 1;

                              if (!Checks.between(index, 1, schools.size()))
                              {
                                    Embed.error(event, "[** %s **] is not between 1 - %d", content, schools.size());
                                    return;
                              }


                              this.schoolRemoving = schools.get(index);
                              channel.sendMessageFormat("Are you sure you want to remove [ ** %s **]", schoolRemoving.getSchoolName()).queue();
                              state = 2;
                        }

                        case 2 -> {
                              if (content.equalsIgnoreCase("yes") || content.equalsIgnoreCase("y"))
                              {
                                    commandEvent.removeSchool(commandEvent, schools.get(0));
                                    Embed.success(event, "Removed [** %s **] successfully", schools.get(0).getSchoolName());
                                    event.getJDA().removeEventListener(this);
                              }
                              else if (content.equalsIgnoreCase("no") || content.equalsIgnoreCase("n") || content.equalsIgnoreCase("nah"))
                              {
                                    channel.sendMessage("Okay.. aborting..").queue();
                                    event.getJDA().removeEventListener(this);
                              }
                              else
                              {
                                    Embed.error(event, "[ ** %s ** ] is not a valid respond.. I will need a **Yes** OR a **No**", content);
                              }
                        }
                  }
            }
      }
}