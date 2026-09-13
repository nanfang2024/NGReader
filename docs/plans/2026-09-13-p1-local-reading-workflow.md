---
intent: 搭建 NGBook 安卓工程并跑通本地核心阅读闭环——书架、SAF 导入、格式嗅探、TXT(UTF-8/GBK)与 EPUB 可读、进度自动续读、JellyTheme 玻璃拟态基座。
success_criteria: gradle assembleDebug 产出可安装 app-debug.apk；TXT/EPUB 样本可读且杀进程后回到上次位置；编码/分页/DAO/主题/续读测试全绿。
risk_level: medium
auto_approve: true
branch: hotl/p1-local-reading
worktree: false
dirty_worktree: allow
---

约定：所有 gradle 命令使用沙箱系统 Gradle 8.14.5（`gradle`，非 wrapper）；`source /workspace/.ngbook-env` 提供 `JAVA_HOME_21` 与 `ANDROID_HOME`；设计依据见 `docs/designs/2026-09-13-phase-1-local-reading-design.md`。

## Steps

- [x] **Step 1: 安装 JDK 21（构建守护 JVM）**
action: 按序尝试三级回退直至成功——(a) `apt-get update && apt-get install -y openjdk-21-jdk-headless`，JAVA_HOME_21 取 `/usr/lib/jvm/java-21-openjdk-amd64`；(b) `mise install java@temurin-21`，JAVA_HOME_21 取 `mise where java@temurin-21`；(c) 从 `https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/x64/linux/` 列目录取最新 OpenJDK21 tar.gz 解压至 `/opt/jdk21`。将 `JAVA_HOME_21`、`ANDROID_HOME=/opt/android-sdk` 两行 export 写入 `/workspace/.ngbook-env`。
loop: until `. /workspace/.ngbook-env && "$JAVA_HOME_21/bin/java" -version 2>&1 | grep -q '21\.'`
max_iterations: 3
verify: bash -lc 'set -a; . /workspace/.ngbook-env; "$JAVA_HOME_21/bin/java" -version' 2>&1 | grep -q '"21\.'
gate: auto

- [x] **Step 2: 安装 Android SDK（cmdline-tools + platform 35 + build-tools 35）**
action: `mkdir -p /opt/android-sdk/cmdline-tools`；下载 `https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip` 解压并将得到的 cmdline-tools 目录移动为 `/opt/android-sdk/cmdline-tools/latest`；所有 sdkmanager 调用一律带前缀 `JAVA_HOME="$JAVA_HOME_21"`（先 source /workspace/.ngbook-env）：`yes | sdkmanager --sdk_root=/opt/android-sdk --licenses`，再 `sdkmanager --sdk_root=/opt/android-sdk "platform-tools" "platforms;android-35" "build-tools;35.0.0"`；写 `/workspace/local.properties` 内容一行 `sdk.dir=/opt/android-sdk`。
loop: until sdkmanager --list_installed 同时包含 platforms;android-35 与 build-tools;35
max_iterations: 3
verify: bash -lc 'set -a; . /workspace/.ngbook-env; JAVA_HOME="$JAVA_HOME_21" "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --list_installed 2>/dev/null | grep -c -e "platforms;android-35" -e "build-tools;35"' 2>&1 | grep -q '[2-9]'
gate: auto

