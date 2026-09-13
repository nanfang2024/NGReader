---
design_type: phase
created_at: 2026-09-13
---

# Phase 1 · 能读 — NGBook 骨架与本地核心阅读

> 上位文档：`docs/designs/ngbook.md`（initiative，已批准）。
> 本阶段实现 initiative 的 P1：工程骨架、JellyTheme 基座、书架、SAF 导入、TXT/EPUB 可读、进度续读。
> 配套工作流：`docs/plans/2026-09-13-p1-local-reading-workflow.md`。

## Intent Contract

```
intent: 搭建 NGBook 安卓工程并在沙箱内跑通构建，实现核心本地阅读闭环——
        书架 + SAF 导入 + 格式嗅探 + TXT(UTF-8/GBK)与 EPUB 可读 + 阅读进度自动续读。
constraints: 不含卷曲动画/书签笔记/PDF/漫画/在线源（分属 P2/P3/P4）；
             不上传任何用户文件；数据模型与主题 token 预留 initiative 架构的扩展位，
             后续阶段只增强不破坏；依赖版本全部与 GPL-3.0 兼容；
             沙箱内可构建（assembleDebug）、可测试（JVM + Robolectric）。
success_criteria: 产出可安装 app-debug.apk；TXT 与 EPUB 样本导入后能翻页/滚动阅读，
                  杀进程重开后回到上次阅读位置；编码检测/分页/DAO/主题切换单测全绿。
risk_level: medium
```

## Verification Contract

```
verify_steps:
  - run: gradle :app:assembleDebug（构建守护进程使用 JDK 21，org.gradle.java.home 指定）
    confirm: BUILD SUCCESSFUL 且产出 app/build/outputs/apk/debug/app-debug.apk
  - run: gradle :app:testDebugUnitTest
    confirm: 全部测试绿色，覆盖以下场景：
  - check: TXT 编码检测——UTF-8/GBK/UTF-16LE(BOM)/纯 ASCII 四类样本判定正确
  - check: 章节切分——中文数字编号/阿拉伯编号/无章节 fallback/空行归并
  - check: 分页器——同一 viewport 分页稳定，相邻页字符区间连续不重叠
  - check: Room DAO——书架与进度插入/查询/删除往返，Flow 可观察
  - check: 主题切换——Robolectric 断言配色 token 全局即时生效，无需重建 Activity
  - check: 续读链路——保存 locator 后重新打开阅读器，恢复至同一页/章节位置
  - check: EPUB 解析——自造 3 章样本经 Readium 链路读出书名与目录
```

## Governance Contract

```
approval_gates:
  - Step 4（工具链+首个 APK）——人工门：确认沙箱构建链路成立（可先真机装 Hello 版）
  - Step 17（阶段收尾）——人工门：按验证契约全量演示，产品负责人真机验收后打 tag
rollback: 全部工作提交在分支 hotl/p1-local-reading（worktree: false，直接在当前检出）；
          阶段失败回退方式=丢弃该分支未合并提交；Room 为本库首个 schema(v1)，无历史迁移负担。
ownership: 实施与沙箱验证=AI 开发；SDK/JDK 安装与真机体验=产品负责人最终确认。
```

## Scope

| In（本阶段做） | Out（明确不做 → 归属） |
|---|---|
| JDK21 + Android SDK 35 工具链安装与镜像回退配置 | 卷曲仿真翻页动画 → P2 |
| Gradle Kotlin DSL 骨架 + 版本目录 + Hilt + Compose | 书签 / 阅读设置完整面板 → P2 |
| JellyTheme 基座：明暗 × 4 套果冻配色 + DataStore 持久化 | PDF / MOBI / AZW3 / CBZ → P3 |
| Room 数据层（书架 + 进度）+ 仓储 | 在线书源/漫画源 → P4 |
| 格式嗅探（魔数 + 扩展名）+ SAF 多文件导入 | 批量下载与 EPUB/CBZ/ZIP 导出 → P5 |
| TXT 引擎：编码检测 / 章节切分 / StaticLayout 分页 / 翻页+滚动双模式 | 笔记、备份恢复 → P5 |
| EPUB：Readium 最简链路（解析 + WebView 章节渲染） | 封面提取与精美书架网格 → P2 顺带 |
| 阅读进度保存与续读恢复 | |
| 测试样本生成 + 单测/Robolectric + README/LICENSE | |

## Decisions

