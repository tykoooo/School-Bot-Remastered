package schoolbot.natives.objects.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import schoolbot.Schoolbot;
import schoolbot.handlers.CommandCooldownHandler;
import schoolbot.natives.util.Embed;
import schoolbot.natives.objects.misc.Emoji;



public abstract class Command 
{


    private List<String> calls;

    private List<Permission> commandPermissions;

    private List<Permission> selfPermissions;

    private boolean enabled;

    private String name;

    private String usage;

    private int minimalArgs;

    private String usageExample;

    private String description;

    private String syntax;

    private long cooldown;



    /**
     * Command constructor with aliases for the command.
     * 
     * @param aliases command aliases
     */
    protected Command(String description, String syntax, int minimalArgs) 
    {
        this.name = this.getClass().getSimpleName();
        this.description = description;
        this.syntax = syntax;
        this.enabled = true;
        this.cooldown = 1000;
        this.minimalArgs = minimalArgs;
        this.calls = new ArrayList<>();
        this.commandPermissions = new ArrayList<>();
        this.selfPermissions = new ArrayList<>();
    }

    /**
     * What the command will do on call.
     * 
     * @param args Arguments sent to the command.
     */
    public abstract void run(CommandEvent event);

    public  String getDescription() 
    {
        return this.description;
    }

    public List<Permission> getSelfPermissions()
    {
        return selfPermissions;
    }

    public String getUsage() 
    {
        return this.usage;
    }

    public String getUsageExample() 
    {
        return this.usageExample;
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
       if (!event.memberPermissionCheck(event.getCommand().getCommandPermissions()))
       {
            List<Permission> permissionsMemberHas = event.getCommand().getCommandPermissions().stream()
                                                                                                .filter(perm -> !event.getMember().hasPermission(perm))
                                                                                                .collect(Collectors.toList());
            StringBuilder perms = new StringBuilder();  
            permissionsMemberHas.forEach(f -> perms.append(" `" + f + "` "));
            Embed.error(event,  "This command requires you to have atleast this permission" + perms.toString() + "in order to execute it!");
       }
       else if (CommandCooldownHandler.isOnCooldown(event.getMember(), this))
       {
            Embed.error(event, "You are on a cooldown!");
       }
       else if (!event.selfPermissionCheck(event.getCommand().getSelfPermissions()))
       {
            Embed.error(event, "I do not have permissions to do this, Please give me");
       }
       else if (!isEnabled())
       {
            Embed.error(event, "This command is disabled!");
       }
       else if (event.getArgs().size() < minimalArgs)
       {
            // minimal args requirement isnt met!
            Embed.error(event, "This minimal amount of args for this command is " + minimalArgs);
       }
       else 
       {
           event.getSchoolbot().getLogger().info("{} has been executed by {} using the args {}", this.name, event.getUser().getName(), event.getArgs());
           run(event);
       } 
    }


    /**
     * ... is like saying String[]
     */

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

    /**
     * Check whether the current command is enabled or not.
     * 
     * @return enabled?
     */
    public boolean isEnabled() 
    {
        return this.enabled;
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
    
    public void setCooldown(long cooldown) {
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

    @Override
    public String toString() {
        return this.name;
    }

}
