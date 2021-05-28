package schoolbot.objects.guild;

import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schoolbot.Schoolbot;
import schoolbot.commands.school.SchoolEdit;
import schoolbot.objects.command.CommandEvent;
import schoolbot.objects.school.Assignment;
import schoolbot.objects.school.Classroom;
import schoolbot.objects.school.Professor;
import schoolbot.objects.school.School;
import schoolbot.util.DatabaseUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GuildWrapper
{
      private final long guildID;
      private final Map<String, School> schoolList;
      private final List<Classroom> classrooms;
      private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());


      public GuildWrapper(@NotNull DatabaseUtil.WrapperReturnValue data)
      {
            this.guildID = data.guildID();
            this.schoolList = data.schoolMap();
            this.classrooms = Collections.synchronizedList(data.classrooms());
      }

      /*
            This is meant to serve kinda as a cache..
            Each Guild will have there own wrapper so I can easily list, add, edit, and remove schools without having to make many database connections
            Things like ListSchools, ListProfessor, ListAssignments, and ListClasses should not have database calls.. Not really needed and slow
       */

      public boolean addSchool(CommandEvent event, School school)
      {
            String lowerCaseSchoolName = school.getName().toLowerCase();
            if (schoolList.containsKey(lowerCaseSchoolName)) return false;

            int id = DatabaseUtil.addSchool(event, school);
            if (id == -1) return false;

            school.setSchoolID(id);
            schoolList.put(lowerCaseSchoolName, school);

            return true;
      }

      public School getSchool(String schoolName)
      {
            return schoolList.get(schoolName.toLowerCase());
      }

      public void updateSchool(CommandEvent event, SchoolEdit.SchoolUpdateDTO schoolUpdateDTO)
      {
            String update = schoolUpdateDTO.updateColumn();
            School school = schoolUpdateDTO.school();
            String schoolName = school.getName().toLowerCase();

            switch (update)
            {
                  case "name" -> {
                        schoolList.remove(schoolName);

                        String updatedName = (String) schoolUpdateDTO.value();
                        school.setName(updatedName);
                        schoolList.put(update.toLowerCase(), school);
                  }
                  case "role_id" -> {
                        Role role = event.getJDA().getRoleById(school.getRoleID());

                        if (role != null)
                        {
                              role.delete().queue(
                                      success ->
                                      {
                                            LOGGER.info("Role for {} successfully has been deleted", school.getName());
                                      },
                                      failure ->
                                      {
                                            LOGGER.error("Role for {} could not be deleted", school.getName(), failure);
                                      });
                        }
                        Long newRoleID = (Long) schoolUpdateDTO.value();
                        schoolList.get(schoolName).setRoleID(newRoleID);

                  }

                  case "url" -> {
                        // Could check if valid URL.... but..
                        String URL = (String) schoolUpdateDTO.value();
                        schoolList.get(schoolName).setURL(URL);
                  }

                  case "email_suffix" -> {
                        String email = (String) schoolUpdateDTO.value();
                        schoolList.get(schoolName).setURL(email);
                  }
            }

            DatabaseUtil.updateSchool(schoolUpdateDTO, event.getSchoolbot());

      }

      public void removeSchool(Schoolbot schoolbot, School school)
      {
            schoolList.remove(school.getName().toLowerCase());
            if (schoolbot.getJda().getRoleById(school.getRoleID()) != null)
            {
                  schoolbot.getJda().getRoleById(school.getRoleID()).delete().queue(
                          success ->
                          {
                                LOGGER.info("Successfully deleted role for {}", school.getName());
                          },

                          failure ->
                          {
                                LOGGER.warn("Could not delete role for {} ", school.getName(), failure);
                          }
                  );
            }
            DatabaseUtil.removeSchool(schoolbot, school.getName());
      }


      public void addPittClass(CommandEvent event, Classroom classroom)
      {
            School school = classroom.getSchool();

            school.addPittClass(event, classroom);
            classrooms.add(classroom);

      }

      public boolean addProfessor(Schoolbot schoolbot, Professor professor)
      {


            int id = DatabaseUtil.addProfessor(schoolbot, professor);

            if (id == -1)
            {
                  return false;
            }
            professor.setId(id);
            professor.getProfessorsSchool().addProfessor(professor);
            return true;
      }

      public void removeProfessor(Schoolbot schoolbot, Professor professor)
      {
            professor.getProfessorsSchool().removeProfessor(professor);
            DatabaseUtil.removeProfessor(schoolbot, professor);
      }

      public void removeClassroom(CommandEvent event, Classroom classroom)
      {
            classroom.getSchool().removeClass(classroom);
            classrooms.remove(classroom);

            if (classroom.getRoleID() != 0)
            {
                  if (event.getJDA().getRoleById(classroom.getRoleID()) != null)
                  {
                        event.getJDA().getRoleById(classroom.getRoleID()).delete().queue(success ->
                                        LOGGER.info("Successfully deleted role for {}", classroom.getClassName()),
                                failure ->
                                        LOGGER.warn("Could not delete role for {} ", classroom.getClassName(), failure)
                        );
                  }
            }

            if (classroom.getChannelID() != 0)
            {
                  if (event.getJDA().getTextChannelById(classroom.getChannelID()) != null)
                  {
                        event.getJDA().getTextChannelById(classroom.getChannelID()).delete().queue(
                                success ->
                                        LOGGER.info("Successfully deleted channel for {}", classroom.getClassName()),
                                failure ->
                                        LOGGER.warn("Could nto delete class for {} ", classroom.getClassName(), failure)
                        );
                  }
            }


            DatabaseUtil.removeClassroom(event, classroom);
      }

      public List<Classroom> getAllClasses(CommandEvent event)
      {
            return classrooms;
      }


      public long getGuildID()
      {
            return guildID;
      }

      public boolean containsSchool(String schoolName)
      {
            return schoolList.containsKey(schoolName.toLowerCase());
      }


      public List<Professor> getProfessorList(String schoolName)
      {
            String lowerCaseSchoolName = schoolName.toLowerCase();

            if (!schoolList.containsKey(lowerCaseSchoolName)) return Collections.emptyList();

            return schoolList.get(lowerCaseSchoolName).getProfessorList();
      }

      public void addAssignment(Schoolbot schoolbot, Assignment assignment)
      {
            String lowerCaseSchoolName = assignment.getName().toLowerCase();

            assignment.getClassroom().addAssignment(schoolbot, assignment);
      }

      public void removeAssignment(Schoolbot schoolbot, Assignment assignment)
      {
            String lowerCaseSchoolName = assignment.getName().toLowerCase();

            assignment.getClassroom().removeAssignment(schoolbot, assignment);
      }


      public List<School> getSchoolList()
      {
            return new ArrayList<School>(schoolList.values());
      }
}
