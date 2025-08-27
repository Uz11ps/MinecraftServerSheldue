package com.Minecraft_server.Narkomanka.database;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Класс для хранения прогресса квестов игрока
 */
@Entity
@Table(name = "quest_progress")
public class QuestProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "player_uuid", nullable = false, length = 36)
    private String playerUuid;
    
    @Column(name = "quest_id", nullable = false)
    private Long questId;
    
    @Column(name = "current_amount", nullable = false)
    private int currentAmount;
    
    @Column(name = "is_completed", nullable = false)
    private boolean completed;
    
    @Column(name = "start_time", nullable = false)
    private long startTime;
    
    @Column(name = "complete_time")
    private Long completeTime;
    
    public QuestProgress() {
    }
    
    public QuestProgress(UUID playerUuid, Long questId) {
        this.playerUuid = playerUuid.toString();
        this.questId = questId;
        this.currentAmount = 0;
        this.completed = false;
        this.startTime = System.currentTimeMillis();
    }
    
    // Геттеры и сеттеры
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
    
    public Long getQuestId() {
        return questId;
    }
    
    public void setQuestId(Long questId) {
        this.questId = questId;
    }
    
    /**
     * Получает текущее количество (прогресс)
     */
    public int getCurrentAmount() {
        return currentAmount;
    }
    
    public void setCurrentAmount(int currentAmount) {
        this.currentAmount = currentAmount;
    }
    
    public void incrementAmount(int amount) {
        this.currentAmount += amount;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed && completeTime == null) {
            this.completeTime = System.currentTimeMillis();
        }
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public Long getCompleteTime() {
        return completeTime;
    }
    
    public void setCompleteTime(Long completeTime) {
        this.completeTime = completeTime;
    }
} 