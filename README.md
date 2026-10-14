# RX100M5 相机应用

两个非官方修补测试版，均未经过相机实机验收。

| 应用 | 下载 | 主要改动 |
| --- | --- | --- |
| Recipe Lab 1.6.2.4 | [APK 与源码](https://github.com/Fanqi-dev/recipe-lab-rx100m5/releases/tag/v1.6.2-rx100m5.4-preview) | Pro-UI 风格蓝色界面、参数卡片、内置 JPEG 回看、移除赞助栏、保留设置写入保护 |
| Sony Time-lapse 3.40 local2 | [APK](https://github.com/Fanqi-dev/recipe-lab-rx100m5/releases/tag/sony-timelapse-3.40-local2-test) | 简短参数提示、计时及取消逻辑修补、回放缓存与刷新优化；保留机内视频合成 |

## 安装前

- Recipe Lab 仅限 **DSC-RX100M5，不含 M5A**。与本项目旧版同签名，可覆盖升级；先备份卡片。出现 `Recovery pending` 时不要卸载或清除数据。
- 延时摄影基于用户提供的 `11.apk`，重新签名，**不能直接覆盖不同签名的原版**。不要为安装而卸载原版、清空数据或删除 `APP_TLPS`；先处理备份与迁移。
- 通过 PMCA 的 **Install app → Select an apk** 选择下载文件。主机测试和静态检查不能替代实机验证，也不代表性能提升已实测。

[Recipe Lab 改动](CHANGELOG-RX100M5.md) · [安装与恢复](RX100M5-安装和修改说明.txt) · [验证记录](VERIFICATION.md) · [延时摄影说明](docs/SONY-TIMELAPSE.md)

## 来源

Recipe Lab 派生自 [voxivoid/recipe-lab-sony-pmca](https://github.com/voxivoid/recipe-lab-sony-pmca) v1.6.2，作者 André Domingues；保留 [MIT 许可](LICENSE)、字体 OFL 和[第三方声明](THIRD-PARTY-NOTICES.md)。蓝色 UI 参考索尼 Pro-UI 设计，不是索尼官方产品或认证。

Sony Time-lapse 是闭源应用，不适用本仓库的 MIT 许可；此处提供的修补包不代表获得索尼授权，原有权利归相应权利人。签名私钥不公开。
