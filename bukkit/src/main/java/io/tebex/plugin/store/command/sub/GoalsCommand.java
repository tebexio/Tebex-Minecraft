package io.tebex.plugin.store.command.sub;

import io.tebex.plugin.Lang;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.store.obj.CommunityGoal;
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

        if(! platform.isStoreSetup()) {
            platform.sendMessage(sender, Lang.NOT_CONNECTED_TO_STORE.getMessage());
            return;
        }

        try {
            List<CommunityGoal> goals = platform.getStoreSDK().getCommunityGoals().get();
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