- [x] **Step 3: 生成工程骨架（Gradle Kotlin DSL + 版本目录 + Compose + Hilt）**
action: 在 /workspace 创建——`settings.gradle.kts`：pluginManagement 与 dependencyResolutionManagement 仓库依次 google()、mavenCentral()，另加 `maven("https://maven.aliyun.com/repository/google")` 与 `.../public` 以 content 过滤作回退，repositoriesMode 设 PREFER_SETTINGS，rootProject.name="NGBook"，include(":app")；`gradle.properties`：`org.gradle.java.home=` 填 Step 1 解析出的 JDK21 字面路径、`org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=768m`、`org.gradle.workers.max=2`、`android.useAndroidX=true`、`kotlin.code.style=official`；`gradle/libs.versions.toml` 锁定：agp=8.13.2, kotlin=2.4.20, ksp=2.3.12, composeBom=2026.09.00, room=2.8.5, hilt=2.60.1, lifecycle=2.11.0, activityCompose=1.13.0, navigation=2.10.1, datastore=1.2.1, coreKtx=1.19.0, coroutines=1.11.0, robolectric=4.16.1, junit=4.13.2；`app/build.gradle.kts`：插件 com.android.application、org.jetbrains.kotlin.android、org.jetbrains.kotlin.plugin.compose、com.google.devtools.ksp、com.google.dagger.hilt.android；namespace=com.book.ng，compileSdk=35，minSdk=24，targetSdk=35，versionName "0.1.0"，jvmTarget/compileOptions 17，compose BOM 依赖 + activity-compose + navigation-compose + lifecycle-viewmodel-compose，测试依赖 junit/robolectric/coroutines-test；`app/src/main/AndroidManifest.xml`（application name=.NGBookApp、MainActivity launcher、theme Material 无 ActionBar）；`NGBookApp.kt`（@HiltAndroidApp）、`MainActivity.kt`（@AndroidEntryPoint，setContent 显示居中 Text "NGBook"）、`ui/theme/Theme.kt` 占位 MaterialTheme、`res/values/strings.xml`（app_name=NGBook）、空 `app/src/test/java/com/book/ng/` 目录。
verify:
  - type: artifact
    path: gradle
    assert:
      kind: matches-glob
      value: "*.toml"
  - type: artifact
    path: app
    assert:
      kind: exists
gate: auto

- [x] **Step 4: 首次构建通过（版本矩阵校准循环）**
<!-- 校准记录(2026-09-13, 12次尝试): 代理需JVM注入-Db http(s).proxy; Kotlin 2.4.20移除kotlinOptions→compilerOptions DSL; Hilt/AGP互锁→升AGP 9.4.0+Gradle 9.6.0(androidx 2026.09代要求minAgp9.1/compileSdk37); AGP9内置Kotlin(移除kotlin.android插件, compose插件保留); 平台37目录android-37.0(ApiLevel=37.0可解析); cgroup4GB防OOM→Xmx1536m+Kotlin daemon 768m; 最终矩阵: agp9.4.0/gradle9.6.0/kotlin2.4.20/ksp2.3.12/hilt2.60.1/composeBom2026.09.00/compileSdk37. 详细偏差在Step4人工门披露 -->
action: `cd /workspace && gradle :app:assembleDebug --console=plain`；失败则按序就地修正并记录进版本目录——KSP 与 Kotlin 2.4.20 不兼容：查 `https://repo.maven.apache.org/maven2/com/google/devtools/ksp/symbol-processing-gradle-plugin/maven-metadata.xml` 取匹配 2.4.20 的 KSP，若无则 kotlin 降为其与 KSP 有匹配的最高 2.3.x（kotlin.plugin.compose 版本随之联动）；AGP 要求更高 Gradle：AGP 降至 8.12.3；androidx 依赖（Compose BOM/room/lifecycle）报 compileSdk 35 过低：用 `JAVA_HOME="$JAVA_HOME_21" sdkmanager "platforms;android-36" "build-tools;36.0.0"` 补装并把 compileSdk/targetSdk 提到 36；JVM 版本类报错：复核 gradle.properties 的 org.gradle.java.home 指向 JDK21；单依赖解析 404：改走 aliyun 镜像坐标或调整该库小版本；Hilt 插件 id 若 2.60.1 变更则查其官方插件坐标修正。
loop: until gradle :app:assembleDebug 输出 BUILD SUCCESSFUL
max_iterations: 5
verify:
  - type: shell
    command: bash -lc 'set -a; . /workspace/.ngbook-env; cd /workspace && gradle :app:assembleDebug --console=plain' 2>&1 | grep -q 'BUILD SUCCESSFUL'
  - type: artifact
    path: app/build/outputs/apk/debug
    assert:
      kind: matches-glob
      value: "*.apk"
gate: human