| # | 决策 | 选定 | 已否选项 |
|---|---|---|---|
| P1-1 | 构建 JVM | 系统 Gradle 8.14.5 + daemon 用 JDK 21（apt→mise→TUNA 三级回退） | 用 Java 25 跑 AGP（兼容性风险）；下载 Gradle wrapper 发行包（多一次大下载） |
| P1-2 | 版本矩阵起点 | AGP 8.13.2 / Kotlin 2.4.20 / Compose BOM 2026.09.00 / Room 2.8.5 / Hilt 2.60.1 / KSP 2.3.12 / Readium 3.4.0 / Robolectric 4.16.1 / lifecycle 2.11.0 / navigation-compose 2.10.1 / datastore 1.2.1 / core-ktx 1.19.0（全部实测存在于仓库） | AGP 9.x（需 Gradle 9，避开）；快照/alpha 版本 |
| P1-3 | SDK 级别 | compileSdk 35 / targetSdk 35 / minSdk 24 | compileSdk 36（依赖 AGP 更新节奏，本阶段保守） |
| P1-4 | 仓库配置 | google()+mavenCentral() 主通道，阿里云镜像 content-filter 回退 | 单一国内镜像（不可控） |
| P1-5 | EPUB 阅读形态 | Readium 解析 + WebView 章节渲染最简链路（翻页=章节切换），卷曲与主题联动延 P2 | P1 直接全量集成 Readium Navigator 翻页容器（耦合重） |
| P1-6 | locator 编码 | 版本前缀冒号分隔紧凑串（`v1:章节号:页码:偏移`），免 JSON 依赖 | kotlinx.serialization（新增版本变量，暂不需要） |
| P1-7 | DI / 测试 | Hilt(KSP) + JUnit4 + Robolectric；无 instrumentation（沙箱无设备） | Koin；Espresso |

## Surface

**APIs**：`FormatParser`（supports/parse）与 `FormatSniffer`（BookRef → BookFormat）为后续六格式与在线源复用入口；`TextPaginator.paginate(chapters, viewport)` 与 `LocatorCodec.encode/decode` 是 TXT 引擎与进度的稳定契约；`LibraryRepository` / `ProgressRepository` 暴露 Flow 供 UI 观察。

**Storage**：Room v1 两表——`books`（id、title、author、相对路径、format、addedAt、lastReadAt）与 `progress`（bookId、locator 串、readMode、updatedAt）；DataStore 偏好键——theme_mode、palette；文件一律拷贝至 `filesDir/books/`（应用私有，零上传）。

**Components**：NGBookApp（Hilt 入口）→ MainActivity（NavHost：shelf → reader_txt/{id} → reader_epub/{id}）→ ShelfScreen/ShelfViewModel、TextReaderScreen/ReaderViewModel（翻页/滚动双模式）、EpubReaderScreen；JellySettingsRepository 驱动 NGBookTheme 根 Composable。

**Files-touched**：根目录工程四件套（settings / gradle.properties / libs.versions.toml / app/build.gradle.kts）+ `local.properties`（不入库）+ `app/src/main/java/com/book/ng/{ui,feature,data,domain}/…` 分包 + `app/src/test/java/…` 对应测试 + `app/src/test/assets/`（样本 EPUB/TXT）+ LICENSE / README / .gitignore。

## Risks & Open Questions

- **KSP×Kotlin 匹配**（中）：KSP 2.3.12 与 Kotlin 2.4.20 的匹配未经构建实证——workflow Step 4 设 5 轮校准循环：优先查 KSP 元数据取支持 2.4.20 的版本，否则 Kotlin 降至与其有匹配的最高 2.3.x（compose 插件与 kotlin 版本同步联动）。
- **Readium 3.4.0 API 形态**（中）：其 Compose 集成非一等公民，按"最简链路"设计隔离风险；坐标解析失败则回退 3.3.0。
- **apt 无 JDK21 候选**（低）：三级回退（mise / TUNA Adoptium 镜像 / 阿里云 apt 源）均可达。
- **内存 5GB 约束**（低）：jvmargs -Xmx2g + workers.max=2；Kotlin 编译守护内存已限制。
- 开放问题：封面提取（EPUB 内嵌图）是否在 P1 顺带做——默认否，P2 书架美化时处理；TXT 超长单章（>50k 字）分页性能——P1 内用测试样本验证，若劣化则在 P2 与卷曲引擎一并优化。
