<div align="center">
  <img src="assets/img/Discord_MCP_full_logo.svg" width="60%" alt="DeepSeek-V3" />
</div>
<hr>
<div align="center" style="line-height: 1;">
    <a href="https://github.com/modelcontextprotocol/servers" target="_blank" style="margin: 2px;">
        <img alt="MCP Server" src="https://badge.mcpx.dev?type=server" style="display: inline-block; vertical-align: middle;"/>
    </a>
    <a href="https://github.com/flashwing-nwrp/discord-mcp/blob/main/LICENSE" target="_blank" style="margin: 2px;">
        <img alt="MIT License" src="https://img.shields.io/github/license/flashwing-nwrp/discord-mcp" style="display: inline-block; vertical-align: middle;"/>
    </a>
</div>

## About This Fork

This is a **FiveM-focused fork** of [SaseQ/discord-mcp](https://github.com/SaseQ/discord-mcp) with extended functionality for managing FiveM roleplay server Discord communities. Added features include:

- **Role Management** - Create, assign, remove, and manage roles
- **Moderation Tools** - Kick, ban, timeout, and member management
- **Rich Embeds** - Announcements, embeds with fields, standardized alerts
- **Interactive Components** - Buttons, select menus, role panels, ticket systems
- **Thread Management** - Create, archive, and manage threads
- **Voice Channels** - Create, manage, and control voice channels
- **Permission Management** - Channel permissions with presets for staff/members
- **Log Analysis** - Scan webhook logs for cheating and suspicious activity

## 📖 Description

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io/introduction) server for the Discord API [(JDA)](https://jda.wiki/),
allowing seamless integration of Discord Bot with MCP-compatible applications like Claude Desktop.

Enable your AI assistants to seamlessly interact with Discord. Manage channels, send messages, and retrieve server information effortlessly. Enhance your Discord experience with powerful automation capabilities.


## 🔬 Installation

### ► 🐳 Docker Installation (Recommended)
> NOTE: Docker installation is required. Full instructions can be found on [docker.com](https://www.docker.com/products/docker-desktop/).

Build and run locally:
```bash
git clone https://github.com/flashwing-nwrp/discord-mcp
cd discord-mcp
docker build -t discord-mcp .
```

Add to your MCP client configuration:
```json
{
  "mcpServers": {
    "discord-mcp": {
      "command": "docker",
      "args": [
        "run", "--rm", "-i",
        "-e", "DISCORD_TOKEN=<YOUR_DISCORD_BOT_TOKEN>",
        "-e", "DISCORD_GUILD_ID=<YOUR_SERVER_ID>",
        "discord-mcp"
      ]
    }
  }
}
```

<details>
    <summary style="font-size: 1.35em; font-weight: bold;">
        🔧 Manual Installation
    </summary>

#### Clone the repository
```bash
git clone https://github.com/flashwing-nwrp/discord-mcp
```

#### Build the project
> NOTE: Maven installation is required to use the mvn command. Full instructions can be found [here](https://www.baeldung.com/install-maven-on-windows-linux-mac).
```bash
cd discord-mcp
mvn clean package # The jar file will be available in the /target directory
```

#### Configure AI client
Many code editors and other AI clients use a configuration file to manage MCP servers.

The Discord MPC server can be configured by adding the following to your configuration file.

> NOTE: You will need to create a Discord Bot token to use this server. Instructions on how to create a Discord Bot token can be found [here](https://discordjs.guide/preparations/setting-up-a-bot-application.html#creating-your-bot).
```json
{
  "mcpServers": {
    "discord-mcp": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/discord-mcp-0.0.1-SNAPSHOT.jar"
      ],
      "env": {
        "DISCORD_TOKEN": "YOUR_DISCORD_BOT_TOKEN",
        "DISCORD_GUILD_ID": "YOUR_SERVER_ID"
      }
    }
  }
}
```
The `DISCORD_GUILD_ID` environment variable is optional. When provided, it sets a default Discord server ID so any tool that accepts a `guildId` parameter can omit it.

</details>

<details>
    <summary style="font-size: 1.35em; font-weight: bold;">
        ⌨️ Claude Code Installation
    </summary>

Run this command. See [Claude Code MCP docs](https://docs.anthropic.com/en/docs/agents-and-tools/claude-code/tutorials#set-up-model-context-protocol-mcp) for more info.
```bash
claude mcp add discord-mcp -- docker run --rm -i -e DISCORD_TOKEN=<YOUR_DISCORD_BOT_TOKEN> -e DISCORD_GUILD_ID=<YOUR_SERVER_ID> discord-mcp
```

</details>

## 🤖 Required Bot Permissions

When creating your Discord bot, ensure it has these permissions:

| Category | Permissions |
|----------|-------------|
| **General** | Manage Channels, Manage Roles, View Channels |
| **Text** | Send Messages, Manage Messages, Embed Links, Attach Files, Read Message History, Mention Everyone, Add Reactions |
| **Voice** | Connect, Speak, Mute Members, Deafen Members, Move Members |
| **Moderation** | Kick Members, Ban Members, Moderate Members (timeout) |
| **Advanced** | Manage Webhooks, Manage Threads |

**Privileged Gateway Intents** (enable in Discord Developer Portal):
- Server Members Intent
- Message Content Intent

## 🛠️ Available Tools

### Server Information
- `get_server_info` - Get detailed discord server information

### User Management
- `get_user_id_by_name` - Get a Discord user's ID by username for ping usage
- `send_private_message` - Send a private message to a specific user
- `edit_private_message` - Edit a private message from a specific user
- `delete_private_message` - Delete a private message from a specific user
- `read_private_messages` - Read recent message history from a specific user

### Message Management
- `send_message` - Send a message to a specific channel
- `edit_message` - Edit a message from a specific channel
- `delete_message` - Delete a message from a specific channel
- `read_messages` - Read recent message history from a specific channel
- `add_reaction` - Add a reaction (emoji) to a specific message
- `remove_reaction` - Remove a specified reaction (emoji) from a message

### Channel Management
- `create_text_channel` - Create a text channel
- `delete_channel` - Delete a channel
- `find_channel` - Find a channel type and ID using name and server ID
- `list_channels` - List all channels

### Category Management
- `create_category` - Create a new category for channels
- `delete_category` - Delete a category
- `find_category` - Find a category ID using name and server ID
- `list_channels_in_category` - List channels in a specific category

### Webhook Management
- `create_webhook` - Create a new webhook on a specific channel
- `delete_webhook` - Delete a webhook
- `list_webhooks` - List webhooks on a specific channel
- `send_webhook_message` - Send a message via webhook

### Role Management (NEW)
- `create_role` - Create a new role with color, hoisted, and mentionable options
- `delete_role` - Delete a role from the server
- `assign_role` - Assign a role to a member
- `remove_role` - Remove a role from a member
- `list_roles` - List all roles in the server
- `find_role` - Find a role by name
- `update_role` - Update role properties (name, color, hoisted, mentionable)
- `get_members_by_role` - Get all members with a specific role

### Moderation (NEW)
- `kick_member` - Kick a member from the server
- `ban_member` - Ban a user from the server
- `unban_member` - Unban a user from the server
- `timeout_member` - Timeout a member (prevent them from sending messages)
- `remove_timeout` - Remove timeout from a member
- `list_bans` - List all banned users
- `get_member_info` - Get detailed information about a member

### Embed Messages (NEW)
- `send_embed` - Send a rich embed message with title, description, images, etc.
- `send_embed_with_fields` - Send an embed with structured fields
- `send_announcement` - Send a standardized announcement (info/success/warning/error)
- `edit_embed` - Edit an existing embed message

### Interactive Components (NEW)
- `send_buttons` - Send a message with interactive buttons
- `send_embed_with_buttons` - Send an embed with buttons
- `send_select_menu` - Send a dropdown select menu
- `send_role_panel` - Send a role selection panel for self-assignable roles
- `send_ticket_panel` - Send a support ticket creation panel
- `remove_components` - Remove all buttons/menus from a message
- `disable_buttons` - Disable all buttons on a message

### Thread Management (NEW)
- `create_thread` - Create a new thread in a text channel
- `create_thread_from_message` - Create a thread attached to a message
- `archive_thread` - Archive a thread (optionally lock it)
- `unarchive_thread` - Unarchive a thread
- `delete_thread` - Delete a thread
- `list_threads` - List all active threads
- `add_thread_member` - Add a member to a thread
- `remove_thread_member` - Remove a member from a thread
- `send_thread_message` - Send a message to a thread

### Voice Channel Management (NEW)
- `create_voice_channel` - Create a voice channel with user limit and bitrate
- `delete_voice_channel` - Delete a voice channel
- `list_voice_channels` - List all voice channels
- `get_voice_members` - Get members in a voice channel
- `move_member` - Move a member to a different voice channel
- `disconnect_member` - Disconnect a member from voice
- `server_mute_member` - Server mute/unmute a member
- `server_deafen_member` - Server deafen/undeafen a member

### Permission Management (NEW)
- `set_channel_permissions` - Set channel permissions for a role (supports presets)
- `set_member_channel_permissions` - Set channel permissions for a specific member
- `clear_role_permissions` - Remove all permission overrides for a role
- `get_channel_permissions` - Get current permission overrides for a channel
- `sync_channel_permissions` - Sync channel permissions with parent category
- `list_permissions` - List all available Discord permission names
- `lock_channel` - Lock a channel (prevent @everyone from sending)
- `unlock_channel` - Unlock a channel

**Permission Presets:**
- `staff_text` - Full text channel access for staff
- `staff_voice` - Full voice channel access for staff
- `member_text` - Standard text channel access for members
- `member_voice` - Standard voice channel access for members
- `readonly` - View and read only
- `hidden` - Channel hidden from role

### Log Analysis (NEW - FiveM Focused)
- `read_log_channel` - Read recent messages from a log channel
- `scan_for_cheats` - Scan logs for suspicious activity and cheat patterns
- `analyze_player_activity` - Analyze a specific player's activity in logs
- `search_logs` - Search log messages for specific keywords or patterns
- `get_log_stats` - Get statistics about log channel activity

**Detected Cheat Patterns:**
- Movement exploits (teleport, noclip, speedhack, flyhack)
- Godmode/invincibility
- Aiming exploits (aimbot, ESP, wallhack)
- Money/item exploits (duplication, spawning)
- Cheat injection (executors, trainers, mod menus)
- Network manipulation (packet editing, lag switch)
- Anti-cheat triggers

---

> If `DISCORD_GUILD_ID` is set, the `guildId` parameter becomes optional for all applicable tools.

## 📜 License

MIT License - see [LICENSE](LICENSE) for details.

Based on [SaseQ/discord-mcp](https://github.com/SaseQ/discord-mcp).
