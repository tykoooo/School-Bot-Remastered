package schoolbot.commands.school;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schoolbot.objects.command.Command;
import schoolbot.objects.command.CommandEvent;
import schoolbot.objects.misc.DatabaseDTO;
import schoolbot.objects.misc.StateMachine;
import schoolbot.objects.school.Assignment;
import schoolbot.objects.school.Classroom;
import schoolbot.objects.school.School;
import schoolbot.util.Checks;
import schoolbot.util.Embed;
import schoolbot.util.Processor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AssignmentEdit extends Command
{
      Logger LOGGER = LoggerFactory.getLogger(this.getClass());
      public AssignmentEdit(Command parent)
      {
            super(parent, "Edits an assignment", "[none]", 0);
            addPermissions(Permission.ADMINISTRATOR);
      }


      @Override
      public void run(@NotNull CommandEvent event, @NotNull List<String> args)
      {
            // TODO: If there is one of anything automatically select it instead of akign

            JDA jda = event.getJDA();

            List<School> schoolList = new ArrayList<>();
            for (School school : event.getGuildSchools())
            {
                  List<Classroom> classroomList = new ArrayList<>();
                  for (Classroom classroom : school.getClassroomList())
                  {
                        if (classroom.hasAssignments())
                        {
                              classroomList.add(classroom);
                        }
                  }
                  school.setClassroomList(classroomList);
                  schoolList.add(school);
            }


            if (schoolList.isEmpty())
            {
                  Embed.error(event, "** %s ** has no classes with assignments in them", event.getGuild().getName());
                  return;
            }

            if (schoolList.size() == 1)
            {
                  School school = schoolList.get(0);

                  evaluateClassroom(event, school.getClassroomList());
                  return;
            }

            if (schoolList.size() > 1)
            {
                  event.sendAsPaginatorWithPageNumbers(schoolList);
                  event.sendMessage("What school would you like to edit an assignment in...");
                  jda.addEventListener(new AssignmentEditStateMachine(event, schoolList, null, null, 1));
                  return;
            }

            School school = schoolList.get(0);

            evaluateClassroom(event, school.getClassroomList());
      }


      private void evaluateClassroom(CommandEvent event, List<Classroom> classroomList)
      {
            JDA jda = event.getJDA();
            if (classroomList.size() == 1)
            {
                  Classroom classroom = classroomList.get(0);
                  List<Assignment> assignments = classroom.getAssignments();


                  if (assignments.size() == 1)
                  {
                        Assignment assignment = classroom.getAssignments().get(0);
                        event.sendMessage("** %s ** has been chosen its the only assignment. What would you like to edit?", assignment.getName());
                        event.sendMessage("""
                                ```
                                1. Name
                                2. Description
                                3. Point Amount
                                4. Type
                                5. Due Date
                                6. Due Time```
                                 """);
                        jda.addEventListener(new AssignmentEditStateMachine(event, assignment));
                  }
                  else
                  {
                        event.sendAsPaginatorWithPageNumbers(assignments);
                        event.sendMessage("Please give me the page number of the assignment you want to edit");
                        jda.addEventListener(new AssignmentEditStateMachine(event, null, classroomList, assignments, 3));

                  }
            }

            if (classroomList.size() > 1)
            {
                  event.sendAsPaginatorWithPageNumbers(classroomList);
                  event.sendMessage("Please give me the page number of the class that contains the assignment you want to edit");
                  jda.addEventListener(new AssignmentEditStateMachine(event, null, classroomList, null, 2));
            }
      }


      public static class AssignmentEditStateMachine extends ListenerAdapter implements StateMachine
      {

            private final CommandEvent commandEvent;
            private final long authorID, channelID;
            private List<School> schools;
            private List<Classroom> classroomList;
            private List<Assignment> assignmentList;
            private Assignment assignment;
            private int state;
            private String updateColumn;

            public AssignmentEditStateMachine(CommandEvent event, Assignment assignment)
            {
                  this.commandEvent = event;
                  this.authorID = event.getUser().getIdLong();
                  this.channelID = event.getChannel().getIdLong();
                  this.assignment = assignment;
                  this.state = 4;
            }

            public AssignmentEditStateMachine(@NotNull CommandEvent event, @Nullable List<School> schoolList,
                                              @Nullable List<Classroom> classroomList, @Nullable List<Assignment> assignments,
                                              int state)
            {
                  this.commandEvent = event;
                  this.authorID = event.getUser().getIdLong();
                  this.channelID = event.getChannel().getIdLong();
                  this.classroomList = classroomList;
                  this.schools = schoolList;
                  this.assignmentList = assignments;
                  this.state = state;
            }


            @Override
            public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event)
            {

                  if (!Checks.eventMeetsPrerequisites(event, this, channelID, authorID))
                  {
                        return;
                  }

                  String message = event.getMessage().getContentRaw();

                  var channel = event.getChannel();
                  var jda = event.getJDA();
                  var guild = event.getGuild();


                  if (message.equalsIgnoreCase("stop"))
                  {
                        channel.sendMessage("Aborting...").queue();
                        jda.removeEventListener(this);
                        return;
                  }

                  switch (state)
                  {
                        case 1 -> {

                              var success = Processor.validateMessage(event, schools);


                              if (success == null)
                              {
                                    return;
                              }


                              classroomList = success.getClassroomList();


                              channel.sendMessageFormat("Now that we have selected ** %s **, I will now need the class you want to the assignment", success.getName()).queue();
                              commandEvent.sendAsPaginatorWithPageNumbers(success.getClassroomList());
                              state = 2;
                        }

                        case 2 -> {

                              var success = Processor.validateMessage(event, classroomList);

                              if (success == null)
                              {
                                    return;
                              }

                              this.assignmentList = success.getAssignments();

                              channel.sendMessageFormat("Now that we have selected ** %s **, I will now need the assignment you would like to edit", success.getName()).queue();


                              commandEvent.sendAsPaginatorWithPageNumbers(assignmentList);
                              state = 3;
                        }

                        case 3 -> {

                              var success = Processor.validateMessage(event, assignmentList);

                              if (success == null)
                              {
                                    return;
                              }

                              assignment = success;

                              channel.sendMessageFormat("What attribute of ** %s ** would you like to edit", assignment.getName()).queue();
                              channel.sendMessage("""
                                      ```
                                         1. Name
                                         2. Description
                                         3. Point Amount
                                         4. Type
                                         5. Due Date
                                         6. Due Time```
                                       """).queue();
                              state = 4;
                        }

                        case 4 -> {
                              String content = message.toLowerCase();

                              if (content.equals("name") || content.equals("1"))
                              {
                                    updateColumn = "name";
                                    channel.sendMessageFormat("Please give me an updated name of the assignment").queue();

                              }
                              else if (content.equals("description") || content.equals("2"))
                              {
                                    updateColumn = "description";
                                    channel.sendMessageFormat("Please give me an updated description of the assignment").queue();
                              }
                              else if (content.contains("point") || content.contains("amount") || content.equals("3"))
                              {
                                    updateColumn = "points_possible";
                                    channel.sendMessageFormat("Please give me an updated point amount of the assignment").queue();

                              }
                              else if (content.contains("4") || content.contains("type"))
                              {
                                    updateColumn = "type";
                                    channel.sendMessage("""
                                            Now I will need the type of assignment it is
                                            ```
                                            Valid Answers
                                            1. Exam
                                            2. Quiz
                                            3. Extra Credit
                                            4. Homework
                                            5. Paper
                                            ```
                                            """).queue();
                              }
                              else if (content.equals("5") || content.contains("date"))
                              {
                                    updateColumn = "due_date";
                                    channel.sendMessage("""
                                            Please give me the updated due date
                                            Please use the following format: `M/dd/yyyy`
                                            An Example: `2/9/2004`
                                            """).queue();
                              }
                              else if (content.equals("6") || content.contains("time"))
                              {
                                    updateColumn = "due_datet";
                                    channel.sendMessage("""
                                            Please give me the updated due time
                                            Please use the following format: `HH:mm AM/PM`
                                            An Example: `12:30pm` or `8:30am`
                                            """).queue();
                              }
                              else
                              {
                                    Embed.error(event, "** %s ** is not a valid response.. Try again please", message);
                                    return;
                              }
                              state = 5;

                        }

                        case 5 -> {
                              evaluateUpdate(commandEvent, message);
                              commandEvent.sendMessage(assignment.getAsEmbed(commandEvent.getSchoolbot()));
                        }
                  }
            }

            private void evaluateUpdate(CommandEvent event, String message)
            {
                  JDA jda = event.getJDA();

                  switch (updateColumn)
                  {
                        case "name", "description" -> {
                              commandEvent.updateAssignment(commandEvent, new DatabaseDTO(assignment, updateColumn, message));
                              event.sendMessage(updateColumn.equals("name") ? "Name" : "Description" + " successfully changed to %s", message);

                        }

                        case "points_possible" -> {
                              if (!Checks.isNumber(message))
                              {
                                    Embed.notANumberError(event.getEvent(), message);
                              }

                              int newPoints = Integer.parseInt(message);

                              commandEvent.updateAssignment(event, new DatabaseDTO(assignment, updateColumn, newPoints));
                              event.sendMessage("Points successfully changed to %d", newPoints);

                        }

                        case "type" -> {
                              Assignment.AssignmentType type;
                              if (message.contains("exam") || message.contains("1"))
                              {
                                    type = Assignment.AssignmentType.EXAM;
                              }
                              else if (message.contains("paper") || message.contains("5"))
                              {
                                    type = Assignment.AssignmentType.PAPER;
                              }
                              else if (message.contains("homework") || message.contains("work") || message.contains("4"))
                              {
                                    type = Assignment.AssignmentType.HOMEWORK;
                              }
                              else if (message.contains("quiz") || message.contains("2"))
                              {
                                    type = Assignment.AssignmentType.QUIZ;
                              }
                              else if (message.contains("extra") || message.contains("credit") || message.contains("3"))
                              {
                                    type = Assignment.AssignmentType.EXTRA_CREDIT;
                              }
                              else
                              {
                                    Embed.error(event, "** %s ** is not a valid entry", message);
                                    return;
                              }
                              commandEvent.updateAssignment(commandEvent, new DatabaseDTO(assignment, updateColumn, type));
                              event.sendMessage("Assignment type successfully changed to %s", type.getAssignmentType());
                        }

                        case "due_date" -> {
                              if (!Checks.isValidAssignmentDate(message, assignment.getClassroom()))
                              {
                                    Embed.error(event, "** %s ** is not a valid date", message);
                              }



                              LocalDateTime localDateTime = LocalDateTime.of(LocalDate.parse(message, DateTimeFormatter.ofPattern("M/d/yyyy")), assignment.getDueDate().toLocalTime());

                              commandEvent.updateAssignment(event, new DatabaseDTO(assignment, updateColumn, localDateTime));
                              event.sendMessage("Date successfully changed to %s", localDateTime);

                        }

                        case "due_datet" -> {
                              updateColumn = updateColumn.substring(0, updateColumn.lastIndexOf("t"));

                              LocalDateTime localDateTime;

                              if (!Checks.checkValidTime(message))
                              {
                                    Embed.error(event, "** %s ** is not a valid time... try again!", message);
                                    return;
                              }

                              String[] time = message.split(":");


                              if (message.toLowerCase().contains("am"))
                              {
                                    int hour = Integer.parseInt(time[0]);
                                    int minute = Integer.parseInt(time[1].replaceAll("am", ""));


                                    localDateTime = LocalDateTime.of(assignment.getDueDate().toLocalDate(), LocalTime.of((hour), minute));

                                    if  (!localDateTime.isAfter(LocalDateTime.now()))
                                    {
                                          String formattedTime = localDateTime.format(DateTimeFormatter.ofPattern("M/dd/yyyy @ HH:mm"));
                                          Embed.error(event, "** %s ** is not a valid date.. Try again", formattedTime);
                                          return;
                                    }

                              }
                              else
                              {
                                    int hour = Integer.parseInt(time[0]);
                                    int minute = Integer.parseInt(time[1].replaceAll("pm", ""));

                                    if (hour == 12)
                                    {
                                          hour = -12;
                                    }

                                    localDateTime = LocalDateTime.of(assignment.getDueDate().toLocalDate(), LocalTime.of((12 + hour), minute));

                                    if  (!localDateTime.isAfter(LocalDateTime.now()))
                                    {
                                          String formattedTime = localDateTime.format(DateTimeFormatter.ofPattern("M/dd/yyyy @ HH:mm"));
                                          Embed.error(event, "** %s ** is not a valid date.. Try again", formattedTime);
                                          return;
                                    }

                              }
                              commandEvent.updateAssignment(event, new DatabaseDTO(assignment, updateColumn, localDateTime));
                              event.sendMessage("Date successfully changed to %s", localDateTime);

                        }
                  }
                  jda.removeEventListener(this);
            }
      }
}

