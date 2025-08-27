package com.Minecraft_server.Narkomanka.database;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "player_data")
public class PlayerData {
    @Id
    @Column(name = "player_uuid", nullable = false, length = 36)
    private String playerUuid;

    @Column(name = "player_name", nullable = false, length = 16)
    private String playerName;

    @Column(name = "cash_balance", nullable = false)
    private double cashBalance;

    @Column(name = "card_balance", nullable = false)
    private double cardBalance;

    @Column(name = "last_login")
    private long lastLogin;

    @Column(name = "phone_quality", nullable = true)
    private Integer phoneQuality;

    public PlayerData() {
        // Устанавливаем значение по умолчанию для телефона
        this.phoneQuality = 1;
    }

    public PlayerData(UUID playerUuid, String playerName) {
        this.playerUuid = playerUuid.toString();
        this.playerName = playerName;
        this.cashBalance = 0.0;
        this.cardBalance = 0.0;
        this.lastLogin = System.currentTimeMillis();
        this.phoneQuality = 1; // Базовое качество телефона
    }

    // Getters and setters
    public UUID getPlayerUuid() {
        return UUID.fromString(playerUuid);
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid.toString();
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
    }

    public void addCash(double amount) {
        this.cashBalance += amount;
    }

    public boolean removeCash(double amount) {
        if (this.cashBalance >= amount) {
            this.cashBalance -= amount;
            return true;
        }
        return false;
    }

    public double getCardBalance() {
        return cardBalance;
    }

    public void setCardBalance(double cardBalance) {
        this.cardBalance = cardBalance;
    }

    public void addCardBalance(double amount) {
        this.cardBalance += amount;
    }

    public boolean removeCardBalance(double amount) {
        if (this.cardBalance >= amount) {
            this.cardBalance -= amount;
            return true;
        }
        return false;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    /**
     * Получает качество телефона игрока (1-4)
     * 1 - Базовый, 2 - Улучшенный, 3 - Продвинутый, 4 - Премиум
     */
    public Integer getPhoneQuality() {
        return phoneQuality;
    }
    
    /**
     * Устанавливает качество телефона игрока (1-4)
     */
    public void setPhoneQuality(Integer phoneQuality) {
        // Проверяем, что значение находится в пределах допустимого диапазона
        if (phoneQuality != null) {
            if (phoneQuality < 1) {
                this.phoneQuality = 1;
            } else if (phoneQuality > 4) {
                this.phoneQuality = 4;
            } else {
                this.phoneQuality = phoneQuality;
            }
        }
    }
    
    /**
     * Улучшает телефон игрока на один уровень
     * @return true если улучшение возможно, false если уже максимальный уровень
     */
    public boolean upgradePhone() {
        if (phoneQuality != null && phoneQuality < 4) {
            phoneQuality++;
            return true;
        }
        return false;
    }
}