# ArBoxMeasure

基于 ARCore 和 Sceneform 的 AR 箱体体积测量应用

## 📋 项目简介

ArBoxMeasure 是一款利用增强现实（AR）技术进行箱体体积测量的 Android 应用。通过手机摄像头，用户可以快速、准确地测量现实世界中箱体的尺寸和体积。

## ✨ 主要功能

- **平面检测**：自动识别并标记平面区域
- **三点定位**：使用平面锚点定位箱体的三个底角
- **高度测量**：
  - 方式一：使用特征点自动定位箱体高度
  - 方式二：通过滑块手动调整高度
- **实时计算**：自动计算并显示箱体的长、宽、高及体积
- **可视化渲染**：3D 可视化展示测量结果

## 🛠️ 技术栈

- **ARCore**：Google 的增强现实开发平台
- **Sceneform**：用于在 Android 上构建 AR 体验的 3D 框架
- **Android SDK**

## 📱 系统要求

- Android 7.0 (API level 24) 或更高版��
- 支持 ARCore 的设备（[查看支持设备列表](https://developers.google.com/ar/devices)）
- 摄像头权限

## 🚀 快速开始

### 安装

1. 克隆仓库：
```bash
git clone https://github.com/gangoogle/ArBoxMeasure.git
```

2. 使用 Android Studio 打开项目

3. 同步 Gradle 依赖

4. 连接支持 ARCore 的 Android 设备

5. 运行应用

### 使用方法

1. **启动应用**：打开应用并授予摄像头权限
2. **扫描平面**：缓慢移动手机以检测平面
3. **标记底角**：点击屏幕标记箱体的三个底角
4. **设置高度**：
   - 使用特征点自动识别高度，或
   - 使用滑块手动调整高度
5. **查看结果**：应用会自动显示测量的尺寸和体积

## 📸 截图

<!-- 建议添加应用截图 -->
_待添加_

## 🤝 致谢

本项目参考并使用了以下开源项目的技术方案：

- **平面锚点测距**：[ARCoreMeasuredDistance](https://github.com/Terran-Marine/ARCoreMeasuredDistance)
- **高度定位方式**：[AR-Measure-Mobilinq](https://github.com/slobodandeveloper/AR-Measure-Mobilinq)

感谢以上项目的开发者们的贡献！

## 📝 开发计划

- [ ] 支持更多形状的测量（圆柱体、不规则形状等）
- [ ] 添加测量历史记录功能
- [ ] 支持导出测量数据
- [ ] 优化测量精度
- [ ] 添加多语言支持

## 📄 许可证

<!-- 请根据实际情况添加许可证信息 -->
_待添加_

## 👤 作者

[@gangoogle](https://github.com/gangoogle)

## 🐛 问题反馈

如果您在使用过程中遇到任何问题或有改进建议，欢迎提交 [Issue](https://github.com/gangoogle/ArBoxMeasure/issues)。

## ⭐ Star History

如果这个项目对您有帮助，欢迎给个 Star！

---

**注意**：本应用需要良好的光照条件和清晰的平面纹理以获得最佳测量效果。