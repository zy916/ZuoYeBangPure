# 作业帮净化 ZuoYeBangPure

针对 **作业帮**（`com.baidu.homework`，14.54.0 / versionCode 2830）的 LSPosed 模块，基于 LibXposed API 102。

## 功能
- 去广告：拦截作业帮 `AdManager` 广告加载，并拦截穿山甲、优量汇、百度、快手等广告 SDK
- 更新提示：拦截新版更新检查入口 `com.baidu.homework.common.utils.d3` 的全部方法，以及升级弹窗、H5 更新检查/下载 Action
- 视频讲解小窗：拦截触发小窗的 5 个 H5 Action，以及小窗唯一的创建工厂 `search.core.i.A1`；再配合 View 级拦截兜底
- 原生“讲解中”标签：拦截 `ExplainLabelContainer` 与 `ImageDecorFuseContainer` 的标签处理
- H5“讲解中”浮层：页面每次加载后注入一次 MutationObserver，只在新增节点时检查，不再定时轮询（避免闪烁和频率变高）
- 额外隐藏广告 View 和更新弹窗 View

## 两个版本
- `ZuoYeBangPure-v1.3.apk`：完整版，底部功能保持原样
- `ZuoYeBangPure-v1.3-tabs.apk`：底部功能只保留「首页」和「我的」（其余标签隐藏，若停留在其它标签会自动切回首页）

## 安装
1. 安装需要的版本（普通版或 tabs 版，二选一）
2. 在 LSPosed 管理器启用「作业帮净化」
3. 作用域只勾选作业帮（`com.baidu.homework`）
4. 强行停止作业帮后重新打开

## 日志
- `adb logcat -s ZuoYeBangPure`
- 关键行：`explain label blocked hooks=`、`video window blocked`、`bottom tabs simplified`、`filter tabs failed`

## 备注
- 主要针对搜题页视频小窗、广告和更新提示。
- 如果某个页面仍出现小窗，请记录作业帮版本和具体页面，再按页面补规则。
