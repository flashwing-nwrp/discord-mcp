package dev.saseq.services;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class InteractiveService {

    private final JDA jda;

    public InteractiveService(JDA jda) {
        this.jda = jda;
    }

    /**
     * Sends a message with buttons.
     *
     * @param channelId The ID of the channel to send the message to.
     * @param content   Optional text content above the buttons.
     * @param buttons   Button definitions: "label|customId|style" separated by semicolons.
     *                  Styles: primary, secondary, success, danger, link
     *                  For link buttons use: "label|url|link"
     * @return A confirmation message with the message link.
     */
    @Tool(name = "send_buttons", description = "Send a message with interactive buttons")
    public String sendButtons(@ToolParam(description = "Discord channel ID") String channelId,
                              @ToolParam(description = "Message text content", required = false) String content,
                              @ToolParam(description = "Buttons: 'Label|CustomId|Style' separated by semicolons (styles: primary, secondary, success, danger, link)") String buttons) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (buttons == null || buttons.isEmpty()) {
            throw new IllegalArgumentException("buttons cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        List<Button> buttonList = parseButtons(buttons);
        if (buttonList.isEmpty()) {
            throw new IllegalArgumentException("No valid buttons provided");
        }

        // Discord allows max 5 buttons per row, max 5 rows
        List<ActionRow> rows = new ArrayList<>();
        for (int i = 0; i < buttonList.size(); i += 5) {
            int end = Math.min(i + 5, buttonList.size());
            rows.add(ActionRow.of(buttonList.subList(i, end)));
        }

        var messageAction = channel.sendMessage(content != null && !content.isEmpty() ? content : "​")
                .setComponents(rows);

        Message message = messageAction.complete();
        return "Message with " + buttonList.size() + " buttons sent. Message link: " + message.getJumpUrl();
    }

    /**
     * Sends an embed with buttons.
     *
     * @param channelId   The ID of the channel to send to.
     * @param title       Embed title.
     * @param description Embed description.
     * @param colorHex    Hex color code.
     * @param buttons     Button definitions.
     * @return A confirmation message with the message link.
     */
    @Tool(name = "send_embed_with_buttons", description = "Send an embed message with interactive buttons")
    public String sendEmbedWithButtons(@ToolParam(description = "Discord channel ID") String channelId,
                                       @ToolParam(description = "Embed title") String title,
                                       @ToolParam(description = "Embed description") String description,
                                       @ToolParam(description = "Hex color code (e.g., #FF5733)", required = false) String colorHex,
                                       @ToolParam(description = "Buttons: 'Label|CustomId|Style' separated by semicolons") String buttons) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (buttons == null || buttons.isEmpty()) {
            throw new IllegalArgumentException("buttons cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        EmbedBuilder embed = new EmbedBuilder();
        if (title != null && !title.isEmpty()) {
            embed.setTitle(title);
        }
        if (description != null && !description.isEmpty()) {
            embed.setDescription(description);
        }
        if (colorHex != null && !colorHex.isEmpty()) {
            String hex = colorHex.startsWith("#") ? colorHex.substring(1) : colorHex;
            embed.setColor(Color.decode("#" + hex));
        }

        List<Button> buttonList = parseButtons(buttons);
        List<ActionRow> rows = new ArrayList<>();
        for (int i = 0; i < buttonList.size(); i += 5) {
            int end = Math.min(i + 5, buttonList.size());
            rows.add(ActionRow.of(buttonList.subList(i, end)));
        }

        Message message = channel.sendMessageEmbeds(embed.build())
                .setComponents(rows)
                .complete();

        return "Embed with " + buttonList.size() + " buttons sent. Message link: " + message.getJumpUrl();
    }

    /**
     * Sends a message with a dropdown select menu.
     *
     * @param channelId   The ID of the channel to send to.
     * @param content     Optional text content above the menu.
     * @param menuId      The custom ID for the select menu.
     * @param placeholder The placeholder text when nothing is selected.
     * @param options     Options: "label|value|description" separated by semicolons.
     * @param minValues   Minimum selections required (default 1).
     * @param maxValues   Maximum selections allowed (default 1).
     * @return A confirmation message with the message link.
     */
    @Tool(name = "send_select_menu", description = "Send a message with a dropdown select menu")
    public String sendSelectMenu(@ToolParam(description = "Discord channel ID") String channelId,
                                 @ToolParam(description = "Message text content", required = false) String content,
                                 @ToolParam(description = "Custom ID for the menu") String menuId,
                                 @ToolParam(description = "Placeholder text") String placeholder,
                                 @ToolParam(description = "Options: 'Label|Value|Description' separated by semicolons") String options,
                                 @ToolParam(description = "Minimum selections", required = false) Integer minValues,
                                 @ToolParam(description = "Maximum selections", required = false) Integer maxValues) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (menuId == null || menuId.isEmpty()) {
            throw new IllegalArgumentException("menuId cannot be null");
        }
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("options cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create(menuId)
                .setPlaceholder(placeholder != null ? placeholder : "Select an option");

        String[] optionParts = options.split(";");
        for (String option : optionParts) {
            String[] parts = option.split("\\|");
            if (parts.length >= 2) {
                String label = parts[0].trim();
                String value = parts[1].trim();
                String desc = parts.length >= 3 ? parts[2].trim() : null;

                if (desc != null && !desc.isEmpty()) {
                    menuBuilder.addOption(label, value, desc);
                } else {
                    menuBuilder.addOption(label, value);
                }
            }
        }

        if (minValues != null) {
            menuBuilder.setMinValues(minValues);
        }
        if (maxValues != null) {
            menuBuilder.setMaxValues(maxValues);
        }

        Message message = channel.sendMessage(content != null && !content.isEmpty() ? content : "​")
                .setComponents(ActionRow.of(menuBuilder.build()))
                .complete();

        return "Select menu sent. Message link: " + message.getJumpUrl();
    }

    /**
     * Sends a role selection panel (commonly used for self-assignable roles).
     *
     * @param channelId   The ID of the channel to send to.
     * @param title       The title for the role panel.
     * @param description Instructions for users.
     * @param roles       Role definitions: "roleName|roleId|emoji" separated by semicolons.
     * @param colorHex    Hex color code for the embed.
     * @return A confirmation message with the message link.
     */
    @Tool(name = "send_role_panel", description = "Send a role selection panel with buttons for self-assignable roles")
    public String sendRolePanel(@ToolParam(description = "Discord channel ID") String channelId,
                                @ToolParam(description = "Panel title") String title,
                                @ToolParam(description = "Panel description/instructions") String description,
                                @ToolParam(description = "Roles: 'RoleName|RoleId|Emoji' separated by semicolons") String roles,
                                @ToolParam(description = "Hex color code", required = false) String colorHex) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("roles cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title != null ? title : "Role Selection")
                .setDescription(description != null ? description : "Click a button to toggle a role")
                .setTimestamp(Instant.now());

        if (colorHex != null && !colorHex.isEmpty()) {
            String hex = colorHex.startsWith("#") ? colorHex.substring(1) : colorHex;
            embed.setColor(Color.decode("#" + hex));
        } else {
            embed.setColor(Color.decode("#5865F2")); // Discord blurple
        }

        List<Button> buttonList = new ArrayList<>();
        String[] roleParts = roles.split(";");

        StringBuilder roleList = new StringBuilder();
        for (String role : roleParts) {
            String[] parts = role.split("\\|");
            if (parts.length >= 2) {
                String roleName = parts[0].trim();
                String roleId = parts[1].trim();
                String emojiStr = parts.length >= 3 ? parts[2].trim() : null;

                Button button;
                if (emojiStr != null && !emojiStr.isEmpty()) {
                    button = Button.secondary("role_" + roleId, roleName)
                            .withEmoji(Emoji.fromFormatted(emojiStr));
                    roleList.append(emojiStr).append(" ");
                } else {
                    button = Button.secondary("role_" + roleId, roleName);
                }
                buttonList.add(button);
                roleList.append("**").append(roleName).append("**\n");
            }
        }

        if (!roleList.isEmpty()) {
            embed.addField("Available Roles", roleList.toString(), false);
        }

        List<ActionRow> rows = new ArrayList<>();
        for (int i = 0; i < buttonList.size(); i += 5) {
            int end = Math.min(i + 5, buttonList.size());
            rows.add(ActionRow.of(buttonList.subList(i, end)));
        }

        Message message = channel.sendMessageEmbeds(embed.build())
                .setComponents(rows)
                .complete();

        return "Role panel with " + buttonList.size() + " roles sent. Message link: " + message.getJumpUrl() +
                "\n\nNote: You'll need a bot with interaction handlers to process role button clicks.";
    }

    /**
     * Sends a ticket panel (commonly used for support ticket creation).
     *
     * @param channelId   The ID of the channel to send to.
     * @param title       The title for the ticket panel.
     * @param description Instructions for users.
     * @param categories  Ticket categories: "name|customId|emoji" separated by semicolons.
     * @param colorHex    Hex color code for the embed.
     * @return A confirmation message with the message link.
     */
    @Tool(name = "send_ticket_panel", description = "Send a support ticket creation panel")
    public String sendTicketPanel(@ToolParam(description = "Discord channel ID") String channelId,
                                  @ToolParam(description = "Panel title") String title,
                                  @ToolParam(description = "Panel description/instructions") String description,
                                  @ToolParam(description = "Categories: 'Name|CustomId|Emoji' separated by semicolons") String categories,
                                  @ToolParam(description = "Hex color code", required = false) String colorHex) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title != null ? title : "🎫 Support Tickets")
                .setDescription(description != null ? description :
                        "Need help? Click a button below to create a support ticket.\n\n" +
                                "**Please include:**\n" +
                                "• A clear description of your issue\n" +
                                "• Any relevant screenshots or evidence\n" +
                                "• Your in-game name if applicable")
                .setTimestamp(Instant.now())
                .setFooter("Support Team");

        if (colorHex != null && !colorHex.isEmpty()) {
            String hex = colorHex.startsWith("#") ? colorHex.substring(1) : colorHex;
            embed.setColor(Color.decode("#" + hex));
        } else {
            embed.setColor(Color.decode("#2ECC71")); // Green
        }

        List<Button> buttonList = new ArrayList<>();

        if (categories != null && !categories.isEmpty()) {
            String[] categoryParts = categories.split(";");
            for (String category : categoryParts) {
                String[] parts = category.split("\\|");
                if (parts.length >= 2) {
                    String name = parts[0].trim();
                    String customId = parts[1].trim();
                    String emojiStr = parts.length >= 3 ? parts[2].trim() : null;

                    Button button = Button.primary("ticket_" + customId, name);
                    if (emojiStr != null && !emojiStr.isEmpty()) {
                        button = button.withEmoji(Emoji.fromFormatted(emojiStr));
                    }
                    buttonList.add(button);
                }
            }
        } else {
            // Default ticket button
            buttonList.add(Button.success("ticket_create", "Create Ticket")
                    .withEmoji(Emoji.fromUnicode("🎫")));
        }

        List<ActionRow> rows = new ArrayList<>();
        for (int i = 0; i < buttonList.size(); i += 5) {
            int end = Math.min(i + 5, buttonList.size());
            rows.add(ActionRow.of(buttonList.subList(i, end)));
        }

        Message message = channel.sendMessageEmbeds(embed.build())
                .setComponents(rows)
                .complete();

        return "Ticket panel sent. Message link: " + message.getJumpUrl() +
                "\n\nNote: You'll need a bot with interaction handlers to process ticket button clicks.";
    }

    /**
     * Removes all components (buttons/menus) from a message.
     *
     * @param channelId The ID of the channel containing the message.
     * @param messageId The ID of the message to edit.
     * @return A confirmation message.
     */
    @Tool(name = "remove_components", description = "Remove all buttons and menus from a message")
    public String removeComponents(@ToolParam(description = "Discord channel ID") String channelId,
                                   @ToolParam(description = "Discord message ID") String messageId) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        Message message = channel.retrieveMessageById(messageId).complete();
        if (message == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }

        message.editMessageComponents().complete();
        return "Components removed from message. Message link: " + message.getJumpUrl();
    }

    /**
     * Disables all buttons on a message (grays them out).
     *
     * @param channelId The ID of the channel containing the message.
     * @param messageId The ID of the message to edit.
     * @return A confirmation message.
     */
    @Tool(name = "disable_buttons", description = "Disable all buttons on a message (gray them out)")
    public String disableButtons(@ToolParam(description = "Discord channel ID") String channelId,
                                 @ToolParam(description = "Discord message ID") String messageId) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        Message message = channel.retrieveMessageById(messageId).complete();
        if (message == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }

        List<ActionRow> disabledRows = message.getActionRows().stream()
                .map(ActionRow::asDisabled)
                .toList();

        message.editMessageComponents(disabledRows).complete();
        return "Buttons disabled on message. Message link: " + message.getJumpUrl();
    }

    // Helper method to parse button definitions
    private List<Button> parseButtons(String buttonDefs) {
        List<Button> buttons = new ArrayList<>();
        String[] buttonParts = buttonDefs.split(";");

        for (String buttonDef : buttonParts) {
            String[] parts = buttonDef.split("\\|");
            if (parts.length >= 3) {
                String label = parts[0].trim();
                String idOrUrl = parts[1].trim();
                String styleStr = parts[2].trim().toLowerCase();

                Button button;
                switch (styleStr) {
                    case "primary":
                    case "blurple":
                        button = Button.primary(idOrUrl, label);
                        break;
                    case "secondary":
                    case "gray":
                    case "grey":
                        button = Button.secondary(idOrUrl, label);
                        break;
                    case "success":
                    case "green":
                        button = Button.success(idOrUrl, label);
                        break;
                    case "danger":
                    case "red":
                        button = Button.danger(idOrUrl, label);
                        break;
                    case "link":
                    case "url":
                        button = Button.link(idOrUrl, label);
                        break;
                    default:
                        button = Button.secondary(idOrUrl, label);
                }
                buttons.add(button);
            }
        }
        return buttons;
    }
}