- [x] **Step 5: JellyTheme 基座（明暗 × 4 套果冻配色）**
action: 创建 `ui/theme/Color.kt`——4 套调色板枚举 JellyPalette{MATCHA, GRAPE, SAKURA, OCEAN}，各含 light/dark 的 primary/secondary/tertiary/容器色（马卡龙低饱和 + 高明度，深底亮字）；`ui/theme/Theme.kt` 重构——`NGBookTheme(themeMode: JellyThemeMode, palette: JellyPalette, content)` 计算 isDark（system 时取平台配置），映射 Material3 ColorScheme 与 Typography，ThemeMode 枚举{SYSTEM, LIGHT, DARK}；`ui/theme/Glass.kt`——Modifier.jellyGlass(shape, alpha) 半透明渐变填充 + 1dp 高光描边 + 投影叠加的玻璃拟态面；MainActivity 根 Composable 接入 NGBookTheme 默认 MATCHA/SYSTEM。
verify: bash -lc 'set -a; . /workspace/.ngbook-env; cd /workspace && gradle :app:assembleDebug --console=plain' 2>&1 | grep -q 'BUILD SUCCESSFUL'
gate: auto

- [x] **Step 6: 主题设置 DataStore + 切换即时生效测试**
action: 创建 `ui/settings/JellySettingsRepository.kt`——DataStore("jelly_prefs")，键 theme_mode 与 palette，暴露 Flow<JellySettings> 与 setThemeMode / setPalette 挂起函数，枚举以 name 存储；MainActivity 收集该 Flow 驱动 NGBookTheme 参数；创建测试 `app/src/test/java/com/book/ng/ui/theme/JellyThemeTest.kt`（Robolectric + createComposeRule）：setContent 分别注入 MATCHA 与 GRAPE，断言两配色 primary 通道值不同，且切换 palette 后同 Composition 内读取的 token 即时变化（不重建 Activity）。
loop: until gradle :app:testDebugUnitTest --tests '*JellyTheme*' 绿
max_iterations: 3
verify: bash -lc 'set -a; . /workspace/.ngbook-env; cd /workspace && gradle :app:testDebugUnitTest --tests "*JellyTheme*" --console=plain' 2>&1 | grep -q 'BUILD SUCCESSFUL'
gate: auto

- [x] **Step 7: 领域模型 + 格式抽象 + LocatorCodec**
action: 创建 `domain/model/Models.kt`——enum BookFormat{EPUB, TXT, MOBI, AZW3, PDF, CBZ, ZIP, UNKNOWN}、data class LibraryBook(id: Long, title, author: String?, fileName, format: BookFormat, addedAt: Long, lastReadAt: Long?)、enum ReadMode{PAGING, SCROLL}、data class ReadingLocator(bookId, chapterIndex: Int, page: Int, scrollOffset: Int, mode: ReadMode)；`domain/locator/LocatorCodec.kt`——encode 输出 `v1:<chapter>:<page>:<offset>:<modeShort>` 紧凑串，decode 反向解析并对非法输入抛 IllegalArgumentException；`domain/parser/FormatParser.kt`——interface FormatParser { fun supports(format: BookFormat): Boolean; suspend fun parse(file: File): ParsedBook }，sealed interface ParsedBook { class Text(val title: String, val chapters: List<TextChapter>); class Comic(val title: String) }，data class TextChapter(index, title, startOffset: Int, endOffset: Int)；测试 `LocatorCodecTest.kt`：往返一致 + 4 类非法串拒绝。
loop: until 新增测试全绿
max_iterations: 3
verify: bash -lc 'set -a; . /workspace/.ngbook-env; cd /workspace && gradle :app:testDebugUnitTest --tests "*LocatorCodec*" --console=plain' 2>&1 | grep -q 'BUILD SUCCESSFUL'
gate: auto

- [x] **Step 8: Room 数据层（书架 + 进度）+ DAO 测试**
action: app 模块应用 room-ktx 依赖 + ksp room-compiler（版本目录 room=2.8.5）；创建 `data/db/BookEntity.kt`（表 books：自增 id、title、author、fileName、format 存 name、addedAt、lastReadAt 索引 addedAt）、`ProgressEntity.kt`（表 progress：bookId 主键外键级联删除、locator 串、mode、updatedAt）、`BookDao.kt`（upsert/Flow 全量按 lastReadAt 优先排序/getById/delete）、`ProgressDao.kt`（upsert/Flow getByBookId/delete）、`NGBookDatabase.kt`（version=1, exportSchema=false）；`di/AppModule.kt` Hilt 提供 database 与两 DAO；`data/repository/LibraryRepository.kt`（拷贝导入文件至 filesDir/books/<uuid>.<ext> 再入库、delete 连带删文件）、`ProgressRepository.kt`（save/get Flow）；测试 `DaoTest.kt`（Robolectric 内存库：insert→Flow 首发射含书名→delete→表空；进度 upsert 覆盖不重复）。
loop: until DaoTest 全绿
max_iterations: 3
verify: bash -lc 'set -a; . /workspace/.ngbook-env; cd /workspace && gradle :app:testDebugUnitTest --tests "*DaoTest*" --console=plain' 2>&1 | grep -q 'BUILD SUCCESSFUL'
gate: auto

