# LastMines

**LastMines** — плагин автошахт для серверов Minecraft (Paper / Folia). Вы выделяете область, а плагин сам
заполняет её блоками по таймеру: руды, камень, редкости, точное количество каждого блока — как настроите.
Игроки добывают блоки, шахта обновляется, считается статистика и топ.

- **Minecraft:** 1.21 – 26.3 (Paper и **Folia**)
- **Java:** 21+
- **Версия:** 0.5
- **Нужен плагин:** [LLib](https://github.com/LastDev1337/LLib) **0.2**
- **Wiki:** <https://wiki.laststudio.space/lastmines> · **Сайт:** <https://laststudio.space>

> Что изменилось с 0.4 → [CHANGELOG.md](CHANGELOG.md). Список плейсхолдеров → [docs/PLACEHOLDERS.md](docs/PLACEHOLDERS.md).

## Содержание

1. [Возможности](#возможности)
2. [Установка](#установка)
3. [Быстрый старт](#быстрый-старт)
4. [Команды и права](#команды-и-права)
5. [Настройка шахты](#настройка-шахты) — режимы, блоки, действия, сброс по проценту, копирование
6. [Статистика и топ](#статистика-и-топ)
7. [Плейсхолдеры PlaceholderAPI](#плейсхолдеры-placeholderapi)
8. [Для разработчиков: API и события](#для-разработчиков-api-и-события)
9. [Сборка](#сборка-из-исходников)
10. [Частые вопросы](#частые-вопросы)

---

## Возможности

- **Формы шахт:** прямоугольник, цилиндр, многоугольник, эллипсоид (область берётся из выделения WorldEdit).
- **Три режима:** `BLOCKS` (шансы блоков), `RARITY` (редкости с разными наборами блоков), `ALL_BLOCKS` (точное количество каждого блока).
- **Сброс** по таймеру, вручную или **по проценту** («осталось ≤ N% блоков — заполнить заново»).
- **Свои дропы** на блок (шанс, Fortune), лимиты `min`–`max` для отдельных блоков.
- **Действия** на события: сообщения, титры, звуки, команды консоли/игрока, деньги (Vault), очки (PlayerPoints), предметы, частицы.
- **Права и условия:** право на добычу в шахте, требования к зачарованиям инструмента, ограничение по онлайну.
- **Голограммы** с таймером (встроенные TextDisplay, DecentHolograms, FancyHolograms).
- **Графическое меню** (BMenu): всё настраивается без правки файлов.
- **Статистика добычи и топ** (H2, SQLite, MySQL, MariaDB, PostgreSQL), плейсхолдеры для табло.
- **Копирование шахт** одной командой, **миграция** из AutoMine, RealMines, CataMines.
- **Folia** — сброс шахты идёт по чанкам в потоках своих регионов.
- Языки: русский и английский.

## Установка

1. Установите **[LLib 0.2](https://github.com/LastDev1337/LLib)** (положите `LLib-0.2.jar` в `plugins/`).
2. Положите `LastMines-0.5.jar` в `plugins/` и **перезапустите** сервер (не `/reload`).
3. Для настройки через меню поставьте **BMenu**. Без него плагин работает, но меню нет — настраивайте файлы.
4. Для создания шахт нестандартной формы и выделения области нужен **WorldEdit** (или FAWE).

**Первый запуск требует интернета:** сервер сам скачает библиотеки баз данных (HikariCP, H2, SQLite, драйверы MySQL/MariaDB/PostgreSQL).
Это один раз, до 1–3 минут на медленном канале.

| Плагин | Нужен? | Зачем |
|---|---|---|
| **LLib 0.2** | да | команды, сообщения, действия, планировщик |
| BMenu | нет | графическое меню |
| WorldEdit / FAWE | нет | выделение области, фигуры |
| PlaceholderAPI | нет | плейсхолдеры `%lastmines_…%` и `%lm_…%` |
| Vault / PlayerPoints | нет | действия `[vault]` / `[playerpoints]` |
| DecentHolograms / FancyHolograms | нет | голограммы (встроенные работают без них) |

## Быстрый старт

1. Выделите область WorldEdit-палкой (`//wand`), встаньте рядом.
2. `/lm create mymine` — шахта создана и сразу заполняется.
3. `/lm gui mymine` — откроется меню: блоки, шансы, время, голограмма, права.
4. `/lm reset mymine` — принудительный сброс.

Алиасы корневой команды: `/lastmines`, `/lm`, `/mines`, `/lmine`, `/lmines`.

## Команды и права

| Команда | Что делает | Право |
|---|---|---|
| `/lm help` | справка | `lastmines.admin` |
| `/lm create <id>` | создать шахту из выделения WorldEdit | `lastmines.admin` |
| `/lm delete <id>` | удалить шахту | `lastmines.admin` |
| `/lm list` | список шахт и время до сброса | `lastmines.admin` |
| `/lm gui <id>` | меню управления (BMenu) | `lastmines.admin` |
| `/lm reset <id>` | сбросить сейчас | `lastmines.admin` |
| `/lm resettimer <id>` | вернуть таймер до сброса к полному значению | `lastmines.admin` |
| `/lm update <id> [rarity] [next]` | сменить редкость | `lastmines.admin` |
| `/lm tp <id> [игрок]` | телепорт к шахте (себя или игрока) | `lastmines.admin` |
| `/lm near [радиус]` | шахты рядом с вами | `lastmines.admin` |
| `/lm copy <шахта> <новая>` | **копия настроек** шахты | `lastmines.copy` |
| `/lm stats [игрок] [материал]` | **статистика добычи** | `lastmines.stats` (чужая — `lastmines.stats.others`) |
| `/lm top [материал] [страница]` | **топ добычи** | `lastmines.top` |
| `/lm migrate <плагин>` | перенос из AutoMine / RealMines / CataMines | `lastmines.admin` |
| `/lm reload` | перезагрузить плагин и конфиги | `lastmines.admin` |
| `/lm defaultmine` | восстановить стандартные шахты | `lastmines.admin` |
| `/lm language <ru\|en>` | сменить язык | `lastmines.admin` |

| Право | По умолчанию | Для чего |
|---|---|---|
| `lastmines.admin` | оператор | все административные команды |
| `lastmines.edit.commands` | оператор | добавлять действия `[console]` и `[player]` через меню |
| `lastmines.copy` | оператор | `/lm copy` |
| `lastmines.stats` | все | `/lm stats` о себе |
| `lastmines.stats.others` | оператор | `/lm stats` о других игроках |
| `lastmines.top` | все | `/lm top` |

Команды `stats` и `top` доступны игрокам без `lastmines.admin`.
Если вам нужно, чтобы шахту ломали только избранные, включите в шахте `permissions` — оно задаёт отдельное право на добычу.

## Настройка шахты

Каждая шахта — файл `plugins/LastMines/mines/<id>.yml`. Проще всего править через `/lm gui`, но файл тоже можно менять
(потом `/lm reload`).

```yaml
name: mymine
world: world
mode: BLOCKS                 # BLOCKS | RARITY | ALL_BLOCKS
reset_time: 5m               # через сколько сбрасывать
reset_percent: 0             # 0 = выключено; 30 = сбросить, когда осталось ≤ 30% блоков
stopped: false               # true = таймер стоит

positions:
  shape: cuboid              # cuboid | cylinder | polygon | ellipsoid
  points:
    - '0;100;0'
    - '5;105;5'

blocks:
  - id: stone
    chance: 80.0
    drop: [ stone ]          # пусто = ванильный дроп
  - id: diamond_ore
    chance: 5.0
```

### Режимы

| Режим | Как работает |
|---|---|
| `BLOCKS` | у каждого блока свой шанс (сумма не обязана быть 100) |
| `RARITY` | несколько «редкостей» с шансом и своим набором блоков; на каждый сброс выбирается следующая |
| `ALL_BLOCKS` | **точное количество** каждого блока; остаток занимает `filler` |

**Пример `ALL_BLOCKS`:**

```yaml
mode: ALL_BLOCKS
filler: stone                # чем заполнить всё остальное
blocks:
  - id: diamond_ore
    count: 5                 # ровно 5 алмазных руд
  - id: gold_ore
    count: 3
```

Блоки перемешиваются и раскладываются по шахте случайно. Если сумма `count` больше объёма шахты,
лишнее обрезается, а в консоль пишется предупреждение.

### Сброс по проценту

`reset_percent: 30` — когда в шахте остаётся не больше 30% блоков, она сразу перезаполняется целиком.
Работает с любым режимом.

### Действия

Строки в разделе `actions:` выполняются на события. Формат:

```
[тип] [когда] [кому] [chance:N] значение
```

| Часть | Варианты |
|---|---|
| **тип** | `message`, `title`, `actionbar`, `sound`, `console`, `player`, `vault`, `playerpoints`, `give`, `particle`… |
| **когда** | `[update]` — после сброса · `[update:15]` — за 15 секунд до сброса · `[break]` — добыт любой блок · `[break:DIAMOND_ORE]` — добыт этот блок |
| **кому** | `[all]` — всем онлайн (по умолчанию) · `[mine]` — тем, кто внутри шахты · `[radius:20]` — в 20 блоках от **границы** шахты · `[self]` — игроку, сломавшему блок (для `[break]` по умолчанию) |
| **шанс** | `[chance:30]` — сработает в 30% случаев |

Теги можно ставить в любом месте строки. Формат 0.4 (`[message] [update:15] текст`) работает как раньше.

```yaml
actions:
  - '[message] [update:10] &fШахта обновится через 10 секунд'
  - '[title] [update] "&6Шахта обновлена!\n&7Приходите за рудой" 10;40;10'
  - '[sound] [update] entity.villager.yes 0.8 1'
  - '[vault] [break:DIAMOND_ORE] give 50'                     # +50 монет за алмазную руду
  - '[message] [break:DIAMOND_ORE] [chance:10] &bВам повезло!'
  - '[console] [update] say Шахта %mine% обновилась'          # выполнится один раз, от консоли
```

Подстановки: `%player%`, `%breaker%` (кто сломал), `%material%`, `%mine%`, `{BLOCKS_LEFT}`, `{BLOCKS_MAX}`,
`%time%`, `%rarity_current%`, а также любые плейсхолдеры PlaceholderAPI.

Правила:
- `[console]`, `[broadcast]` и `*_all` выполняются **один раз** от консоли (даже если игроков нет онлайн).
- `[vault]`/`[playerpoints]` работают только если установлены Vault / PlayerPoints; нет плагина — одно предупреждение в консоли.
- Команды `[console]`/`[player]` через меню может добавлять только игрок с правом `lastmines.edit.commands`.
- Если у игрока не хватает денег, `take` не выполняется.

Полный список типов действий — в [README LLib](https://github.com/LastDev1337/LLib#встроенные-действия).

### Права, зачарования, онлайн, телепорт, голограмма

```yaml
permissions:                 # кто может добывать
  enable: true
  value: 'lastmines.mine.mymine'
  messages:
    - '[message] &cУ вас нет права добывать здесь!'

enchant_requirements:        # какая кирка нужна
  enable: true
  enchantments:
    efficiency: 3
  actions:
    - '[message] &cВаша кирка недостаточно зачарована!'

online:                      # таймер идёт только при нужном онлайне
  enable: true
  min: 1
  max: 1000

teleport:
  enable: true
  pos: '0;101;0;0.0;0.0'     # x;y;z;yaw;pitch

hologram:
  enable: true
  provider: Vanilla          # Vanilla | DecentHolograms | FancyHolograms
  offset: '2;106;2'
  texts:
    - '&6Моя шахта'
    - '&fДо обновления: %time:detail%'
```

### Копирование шахты

`/lm copy mymine mymine2` копирует **все настройки** (блоки, шансы, действия, права, время…).
**Не копируются:** область, точка телепорта, голограмма, статистика.
Копия создаётся **остановленной и без области** — задайте ей область и снимите остановку.

### Файлы плагина

| Файл | Назначение |
|---|---|
| `config.yml` | язык, отладка, модули |
| `messages.yml` | все сообщения (редактируемые) |
| `sql.yml` | хранилище статистики и её настройки |
| `modules/commands.yml` | включение и алиасы команд |
| `modules/time-format.yml` | формат времени |
| `menus/*.yml` | внешний вид меню |
| `mines/*.yml` | ваши шахты |

## Статистика и топ

Считаются добытые блоки: всего и по каждому материалу. Данные копятся в памяти и раз в несколько секунд
пачкой пишутся в базу в фоне — добыча не тормозит сервер.

Хранилище выбирается в `sql.yml`:

```yaml
driver: H2                   # Memory | H2 | SQLite | MySQL | MariaDB | PostgreSQL
table_prefix: 'lastmines'

mysql:
  host: 'localhost:3306'
  name: 'lastmines'
  user: 'user'
  pass: 'password'

stats:
  enable: true
  flush_interval: 5s         # как часто писать в базу
  top_refresh: 45s           # как часто обновлять топ
  top_size: 100              # сколько мест в топе держать в памяти
```

- `H2` (по умолчанию) и `SQLite` — файл рядом с плагином, для одного сервера.
- `MySQL` / `MariaDB` / `PostgreSQL` — общая база для нескольких серверов (данные суммируются, а не перезаписываются).
- `Memory` — без сохранения, для проверки.
- Не нужна статистика — `stats.enable: false`.

Команды: `/lm stats` (о себе), `/lm stats Steve diamond_ore`, `/lm top`, `/lm top diamond_ore 2`.

## Плейсхолдеры PlaceholderAPI

Нужен PlaceholderAPI. Есть два префикса с одинаковым набором статистики: `%lastmines_…%` и короткий `%lm_…%`.

**Про шахты** (`%lastmines_…%`, `<id>` — id шахты):

| Плейсхолдер | Значение |
|---|---|
| `%lastmines_time_<id>%` | время до сброса |
| `%lastmines_time_detail_<id>%`, `%lastmines_time_clock_<id>%` | то же в других форматах |
| `%lastmines_rarity_current_<id>%`, `%lastmines_rarity_next_<id>%` | текущая / следующая редкость |
| `%lastmines_blocks_current_count_<id>%` | сколько блоков осталось |
| `%lastmines_blocks_max_count_<id>%` | сколько блоков было при сбросе |
| `%lastmines_count%` | число шахт |

**Статистика** (читают только память, базу не трогают):

| Плейсхолдер | Значение |
|---|---|
| `%lm_top_1_name%`, `%lm_top_1_value%` | имя и количество игрока на 1-м месте |
| `%lm_top_3_name_diamond_ore%`, `%lm_top_3_value_diamond_ore%` | топ по материалу |
| `%lm_blocks_total%` | сколько блоков добыл игрок |
| `%lm_blocks_diamond_ore%` | сколько алмазных руд добыл игрок |
| `%lm_rank%`, `%lm_rank_diamond_ore%` | место игрока в топе (`-`, если не в топе) |

Пустое место в топе: имя `-`, значение `0`. Регистр материала не важен.
Те же значения доступны как `%lastmines_top_1_name%`, `%lastmines_stats_total%`, `%lastmines_stats_diamond_ore%`.
Локальные плейсхолдеры внутри плагина (`{MINE_ID}`, `%time%`, …) — в [docs/PLACEHOLDERS.md](docs/PLACEHOLDERS.md).

---

## Для разработчиков: API и события

LastMines сам по себе — плагин, но у него есть открытое API.

### Подключение

Репозиторий: `https://repo.laststudio.space/releases`, координаты `ru.last.mines:lastmines:0.5`.

**Maven**

```xml
<repository><id>laststudio</id><url>https://repo.laststudio.space/releases</url></repository>

<dependency>
    <groupId>ru.last.mines</groupId>
    <artifactId>lastmines</artifactId>
    <version>0.5</version>
    <scope>provided</scope>
</dependency>
```

**Gradle (Groovy)**

```groovy
repositories { maven { url 'https://repo.laststudio.space/releases' } }
dependencies { compileOnly 'ru.last.mines:lastmines:0.5' }
```

**Gradle Kotlin DSL**

```kotlin
repositories { maven("https://repo.laststudio.space/releases") }
dependencies { compileOnly("ru.last.mines:lastmines:0.5") }
```

В `plugin.yml` вашего плагина: `depend: [ LastMines ]` (или `softdepend`).

### API

```java
import ru.last.mines.api.LastMinesAPI;
import ru.last.mines.api.LastMinesProvider;
import ru.last.mines.mine.Mine;

LastMinesAPI api = LastMinesProvider.getApi();

Mine mine = api.getMine("mymine");           // по id
Mine here = api.getMineAt(player);            // шахта, в которой стоит игрок (или null)
api.resetMine("mymine");                      // принудительный сброс
api.openGui(player, "mymine");                // меню (нужен BMenu)
api.getMinesInRadius(player.getLocation(), 50);
```

### События (Bukkit)

`MineCreateEvent`, `MineDeleteEvent`, `MineLoadEvent`, `MineUnloadEvent`, `MinePreResetEvent` (можно отменить),
`MineResetEvent`, `MineBlockBreakEvent` (можно отменить), `MineTeleportEvent` (можно отменить), `MineUpdateRarityEvent`.

```java
@EventHandler
public void onReset(MineResetEvent e) {
    getLogger().info("Шахта " + e.getMine().getId() + " обновлена");
}
```

> Если ваш код работает на Folia: не вызывайте методы Bukkit из события в чужом потоке — используйте `LLibAPI.get().scheduler()`
> (см. [README LLib](https://github.com/LastDev1337/LLib#планировщик-paper-и-folia)).

## Сборка из исходников

```bash
git clone https://github.com/LastDev1337/LastMines
cd LastMines
gradle clean build           # jar: build/libs/LastMines-0.5.jar
gradle --stop                # остановить демон Gradle, чтобы не занимал память
```

LastMines собирается против LLib. Если исходники LLib лежат рядом (`../LLib`), подключение уже настроено в `settings.gradle.kts`
через `includeBuild` с `dependencySubstitution`. Нужна Java 21+.

## Частые вопросы

**Плагин выключился при старте.** Проверьте консоль: нужна версия 1.21–26.3 и установленная LLib 0.2.

**Ошибка загрузки библиотек при первом запуске.** Нужен интернет; повторите запуск. Библиотеки сохраняются в `libraries/`.

**Шахта не заполняется.** Проверьте, что у неё есть область (`positions`) и блоки; копия через `/lm copy` создаётся **без области**.

**В консоли `Статистика недоступна`.** Откройте `sql.yml`: проверьте `driver`, доступ к базе, или отключите статистику (`stats.enable: false`).

**Работает ли на Folia?** Да, проверено на Folia 1.21.11 и 26.2. Графическое меню на Folia недоступно: в BMenu нет пометки `folia-supported`, и Folia его не загружает — настраивайте шахты файлами.

**Как перенести шахты из другого плагина?** `/lm migrate AutoMine` (или `RealMines`, `CataMines`).

**Как изменить язык?** `/lm language en` (или `ru`).

---

Лицензия — см. файл [LICENSE](LICENSE). Автор: Last · LastStudio.
