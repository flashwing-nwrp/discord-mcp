package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VoiceChannelService {

    private final JDA jda;

    @Value("${DISCORD_GUILD_ID:}")
    private String defaultGuildId;

    public VoiceChannelService(JDA jda) {
        this.jda = jda;
    }

    private String resolveGuildId(String guildId) {
        if ((guildId == null || guildId.isEmpty()) && defaultGuildId != null && !defaultGuildId.isEmpty()) {
            return defaultGuildId;
        }
        return guildId;
    }

    /**
     * Creates a new voice channel in a specified Discord server.
     *
     * @param guildId    Optional ID of the Discord server. If not provided, the default server will be used.
     * @param name       The name for the new voice channel.
     * @param categoryId Optional ID of the category to place the channel in.
     * @param userLimit  Optional maximum number of users (0 = unlimited).
     * @param bitrate    Optional audio bitrate in kbps (8-384, server boost dependent).
     * @return A confirmation message with the channel details.
     */
    @Tool(name = "create_voice_channel", description = "Create a new voice channel")
    public String createVoiceChannel(@ToolParam(description = "Discord server ID", required = false) String guildId,
                                     @ToolParam(description = "Voice channel name") String name,
                                     @ToolParam(description = "Category ID", required = false) String categoryId,
                                     @ToolParam(description = "User limit (0 = unlimited)", required = false) Integer userLimit,
                                     @ToolParam(description = "Audio bitrate in kbps (8-384)", required = false) Integer bitrate) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        VoiceChannel voiceChannel;
        if (categoryId != null && !categoryId.isEmpty()) {
            Category category = guild.getCategoryById(categoryId);
            if (category == null) {
                throw new IllegalArgumentException("Category not found by categoryId");
            }
            var action = category.createVoiceChannel(name);
            if (userLimit != null && userLimit >= 0) {
                action = action.setUserlimit(userLimit);
            }
            if (bitrate != null && bitrate >= 8) {
                action = action.setBitrate(Math.min(bitrate, 384) * 1000);
            }
            voiceChannel = action.complete();
            return "Created voice channel: " + voiceChannel.getName() + " (ID: " + voiceChannel.getId() + ") in category: " + category.getName();
        } else {
            var action = guild.createVoiceChannel(name);
            if (userLimit != null && userLimit >= 0) {
                action = action.setUserlimit(userLimit);
            }
            if (bitrate != null && bitrate >= 8) {
                action = action.setBitrate(Math.min(bitrate, 384) * 1000);
            }
            voiceChannel = action.complete();
            return "Created voice channel: " + voiceChannel.getName() + " (ID: " + voiceChannel.getId() + ")";
        }
    }

    /**
     * Deletes a voice channel.
     *
     * @param channelId The ID of the voice channel to delete.
     * @return A confirmation message.
     */
    @Tool(name = "delete_voice_channel", description = "Delete a voice channel")
    public String deleteVoiceChannel(@ToolParam(description = "Discord voice channel ID") String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        VoiceChannel channel = jda.getVoiceChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Voice channel not found by channelId");
        }

        String channelName = channel.getName();
        channel.delete().complete();
        return "Deleted voice channel: " + channelName;
    }

    /**
     * Lists all voice channels in a server.
     *
     * @param guildId Optional ID of the Discord server. If not provided, the default server will be used.
     * @return A formatted string listing all voice channels.
     */
    @Tool(name = "list_voice_channels", description = "List all voice channels in the server")
    public String listVoiceChannels(@ToolParam(description = "Discord server ID", required = false) String guildId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        List<VoiceChannel> channels = guild.getVoiceChannels();
        if (channels.isEmpty()) {
            return "No voice channels found in server";
        }

        return "Retrieved " + channels.size() + " voice channels:\n" +
                channels.stream()
                        .map(c -> String.format("- %s (ID: %s) [Users: %d/%s, Bitrate: %dkbps]",
                                c.getName(),
                                c.getId(),
                                c.getMembers().size(),
                                c.getUserLimit() == 0 ? "∞" : c.getUserLimit(),
                                c.getBitrate() / 1000))
                        .collect(Collectors.joining("\n"));
    }

    /**
     * Gets members currently in a voice channel.
     *
     * @param channelId The ID of the voice channel.
     * @return A formatted string listing members in the voice channel.
     */
    @Tool(name = "get_voice_members", description = "Get members currently in a voice channel")
    public String getVoiceMembers(@ToolParam(description = "Discord voice channel ID") String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        VoiceChannel channel = jda.getVoiceChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Voice channel not found by channelId");
        }

        List<Member> members = channel.getMembers();
        if (members.isEmpty()) {
            return "No members in voice channel: " + channel.getName();
        }

        return "Members in " + channel.getName() + " (" + members.size() + "):\n" +
                members.stream()
                        .map(m -> String.format("- %s (ID: %s)%s%s",
                                m.getEffectiveName(),
                                m.getId(),
                                m.getVoiceState() != null && m.getVoiceState().isMuted() ? " [Muted]" : "",
                                m.getVoiceState() != null && m.getVoiceState().isDeafened() ? " [Deafened]" : ""))
                        .collect(Collectors.joining("\n"));
    }

    /**
     * Moves a member to a different voice channel.
     *
     * @param guildId   Optional ID of the Discord server. If not provided, the default server will be used.
     * @param userId    The ID of the user to move.
     * @param channelId The ID of the target voice channel.
     * @return A confirmation message.
     */
    @Tool(name = "move_member", description = "Move a member to a different voice channel")
    public String moveMember(@ToolParam(description = "Discord server ID", required = false) String guildId,
                             @ToolParam(description = "Discord user ID") String userId,
                             @ToolParam(description = "Target voice channel ID") String channelId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        Member member = guild.retrieveMemberById(userId).complete();
        if (member == null) {
            throw new IllegalArgumentException("Member not found by userId");
        }
        VoiceChannel channel = jda.getVoiceChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Voice channel not found by channelId");
        }

        guild.moveVoiceMember(member, channel).complete();
        return "Moved " + member.getEffectiveName() + " to " + channel.getName();
    }

    /**
     * Disconnects a member from voice.
     *
     * @param guildId Optional ID of the Discord server. If not provided, the default server will be used.
     * @param userId  The ID of the user to disconnect.
     * @return A confirmation message.
     */
    @Tool(name = "disconnect_member", description = "Disconnect a member from voice")
    public String disconnectMember(@ToolParam(description = "Discord server ID", required = false) String guildId,
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

        guild.kickVoiceMember(member).complete();
        return "Disconnected " + member.getEffectiveName() + " from voice";
    }

    /**
     * Server mutes a member in voice.
     *
     * @param guildId Optional ID of the Discord server. If not provided, the default server will be used.
     * @param userId  The ID of the user to mute.
     * @param mute    Whether to mute or unmute.
     * @return A confirmation message.
     */
    @Tool(name = "server_mute_member", description = "Server mute/unmute a member in voice")
    public String serverMuteMember(@ToolParam(description = "Discord server ID", required = false) String guildId,
                                   @ToolParam(description = "Discord user ID") String userId,
                                   @ToolParam(description = "Mute (true) or unmute (false)") Boolean mute) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (mute == null) {
            throw new IllegalArgumentException("mute cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        Member member = guild.retrieveMemberById(userId).complete();
        if (member == null) {
            throw new IllegalArgumentException("Member not found by userId");
        }

        guild.mute(member, mute).complete();
        return (mute ? "Server muted " : "Server unmuted ") + member.getEffectiveName();
    }

    /**
     * Server deafens a member in voice.
     *
     * @param guildId Optional ID of the Discord server. If not provided, the default server will be used.
     * @param userId  The ID of the user to deafen.
     * @param deafen  Whether to deafen or undeafen.
     * @return A confirmation message.
     */
    @Tool(name = "server_deafen_member", description = "Server deafen/undeafen a member in voice")
    public String serverDeafenMember(@ToolParam(description = "Discord server ID", required = false) String guildId,
                                     @ToolParam(description = "Discord user ID") String userId,
                                     @ToolParam(description = "Deafen (true) or undeafen (false)") Boolean deafen) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (deafen == null) {
            throw new IllegalArgumentException("deafen cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        Member member = guild.retrieveMemberById(userId).complete();
        if (member == null) {
            throw new IllegalArgumentException("Member not found by userId");
        }

        guild.deafen(member, deafen).complete();
        return (deafen ? "Server deafened " : "Server undeafened ") + member.getEffectiveName();
    }
}
