# NGBook

一个把漫画与图书收进同一座书架的安卓原生离线阅读器：本地文件导进来就能读，进度自动记住，数据只留在你自己手里。

- 包名：`com.book.ng`
- 协议：GPL-3.0（见 [LICENSE](LICENSE)）
- 当前版本：`0.1.0`（P1「能读」阶段）

## 功能清单（对照愿景）

已实现（P1）：

- 本地导入：SAF 多选导入，魔数 + 扩展名双重嗅探，识别 TXT / EPUB / CBZ / ZIP / MOBI / AZW3 / PDF 并分流
- TXT 引擎：UTF-8 / GBK 自动编码识别（含 BOM 与 CRLF），按标题行自动切章（无标题时按固定字数分节），`StaticLayout` 精确分页
- TXT 阅读：翻页与上下连续滚动两种模式，字号增减即时重排并保持内容锚点
- EPUB 阅读：Readium 解析书目与 spine，WebView 渲染章节正文，深色模式反色，章节抽屉
- 进度续读：页码 / 字符偏移 / 章节 / 模式编码为紧凑 locator 存 Room，页面切换、字号重排、`onCleared` 三处写回；杀进程重开回到上次位置
- 书架：网格卡片、最近阅读排序、进度角标（第 N 章·第 M 页）、删除连带清理本地文件
- JellyTheme：玻璃拟态基座，明暗双模式 × 抹茶 / 葡萄 / 樱花 / 海洋四套配色，DataStore 持久化偏好

尚未实现（后续阶段）：仿真翻页圆柱卷曲动画与水墨楷体联动、书签与笔记、PDF / MOBI / AZW3 / CBZ 的实际渲染、在线书源与漫画源聚合、批量离线下载与多格式打包导出。

## 路线图

| 阶段 | 主题 | 状态 |
|---|---|---|
| P1 | 能读：工程骨架、JellyTheme、书架、导入嗅探、TXT + EPUB、进度续读 | 进行中（代码与单测完成，待全量回归与真机验收） |
| P2 | 好看：仿真卷曲翻页、水墨楷体、阅读设置面板、书签 | 未开始 |
| P3 | 全格式：PDF、MOBI、AZW3、CBZ 与漫画引擎 | 未开始 |
| P4 | 聚合：书源规则引擎、漫画源适配器、搜索 / 详情 / 在线阅读 | 未开始 |
| P5 | 离线沉淀：批量下载、EPUB / CBZ / ZIP 导出、笔记、备份恢复 | 未开始 |

P1 明细见 [工作流清单](docs/plans/2026-09-13-p1-local-reading-workflow.md)，设计与决策依据见 [P1 设计文档](docs/designs/2026-09-13-phase-1-local-reading-design.md) 与 [Initiative 文档](docs/designs/ngbook.md)。

## 构建

工具链（实测通过的组合）：

| 组件 | 版本 |
|---|---|
| JDK | 21（守护进程）/ 字节码 17 |
| Gradle | 9.6.0（系统安装，本仓库暂未提供 wrapper） |
| AGP | 9.4.0 |
| Kotlin / KSP | 2.4.20 / 2.3.12 |
| compileSdk / targetSdk / minSdk | 37 / 35 / 24 |
| Compose BOM | 2026.09.00 |
| Room / Hilt / Readium | 2.8.5 / 2.60.1 / 3.4.0 |
| Robolectric | 4.16.1（单测跑 sdk 35） |

```bash
# 前置：JDK 21 与 Android SDK（platforms;android-35/37、build-tools）
sdk.dir=/path/to/android-sdk   # 写入 local.properties

gradle assembleDebug                      # 产出 app/build/outputs/apk/debug/app-debug.apk
gradle :app:testDebugUnitTest             # 全量单元测试
gradle :app:testDebugUnitTest --tests "*ProgressResume*"   # 单个测试类
```

安装调试包：`adb install app/build/outputs/apk/debug/app-debug.apk`。

国内网络下依赖解析已配置 `google()` + `mavenCentral()` 主通道与阿里云镜像回退（见 `settings.gradle.kts`）；若走代理，请以 `-Dhttp.proxyHost/-Dhttps.proxyHost` 或 `gradle.properties` 注入，Robolectric 测试 JVM 会继承这些属性。

## 免责声明

- 本软件不内置、不分发、不代理任何第三方内容源。联网阅读能力（P4）只提供空白的源适配框架，源规则完全由用户自行导入与维护。
- 用户自带源的内容合法性、抓取行为合规性由用户自行承担；请勿用于盗版分发、高频压站或违反目标站点服务条款的行为。
- 所有解析、下载、笔记、进度均在本机完成，不含任何遥测或云端上传。
- 本地导入的电子书须为你有权阅读的副本；本项目不对内容版权负责。
- 衍生作品须以 GPL-3.0 同源开源，并保留版权声明。