- [x] **Step 9: 格式嗅探 + TXT 编码检测（先红后绿）**
action: 创建测试 `FormatSnifferTest.kt` 与 `TxtCharsetDetectorTest.kt` 并先行实现对应类——`data/importer/FormatSniffer.kt`：读头部魔数——`PK\x03\x04` 再扫中央目录判定：含 META-INF/container.xml→EPUB、含 image entry→CBZ、否则 ZIP；`%PDF`→PDF；偏移 60 处 `BOOKMOBI`→MOBI；其余按扩展名 txt 命中 TXT 否则 UNKNOWN；`domain/text/TxtCharsetDetector.kt`：detect(bytes) 顺序——BOM 命中（UTF-8/UTF-16LE/BE）；严格 UTF-8 字节序列校验通过→UTF-8；GBK 解码且 CJK 常用区字符占比≥0.6→GBK；兜底 UTF-8+替换符；测试用例含"中文字样"GBK 字节样本、UTF-8 无 BOM 样本、UTF-16LE+BOM、纯 ASCII、随机字节兜底。
loop: until 两类测试全绿
max_iterations: 4
verify: bash -lc 'set -a; . /workspace/.ngbook-env; cd /workspace && gradle :app:testDebugUnitTest --tests "*FormatSniffer*" --tests "*TxtCharset*" --console=plain' 2>&1 | grep -q 'BUILD SUCCESSFUL'
gate: auto

- [x] **Step 10: TXT 章节切分器 + 测试**
action: 创建 `domain/text/TxtChapterSplitter.kt`——按行扫描，正则匹配标题行 `^\s*(第\s*[0-9〇零一二三四五六七八九十百千万两]+\s*[章卷回节篇集]|序言|前言|楔子|后记|尾声|附录)`（允许行尾空白、CRLF），命中则以前一行为上一章边界，生成 List<TextChapter>（index、title=该行截 30 字、startOffset/endOffset 为字符区间）；命中数<2 时回退固定 3000 字切章并命名"第N部分"；空文件返回单空章；测试覆盖：50 章中文数字+阿拉伯混编、CRLF 文件、无章节书回退、标题行前后空白容错。
loop: until 测试全绿
max_iterations: 3
verify: bash -lc 'set -a; . /workspace/.ngbook-env; cd /workspace && gradle :app:testDebugUnitTest --tests "*TxtChapterSplitter*" --console=plain' 2>&1 | grep -q 'BUILD SUCCESSFUL'
gate: auto

- [x] **Step 11: TXT 分页器（StaticLayout 测量）+ 测试**
action: 创建 `feature/reader/text/TextPaginator.kt`——输入 List<TextChapter> 与 Viewport(宽dp、高dp、字号sp、行距倍率、内边距)，用 android.text.StaticLayout 逐章测量断页，输出 PagedDocument(pages: List<PageSpec(chapterIndex, startOffset, endOffset)>, pageCount) 与 API：pageAt(i)、globalIndexOf(chapterIndex, charOffset)（二分）、相邻页边界连续校验；测试（Robolectric 提供真实文本度量）：1 万字样本在 360×640/18sp 下 pageCount>10、同参数重复分页结果一致、pageAt 区间首尾相接无重叠、globalIndexOf 命中所在页。
loop: until 测试全绿
max_iterations: 4
verify: bash -lc 'set -a; . /workspace/.ngbook-env; cd /workspace && gradle :app:testDebugUnitTest --tests "*TextPaginator*" --console=plain' 2>&1 | grep -q 'BUILD SUCCESSFUL'
gate: auto

