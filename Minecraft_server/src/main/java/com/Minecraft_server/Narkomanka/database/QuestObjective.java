package com.Minecraft_server.Narkomanka.database;

import jakarta.persistence.*;

// Making QuestObjective a separate top-level class with @Entity annotation
@Entity
@Table(name = "quest_objectives")
public class QuestObjective {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quest_id")
    private QuestData quest;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "target_amount")
    private int targetAmount;

    @Column(name = "objective_type")
    private String objectiveType;

    @Column(name = "target_item")
    private String targetItem;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public QuestData getQuest() {
        return quest;
    }

    public void setQuest(QuestData quest) {
        this.quest = quest;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(int targetAmount) {
        this.targetAmount = targetAmount;
    }

    public String getObjectiveType() {
        return objectiveType;
    }

    public void setObjectiveType(String objectiveType) {
        this.objectiveType = objectiveType;
    }

    public String getTargetItem() {
        return targetItem;
    }

    public void setTargetItem(String targetItem) {
        this.targetItem = targetItem;
    }
}
