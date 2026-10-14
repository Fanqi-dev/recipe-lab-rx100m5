# 验证记录

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
