package com.Minecraft_server.Narkomanka.database;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quests")
public class QuestData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "reward_cash")
    private double rewardCash;

    @Column(name = "reward_card_money")
    private double rewardCardMoney;

    @Column(name = "is_repeatable")
    private boolean repeatable;

    @Column(name = "quest_type")
    private String questType;

    @Column(name = "target_amount")
    private int targetAmount;

    @Column(name = "target_item")
    private String targetItem;

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestObjective> objectives = new ArrayList<>();

    public QuestData() {
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getRewardCash() {
        return rewardCash;
    }

    public void setRewardCash(double rewardCash) {
        this.rewardCash = rewardCash;
    }

    public double getRewardCardMoney() {
        return rewardCardMoney;
    }

    public void setRewardCardMoney(double rewardCardMoney) {
        this.rewardCardMoney = rewardCardMoney;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }

    public String getQuestType() {
        return questType;
    }

    public void setQuestType(String questType) {
        this.questType = questType;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(int targetAmount) {
        this.targetAmount = targetAmount;
    }

    public String getTargetItem() {
        return targetItem;
    }

    public void setTargetItem(String targetItem) {
        this.targetItem = targetItem;
    }

    public List<QuestObjective> getObjectives() {
        return objectives;
    }

    public void setObjectives(List<QuestObjective> objectives) {
        this.objectives = objectives;
    }

    public void addObjective(QuestObjective objective) {
        objectives.add(objective);
        objective.setQuest(this);
    }

    public void removeObjective(QuestObjective objective) {
        objectives.remove(objective);
        objective.setQuest(null);
    }
}