- [x] **Step 12: 书架页 + SAF 导入 + 路由**
action: 创建 `feature/shelf/ShelfViewModel.kt`（Hilt 注入 LibraryRepository，StateFlow<ShelfUiState>：books/isEmpty/importing 进度消息）、`feature/shelf/ShelfScreen.kt`——LazyVerticalGrid 卡片（jellyGlass 面 + 书名首字渐变占位封面 + 标题 + 进度百分比角标）、顶栏标题 NGBook、右下 FAB"导入"启动 Activity result 契约 `OpenMultipleDocuments`（mime */* 多选），onResult 逐份 contentResolver 流式拷贝并嗅探入库，非本地支持格式仅入库不路由；未知格式卡片点击 Snackbar "该格式将在后续阶段支持"；创建 `ui/nav/NavGraph.kt` navigation-compose：routes shelf、reader_txt/{bookId}、reader_epub/{bookId}；MainActivity 接管 SAF 回调并接入导航。
verify: bash -lc 'set -a; . /workspace/.ngbook-env; cd /workspace && gradle :app:assembleDebug --console=plain' 2>&1 | grep -q 'BUILD SUCCESSFUL'
gate: auto

- [x] **Step 13: TXT 阅读器（翻页/滚动双模式 + 进度写回）**
action: 创建 `feature/reader/text/ReaderViewModel.kt`——加载 TXT（检测编码→切章→分页），收集 ReadingLocator 恢复初始页，暴露 state(当前页、章节号、mode、fontSize) 与 onPageChange/onModeChange/onFontSizeChange，页面变化与 onCleared 时 LocatorCodec 写回 ProgressRepository；`TextReaderScreen.kt`——模式 A 翻页：HorizontalPager 每页 Canvas 绘制 StaticLayout，左右 1/3 点击=上/下一页、中央=浮层菜单；模式 B 滚动：LazyColumn 按章渲染全文，滚动位置映射 locator.scrollOffset；浮层菜单（jellyGlass 面板）：翻页/滚动切换、字号 -/+（14–30sp）、顶部章节标题底部进度条；TXT 格式路由接入 reader_txt。
loop: until assembleDebug 成功
max_iterations: 3
verify: bash -lc 'set -a; . /workspace/.ngbook-env; cd /workspace && gradle :app:assembleDebug --console=plain' 2>&1 | grep -q 'BUILD SUCCESSFUL'
gate: auto

- [x] **Step 14: EPUB 样本构造 + Readium 最简链路**
action: 构造测试样本——在 /tmp/epubsrc 建 `mimetype`（内容 application/epub+zip，zip -X -0 首个存储条目）、META-INF/container.xml（指向 OEBPS/content.opf）、content.opf（title=测试之书，manifest+spine 三章）、nav.xhtml 与三节 xhtml（各含标题与 500 字文本），`zip -X -0 ../sample.epub mimetype && zip -X -r -9 ../sample.epub META-INF OEBPS`，拷入 `app/src/test/assets/sample.epub`（unzip -t 校验）；添加依赖 org.readium.kotlin-toolkit:readium-shared:3.4.0（版本目录 readium 条目，失败回退 3.3.0）；创建 `data/parser/EpubFormatParser.kt`——Readium EpubParser+Asset 打开 filesDir 文件→提取 title/author/spine 目录 List<TextChapter> 级结构；`feature/reader/epub/EpubReaderScreen.kt`——左栏章节目录 + AndroidView(WebView) 渲染当前章 XHTML（注入本地图片路径与基础反色样式跟随明暗），切章即翻页，locator 保存 {chapterIndex, scrollY}；reader_epub 路由接入；测试 `EpubParseTest.kt`（Robolectric 读 assets 样本：断言 title=测试之书 且 spine 章节数=3）。
loop: until EpubParseTest 绿且 assembleDebug 成功
max_iterations: 5
verify: bash -lc 'set -a; . /workspace/.ngbook-env; cd /workspace && gradle :app:testDebugUnitTest --tests "*EpubParse*" :app:assembleDebug --console=plain' 2>&1 | grep -q 'BUILD SUCCESSFUL'
gate: auto

- [x] **Step 15: 续读链路端到端测试**
action: 创建 `app/src/test/java/com/book/ng/feature/ProgressResumeTest.kt`（Robolectric）——(a) TXT：1.5 万字样本建书→ReaderViewModel 分页→模拟跳转第 12 页并触发保存→以新 ReaderViewModel 实例+持久层重开→断言初始页=12；(b) TXT 改字号后重开→locator 仍命中保存时页；(c) EPUB：以 sample.epub 元数据构造 locator chapterIndex=2→重开 ReaderViewModel 恢复章节=2；确认 ProgressRepository 写回发生在页面变化与 ViewModel onCleared 两处。
loop: until ProgressResumeTest 全绿
max_iterations: 3
verify: bash -lc 'set -a; . /workspace/.ngbook-env; cd /workspace && gradle :app:testDebugUnitTest --tests "*ProgressResume*" --console=plain' 2>&1 | grep -q 'BUILD SUCCESSFUL'
gate: auto

- [x] **Step 16: 开源工程收尾（LICENSE / README / .gitignore）**
action: `curl -sL https://www.gnu.org/licenses/gpl-3.0.txt -o /workspace/LICENSE`；创建 `/workspace/.gitignore`：.gradle/、build/、local.properties、.ngbook-env、/tmp 产物、.idea/、*.iml；创建 `/workspace/README.md`：项目一句话简介、功能清单（对照 initiative 愿景）、五阶段路线图（P1 已完成项勾选）、构建说明（JDK21+SDK35+gradle assembleDebug）、免责声明（不内置内容源、用户自带源合规边界、GPL-3.0）；`git add -A && git commit -m "chore: open-source housekeeping (license, readme, gitignore)"`。
verify:
  - type: artifact
    path: /workspace
    assert:
      kind: matches-glob
      value: "LICENSE"
  - type: shell
    command: test -f /workspace/README.md && test -f /workspace/.gitignore && git -C /workspace log -1 --pretty=%s | grep -q 'open-source housekeeping'
gate: auto

- [x] **Step 17: 全量回归 + APK 交付 + 阶段验收**
action: `gradle :app:testDebugUnitTest :app:assembleDebug --console=plain` 全量回归；汇总输出：APK 路径与体积、各测试类通过数、版本矩阵最终值（从 libs.versions.toml 读出贴进 PR 说明）；`git tag p1-local-reading-done`；将 app-debug.apk 交付产品负责人真机安装，验收项：导入 TXT/EPUB 阅读、翻页/滚动切换、杀进程续读、明暗与配色切换。
loop: until 全量绿
max_iterations: 4
verify:
  - type: shell
    command: bash -lc 'set -a; . /workspace/.ngbook-env; cd /workspace && gradle :app:testDebugUnitTest :app:assembleDebug --console=plain' 2>&1 | grep -q 'BUILD SUCCESSFUL'
  - type: artifact
    path: app/build/outputs/apk/debug
    assert:
      kind: matches-glob
      value: "app-debug.apk"
gate: human

人工门裁决（2026-09-13）：全量回归 49 例全绿、APK 16.47 MiB、tag `p1-local-reading-done` 已打；产品负责人选择「先按通过落账，我自测」——真机 8 项验收（TXT UTF-8/GBK 导入、EPUB 秒开+TOC+反色、翻页/滚动切换、字号连改不回书首、杀进程续读、明暗×四配色、logcat 无崩溃）列为**待复验项**，不阻塞 P1 关闭。P1 至此关闭。

## 真机缺陷热修（P1 关闭后追加，2026-09-13）

用户真机自测报障"导入 txt 打开一直转圈"。定位为视口-文档循环等待死锁：
`onSizeChanged` 原挂在仅 `document != null` 才渲染的 `PagingContent` 内，而
ViewModel 必须先拿到视口才执行首次分页。旧测试手工调 `onViewportSize` 绕过了
UI 布线，故 49 例全绿未能拦截（Step 17 登记的"测试判据盲区"如约兑现）。

- 修复：视口上报上移至 `TextReaderScreen` 最外层 Box，与文档加载解耦（09a5660）
- 回归：新增端到端 `PagingViewportContractTest`（RED 复现转圈 → GREEN），全量 50 例全绿
- 产物：新 debug APK 17172075B，待用户重装复验
