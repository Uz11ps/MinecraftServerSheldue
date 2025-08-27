package com.Minecraft_server.Narkomanka.database;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "player_quest_progress")
public class PlayerQuestProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_uuid", nullable = false, length = 36)
    private String playerUuid;

    @ManyToOne
    @JoinColumn(name = "quest_id", nullable = false)
    private QuestData quest;

    @Column(name = "is_completed")
    private boolean completed;

    @Column(name = "current_progress")
    private int currentProgress;

    @Column(name = "start_time")
    private long startTime;

    @Column(name = "completion_time")
    private Long completionTime;

    public PlayerQuestProgress() {
    }

    public PlayerQuestProgress(UUID playerUuid, QuestData quest) {
        this.playerUuid = playerUuid.toString();
        this.quest = quest;
        this.completed = false;
        this.currentProgress = 0;
        this.startTime = System.currentTimeMillis();
        this.completionTime = null;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getPlayerUuid() {
        return UUID.fromString(playerUuid);
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid.toString();
    }

    public QuestData getQuest() {
        return quest;
    }

    public void setQuest(QuestData quest) {
        this.quest = quest;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed && completionTime == null) {
            this.completionTime = System.currentTimeMillis();
        }
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;

        // Check if quest is completed
        if (this.currentProgress >= this.quest.getTargetAmount() && !this.completed) {
            this.completed = true;
            this.completionTime = System.currentTimeMillis();
        }
    }

    public void incrementProgress(int amount) {
        this.currentProgress += amount;

        // Check if quest is completed
        if (this.currentProgress >= this.quest.getTargetAmount() && !this.completed) {
            this.completed = true;
            this.completionTime = System.currentTimeMillis();
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Long getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(Long completionTime) {
        this.completionTime = completionTime;
    }
}