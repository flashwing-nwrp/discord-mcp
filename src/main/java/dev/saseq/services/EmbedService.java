package dev.saseq.services;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.time.Instant;

@Service
public class EmbedService {

    private final JDA jda;

    public EmbedService(JDA jda) {
        this.jda = jda;
    }

    /**
     * Sends a rich embed message to a Discord channel.
     *
     * @param channelId   The ID of the channel to send the embed to.
     * @param title       Optional title of the embed.
     * @param description Optional description/body of the embed.
     * @param colorHex    Optional hex color code for the embed sidebar.
     * @param authorName  Optional author name displayed at top.
     * @param authorIcon  Optional author icon URL.
     * @param thumbnail   Optional thumbnail URL (small image on right).
     * @param imageUrl    Optional large image URL (displayed at bottom).
     * @param footerText  Optional footer text.
     * @param footerIcon  Optional footer icon URL.
     * @param timestamp   Optional whether to add current timestamp.
     * @return A confirmation message with the message link.
     */
    @Tool(name = "send_embed", description = "Send a rich embed message to a channel")
    public String sendEmbed(@ToolParam(description = "Discord channel ID") String channelId,
                            @ToolParam(description = "Embed title", required = false) String title,
                            @ToolParam(description = "Embed description/body", required = false) String description,
                            @ToolParam(description = "Hex color code (e.g., #FF5733)", required = false) String colorHex,
                            @ToolParam(description = "Author name", required = false) String authorName,
                            @ToolParam(description = "Author icon URL", required = false) String authorIcon,
                            @ToolParam(description = "Thumbnail URL (small image on right)", required = false) String thumbnail,
                            @ToolParam(description = "Large image URL", required = false) String imageUrl,
                            @ToolParam(description = "Footer text", required = false) String footerText,
                            @ToolParam(description = "Footer icon URL", required = false) String footerIcon,
                            @ToolParam(description = "Add current timestamp", required = false) Boolean timestamp) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
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
        if (authorName != null && !authorName.isEmpty()) {
            embed.setAuthor(authorName, null, authorIcon);
        }
        if (thumbnail != null && !thumbnail.isEmpty()) {
            embed.setThumbnail(thumbnail);
        }
        if (imageUrl != null && !imageUrl.isEmpty()) {
            embed.setImage(imageUrl);
        }
        if (footerText != null && !footerText.isEmpty()) {
            embed.setFooter(footerText, footerIcon);
        }
        if (timestamp != null && timestamp) {
            embed.setTimestamp(Instant.now());
        }

        if (embed.isEmpty()) {
            throw new IllegalArgumentException("Embed must have at least one of: title, description, author, image, or footer");
        }

