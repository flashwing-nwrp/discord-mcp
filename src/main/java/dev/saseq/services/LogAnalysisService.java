package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LogAnalysisService {

    private final JDA jda;

    // Common FiveM cheat detection patterns
    private static final Map<String, String> SUSPICIOUS_PATTERNS = new LinkedHashMap<>();
    static {
        // Movement/Position anomalies
        SUSPICIOUS_PATTERNS.put("(?i)(teleport|noclip|speedhack|fly\\s*hack)", "Movement Exploit");
        SUSPICIOUS_PATTERNS.put("(?i)(godmode|god\\s*mode|invincib)", "Godmode/Invincibility");
        SUSPICIOUS_PATTERNS.put("(?i)(aimbot|aim\\s*bot|esp|wallhack)", "Aiming Exploit");

        // Resource/Money exploits
        SUSPICIOUS_PATTERNS.put("(?i)(money\\s*hack|cash\\s*exploit|duplication|dupe)", "Money Exploit");
        SUSPICIOUS_PATTERNS.put("(?i)(spawned\\s*item|item\\s*spawn|give\\s*weapon)", "Item Spawn");
        SUSPICIOUS_PATTERNS.put("(?i)(vehicle\\s*spawn|car\\s*spawn|spawn\\s*vehicle)", "Vehicle Spawn");

        // Injection/Executor
        SUSPICIOUS_PATTERNS.put("(?i)(lua\\s*executor|inject|cheat\\s*engine|trainer)", "Cheat Injection");
        SUSPICIOUS_PATTERNS.put("(?i)(menu\\s*detected|mod\\s*menu|eulen|stand|kiddion)", "Mod Menu");
        SUSPICIOUS_PATTERNS.put("(?i)(resource\\s*stop|resource\\s*exploit)", "Resource Exploit");

        // Anti-cheat triggers
        SUSPICIOUS_PATTERNS.put("(?i)(anti.?cheat|violation|banned|kicked\\s*for)", "Anti-Cheat Trigger");
        SUSPICIOUS_PATTERNS.put("(?i)(suspicious|abnormal|unusual|impossible)", "Suspicious Activity");

        // Network manipulation
        SUSPICIOUS_PATTERNS.put("(?i)(packet|desync|lag\\s*switch)", "Network Manipulation");
    }

    // Patterns for player identification in FiveM logs
    private static final Pattern PLAYER_ID_PATTERN = Pattern.compile(
            "(?:player|id|source|src)[:\\s#]*([0-9]+)|\\[([0-9]+)\\]|(?:license|steam|discord|fivem)[:\\s]*([a-zA-Z0-9:]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern MONEY_AMOUNT_PATTERN = Pattern.compile(
            "\\$?([0-9,]+(?:\\.[0-9]{2})?)|([0-9]+)\\s*(?:cash|bank|money|\\$)",
            Pattern.CASE_INSENSITIVE);

    public LogAnalysisService(JDA jda) {
        this.jda = jda;
    }

    /**
     * Reads recent messages from a log channel for analysis.
     *
     * @param channelId The ID of the log channel.
     * @param limit     Maximum number of messages to retrieve (1-100).
     * @return A formatted string with the messages.
     */
    @Tool(name = "read_log_channel", description = "Read recent messages from a log channel")
    public String readLogChannel(@ToolParam(description = "Discord channel ID") String channelId,
                                 @ToolParam(description = "Number of messages (1-100)", required = false) Integer limit) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        int messageLimit = limit != null ? Math.min(Math.max(limit, 1), 100) : 50;
        List<Message> messages = channel.getHistory().retrievePast(messageLimit).complete();

        if (messages.isEmpty()) {
            return "No messages found in channel";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Retrieved ").append(messages.size()).append(" messages from #").append(channel.getName()).append(":\n\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Message msg : messages) {
            sb.append("**[").append(msg.getTimeCreated().format(formatter)).append("]** ");
            sb.append(msg.getAuthor().getName());

            if (!msg.getContentRaw().isEmpty()) {
                sb.append(": ").append(msg.getContentRaw());
            }

            // Include embed content
            for (MessageEmbed embed : msg.getEmbeds()) {
                if (embed.getTitle() != null) {
                    sb.append("\n  📋 **").append(embed.getTitle()).append("**");
                }
                if (embed.getDescription() != null) {
                    sb.append("\n  ").append(embed.getDescription().replace("\n", "\n  "));
                }
                for (MessageEmbed.Field field : embed.getFields()) {
                    sb.append("\n  • ").append(field.getName()).append(": ").append(field.getValue());
                }
            }
            sb.append("\n---\n");
        }

        return sb.toString();
    }

    /**
     * Scans a log channel for suspicious activity patterns.
     *
     * @param channelId The ID of the log channel to scan.
     * @param limit     Maximum number of messages to scan (1-100).
     * @return A report of detected suspicious patterns.
     */
    @Tool(name = "scan_for_cheats", description = "Scan a log channel for suspicious activity and cheat patterns")
    public String scanForCheats(@ToolParam(description = "Discord channel ID") String channelId,
                                @ToolParam(description = "Number of messages to scan (1-100)", required = false) Integer limit) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        int messageLimit = limit != null ? Math.min(Math.max(limit, 1), 100) : 100;
        List<Message> messages = channel.getHistory().retrievePast(messageLimit).complete();

        if (messages.isEmpty()) {
            return "No messages found in channel";
        }

        Map<String, List<SuspiciousEntry>> findings = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Message msg : messages) {
            String content = getFullMessageContent(msg);

            for (Map.Entry<String, String> patternEntry : SUSPICIOUS_PATTERNS.entrySet()) {
                Pattern pattern = Pattern.compile(patternEntry.getKey());
                Matcher matcher = pattern.matcher(content);

                if (matcher.find()) {
                    String category = patternEntry.getValue();
                    findings.computeIfAbsent(category, k -> new ArrayList<>())
                            .add(new SuspiciousEntry(
                                    msg.getTimeCreated(),
                                    msg.getJumpUrl(),
                                    extractPlayerInfo(content),
                                    matcher.group(),
                                    truncate(content, 150)
                            ));
                }
            }
        }

        if (findings.isEmpty()) {
            return "✅ **No suspicious patterns detected** in " + messageLimit + " messages from #" + channel.getName();
        }

        StringBuilder report = new StringBuilder();
        report.append("⚠️ **Suspicious Activity Report** - #").append(channel.getName()).append("\n");
        report.append("Scanned: ").append(messageLimit).append(" messages\n\n");

        int totalFindings = 0;
        for (Map.Entry<String, List<SuspiciousEntry>> entry : findings.entrySet()) {
            report.append("### 🚨 ").append(entry.getKey()).append(" (").append(entry.getValue().size()).append(" hits)\n");

            for (SuspiciousEntry finding : entry.getValue()) {
                totalFindings++;
                report.append("- **").append(finding.timestamp.format(formatter)).append("**");
                if (!finding.playerInfo.isEmpty()) {
                    report.append(" | Player: `").append(finding.playerInfo).append("`");
                }
                report.append("\n  Match: `").append(finding.matchedText).append("`\n");
                report.append("  Context: ").append(finding.context).append("\n");
                report.append("  [Jump to message](").append(finding.messageUrl).append(")\n");
            }
            report.append("\n");
        }

        report.append("---\n**Total Findings: ").append(totalFindings).append("**");
        return report.toString();
    }

    /**
     * Analyzes player activity across log messages.
     *
     * @param channelId    The ID of the log channel.
     * @param playerSearch The player ID, name, or identifier to search for.
     * @param limit        Maximum number of messages to search (1-100).
     * @return A report of the player's activity.
     */
    @Tool(name = "analyze_player_activity", description = "Analyze a specific player's activity in logs")
    public String analyzePlayerActivity(@ToolParam(description = "Discord channel ID") String channelId,
                                        @ToolParam(description = "Player ID, name, or identifier") String playerSearch,
                                        @ToolParam(description = "Number of messages to search (1-100)", required = false) Integer limit) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (playerSearch == null || playerSearch.isEmpty()) {
            throw new IllegalArgumentException("playerSearch cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        int messageLimit = limit != null ? Math.min(Math.max(limit, 1), 100) : 100;
        List<Message> messages = channel.getHistory().retrievePast(messageLimit).complete();

        if (messages.isEmpty()) {
            return "No messages found in channel";
        }

        List<Message> playerMessages = new ArrayList<>();
        Pattern searchPattern = Pattern.compile(Pattern.quote(playerSearch), Pattern.CASE_INSENSITIVE);

        for (Message msg : messages) {
            String content = getFullMessageContent(msg);
            if (searchPattern.matcher(content).find()) {
                playerMessages.add(msg);
            }
        }

        if (playerMessages.isEmpty()) {
            return "No activity found for player: " + playerSearch;
        }

        // Analyze patterns
        Map<String, Integer> activityTypes = new LinkedHashMap<>();
        List<String> suspiciousEntries = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Message msg : playerMessages) {
            String content = getFullMessageContent(msg);

            // Categorize activity
            if (content.toLowerCase().contains("kill") || content.toLowerCase().contains("death")) {
                activityTypes.merge("Kills/Deaths", 1, Integer::sum);
            }
            if (content.toLowerCase().contains("money") || content.toLowerCase().contains("cash") || content.toLowerCase().contains("bank")) {
                activityTypes.merge("Money Transactions", 1, Integer::sum);
            }
            if (content.toLowerCase().contains("item") || content.toLowerCase().contains("inventory")) {
                activityTypes.merge("Item Activity", 1, Integer::sum);
            }
            if (content.toLowerCase().contains("join") || content.toLowerCase().contains("leave") || content.toLowerCase().contains("connect")) {
                activityTypes.merge("Connection Events", 1, Integer::sum);
            }
            if (content.toLowerCase().contains("vehicle") || content.toLowerCase().contains("car")) {
                activityTypes.merge("Vehicle Activity", 1, Integer::sum);
            }

            // Check for suspicious patterns
            for (Map.Entry<String, String> pattern : SUSPICIOUS_PATTERNS.entrySet()) {
                if (Pattern.compile(pattern.getKey()).matcher(content).find()) {
                    suspiciousEntries.add(String.format("[%s] %s: %s",
                            msg.getTimeCreated().format(formatter),
                            pattern.getValue(),
                            truncate(content, 100)));
                }
            }
        }

        StringBuilder report = new StringBuilder();
        report.append("📊 **Player Activity Report: ").append(playerSearch).append("**\n");
        report.append("Channel: #").append(channel.getName()).append("\n");
        report.append("Entries Found: ").append(playerMessages.size()).append(" / ").append(messageLimit).append(" messages scanned\n\n");

        if (!activityTypes.isEmpty()) {
            report.append("### Activity Breakdown:\n");
            for (Map.Entry<String, Integer> activity : activityTypes.entrySet()) {
                report.append("- ").append(activity.getKey()).append(": ").append(activity.getValue()).append("\n");
            }
            report.append("\n");
        }

        if (!suspiciousEntries.isEmpty()) {
            report.append("### ⚠️ Suspicious Activity (").append(suspiciousEntries.size()).append("):\n");
            for (String entry : suspiciousEntries) {
                report.append("- ").append(entry).append("\n");
            }
        } else {
            report.append("✅ **No suspicious patterns detected for this player**\n");
        }

        // Show recent entries
        report.append("\n### Recent Log Entries:\n");
        int shown = 0;
        for (Message msg : playerMessages) {
            if (shown >= 5) break;
            report.append("- [").append(msg.getTimeCreated().format(formatter)).append("] ");
            report.append(truncate(getFullMessageContent(msg), 100)).append("\n");
            shown++;
        }

        return report.toString();
    }

    /**
     * Searches log messages for specific keywords or patterns.
     *
     * @param channelId The ID of the log channel.
     * @param query     The search query (supports basic regex).
     * @param limit     Maximum number of messages to search (1-100).
     * @return Matching log entries.
     */
    @Tool(name = "search_logs", description = "Search log messages for specific keywords or patterns")
    public String searchLogs(@ToolParam(description = "Discord channel ID") String channelId,
                             @ToolParam(description = "Search query (supports regex)") String query,
                             @ToolParam(description = "Number of messages to search (1-100)", required = false) Integer limit) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("query cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        int messageLimit = limit != null ? Math.min(Math.max(limit, 1), 100) : 100;
        List<Message> messages = channel.getHistory().retrievePast(messageLimit).complete();

        if (messages.isEmpty()) {
            return "No messages found in channel";
        }

        Pattern searchPattern;
        try {
            searchPattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            // Fall back to literal search if regex is invalid
            searchPattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
        }

        List<Message> matches = new ArrayList<>();
        for (Message msg : messages) {
            String content = getFullMessageContent(msg);
            if (searchPattern.matcher(content).find()) {
                matches.add(msg);
            }
        }

        if (matches.isEmpty()) {
            return "No matches found for: " + query;
        }

        StringBuilder result = new StringBuilder();
        result.append("🔍 **Search Results for:** `").append(query).append("`\n");
        result.append("Found ").append(matches.size()).append(" matches in ").append(messageLimit).append(" messages\n\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Message msg : matches) {
            result.append("**[").append(msg.getTimeCreated().format(formatter)).append("]**\n");
            result.append(truncate(getFullMessageContent(msg), 200)).append("\n");
            result.append("[Jump](").append(msg.getJumpUrl()).append(")\n---\n");
        }

        return result.toString();
    }

    /**
     * Gets statistics about log channel activity.
     *
     * @param channelId The ID of the log channel.
     * @param limit     Number of messages to analyze (1-100).
     * @return Channel statistics and activity summary.
     */
    @Tool(name = "get_log_stats", description = "Get statistics about log channel activity")
    public String getLogStats(@ToolParam(description = "Discord channel ID") String channelId,
                              @ToolParam(description = "Number of messages to analyze (1-100)", required = false) Integer limit) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        int messageLimit = limit != null ? Math.min(Math.max(limit, 1), 100) : 100;
        List<Message> messages = channel.getHistory().retrievePast(messageLimit).complete();

        if (messages.isEmpty()) {
            return "No messages found in channel";
        }

        // Gather stats
        Map<String, Integer> authorCounts = new LinkedHashMap<>();
        Map<String, Integer> hourCounts = new LinkedHashMap<>();
        int embedCount = 0;
        int webhookCount = 0;
        OffsetDateTime oldest = null;
        OffsetDateTime newest = null;

        for (Message msg : messages) {
            // Author counts
            String author = msg.getAuthor().getName();
            authorCounts.merge(author, 1, Integer::sum);

            // Time distribution
            String hour = String.format("%02d:00", msg.getTimeCreated().getHour());
            hourCounts.merge(hour, 1, Integer::sum);

            if (!msg.getEmbeds().isEmpty()) {
                embedCount++;
            }
            if (msg.isWebhookMessage()) {
                webhookCount++;
            }

            // Track time range
            if (oldest == null || msg.getTimeCreated().isBefore(oldest)) {
                oldest = msg.getTimeCreated();
            }
            if (newest == null || msg.getTimeCreated().isAfter(newest)) {
                newest = msg.getTimeCreated();
            }
        }

        // Sort by count
        List<Map.Entry<String, Integer>> sortedAuthors = authorCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .toList();

        StringBuilder stats = new StringBuilder();
        stats.append("📈 **Log Channel Statistics: #").append(channel.getName()).append("**\n\n");
        stats.append("**Messages Analyzed:** ").append(messageLimit).append("\n");
        stats.append("**With Embeds:** ").append(embedCount).append("\n");
        stats.append("**From Webhooks:** ").append(webhookCount).append("\n");

        if (oldest != null && newest != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            stats.append("**Time Range:** ").append(oldest.format(formatter))
                    .append(" to ").append(newest.format(formatter)).append("\n");
        }

        stats.append("\n### Top Sources:\n");
        for (Map.Entry<String, Integer> entry : sortedAuthors) {
            stats.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" messages\n");
        }

        stats.append("\n### Activity by Hour:\n");
        hourCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> stats.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append("\n"));

        return stats.toString();
    }

    // Helper methods

    private String getFullMessageContent(Message msg) {
        StringBuilder content = new StringBuilder();
        content.append(msg.getContentRaw());

        for (MessageEmbed embed : msg.getEmbeds()) {
            if (embed.getTitle() != null) {
                content.append(" ").append(embed.getTitle());
            }
            if (embed.getDescription() != null) {
                content.append(" ").append(embed.getDescription());
            }
            for (MessageEmbed.Field field : embed.getFields()) {
                content.append(" ").append(field.getName()).append(" ").append(field.getValue());
            }
        }

        return content.toString();
    }

    private String extractPlayerInfo(String content) {
        Matcher matcher = PLAYER_ID_PATTERN.matcher(content);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    return matcher.group(i);
                }
            }
        }
        return "";
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        text = text.replace("\n", " ").trim();
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private static class SuspiciousEntry {
        OffsetDateTime timestamp;
        String messageUrl;
        String playerInfo;
        String matchedText;
        String context;

        SuspiciousEntry(OffsetDateTime timestamp, String messageUrl, String playerInfo, String matchedText, String context) {
            this.timestamp = timestamp;
            this.messageUrl = messageUrl;
            this.playerInfo = playerInfo;
            this.matchedText = matchedText;
            this.context = context;
        }
    }
}
