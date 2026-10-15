# 验证记录

## Recipe Lab 1.6.2-rx100m5.7-test

- 363 项主机测试通过，0 失败/跳过；243 个三语言桌面布局状态、147 项对齐断言通过，3084 处文字均使用共享字号角色且无伪粗体。增加确认/删除/丢弃/配方选项长文案、选项完整显示、键帽/文字间距及中文断行检查。
- 使用共享 SonyUi/UiChrome；标题/正文/辅助文字为 14/12/10 逻辑像素。中文常规首页 144/480 像素、静简 108/480；对照图为源码驱动的桌面绘制，不是相机截图。
- 生产资源、Java、min-API-10 DEX 编译通过。SafeSettings、SafeRecipeFiles、Rx100M5Policy、Params、NativeBackup 与已发布 v4 一致，JNI 未改。普通控件的缩放及原生照片页仍需相机验收。
- SDK 10 v1 签名及 zipalign 通过；完整 JADX 导出 0 错误。包名不变，versionCode 为 10603007，证书与 v4/v5/v6 相同。APK：350067 字节，SHA-256：`acd55b48b1f7bc57076fd4a6b6bd420d1c423f9d961cfce37ef42b61a85e1eab`。
- REA 复核最终包的 12 个关键方法，覆盖直接选用、回看交接/恢复与关闭键、原生资源释放、统一缩放/页眉/状态标签、首页高度及确认框。无桌面预览替身、旧 Java 位图相册或签名私钥；结果：`out-safe/verify-package-rx7.log` 与 `out-safe/evidence-rx7`。
- 原 1.6.2.6 APK 保留，本版以未实机验收的测试版发布，未自动安装。日志保留在本地：`out-safe/test-package-rx7.log`、`out-safe/layout-package-rx7.log`、`out-safe/build-package-rx7.log`；JADX 完整导出日志位于审计目录 `jadx-rx7-final.log`。

## 前一稿首页调整（已由上方视觉规范替代）

- 360 项主机测试、222 个桌面布局状态、99 项字体度量对齐断言通过；增加内置配方三语言摘要完整宽度、不修改参数、字号层级和行间无重叠检查。常规中文面板 136/480 像素，静简面板 104/480 像素；英文长提示允许换行。
- 生产资源、Java 及 min-API-10 DEX 编译通过。存储保护五个类和 JNI 未改；未签名、安装或发布新 APK。桌面 1.6.2.6 APK 仍是下述已记录的原包，不包含这次首页调整。
- 浏览器复用真实 Canvas 视图；画面仍是占位，不验证相机性能。检查日志：`out-safe/test-info-panel.log`、`out-safe/layout-info-panel.log`、`out-safe/compile-info-panel.log`。

## Recipe Lab 1.6.2-rx100m5.6-test（本地）

- 360 项主机测试通过，0 失败/跳过，包含 12 项新回看命令、索引选择和过期请求模型测试；不是 Sony API 运行测试。
- 222 个三语言桌面布局状态通过文本边界检查；另有 75 项字体度量对齐断言，检查页眉、等宽状态标签、菜单值列、参数卡片和回看底栏。640×480 首页底栏仍为 52 像素。
- 完整 JADX 导出 0 错误；最终包不含旧 PhotoReviewView、ReviewIndex、PreviewBridge、Java2D 或 Android 绘图替身。原生显示通过反射调用设备现有接口，没有打包 Sony 闭源实现。
- SDK 10 v1 签名与 zipalign 通过，证书与 v4/v5 相同。APK：348116 字节；SHA-256：`ebdaf15e3affa43a3b9d19929f1f7e4ff198ef6364f7ef74b68ab72cf5b4a398`。
- SafeSettings、SafeRecipeFiles、Rx100M5Policy、Params、NativeBackup 与 v4 一致，JNI 哈希仍为 `59606b2d7a58438953fae314d0082ce3ff43c0ed37f90a1838b341ebbab96f4d`。未调整画质、绕过权限或改固件。
- 本地浏览器已核对首页、配方、设置及参数页；源代码修改会重新编译界面。该预览不执行 MainActivity、CameraEx、原生照片显示或 JNI，灰色背景只是占位。
- 新回看采用 Avindex 只读查询、原生 SCN/MAIN 解码、同一 Activity 内返回。声明 ACCESS_SURFACE_FLINGER 权限；真实授权、照片方向/清晰度、放大、内存释放、返回后相机重开及速度均未实机验收，不保证适用所有固件。
- 最终 APK 的 14 个关键方法通过 REA 复核，涉及直接选用、相机交接/恢复、回看关闭键、原生解码/释放及对齐规则；结果保存在 `out-safe/verify-rx6-final.log` 和 `out-safe/evidence-rx6`。未自动安装到相机，未上传此版至 GitHub。

