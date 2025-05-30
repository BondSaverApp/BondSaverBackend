## Описание проекта

BondSaver Backend — это серверное приложение, разработанное для поддержки функционала мобильных приложений на платформах Android и iOS, предназначенных для помощи в поддержании социальных связей с использованием технологий искусственного интеллекта. Бэкенд обеспечивает авторизацию, регистрацию, синхронизацию данных и генерацию персонализированных тем для обсуждения на основе истории взаимодействий.

## Технологический стек

- **Язык программирования**: Kotlin
- **Фреймворк**: Spring
- **База данных**: MongoDB
- **Аутентификация**: JSON Web Tokens (JWT) и Cookie (secure, http-only)

## Основной функционал

1. **Авторизация и регистрация**:
   - Генерация и хранение сессий с использованием JWT.
   - Поддержка endpoint’ов для регистрации, авторизации, обновления токенов и выхода из аккаунта.
   - Безопасная передача refresh-токенов через cookie.

2. **Синхронизация данных**:
   - Получение от клиента списка актуальных и неактуальных контактов и встреч на основе дат обновления.
   - Отправка клиенту обновлённых данных и списка сущностей для синхронизации.
   - Разрешение конфликтов с приоритетом более поздней версии данных.

3. **Генерация тем для обсуждения**:
   - Использование API Large Language Model (LLM) для создания персонализированных тем на основе истории взаимодействий с контактами.
   - Обработка запросов от мобильного приложения и возврат сгенерированных тем.

## Структура проекта

- **Контроллеры**: Обработка HTTP-запросов для авторизации, синхронизации и генерации тем.
- **Сервисы**: Бизнес-логика для работы с JWT, синхронизацией данных и взаимодействием с LLM.
- **Репозитории**: Взаимодействие с MongoDB для хранения данных о пользователях, контактах и встречах.
- **Модели**: Описание сущностей (пользователи, контакты, встречи).
- **Конфигурация**: Настройка Spring, JWT и подключения к MongoDB.

## Требования

- Java 17+
- Kotlin 1.8+
- Spring Boot 3.x
- MongoDB 5.0+

## Установка и запуск

1. Склонируйте репозиторий: `git clone <repository-url>`.
2. Настройте MongoDB и укажите параметры подключения в `application.properties`.
3. Выполните сборку: `./gradlew build`.
4. Запустите приложение: `./gradlew bootRun`.
