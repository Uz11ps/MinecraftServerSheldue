# 🎮 Narkomanka - Minecraft Plugin

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.4-green?style=for-the-badge&logo=minecraft)
![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Paper](https://img.shields.io/badge/Paper-API-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

*Плагин, вдохновленный игрой Schedule, с системой квестов и экономики*

</div>

## 📋 Описание

**Narkomanka** - это комплексный плагин для Minecraft, который добавляет в игру элементы из игры Schedule. Плагин включает в себя систему квестов, экономику, NPC, систему выращивания растений и многое другое.

## ✨ Основные возможности

### 🎯 Система квестов
- Динамические квесты с различными целями
- Прогресс квестов сохраняется в базе данных
- Награды за выполнение квестов

### 💰 Экономическая система
- Управление наличными и картой
- Переводы между игроками
- Снятие средств

### 🌱 Система выращивания
- Различные типы растений
- Система почвы и удобрений
- Гроубоксы для выращивания

### 🤖 NPC система
- Торговцы наркотиками
- Поставщики товаров
- Полицейские
- Наркоманы и граждане
- Квестодатели

### 📞 Телефонная система
- Симуляция телефонных звонков
- Телефонные будки
- Миссии через телефон

### 🗑️ Система мусора
- Сбор и переработка мусора
- Мусорные станции
- Различные типы мусора

## 🛠️ Технические характеристики

- **Версия Minecraft:** 1.20.4
- **API:** Paper API
- **Java:** 17+
- **База данных:** PostgreSQL / H2 (fallback)
- **ORM:** Hibernate 6.2.13

## 📦 Установка

### Требования
- Minecraft Server 1.20.4
- Java 17 или выше
- Paper/Spigot сервер

### Шаги установки

1. **Скачайте плагин**
   ```bash
   git clone https://github.com/Uz11ps/MinecraftServerSheldue.git
   cd MinecraftServerSheldue
   ```

2. **Соберите проект**
   ```bash
   mvn clean package
   ```

3. **Установите плагин**
   - Скопируйте `target/Narkomanka-1.0-SNAPSHOT.jar` в папку `plugins/` вашего сервера
   - Перезапустите сервер

4. **Настройте базу данных** (опционально)
   - Отредактируйте `plugins/Narkomanka/config.yml`
   - Укажите параметры подключения к PostgreSQL

## ⚙️ Конфигурация

Основные настройки находятся в файле `config.yml`:

```yaml
database:
  type: "h2" # или "postgresql"
  host: "localhost"
  port: 5432
  database: "narkomanka"
  username: "user"
  password: "password"
```

## 🎮 Команды

### Основные команды
| Команда | Описание | Разрешение |
|---------|----------|------------|
| `/hello [игрок]` | Поприветствовать игрока | `narkomanka.hello` |
| `/balance` | Управление балансом | `narkomanka.balance` |
| `/quest [list/info/start] [id]` | Управление квестами | `narkomanka.quest` |
| `/grow [help/box/seed/soil/water/fertilizer]` | Система выращивания | `narkomanka.grow` |
| `/phone [help/simulate_call/give]` | Телефонная система | `narkomanka.phone` |
| `/trash [help/collector/info/reload]` | Система мусора | `narkomanka.trash` |

### Административные команды
| Команда | Описание | Разрешение |
|---------|----------|------------|
| `/spawn_dealer` | Создать торговца наркотиками | `narkomanka.admin` |
| `/spawn_vendor` | Создать поставщика товаров | `narkomanka.admin` |
| `/npc [populate/spawn/clear]` | Управление NPC | `narkomanka.admin` |
| `/trashstation [create/remove/list]` | Управление мусорными станциями | `narkomanka.admin` |

## 🏗️ Архитектура проекта

```
src/main/java/com/Minecraft_server/Narkomanka/
├── commands/          # Команды плагина
├── database/          # Управление базой данных
├── events/            # Обработчики событий
├── items/             # Кастомные предметы
├── listeners/         # Слушатели событий
├── npc/               # Система NPC
├── services/          # Бизнес-логика
├── trash/             # Система мусора
├── ui/                # Пользовательские интерфейсы
├── util/              # Утилиты
└── world/             # Управление миром
```

## 🔧 Разработка

### Сборка проекта
```bash
mvn clean compile
```

### Запуск тестов
```bash
mvn test
```

### Создание JAR файла
```bash
mvn clean package
```

## 🤝 Участие в разработке

1. Форкните репозиторий
2. Создайте ветку для новой функции (`git checkout -b feature/AmazingFeature`)
3. Зафиксируйте изменения (`git commit -m 'Add some AmazingFeature'`)
4. Отправьте в ветку (`git push origin feature/AmazingFeature`)
5. Откройте Pull Request

## 📝 Лицензия

Этот проект лицензирован под MIT License - см. файл [LICENSE](LICENSE) для деталей.

## 👨‍💻 Автор

**Uz1ps** - [GitHub](https://github.com/Uz11ps)

## 🙏 Благодарности

- Команде Paper за отличное API
- Сообществу Minecraft за вдохновение
- Разработчикам игры Schedule за оригинальную концепцию

---

<div align="center">

**⭐ Поставьте звезду, если проект вам понравился! ⭐**

</div>