## Recipe Lab 1.6.2-rx100m5.5-test（本地）

- 348 项主机测试通过，0 失败/跳过；数量减少是移除了已删除相册的 13 项旧测试，增加 3 项会话测试和 1 项色彩对比度测试，并替换 2 项 UI 测试。
- 165 个桌面绘制状态通过文本边界检查，包含三种语言、长名称、已启用/预览/修改状态。首页底栏在 640×480 下为 52 像素，不代表实机测量。
- SDK 10 v1 签名与 zipalign 通过；JADX 完整导出 0 错误。最终包不含 PhotoReviewView、ReviewIndex 或桌面绘图替身。
- REA 复核最终包中的 writeAll、首页/列表选用、按键下压/松开、成功收起编辑区、原生回看登记及底栏绘制/测量方法，确认不再调用二次选用确认。
- SafeSettings、SafeRecipeFiles、Rx100M5Policy、Params、NativeBackup 与 v4 源码一致；原生库不变。
- APK：336178 字节；SHA-256：`5a7a6ce9d64d47635ca6875232768d8e28a76fe2111ba9936b5e129ce0589fcc`。签名证书与 v4 相同。
- 原生回看、返回应用、按键固件事件和相机拍摄表现尚未实测。未上传此版至 GitHub，未自动安装到相机。

## Recipe Lab 1.6.2-rx100m5.4-preview

- 主机 Java 测试：357 项通过，0 失败或跳过。
- 桌面 UI 绘制：132 个状态检查文本边界；不是 Android 或相机截图测试。
- 最终 APK：v1 签名、SDK 10 签名检查及 zipalign 通过；完整 JADX 导出 0 错误，REA 复核关键 UI、回看按键和设置事务方法。
- 写入保护、画质保留策略及 ARM JNI 与前版保持一致。JNI SHA-256：`59606b2d7a58438953fae314d0082ce3ff43c0ed37f90a1838b341ebbab96f4d`。
- APK：343455 字节。SHA-256：`80d8d0b4d9e050881dd1f18245a69ffdba7dfc845532d2b9ccacb898b7c74ccd`。
- 签名证书 SHA-256：`92673172ec26c9282f47d53282a0ac48a46a5fc15fc1e1935e09d8ee13e47bd7`，与本项目旧版相同。

测试可通过 `test-safe.ps1` 或 `tools/test.sh` 重跑；本地构建使用 `build-safe.ps1`。构建依赖布局见脚本，自行签名不能直接覆盖本项目签名的 APK。桌面绘图替身位于 `verification/ui-shim`，不打入 APK。

## Sony Time-lapse 3.40 local2

- local1 逻辑模型检查 77 项；local2 提示与缓存逻辑检查 14152 项。均不是相机 DEX 运行测试。
- 仅修改 11 个原有类，新增 2 个辅助类；656 个非 DEX、非签名条目与输入逐字节一致，包括原有资源和原生库。
- 签名、zipalign、REA 关键方法检查通过。JADX 原包和修补包均报告 4 个错误，没有新增反编译错误；这不证明代码完全正确。
- APK：2868558 字节。SHA-256：`2695da0054380de7547c5d34e96222fca9551e664b080ab13e3e1de389c6592f`。
- 本地签名证书 SHA-256：`e3be75bde45a403a10f0157affebbece2a04e75d7ad70ef6608439cd7bae27fc`，与输入不同。

## 未验证

两者均未进行相机实机验收、真实掉电测试、拍摄效果验证或速度/内存基准测试。不保证所有固件行为或所有操作无故障。公开附件不含签名私钥、私人日志或完整 Sony 反编译源码。
