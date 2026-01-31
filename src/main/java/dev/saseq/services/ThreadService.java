package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ThreadService {

    private final JDA jda;

    @Value("${DISCORD_GUILD_ID:}")
    private String defaultGuildId;

    public ThreadService(JDA jda) {
        this.jda = jda;
    }

    private String resolveGuildId(String guildId) {
        if ((guildId == null || guildId.isEmpty()) && defaultGuildId != null && !defaultGuildId.isEmpty()) {
            return defaultGuildId;
        }
        return guildId;
    }

    /**
     * Creates a new thread in a text channel.
     *
     * @param channelId The ID of the channel to create the thread in.
     * @param name      The name of the thread.
     * @param isPrivate Whether the thread should be private.
     * @param message   Optional initial message content for the thread.
     * @return A confirmation message with the thread details.
     */
    @Tool(name = "create_thread", description = "Create a new thread in a text channel")
    public String createThread(@ToolParam(description = "Discord channel ID") String channelId,
                               @ToolParam(description = "Thread name") String name,
                               @ToolParam(description = "Create as private thread", required = false) Boolean isPrivate,
                               @ToolParam(description = "Initial message content", required = false) String message) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        ThreadChannel thread;
        if (isPrivate != null && isPrivate) {
            thread = channel.createThreadChannel(name, true).complete();
        } else {
            thread = channel.createThreadChannel(name, false).complete();
        }

        if (message != null && !message.isEmpty()) {
            thread.sendMessage(message).complete();
        }

        return "Created thread: " + thread.getName() + " (ID: " + thread.getId() + ") in #" + channel.getName();
    }

    /**
     * Creates a thread from an existing message.
     *
     * @param channelId The ID of the channel containing the message.
     * @param messageId The ID of the message to create the thread from.
     * @param name      The name of the thread.
     * @return A confirmation message with the thread details.
     */
    @Tool(name = "create_thread_from_message", description = "Create a thread attached to an existing message")
    public String createThreadFromMessage(@ToolParam(description = "Discord channel ID") String channelId,
                                          @ToolParam(description = "Discord message ID") String messageId,
                                          @ToolParam(description = "Thread name") String name) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        Message msg = channel.retrieveMessageById(messageId).complete();
        if (msg == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }

        ThreadChannel thread = msg.createThreadChannel(name).complete();
        return "Created thread: " + thread.getName() + " (ID: " + thread.getId() + ") from message";
    }

    /**
     * Archives a thread.
     *
     * @param threadId The ID of the thread to archive.
     * @param locked   Whether to lock the thread (prevent unarchiving by non-moderators).
     * @return A confirmation message.
     */
    @Tool(name = "archive_thread", description = "Archive a thread (optionally lock it)")
    public String archiveThread(@ToolParam(description = "Discord thread ID") String threadId,
                                @ToolParam(description = "Lock thread to prevent unarchiving", required = false) Boolean locked) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found by threadId");
        }

        var manager = thread.getManager().setArchived(true);
        if (locked != null && locked) {
            manager = manager.setLocked(true);
        }
        manager.complete();

        return "Archived thread: " + thread.getName() + (locked != null && locked ? " (locked)" : "");
    }

    /**
     * Unarchives a thread.
     *
     * @param threadId The ID of the thread to unarchive.
     * @return A confirmation message.
     */
    @Tool(name = "unarchive_thread", description = "Unarchive a thread")
    public String unarchiveThread(@ToolParam(description = "Discord thread ID") String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found by threadId");
        }

        thread.getManager().setArchived(false).setLocked(false).complete();
        return "Unarchived thread: " + thread.getName();
    }

    /**
     * Deletes a thread.
     *
     * @param threadId The ID of the thread to delete.
     * @return A confirmation message.
     */
    @Tool(name = "delete_thread", description = "Delete a thread")
    public String deleteThread(@ToolParam(description = "Discord thread ID") String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found by threadId");
        }

        String threadName = thread.getName();
        thread.delete().complete();
        return "Deleted thread: " + threadName;
    }

    /**
     * Lists all active threads in a server.
     *
     * @param guildId Optional ID of the Discord server. If not provided, the default server will be used.
     * @return A formatted string listing all active threads.
     */
    @Tool(name = "list_threads", description = "List all active threads in the server")
    public String listThreads(@ToolParam(description = "Discord server ID", required = false) String guildId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        List<ThreadChannel> threads = guild.getThreadChannels();
        if (threads.isEmpty()) {
            return "No active threads found in server";
        }

        return "Retrieved " + threads.size() + " active threads:\n" +
                threads.stream()
                        .map(t -> String.format("- %s (ID: %s) in #%s [%s%s]",
                                t.getName(),
                                t.getId(),
                                t.getParentChannel().getName(),
                                t.isArchived() ? "Archived" : "Active",
                                t.isLocked() ? ", Locked" : ""))
                        .collect(Collectors.joining("\n"));
    }

    /**
     * Adds a member to a thread.
     *
     * @param threadId The ID of the thread.
     * @param userId   The ID of the user to add.
     * @return A confirmation message.
     */
    @Tool(name = "add_thread_member", description = "Add a member to a thread")
    public String addThreadMember(@ToolParam(description = "Discord thread ID") String threadId,
                                  @ToolParam(description = "Discord user ID") String userId) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found by threadId");
        }

        thread.addThreadMemberById(userId).complete();
        return "Added user " + userId + " to thread: " + thread.getName();
    }

    /**
     * Removes a member from a thread.
     *
     * @param threadId The ID of the thread.
     * @param userId   The ID of the user to remove.
     * @return A confirmation message.
     */
    @Tool(name = "remove_thread_member", description = "Remove a member from a thread")
    public String removeThreadMember(@ToolParam(description = "Discord thread ID") String threadId,
                                     @ToolParam(description = "Discord user ID") String userId) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found by threadId");
        }

        thread.removeThreadMemberById(userId).complete();
        return "Removed user " + userId + " from thread: " + thread.getName();
    }

    /**
     * Sends a message to a thread.
     *
     * @param threadId The ID of the thread.
     * @param content  The message content.
     * @return A confirmation message with the message link.
     */
    @Tool(name = "send_thread_message", description = "Send a message to a thread")
    public String sendThreadMessage(@ToolParam(description = "Discord thread ID") String threadId,
                                    @ToolParam(description = "Message content") String content) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("content cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found by threadId");
        }

        Message message = thread.sendMessage(content).complete();
        return "Message sent to thread. Message link: " + message.getJumpUrl();
    }
}
