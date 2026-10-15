# 本地开发预览

在项目根目录运行 `./verification/live-preview/start-preview.ps1`，打开其返回的本地 URL。已有服务可重用；关闭浏览器不会结束服务。

生产 Canvas 视图、语言与配方数据在 Java2D 绘图替身上渲染。修改 `src`、`res`、字体或预览源码后自动编译刷新，保留页面、配方及参数状态。页面上的机身按键仅模拟部分 Activity 流程；灰色图像是占位，不是实时相机流、滤镜样张或照片解码质量演示。

不运行 Android MainActivity、CameraEx、OptimizedImageView、JNI 或相机参数写入。原生照片接口、权限、按键事件、内存及速度必须在 RX100M5 验证。此目录和 `verification/ui-shim` 不打入 APK。

仅监听 127.0.0.1，不对局域网开放。服务 PID/URL 记录于 `out-safe/live-preview/server-info.json`；停止前应核对 PID 的命令行属于本项目的 `server.mjs`，不要结束其他 Node/Java 进程。日志和运行数据均在忽略目录 `out-safe`。
