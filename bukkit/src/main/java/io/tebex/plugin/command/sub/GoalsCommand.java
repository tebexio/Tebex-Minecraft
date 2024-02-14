package io.tebex.plugin.command.sub;

import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.obj.CommunityGoal;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class GoalsCommand extends SubCommand {
    public GoalsCommand(TebexPlugin platform) {
        super(platform, "goals", "tebex.goals");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        try {
            List<CommunityGoal> goals = platform.getSDK().getCommunityGoals().get();
            for (CommunityGoal goal: goals) {
                if (goal.getStatus() != CommunityGoal.Status.DISABLED) {
                    platform.sendMessage(sender, "&fCommunity Goals: ");
                    platform.sendMessage(sender, String.format("&7- %s (%.2f/%.2f) [%s]", goal.getName(), goal.getCurrent(), goal.getTarget(), goal.getStatus()));
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            platform.sendMessage(sender, "&cUnexpected response: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "Shows active and completed community goals.";
    }
}
