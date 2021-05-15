package schoolbot.objects.command;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schoolbot.SchoolbotConstants;
import schoolbot.handlers.CommandCooldownHandler;
import schoolbot.util.Embed;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public abstract class Command
{
      private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

      private final List<String> calls;
      private final List<Permission> commandPermissions;
      private final List<Permission> selfPermissions;
      private final List<Command> children;
      private final List<CommandFlag> commandFlags;

      private boolean enabled;

      private final String name;
      private String usageExample;
      private final String description;
      private final String syntax;

      private final int minimalArgs;

      private long cooldown;

      private final Command parent;


      /**
       * Command constructor with aliases for the command.
       *
       * @param description command aliases
       * @param minimalArgs
       * @parm syntax
       */
      protected Command(String description, String syntax, int minimalArgs)
      {
            this.name = this.getClass().getSimpleName();
            this.description = description;
            this.parent = null;
            this.syntax = syntax;
            this.enabled = true;
            this.cooldown = 1000;
            this.minimalArgs = minimalArgs;
            this.calls = new ArrayList<>();
            this.commandPermissions = new ArrayList<>();
            this.selfPermissions = new ArrayList<>();
            this.children = new ArrayList<>();
            this.commandFlags = new ArrayList<>();
            this.usageExample = "N/A";
      }

      /**
       * @param parent
       * @param description
       * @param syntax
       * @param minimalArgs
       */
      protected Command(Command parent, String description, String syntax, int minimalArgs)
      {
            this.name = this.getClass().getSimpleName();
            this.parent = parent;
            this.description = description;
            this.syntax = syntax;
            this.enabled = true;
            this.cooldown = 1000;
            this.minimalArgs = minimalArgs;
            this.calls = new ArrayList<>();
            this.commandPermissions = new ArrayList<>();
            this.selfPermissions = new ArrayList<>();
            this.commandFlags = new ArrayList<>();
            this.children = new ArrayList<>();
            this.usageExample = "N/A";

      }


      public abstract void run(@NotNull CommandEvent event, @NotNull List<String> args);

      /**
       * @return
       */
      public String getDescription()
      {
            return this.description;
      }

      /**
       * @return
       */
      public List<Permission> getSelfPermissions()
      {
            return selfPermissions;
      }




      public List<String> getCalls()
      {
            return this.calls;
      }

      public long getCooldown()
      {
            return cooldown;
      }

      public List<Permission> getCommandPermissions()
      {
            return commandPermissions;
      }

      public String getSyntax()
      {
            return syntax;
      }

      public void process(CommandEvent event)
      {
            if (event.getArgs().size() < minimalArgs)
            {
                  Embed.error(event, "This minimal amount of args for this command is " + minimalArgs);
            }
            else if (!event.selfPermissionCheck(event.getCommand().getSelfPermissions()))
            {
                  Embed.error(event, "I do not have permissions to do this");
            }
            else if (event.isDeveloper())
            {
                  LOGGER.info("{} executed using args {} by {}", name, event.getArgs(), event.getUser().getName());
                  run(event, event.getArgs());
            }
            else if (!event.memberPermissionCheck(event.getCommand().getCommandPermissions()))
            {
                  Embed.sendInvalidMemberPermissions(event);
            }
            else if (CommandCooldownHandler.isOnCooldown(event.getMember(), this))
            {
                  Embed.sendInvalidMemberPermissions(event);
            }

            else if (!isEnabled())
            {
                  Embed.error(event, "This command is disabled!");
            }

            else if (event.getUser().isBot())
            {
                  Embed.error(event, "You are a bot silly goose :P");
            }
            else
            {

                  if (hasCommandFlags(CommandFlag.INTERNET, CommandFlag.DATABASE))
                  {
                        addUserToCooldown(event.getMember());
                  }

                  LOGGER.info("{} executed using args {} by {}", name, event.getArgs(), event.getUser().getAsMention());
                  run(event, event.getArgs());
            }
      }


      public void addCooldown(long cooldown)
      {
            this.cooldown = cooldown;
      }

      public void addUserToCooldown(Member member)
      {
            CommandCooldownHandler.addCooldown(member, this);
      }

      public void addCalls(String... calls)
      {
            this.calls.addAll(List.of(calls));
      }

      public void addPermissions(Permission... permissions)
      {
            this.commandPermissions.addAll(List.of(permissions));
      }

      public void addSelfPermissions(Permission... permissions)
      {
            this.selfPermissions.addAll(List.of(permissions));
      }

      public void addChildren(Command... children)
      {
            this.children.addAll(List.of(children));
      }

      public void addFlags(CommandFlag... flags)
      {
            this.commandFlags.addAll(List.of(flags));
      }

      public void addUsageExample(String usage)
      {
            this.usageExample = usage;
      }

      public int getMinimalArgs()
      {
            return minimalArgs;
      }

      public String getUsageExample()
      {
            return usageExample;
      }

      public EmbedBuilder getAsHelpEmbed()
      {
            return new EmbedBuilder()
                    .setTitle("Help for **" + this.name + "**")
                    .addField("Description", "`" + this.description + "`", false)
                    .addField("Syntax", "`" + this.syntax + "`", false)
                    .addField("Usage Example",
                            this.usageExample.equalsIgnoreCase("N/A") ?
                                    "`" + this.usageExample + "`" : "`" + SchoolbotConstants.DEFAULT_PREFIX + this.usageExample + "`", false)
                    .addField("Aliases", String.valueOf(this.calls), false)
                    .setColor(Color.BLACK)
                    .setFooter("[] = Required | <> = Optional");
      }


      /**
       * Check whether the current command is enabled or not.
       *
       * @return enabled?
       */
      public boolean isEnabled()
      {
            return this.enabled;
      }

      public boolean hasChildren()
      {
            return !getChildren().isEmpty();
      }

      public boolean hasCommandFlags(CommandFlag... flags)
      {
            for (CommandFlag flag : flags)
            {
                  if (this.commandFlags.contains(flag))
                  {
                        return true;
                  }
            }
            return false;
      }


      /**
       * Set this command to be enabled or disabled.
       *
       * @param enabled {@code true} for enabled or {@code false} for disabled.
       */
      public void setEnabled(boolean enabled)
      {
            this.enabled = enabled;
      }

      public void setCooldown(long cooldown)
      {
            this.cooldown = cooldown;
      }

      /**
       * Function to get the name of the command
       *
       * @return the name of the command
       */
      public String getName()
      {
            return name;
      }

      public List<Command> getChildren()
      {
            return children;
      }

      public Command getParent()
      {
            return parent;
      }


      @Override
      public String toString()
      {
            return this.name;
      }

}