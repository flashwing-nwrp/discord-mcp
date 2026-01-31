package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final JDA jda;

    @Value("${DISCORD_GUILD_ID:}")
    private String defaultGuildId;

    public RoleService(JDA jda) {
        this.jda = jda;
    }

    private String resolveGuildId(String guildId) {
        if ((guildId == null || guildId.isEmpty()) && defaultGuildId != null && !defaultGuildId.isEmpty()) {
            return defaultGuildId;
        }
        return guildId;
    }

    /**
     * Creates a new role in a specified Discord server.
     *
     * @param guildId     Optional ID of the Discord server. If not provided, the default server will be used.
     * @param name        The name for the new role.
     * @param colorHex    Optional hex color code (e.g., "#FF5733" or "FF5733").
     * @param hoisted     Optional whether the role should be displayed separately in member list.
     * @param mentionable Optional whether the role can be mentioned.
     * @return A confirmation message with the name and ID of the created role.
     */
    @Tool(name = "create_role", description = "Create a new role in the server")
    public String createRole(@ToolParam(description = "Discord server ID", required = false) String guildId,
                             @ToolParam(description = "Role name") String name,
                             @ToolParam(description = "Hex color code (e.g., #FF5733)", required = false) String colorHex,
                             @ToolParam(description = "Display role separately in member list", required = false) Boolean hoisted,
                             @ToolParam(description = "Allow role to be mentioned", required = false) Boolean mentionable) {
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

        var roleAction = guild.createRole().setName(name);

        if (colorHex != null && !colorHex.isEmpty()) {
            String hex = colorHex.startsWith("#") ? colorHex.substring(1) : colorHex;
            roleAction = roleAction.setColor(Color.decode("#" + hex));
        }
        if (hoisted != null) {
            roleAction = roleAction.setHoisted(hoisted);
        }
        if (mentionable != null) {
            roleAction = roleAction.setMentionable(mentionable);
        }

        Role role = roleAction.complete();
        return "Created role: " + role.getName() + " (ID: " + role.getId() + ")";
    }

    /**
     * Deletes a role from a specified Discord server.
     *
     * @param guildId Optional ID of the Discord server. If not provided, the default server will be used.
     * @param roleId  The ID of the role to be deleted.
     * @return A confirmation message with the name of the deleted role.
     */
    @Tool(name = "delete_role", description = "Delete a role from the server")
    public String deleteRole(@ToolParam(description = "Discord server ID", required = false) String guildId,
                             @ToolParam(description = "Discord role ID") String roleId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (roleId == null || roleId.isEmpty()) {
            throw new IllegalArgumentException("roleId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Role not found by roleId");
        }

        String roleName = role.getName();
        role.delete().complete();
        return "Deleted role: " + roleName;
    }

    /**
     * Assigns a role to a member in a specified Discord server.
     *
     * @param guildId  Optional ID of the Discord server. If not provided, the default server will be used.
     * @param userId   The ID of the user to assign the role to.
     * @param roleId   The ID of the role to assign.
     * @return A confirmation message.
     */
    @Tool(name = "assign_role", description = "Assign a role to a member")
    public String assignRole(@ToolParam(description = "Discord server ID", required = false) String guildId,
                             @ToolParam(description = "Discord user ID") String userId,
                             @ToolParam(description = "Discord role ID") String roleId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (roleId == null || roleId.isEmpty()) {
            throw new IllegalArgumentException("roleId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        Member member = guild.retrieveMemberById(userId).complete();
        if (member == null) {
            throw new IllegalArgumentException("Member not found by userId");
        }
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Role not found by roleId");
        }

        guild.addRoleToMember(member, role).complete();
        return "Assigned role " + role.getName() + " to " + member.getEffectiveName();
    }

    /**
     * Removes a role from a member in a specified Discord server.
     *
     * @param guildId  Optional ID of the Discord server. If not provided, the default server will be used.
     * @param userId   The ID of the user to remove the role from.
     * @param roleId   The ID of the role to remove.
     * @return A confirmation message.
     */
    @Tool(name = "remove_role", description = "Remove a role from a member")
    public String removeRole(@ToolParam(description = "Discord server ID", required = false) String guildId,
                             @ToolParam(description = "Discord user ID") String userId,
                             @ToolParam(description = "Discord role ID") String roleId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (roleId == null || roleId.isEmpty()) {
            throw new IllegalArgumentException("roleId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        Member member = guild.retrieveMemberById(userId).complete();
        if (member == null) {
            throw new IllegalArgumentException("Member not found by userId");
        }
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Role not found by roleId");
        }

        guild.removeRoleFromMember(member, role).complete();
        return "Removed role " + role.getName() + " from " + member.getEffectiveName();
    }

    /**
     * Lists all roles in a specified Discord server.
     *
     * @param guildId Optional ID of the Discord server. If not provided, the default server will be used.
     * @return A formatted string listing all roles in the server.
     */
    @Tool(name = "list_roles", description = "List all roles in the server")
    public String listRoles(@ToolParam(description = "Discord server ID", required = false) String guildId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        List<Role> roles = guild.getRoles();
        if (roles.isEmpty()) {
            return "No roles found in server";
        }

        return "Retrieved " + roles.size() + " roles:\n" +
                roles.stream()
                        .map(r -> String.format("- %s (ID: %s) [Members: %d, Color: %s]",
                                r.getName(),
                                r.getId(),
                                guild.getMembersWithRoles(r).size(),
                                r.getColor() != null ? String.format("#%06X", r.getColor().getRGB() & 0xFFFFFF) : "none"))
                        .collect(Collectors.joining("\n"));
    }

    /**
     * Finds a role by name in a specified Discord server.
     *
     * @param guildId  Optional ID of the Discord server. If not provided, the default server will be used.
     * @param roleName The name of the role to find.
     * @return A formatted string with the role details.
     */
    @Tool(name = "find_role", description = "Find a role by name")
    public String findRole(@ToolParam(description = "Discord server ID", required = false) String guildId,
                           @ToolParam(description = "Role name to search for") String roleName) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (roleName == null || roleName.isEmpty()) {
            throw new IllegalArgumentException("roleName cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        List<Role> roles = guild.getRolesByName(roleName, true);
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("No roles found with name: " + roleName);
        }

        if (roles.size() == 1) {
            Role role = roles.get(0);
            return String.format("Found role: %s (ID: %s) [Members: %d, Color: %s, Position: %d]",
                    role.getName(),
                    role.getId(),
                    guild.getMembersWithRoles(role).size(),
                    role.getColor() != null ? String.format("#%06X", role.getColor().getRGB() & 0xFFFFFF) : "none",
                    role.getPosition());
        }

        return "Found " + roles.size() + " roles matching '" + roleName + "':\n" +
                roles.stream()
                        .map(r -> String.format("- %s (ID: %s)", r.getName(), r.getId()))
                        .collect(Collectors.joining("\n"));
    }

    /**
     * Updates an existing role's properties.
     *
     * @param guildId     Optional ID of the Discord server. If not provided, the default server will be used.
     * @param roleId      The ID of the role to update.
     * @param name        Optional new name for the role.
     * @param colorHex    Optional new hex color code.
     * @param hoisted     Optional whether the role should be displayed separately.
     * @param mentionable Optional whether the role can be mentioned.
     * @return A confirmation message.
     */
    @Tool(name = "update_role", description = "Update a role's properties (name, color, hoisted, mentionable)")
    public String updateRole(@ToolParam(description = "Discord server ID", required = false) String guildId,
                             @ToolParam(description = "Discord role ID") String roleId,
                             @ToolParam(description = "New role name", required = false) String name,
                             @ToolParam(description = "New hex color code (e.g., #FF5733)", required = false) String colorHex,
                             @ToolParam(description = "Display role separately in member list", required = false) Boolean hoisted,
                             @ToolParam(description = "Allow role to be mentioned", required = false) Boolean mentionable) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (roleId == null || roleId.isEmpty()) {
            throw new IllegalArgumentException("roleId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Role not found by roleId");
        }

        var manager = role.getManager();
        StringBuilder changes = new StringBuilder();

        if (name != null && !name.isEmpty()) {
            manager = manager.setName(name);
            changes.append("name to '").append(name).append("', ");
        }
        if (colorHex != null && !colorHex.isEmpty()) {
            String hex = colorHex.startsWith("#") ? colorHex.substring(1) : colorHex;
            manager = manager.setColor(Color.decode("#" + hex));
            changes.append("color to ").append(colorHex).append(", ");
        }
        if (hoisted != null) {
            manager = manager.setHoisted(hoisted);
            changes.append("hoisted to ").append(hoisted).append(", ");
        }
        if (mentionable != null) {
            manager = manager.setMentionable(mentionable);
            changes.append("mentionable to ").append(mentionable).append(", ");
        }

        if (changes.length() == 0) {
            return "No changes specified for role: " + role.getName();
        }

        manager.complete();
        return "Updated role " + role.getName() + ": " + changes.substring(0, changes.length() - 2);
    }

    /**
     * Gets all members with a specific role.
     *
     * @param guildId Optional ID of the Discord server. If not provided, the default server will be used.
     * @param roleId  The ID of the role to query.
     * @return A formatted string listing all members with the role.
     */
    @Tool(name = "get_members_by_role", description = "Get all members with a specific role")
    public String getMembersByRole(@ToolParam(description = "Discord server ID", required = false) String guildId,
                                   @ToolParam(description = "Discord role ID") String roleId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (roleId == null || roleId.isEmpty()) {
            throw new IllegalArgumentException("roleId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Role not found by roleId");
        }

        List<Member> members = guild.getMembersWithRoles(role);
        if (members.isEmpty()) {
            return "No members found with role: " + role.getName();
        }

        return "Found " + members.size() + " members with role " + role.getName() + ":\n" +
                members.stream()
                        .map(m -> String.format("- %s (%s) [ID: %s]",
                                m.getEffectiveName(),
                                m.getUser().getName(),
                                m.getId()))
                        .collect(Collectors.joining("\n"));
    }
}
