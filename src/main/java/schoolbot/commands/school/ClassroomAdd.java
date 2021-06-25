package schoolbot.commands.school;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import schoolbot.Constants;
import schoolbot.objects.command.Command;
import schoolbot.objects.command.CommandEvent;
import schoolbot.objects.command.CommandFlag;
import schoolbot.objects.misc.Emoji;
import schoolbot.objects.misc.StateMachine;
import schoolbot.objects.misc.StateMachineValues;
import schoolbot.objects.school.Classroom;
import schoolbot.objects.school.School;
import schoolbot.util.Checks;
import schoolbot.util.Embed;
import schoolbot.util.Parser;
import schoolbot.util.Processor;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ClassroomAdd extends Command
{

      public ClassroomAdd(Command parent)
      {
            super(parent, "Adds a class given a target school", "[none]", 0);
            addSelfPermissions(Permission.MANAGE_ROLES);
            addFlags(CommandFlag.DATABASE);
      }


      @Override
      public void run(@NotNull CommandEvent event, @NotNull List<String> args)
      {
            var jda = event.getJDA();
            var schools = event.getGuildSchools();


            if (schools.isEmpty())
            {
                  Embed.error(event, "This server has no schools");
                  return;
            }

            Embed.information(event, """
                    I use a Special System for any schools that belong to the **University of Pittsburgh**
                                        
                    This is Limited to the: Main Campus, Johnstown Campus, Bradford Campus, and the Titusville Campus
                                        
                    So with that being said, to begin. Do you attend any of the University of Pittsburgh Campuses?
                    """);
            jda.addEventListener(new ClassAddStateMachine(event));


      }


      /**
       * Returns true if PeopleSoft is up and functioning if not it return false
       * The values parameter is all of the state machines possible values
       *
       * @param values All of the state machines possible values
       * @return False is PeopleSoft is up and functioning. Otherwise fails
       */
      private static boolean isDown(@NotNull StateMachineValues values)
      {
            var jda = values.getJda();
            var event = values.getCommandEvent();
            var machine = values.getMachine();

            Connection.Response response;
            try
            {
                  response = Jsoup.connect("https://psmobile.pitt.edu/app/catalog/classSearch")
                          .followRedirects(true)
                          .execute();

            }
            catch (Exception e)
            {
                  Embed.error(event, "Error while attempting to connect to PeopleSoft");
                  jda.removeEventListener(machine);
                  return true;
            }


            if (response.url().toString().equals("https://prd.ps.pitt.edu/Maintenance.html"))
            {
                  Embed.error(event, "People soft is currently down for maintenance");
                  jda.removeEventListener(machine);
                  return true;
            }
            return false;
      }

      private static int termValidator(String content)
      {
            Map<String, Integer> map = Map.of("fall", 1, "spring", 4, "summer", 7);

            if (content.split("\\s+").length != 2) return -1;

            String season = content.split("\\s")[0].toLowerCase();
            String yearString = content.split("\\s")[1];

            if (yearString.length() != 4) return -1;
            if (!season.chars().allMatch(Character::isLetter)) return -1;
            if (yearString.chars().noneMatch(Character::isDigit)) return -1;

            // Computer Generated
            int curYear = LocalDateTime.now().getYear();
            int millennium = curYear / 1000;
            int trailingYear = curYear % 100;

            //User
            int yearInt = Integer.parseInt(yearString);
            int userMillennium = yearInt / 1000;
            int userTrailingYear = (season.equalsIgnoreCase("fall")) ? (yearInt % 100) + 1 : (yearInt % 100);
            if (!season.equalsIgnoreCase("spring") && !season.equalsIgnoreCase("summer") && !season.equalsIgnoreCase("fall"))
                  return -1;
            if (millennium != userMillennium) return -1;
            if (userTrailingYear == trailingYear || userTrailingYear == trailingYear + 1) ;
            else return -1;

            return (((userMillennium * 100) + userTrailingYear) * 10) + map.get(season);
      }

      private static String termFixed(String term)
      {
            char[] termCharArr = term.split("\\s")[0].toCharArray();

            for (int i = 0; i < termCharArr.length; i++)
            {
                  if (i == 0)
                  {
                        termCharArr[i] = Character.toUpperCase(termCharArr[i]);
                  }
                  else
                  {
                        termCharArr[i] = Character.toLowerCase(termCharArr[i]);
                  }
            }

            return String.valueOf(termCharArr) + " " + term.split("\\s+")[1];
      }

      public static class ClassAddStateMachine extends ListenerAdapter implements StateMachine
      {
            private String CLASS_SEARCH_URL = "https://psmobile.pitt.edu/app/catalog/classsection/UPITT/";
            private final StateMachineValues values;

            public ClassAddStateMachine(CommandEvent event)
            {
                  values = new StateMachineValues(event, this);
            }

            @Override
            public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event)
            {
                  // tODO: fIX YES OR NO ERROR

                  values.setMessageReceivedEvent(event);
                  long authorId = values.getAuthorId();
                  long channelId = values.getChannelId();

                  var requirementsMet = Checks.eventMeetsPrerequisites(values);

                  if (!requirementsMet)
                  {
                        return;
                  }


                  String message = event.getMessage().getContentRaw();
                  var jda = values.getJda();
                  var channel = event.getChannel();
                  var commandEvent = values.getCommandEvent();
                  var guild = event.getGuild();
                  var classroom = values.getClassroom();


                  int state = values.getState();


                  switch (state)
                  {
                        case 1 -> {
                              classroom.setGuildID(guild.getIdLong());

                              if (message.equalsIgnoreCase("Yes") || message.equalsIgnoreCase("y"))
                              {
                                    if (isDown(values))
                                    {
                                          return;
                                    }

                                    var success = Processor.processGenericList(values, values.getPittClass(), School.class);

                                    if (success == 1)
                                    {
                                          var school = values.getSchool();
                                          channel.sendMessageFormat("""
                                                                                                    
                                                  ** %s ** has been selected successfully. I will now need your term. I only understand pitt term like
                                                  ```
                                                  Fall 2021
                                                  Spring 2020
                                                  Summer 2019
                                                                                
                                                  Format: <Season> <Year number>
                                                  ```
                                                  """, school.getName()).queue();
                                          classroom.setSchool(school);
                                          state = 3;
                                    }
                                    else if (success == 2) state = 2;
                                    // else case here dont forget


                              }
                              else if (message.equalsIgnoreCase("no") || message.equalsIgnoreCase("nah"))
                              {

                                    var schoolList = values.getSchoolList()
                                            .stream()
                                            .filter(School::hasProfessors)
                                            .collect(Collectors.toList());

                                    if (schoolList.isEmpty())
                                    {
                                          Embed.error(event, "There are no schools with professors with them. Please use professor add to add a professor to the target school.");
                                          jda.removeEventListener(this);
                                          return;
                                    }


                                    values.setSchoolList(schoolList);
                                    channel.sendMessage("We will start by getting the school you want to add the class to").queue();
                                    var schools = Processor.processGenericList(values, values.getSchoolList(), School.class);

                                    if (schools == 1)
                                    {
                                          classroom.setSchool(values.getSchool());
                                          Embed.information(event, """
                                                  This school has been selected because there is only one available
                                                                                                    
                                                  To begin can you give me the class name?
                                                  """);
                                          values.setState(11);
                                    }
                                    else if (schools > 1)
                                    {
                                          values.setState(10);
                                    }
                              }
                              else
                              {
                                    channel.sendMessageFormat("** %s ** is not a yes or no answer.. Try again", message).queue();
                              }


                        }
                        case 2 -> {

                              var pittClasses = values.getPittClass();
                              var success = Processor.validateMessage(event, pittClasses);

                              if (success == null)
                              {
                                    return;
                              }

                              classroom.setSchool(success);


                              Embed.success(event, "Successfully set school to %s", success.getName());
                              channel.sendMessage("""
                                      I will now need your term. I only understand pitt term like
                                      ```
                                      Fall 2021
                                      Spring 2020
                                      Summer 2019
                                                                    
                                      Format: <Season> <Year number>
                                      ```
                                      """).queue();
                              state = 3;
                        }
                        case 3 -> {
                              int term = termValidator(message);
                              if (term == -1)
                              {
                                    Embed.error(event, """
                                            Not a valid term. Aborting..
                                            Reason for Aborting
                                            1. **Term is either to old or too far ahead in the future**
                                            2. **You mistyped the term**
                                            3. **You did not input a valid season**""");
                                    jda.removeEventListener(this);
                                    state = 1;
                                    break;
                              }
                              classroom.setTerm(termFixed(message));
                              CLASS_SEARCH_URL += term + "/";
                              channel.sendMessage("""
                                      What is your class number.
                                                                            
                                      `Hint: This can normally be found on your Syllabus, PsMobile or PeopleSoft, or in the link of your class`
                                      """).queue();
                              state = 4;
                        }
                        case 4 -> {


                              if (!Checks.isNumber(message))
                              {
                                    Embed.notANumberError(event, message);
                                    return;
                              }

                              CLASS_SEARCH_URL += message;


                              values.getClassroom().setURL(CLASS_SEARCH_URL);
                              values.getClassroom().setNumber(Integer.parseInt(message));

                              final var school = classroom.getSchool();


                              commandEvent.getCommandThreadPool().execute(() -> commandEvent.addPittClass(classroom));

                              jda.removeEventListener(this);
                        }


                        case 10 -> {
                              var valid = Processor.validateMessage(values, values.getSchoolList());

                              if (!valid)
                              {
                                    return;
                              }

                              var school = values.getSchool();

                              values.getClassroom().setSchool(school);

                              Embed.information(event, """
                                      Thank you. Your school choice is %s.
                                                                            
                                      Can you start by giving me the name you would like the class to be called.
                                      """, school.getName());
                        }

                        case 11 -> {
                              String className = message;


                              boolean duplicateClassNames = commandEvent.getGuildClasses()
                                      .stream()
                                      .map(Classroom::getName)
                                      .anyMatch(clazzroom -> clazzroom.replaceAll("-", " ").equalsIgnoreCase(className));

                              if (duplicateClassNames)
                              {
                                    Embed.error(event, "There is a class that already exist with that name. Try again!");
                                    return;
                              }

                              values.getClassroom().setName(className);

                              var professorList = values.getProfessorList();

                              if (professorList.size() == 1)
                              {
                                    var professor = professorList.get(0);
                                    classroom.setProfessor(professor);
                                    Embed.information(event, """
                                            ** %s ** has been chosen because they are the only professor.
                                                                                        
                                            I will now need the description of the class. If you dont want to input one just type **N/A** or whatever you wish.
                                            """, professor.getFullName());
                                    values.setState(13);
                                    return;
                              }


                              commandEvent.sendMessage("With that all set. I will now need the professor of your choice.");
                              commandEvent.sendAsPaginatorWithPageNumbers(professorList);
                              values.incrementMachineState();
                        }

                        case 12 -> {
                              var valid = Processor.validateMessage(values, values.getProfessorList());

                              if (!valid)
                              {
                                    return;
                              }

                              var professor = values.getProfessor();
                              classroom.setProfessor(professor);
                              Embed.information(event, """
                                      ** %s ** has been chosen
                                                                                  
                                      I will now need the description of the class. If you dont want to input one just type **N/A** or whatever you wish.
                                      """, professor.getFullName());
                        }

                        case 13 -> {
                              if (message.length() > Constants.MAX_FIELD_VALUE)
                              {
                                    Embed.warn(event, "The message you sent is too long to display in embeds. Would you like to reapply another description?");
                                    values.setState(14);
                                    classroom.setDescription(message);
                                    return;
                              }
                              classroom.setDescription(message);
                              Embed.information(event, """
                                      Thank you for that. I will now need your class identifier.
                                                                                 
                                      An example of what I am talking about is below:
                                      Ex: **CS 410, ECON 101, etc**.
                                      """);
                              values.setState(15);
                        }

                        case 14 -> {
                              if (message.toLowerCase().startsWith("y"))
                              {
                                    values.setState(13);

                              }
                              else if (message.toLowerCase().startsWith("n"))
                              {
                                    values.setState(15);
                                    Embed.information(event, """
                                            Thank you for that. I will now need your class identifier.
                                                                                       
                                            An example of what I am talking about is below:
                                            Ex: **CS 410, ECON 101, etc**.
                                            """);
                              }
                              else
                              {
                                    Embed.error(event, "[ ** %s ** ] is not a valid respond.. I will need a **Yes** OR a **No**", message);
                              }
                        }

                        case 15 -> {

                              if (message.length() > 50)
                              {
                                    Embed.error(event, "That is way too long for a class identifier. Please try again or use a simpler version please.");
                                    return;
                              }

                              classroom.setClassIdentifier(message);

                              Embed.information(event, """
                                      Nice! Now that we have your identifier I would like to have the term your class is in.
                                                                            
                                      As for now I only support Seasonal Terms. Here is the format:
                                                                            
                                      Format: <Season> <Year> (Year has to be within a year of the current one (Ex. Cur Year: 2021, Valid Years 2022, 2021, 2020))
                                                                            
                                      Example: Fall 2021
                                      """);

                              values.incrementMachineState();
                        }

                        case 16 -> {
                              var result = termValidator(message);

                              if (result == -1)
                              {
                                    Embed.error(event, "That input was not correct. Please try again");
                                    return;
                              }
                              classroom.setTerm(termFixed(message));

                              Embed.information(event, """
                                      Nice! %s I have successfully applied your identifier.
                                                                            
                                      I will now need your class start and end dates.
                                                                            
                                      This can be a little finicky because I am in my alpha stages, but I will try my hardest.
                                                                            
                                      Here is the format: ** 1/19/2021 - 4/23/2021 ** | <M/dd/yyyy> - <M/dd/yyyy>
                                      """, Emoji.SMILEY_FACE.getAsChat());

                              values.incrementMachineState();
                        }

                        case 17 -> {

                              if (!parseDates(values))
                              {
                                    return;
                              }

                              Embed.information(event, """
                                      That was easier than I thought! %s. That was Part 1 of 2.
                                                                            
                                      I will now need your class time. Just like the date this will be a little finicky. So please bare with me!
                                                                            
                                      Here is the format: **MoTuWeThFr 2:00PM - 3:20PM** | <Days> <Start time (IN EST) - <End time> (in EST)
                                      """, Emoji.SMILEY_FACE.getAsChat());


                              values.incrementMachineState();
                        }

                        case 18 -> {
                              if (!parseTime(values))
                              {
                                    return;
                              }

                              classroom.setTime(message);

                              Embed.information(event, "Awesome.. I will now add all of this information to my database.. One moment!");

                              commandEvent.addClass(classroom);
                              jda.removeEventListener(this);

                        }


                  }
            }

            private boolean parseTime(StateMachineValues values)
            {
                  String message = values.getMessageReceivedEvent().getMessage().getContentRaw();
                  var commandEvent = values.getCommandEvent();
                  var classroom = values.getClassroom();

                  Map<DayOfWeek, LocalDateTime> localDateTimeMap = Parser.parseTime(classroom, message);

                  if (localDateTimeMap == null || localDateTimeMap.isEmpty())
                  {
                        Embed.error(commandEvent, "There was an error while parsing your time!");
                        return false;
                  }

                  return true;
            }

            private boolean parseDates(StateMachineValues values)
            {
                  String potentialDate = values.getMessageReceivedEvent().getMessage().getContentRaw();
                  var commandEvent = values.getCommandEvent();

                  if (!potentialDate.contains("-"))
                  {
                        Embed.error(commandEvent, "Invalid Syntax: Missing \"**-**\"");
                        return false;
                  }

                  String[] splitDate = potentialDate.replaceAll("\\s+", "").split("-");

                  var format = DateTimeFormatter.ofPattern("M/dd/yyyy");
                  LocalDate startDate;
                  LocalDate endDate;

                  try
                  {
                        startDate = LocalDate.parse(splitDate[0], format);
                  }
                  catch (Exception e)
                  {
                        Embed.error(commandEvent, "Error parsing Start Date. Please try again!");
                        return false;
                  }

                  try
                  {
                        endDate = LocalDate.parse(splitDate[1], format);
                  }
                  catch (Exception e)
                  {
                        Embed.error(commandEvent, "Error parsing End Date. Please try again!");
                        return false;
                  }

                  int year = Math.abs(startDate.getYear() - LocalDate.now().getYear());


                  if (year != 1 && year != 0)
                  {
                        Embed.error(commandEvent, "That year is too far from the current one.. Please try a more recent date.");
                        return false;
                  }


                  int dateTest = Math.abs(startDate.getYear() - endDate.getYear());

                  if (dateTest != 0 && dateTest != 1)
                  {
                        Embed.error(commandEvent, "%s is %d years apart from %s. The max is one year apart", startDate, dateTest, endDate);
                        return false;
                  }


                  var classroom = values.getClassroom();

                  classroom.setStartDate(Date.valueOf(startDate));
                  classroom.setEndDate(Date.valueOf(endDate));

                  return true;
            }

      }
}