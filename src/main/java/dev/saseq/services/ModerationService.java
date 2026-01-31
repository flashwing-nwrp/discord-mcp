package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ModerationService {

    private final JDA jda;

    @Value("${DISCORD_GUILD_ID:}")
    private String defaultGuildId;

    public ModerationService(JDA jda) {
        this.jda = jda;
    }

    private String resolveGuildId(String guildId) {
        if ((guildId == null || guildId.isEmpty()) && defaultGuildId != null && !defaultGuildId.isEmpty()) {
            return defaultGuildId;
        }
        return guildId;
    }

    /**
     * Kicks a member from the Discord server.
     *
     * @param guildId Optional ID of the Discord server. If not provided, the default server will be used.
     * @param userId  The ID of the user to kick.
     * @param reason  Optional reason for the kick.
     * @return A confirmation message.
     */
    @Tool(name = "kick_member", description = "Kick a member from the server")
    public String kickMember(@ToolParam(description = "Discord server ID", required = false) String guildId,
                             @ToolParam(description = "Discord user ID") String userId,
                             @ToolParam(description = "Reason for kick", required = false) String reason) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        Member member = guild.retrieveMemberById(userId).complete();
        if (member == null) {
            throw new IllegalArgumentException("Member not found by userId");
        }

        String memberName = member.getEffectiveName();
        guild.kick(member).reason(reason != null ? reason : "No reason provided").complete();
        return "Kicked " + memberName + " from the server" + (reason != null ? ". Reason: " + reason : "");
    }

    /**
     * Bans a user from the Discord server.
     *
     * @param guildId        Optional ID of the Discord server. If not provided, the default server will be used.
     * @param userId         The ID of the user to ban.
     * @param reason         Optional reason for the ban.
     * @param deleteMessages Optional number of days of messages to delete (0-7).
     * @return A confirmation message.
     */
    @Tool(name = "ban_member", description = "Ban a user from the server")
    public String banMember(@ToolParam(description = "Discord server ID", required = false) String guildId,
                            @ToolParam(description = "Discord user ID") String userId,
                            @ToolParam(description = "Reason for ban", required = false) String reason,
                            @ToolParam(description = "Days of messages to delete (0-7)", required = false) Integer deleteMessages) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        // Try to get member info first for the name
        String userName;
        try {
            Member member = guild.retrieveMemberById(userId).complete();
            userName = member != null ? member.getEffectiveName() : "User " + userId;
        } catch (Exception e) {
            userName = "User " + userId;
        }

        int deleteDays = deleteMessages != null ? Math.min(Math.max(deleteMessages, 0), 7) : 0;
        guild.ban(UserSnowflake.fromId(userId), deleteDays, TimeUnit.DAYS)
                .reason(reason != null ? reason : "No reason provided")
                .complete();

        return "Banned " + userName + " from the server" + (reason != null ? ". Reason: " + reason : "");
    }

    /**
     * Unbans a user from the Discord server.
     *
     * @param guildId Optional ID of the Discord server. If not provided, the default server will be used.
     * @param userId  The ID of the user to unban.
     * @return A confirmation message.
     */
    @Tool(name = "unban_member", description = "Unban a user from the server")
    public String unbanMember(@ToolParam(description = "Discord server ID", required = false) String guildId,
                              @ToolParam(description = "Discord user ID") String userId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        guild.unban(UserSnowflake.fromId(userId)).complete();
        return "Unbanned user " + userId + " from the server";
    }

    /**
     * Times out a member in the Discord server.
     *
     * @param guildId  Optional ID of the Discord server. If not provided, the default server will be used.
     * @param userId   The ID of the user to timeout.
     * @param duration Duration in minutes (max 40320 = 28 days).
     * @param reason   Optional reason for the timeout.
     * @return A confirmation message.
     */
    @Tool(name = "timeout_member", description = "Timeout a member (prevent them from sending messages)")
    public String timeoutMember(@ToolParam(description = "Discord server ID", required = false) String guildId,
                                @ToolParam(description = "Discord user ID") String userId,
                                @ToolParam(description = "Timeout duration in minutes (max 40320 = 28 days)") Integer duration,
                                @ToolParam(description = "Reason for timeout", required = false) String reason) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (duration == null || duration <= 0) {
            throw new IllegalArgumentException("duration must be a positive number");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        Member member = guild.retrieveMemberById(userId).complete();
        if (member == null) {
            throw new IllegalArgumentException("Member not found by userId");
        }

        // Max timeout is 28 days (40320 minutes)
        int cappedDuration = Math.min(duration, 40320);
        member.timeoutFor(Duration.ofMinutes(cappedDuration))
                .reason(reason != null ? reason : "No reason provided")
                .complete();

        return "Timed out " + member.getEffectiveName() + " for " + cappedDuration + " minutes" +
                (reason != null ? ". Reason: " + reason : "");
    }

    /**
     * Removes a timeout from a member.
     *
     * @param guildId Optional ID of the Discord server. If not provided, the default server will be used.
     * @param userId  The ID of the user to remove timeout from.
     * @return A confirmation message.
     */
    @Tool(name = "remove_timeout", description = "Remove timeout from a member")
    public String removeTimeout(@ToolParam(description = "Discord server ID", required = false) String guildId,
                                @ToolParam(description = "Discord user ID") String userId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        Member member = guild.retrieveMemberById(userId).complete();
        if (member == null) {
            throw new IllegalArgumentException("Member not found by userId");
        }

        member.removeTimeout().complete();
        return "Removed timeout from " + member.getEffectiveName();
    }

    /**
     * Lists all banned users in the server.
     *
     * @param guildId Optional ID of the Discord server. If not provided, the default server will be used.
     * @return A formatted string listing all banned users.
     */
    @Tool(name = "list_bans", description = "List all banned users in the server")
    public String listBans(@ToolParam(description = "Discord server ID", required = false) String guildId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        List<Guild.Ban> bans = guild.retrieveBanList().complete();
        if (bans.isEmpty()) {
            return "No banned users in server";
        }

        return "Retrieved " + bans.size() + " banned users:\n" +
                bans.stream()
                        .map(b -> String.format("- %s (ID: %s) - Reason: %s",
                                b.getUser().getName(),
                                b.getUser().getId(),
                                b.getReason() != null ? b.getReason() : "No reason"))
                        .collect(Collectors.joining("\n"));
    }

    /**
     * Gets information about a specific member.
     *
     * @param guildId Optional ID of the Discord server. If not provided, the default server will be used.
     * @param userId  The ID of the user to get info about.
     * @return A formatted string with member information.
     */
    @Tool(name = "get_member_info", description = "Get detailed information about a member")
    public String getMemberInfo(@ToolParam(description = "Discord server ID", required = false) String guildId,
                                @ToolParam(description = "Discord user ID") String userId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        Member member = guild.retrieveMemberById(userId).complete();
        if (member == null) {
            throw new IllegalArgumentException("Member not found by userId");
        }

        User user = member.getUser();
        String roles = member.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.joining(", "));

        StringBuilder info = new StringBuilder();
        info.append("**Member Info:**\n");
        info.append("- Display Name: ").append(member.getEffectiveName()).append("\n");
        info.append("- Username: ").append(user.getName()).append("\n");
        info.append("- ID: ").append(user.getId()).append("\n");
        info.append("- Bot: ").append(user.isBot() ? "Yes" : "No").append("\n");
        info.append("- Joined Server: ").append(member.getTimeJoined()).append("\n");
        info.append("- Account Created: ").append(user.getTimeCreated()).append("\n");
        info.append("- Roles: ").append(roles.isEmpty() ? "None" : roles).append("\n");
        if (member.isTimedOut()) {
            info.append("- Timeout Until: ").append(member.getTimeOutEnd()).append("\n");
        }

        return info.toString();
    }
}