        Message message = channel.sendMessageEmbeds(embed.build()).complete();
        return "Embed sent successfully. Message link: " + message.getJumpUrl();
    }

    /**
     * Sends a rich embed with fields (for structured information).
     *
     * @param channelId   The ID of the channel to send the embed to.
     * @param title       Optional title of the embed.
     * @param description Optional description/body of the embed.
     * @param colorHex    Optional hex color code for the embed sidebar.
     * @param fields      Pipe-separated field definitions: "Name|Value|Inline" (e.g., "Status|Online|true|Players|64/64|true").
     * @param footerText  Optional footer text.
     * @param timestamp   Optional whether to add current timestamp.
     * @return A confirmation message with the message link.
     */
    @Tool(name = "send_embed_with_fields", description = "Send a rich embed with structured fields")
    public String sendEmbedWithFields(@ToolParam(description = "Discord channel ID") String channelId,
                                      @ToolParam(description = "Embed title", required = false) String title,
                                      @ToolParam(description = "Embed description/body", required = false) String description,
                                      @ToolParam(description = "Hex color code (e.g., #FF5733)", required = false) String colorHex,
                                      @ToolParam(description = "Fields: 'Name|Value|Inline' separated by semicolons (e.g., 'Status|Online|true;Players|64|true')") String fields,
                                      @ToolParam(description = "Footer text", required = false) String footerText,
                                      @ToolParam(description = "Add current timestamp", required = false) Boolean timestamp) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("fields cannot be null");
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

        // Parse fields: "Name|Value|Inline;Name2|Value2|Inline2"
        String[] fieldParts = fields.split(";");
        for (String field : fieldParts) {
            String[] parts = field.split("\\|");
            if (parts.length >= 2) {
                String name = parts[0].trim();
                String value = parts[1].trim();
                boolean inline = parts.length >= 3 && Boolean.parseBoolean(parts[2].trim());
                embed.addField(name, value, inline);
            }
        }

        if (footerText != null && !footerText.isEmpty()) {
            embed.setFooter(footerText);
        }
        if (timestamp != null && timestamp) {
            embed.setTimestamp(Instant.now());
        }

        Message message = channel.sendMessageEmbeds(embed.build()).complete();
        return "Embed with fields sent successfully. Message link: " + message.getJumpUrl();
    }

    /**
     * Sends a simple announcement embed with a standardized format.
     *
     * @param channelId   The ID of the channel to send the announcement to.
     * @param title       The announcement title.
     * @param content     The announcement content.
     * @param type        The type of announcement: info, success, warning, error.
     * @param pingRole    Optional role ID to ping with the announcement.
     * @return A confirmation message with the message link.
     */
    @Tool(name = "send_announcement", description = "Send a standardized announcement embed (info/success/warning/error)")
    public String sendAnnouncement(@ToolParam(description = "Discord channel ID") String channelId,
                                   @ToolParam(description = "Announcement title") String title,
                                   @ToolParam(description = "Announcement content") String content,
                                   @ToolParam(description = "Type: info, success, warning, or error") String type,
                                   @ToolParam(description = "Role ID to ping", required = false) String pingRole) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("title cannot be null");
        }
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("content cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        Color color;
        String emoji;
        switch (type != null ? type.toLowerCase() : "info") {
            case "success":
                color = Color.decode("#2ECC71"); // Green
                emoji = "✅";
                break;
            case "warning":
                color = Color.decode("#F39C12"); // Orange
                emoji = "⚠️";
                break;
            case "error":
                color = Color.decode("#E74C3C"); // Red
                emoji = "❌";
                break;
            case "info":
            default:
                color = Color.decode("#3498DB"); // Blue
                emoji = "ℹ️";
                break;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(emoji + " " + title)
                .setDescription(content)
                .setColor(color)
                .setTimestamp(Instant.now());

        String mentionText = "";
        if (pingRole != null && !pingRole.isEmpty()) {
            mentionText = "<@&" + pingRole + "> ";
        }

        Message message = channel.sendMessage(mentionText)
                .addEmbeds(embed.build())
                .complete();
        return "Announcement sent successfully. Message link: " + message.getJumpUrl();
    }

    /**
     * Edits an existing embed message.
     *
     * @param channelId   The ID of the channel containing the message.
     * @param messageId   The ID of the message to edit.
     * @param title       Optional new title.
     * @param description Optional new description.
     * @param colorHex    Optional new color.
     * @return A confirmation message.
     */
    @Tool(name = "edit_embed", description = "Edit an existing embed message")
    public String editEmbed(@ToolParam(description = "Discord channel ID") String channelId,
                            @ToolParam(description = "Discord message ID") String messageId,
                            @ToolParam(description = "New embed title", required = false) String title,
                            @ToolParam(description = "New embed description", required = false) String description,
                            @ToolParam(description = "New hex color code", required = false) String colorHex) {
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

        if (message.getEmbeds().isEmpty()) {
            throw new IllegalArgumentException("Message does not contain an embed");
        }

        MessageEmbed oldEmbed = message.getEmbeds().get(0);
        EmbedBuilder embed = new EmbedBuilder(oldEmbed);

        if (title != null) {
            embed.setTitle(title.isEmpty() ? null : title);
        }
        if (description != null) {
            embed.setDescription(description.isEmpty() ? null : description);
        }
        if (colorHex != null && !colorHex.isEmpty()) {
            String hex = colorHex.startsWith("#") ? colorHex.substring(1) : colorHex;
            embed.setColor(Color.decode("#" + hex));
        }

        message.editMessageEmbeds(embed.build()).complete();
        return "Embed edited successfully. Message link: " + message.getJumpUrl();
    }
}
