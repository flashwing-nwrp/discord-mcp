package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    private final JDA jda;

    @Value("${DISCORD_GUILD_ID:}")
    private String defaultGuildId;

    // Common permission sets for FiveM servers
    private static final Map<String, EnumSet<Permission>> PERMISSION_PRESETS = new LinkedHashMap<>();
    static {
        PERMISSION_PRESETS.put("staff_text", EnumSet.of(
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_HISTORY,
                Permission.MESSAGE_ATTACH_FILES,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_MANAGE,
                Permission.MESSAGE_MENTION_EVERYONE
        ));
        PERMISSION_PRESETS.put("staff_voice", EnumSet.of(
                Permission.VIEW_CHANNEL,
                Permission.VOICE_CONNECT,
                Permission.VOICE_SPEAK,
                Permission.VOICE_STREAM,
                Permission.VOICE_MUTE_OTHERS,
                Permission.VOICE_DEAF_OTHERS,
                Permission.VOICE_MOVE_OTHERS
        ));
        PERMISSION_PRESETS.put("member_text", EnumSet.of(
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_HISTORY,
                Permission.MESSAGE_ATTACH_FILES,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_ADD_REACTION
        ));
        PERMISSION_PRESETS.put("member_voice", EnumSet.of(
                Permission.VIEW_CHANNEL,
                Permission.VOICE_CONNECT,
                Permission.VOICE_SPEAK,
                Permission.VOICE_STREAM
        ));
        PERMISSION_PRESETS.put("readonly", EnumSet.of(
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_HISTORY
        ));
        PERMISSION_PRESETS.put("hidden", EnumSet.noneOf(Permission.class));
    }

    public PermissionService(JDA jda) {
        this.jda = jda;
    }

    private String resolveGuildId(String guildId) {
        if ((guildId == null || guildId.isEmpty()) && defaultGuildId != null && !defaultGuildId.isEmpty()) {
            return defaultGuildId;
        }
        return guildId;
    }

    /**
     * Sets channel permissions for a role using a preset or specific permissions.
     *
     * @param channelId   The ID of the channel to modify.
     * @param roleId      The ID of the role to set permissions for.
     * @param preset      Optional preset: staff_text, staff_voice, member_text, member_voice, readonly, hidden.
     * @param allow       Optional comma-separated permissions to allow.
     * @param deny        Optional comma-separated permissions to deny.
     * @return A confirmation message.
     */
    @Tool(name = "set_channel_permissions", description = "Set channel permissions for a role (use presets or specific permissions)")
    public String setChannelPermissions(@ToolParam(description = "Discord channel ID") String channelId,
                                        @ToolParam(description = "Discord role ID") String roleId,
                                        @ToolParam(description = "Preset: staff_text, staff_voice, member_text, member_voice, readonly, hidden", required = false) String preset,
                                        @ToolParam(description = "Comma-separated permissions to ALLOW (e.g., VIEW_CHANNEL,MESSAGE_SEND)", required = false) String allow,
                                        @ToolParam(description = "Comma-separated permissions to DENY", required = false) String deny) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (roleId == null || roleId.isEmpty()) {
            throw new IllegalArgumentException("roleId cannot be null");
        }

        GuildChannel channel = jda.getGuildChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        Guild guild = channel.getGuild();
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Role not found by roleId");
        }

        EnumSet<Permission> allowPerms = EnumSet.noneOf(Permission.class);
        EnumSet<Permission> denyPerms = EnumSet.noneOf(Permission.class);

        // Apply preset if provided
        if (preset != null && !preset.isEmpty()) {
            EnumSet<Permission> presetPerms = PERMISSION_PRESETS.get(preset.toLowerCase());
            if (presetPerms != null) {
                allowPerms.addAll(presetPerms);
                // For hidden preset, deny view channel
                if (preset.equalsIgnoreCase("hidden")) {
                    denyPerms.add(Permission.VIEW_CHANNEL);
                }
            } else {
                throw new IllegalArgumentException("Unknown preset: " + preset + ". Available: " +
                        String.join(", ", PERMISSION_PRESETS.keySet()));
            }
        }

        // Parse custom allow permissions
        if (allow != null && !allow.isEmpty()) {
            for (String perm : allow.split(",")) {
                try {
                    allowPerms.add(Permission.valueOf(perm.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Skip invalid permission names
                }
            }
        }

        // Parse custom deny permissions
        if (deny != null && !deny.isEmpty()) {
            for (String perm : deny.split(",")) {
                try {
                    denyPerms.add(Permission.valueOf(perm.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Skip invalid permission names
                }
            }
        }

        channel.getPermissionContainer()
                .upsertPermissionOverride(role)
                .setAllowed(allowPerms)
                .setDenied(denyPerms)
                .complete();

        return "Set permissions for role " + role.getName() + " in #" + channel.getName() +
                "\nAllowed: " + (allowPerms.isEmpty() ? "None" : allowPerms.stream().map(Permission::getName).collect(Collectors.joining(", "))) +
                "\nDenied: " + (denyPerms.isEmpty() ? "None" : denyPerms.stream().map(Permission::getName).collect(Collectors.joining(", ")));
    }

    /**
     * Sets channel permissions for a specific member.
     *
     * @param channelId The ID of the channel to modify.
     * @param userId    The ID of the user to set permissions for.
     * @param allow     Comma-separated permissions to allow.
     * @param deny      Comma-separated permissions to deny.
     * @return A confirmation message.
     */
    @Tool(name = "set_member_channel_permissions", description = "Set channel permissions for a specific member")
    public String setMemberChannelPermissions(@ToolParam(description = "Discord channel ID") String channelId,
                                              @ToolParam(description = "Discord user ID") String userId,
                                              @ToolParam(description = "Comma-separated permissions to ALLOW", required = false) String allow,
                                              @ToolParam(description = "Comma-separated permissions to DENY", required = false) String deny) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        GuildChannel channel = jda.getGuildChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        Guild guild = channel.getGuild();
        Member member = guild.retrieveMemberById(userId).complete();
        if (member == null) {
            throw new IllegalArgumentException("Member not found by userId");
        }

        EnumSet<Permission> allowPerms = EnumSet.noneOf(Permission.class);
        EnumSet<Permission> denyPerms = EnumSet.noneOf(Permission.class);

        if (allow != null && !allow.isEmpty()) {
            for (String perm : allow.split(",")) {
                try {
                    allowPerms.add(Permission.valueOf(perm.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Skip invalid
                }
            }
        }

        if (deny != null && !deny.isEmpty()) {
            for (String perm : deny.split(",")) {
                try {
                    denyPerms.add(Permission.valueOf(perm.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Skip invalid
                }
            }
        }

        channel.getPermissionContainer()
                .upsertPermissionOverride(member)
                .setAllowed(allowPerms)
                .setDenied(denyPerms)
                .complete();

        return "Set permissions for " + member.getEffectiveName() + " in #" + channel.getName();
    }

    /**
     * Removes all permission overrides for a role from a channel.
     *
     * @param channelId The ID of the channel.
     * @param roleId    The ID of the role.
     * @return A confirmation message.
     */
    @Tool(name = "clear_role_permissions", description = "Remove all permission overrides for a role from a channel")
    public String clearRolePermissions(@ToolParam(description = "Discord channel ID") String channelId,
                                       @ToolParam(description = "Discord role ID") String roleId) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (roleId == null || roleId.isEmpty()) {
            throw new IllegalArgumentException("roleId cannot be null");
        }

        GuildChannel channel = jda.getGuildChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        Guild guild = channel.getGuild();
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Role not found by roleId");
        }

        var override = channel.getPermissionContainer().getPermissionOverride(role);
        if (override != null) {
            override.delete().complete();
            return "Cleared permission overrides for role " + role.getName() + " in #" + channel.getName();
        }
        return "No permission overrides found for role " + role.getName() + " in #" + channel.getName();
    }

    /**
     * Gets current permission overrides for a channel.
     *
     * @param channelId The ID of the channel.
     * @return A formatted string listing all permission overrides.
     */
    @Tool(name = "get_channel_permissions", description = "Get current permission overrides for a channel")
    public String getChannelPermissions(@ToolParam(description = "Discord channel ID") String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        GuildChannel channel = jda.getGuildChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        var overrides = channel.getPermissionContainer().getPermissionOverrides();
        if (overrides.isEmpty()) {
            return "No permission overrides for #" + channel.getName();
        }

        StringBuilder result = new StringBuilder();
        result.append("**Permission Overrides for #").append(channel.getName()).append(":**\n\n");

        for (var override : overrides) {
            if (override.isRoleOverride()) {
                result.append("**Role: ").append(override.getRole().getName()).append("**\n");
            } else {
                result.append("**Member: ").append(override.getMember().getEffectiveName()).append("**\n");
            }

            if (!override.getAllowed().isEmpty()) {
                result.append("  ✅ Allowed: ").append(
                        override.getAllowed().stream()
                                .map(Permission::getName)
                                .collect(Collectors.joining(", "))
                ).append("\n");
            }
            if (!override.getDenied().isEmpty()) {
                result.append("  ❌ Denied: ").append(
                        override.getDenied().stream()
                                .map(Permission::getName)
                                .collect(Collectors.joining(", "))
                ).append("\n");
            }
            result.append("\n");
        }

        return result.toString();
    }

    /**
     * Syncs channel permissions with its category.
     *
     * @param channelId The ID of the channel to sync.
     * @return A confirmation message.
     */
    @Tool(name = "sync_channel_permissions", description = "Sync channel permissions with its parent category")
    public String syncChannelPermissions(@ToolParam(description = "Discord channel ID") String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        GuildChannel channel = jda.getGuildChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        // Check if channel supports categories
        if (!(channel instanceof net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel)) {
            throw new IllegalArgumentException("This channel type does not support categories");
        }

        var categorizableChannel = (net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel) channel;
        var category = categorizableChannel.getParentCategory();

        if (category == null) {
            throw new IllegalArgumentException("Channel has no parent category to sync with");
        }

        var permContainer = channel.getPermissionContainer();

        // Clear existing overrides
        for (var override : permContainer.getPermissionOverrides()) {
            override.delete().complete();
        }

        // Copy category overrides
        for (var catOverride : category.getPermissionOverrides()) {
            if (catOverride.isRoleOverride()) {
                permContainer.upsertPermissionOverride(catOverride.getRole())
                        .setAllowed(catOverride.getAllowed())
                        .setDenied(catOverride.getDenied())
                        .complete();
            } else {
                permContainer.upsertPermissionOverride(catOverride.getMember())
                        .setAllowed(catOverride.getAllowed())
                        .setDenied(catOverride.getDenied())
                        .complete();
            }
        }

        return "Synced #" + channel.getName() + " permissions with category: " + category.getName();
    }

    /**
     * Lists available permission names.
     *
     * @return A list of all Discord permission names.
     */
    @Tool(name = "list_permissions", description = "List all available Discord permission names")
    public String listPermissions() {
        StringBuilder result = new StringBuilder();
        result.append("**Available Permissions:**\n\n");

        result.append("**Text Channels:**\n");
        result.append("VIEW_CHANNEL, MESSAGE_SEND, MESSAGE_SEND_IN_THREADS, CREATE_PUBLIC_THREADS, CREATE_PRIVATE_THREADS, ");
        result.append("MESSAGE_EMBED_LINKS, MESSAGE_ATTACH_FILES, MESSAGE_ADD_REACTION, MESSAGE_EXT_EMOJI, MESSAGE_EXT_STICKER, ");
        result.append("MESSAGE_MENTION_EVERYONE, MESSAGE_MANAGE, MESSAGE_HISTORY, MESSAGE_TTS, USE_APPLICATION_COMMANDS\n\n");

        result.append("**Voice Channels:**\n");
        result.append("VOICE_CONNECT, VOICE_SPEAK, VOICE_STREAM, VOICE_USE_VAD, PRIORITY_SPEAKER, ");
        result.append("VOICE_MUTE_OTHERS, VOICE_DEAF_OTHERS, VOICE_MOVE_OTHERS, VOICE_USE_SOUNDBOARD, ");
        result.append("VOICE_USE_EXTERNAL_SOUNDS, VOICE_START_ACTIVITIES\n\n");

        result.append("**Management:**\n");
        result.append("MANAGE_CHANNEL, MANAGE_PERMISSIONS, MANAGE_WEBHOOKS, MANAGE_THREADS, MANAGE_EVENTS\n\n");

        result.append("**Available Presets:**\n");
        for (Map.Entry<String, EnumSet<Permission>> entry : PERMISSION_PRESETS.entrySet()) {
            result.append("- **").append(entry.getKey()).append("**: ");
            result.append(entry.getValue().stream()
                    .map(Permission::getName)
                    .collect(Collectors.joining(", ")));
            result.append("\n");
        }

        return result.toString();
    }

    /**
     * Locks a channel (prevents everyone from sending messages).
     *
     * @param guildId   Optional ID of the Discord server.
     * @param channelId The ID of the channel to lock.
     * @param reason    Optional reason for the lock.
     * @return A confirmation message.
     */
    @Tool(name = "lock_channel", description = "Lock a channel (prevent @everyone from sending messages)")
    public String lockChannel(@ToolParam(description = "Discord server ID", required = false) String guildId,
                              @ToolParam(description = "Discord channel ID") String channelId,
                              @ToolParam(description = "Reason for lock", required = false) String reason) {
        guildId = resolveGuildId(guildId);
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        GuildChannel channel = jda.getGuildChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        Guild guild = channel.getGuild();
        Role everyoneRole = guild.getPublicRole();

        channel.getPermissionContainer()
                .upsertPermissionOverride(everyoneRole)
                .deny(Permission.MESSAGE_SEND, Permission.MESSAGE_ADD_REACTION)
                .complete();

        return "🔒 Locked #" + channel.getName() + (reason != null ? " - " + reason : "");
    }

    /**
     * Unlocks a channel (allows everyone to send messages again).
     *
     * @param guildId   Optional ID of the Discord server.
     * @param channelId The ID of the channel to unlock.
     * @return A confirmation message.
     */
    @Tool(name = "unlock_channel", description = "Unlock a channel (allow @everyone to send messages)")
    public String unlockChannel(@ToolParam(description = "Discord server ID", required = false) String guildId,
                                @ToolParam(description = "Discord channel ID") String channelId) {
        guildId = resolveGuildId(guildId);
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        GuildChannel channel = jda.getGuildChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        Guild guild = channel.getGuild();
        Role everyoneRole = guild.getPublicRole();

        var override = channel.getPermissionContainer().getPermissionOverride(everyoneRole);
        if (override != null) {
            // Clear the deny for MESSAGE_SEND
            override.getManager()
                    .clear(Permission.MESSAGE_SEND, Permission.MESSAGE_ADD_REACTION)
                    .complete();
        }

        return "🔓 Unlocked #" + channel.getName();
    }
}
