# 构建状态与云环境依赖

## 1.0.1 世界加载修复

已使用真实成品服务器复现 1.0.0 的普通世界生成崩溃：[复现构建](https://github.com/susu2020oxo/bolishijie/actions/runs/41782598821)。结构保护读取的结构起点可能位于八个区块之外，而原 SPAWN 生成步骤只允许读取中心 LIGHT 和相邻 BIOMES，导致 `Requested chunk unavailable during world generation`。

1.0.1 仅在 GENERATION 的 SPAWN 步骤补齐 `STRUCTURE_STARTS/8` 和 `FEATURES/1` 依赖，保留原版 LOADING 路径及既有区块。新测试验证读取依赖和旧存档加载依赖保持分离；[初次修复构建](https://github.com/susu2020oxo/bolishijie/actions/runs/41783053272) 中 11 项必需 GameTests 通过。发布另须经过真实成品的新建、重载和原版旧存档安装检查，具体结果见下载分支构建记录。

1.0.0 的测试覆盖存在缺口：普通世界生成没有被原测试房间覆盖。下列历史发布记录保留作追溯，不能作为继续使用 1.0.0 的依据。

当前环境 `/workspace/bolishijie`：Java 21.0.12.1、Gradle 8.14.3。仓库最初为空，本次新增完整模组工程。

## 已公开发布的成品

[GitHub Actions 首次成功构建](https://github.com/susu2020oxo/bolishijie/actions/runs/41775891873)于 2026 年 11 月 4 日完成 Java 编译、资源处理、JAR 重映射和服务端游戏测试：

```text
All 10 required tests passed :)
BUILD SUCCESSFUL in 1m 17s
```

源码提交为 `b0a0166a92aff870d1ffcc263c4e54ac3dad0894`。构建产物已推送到公开 `downloads` 分支，并实际从公开直链下载，验证了模组 ID `bolishijie`、版本 `1.0.0` 和 ZIP 完整性。

- [成品 JAR](https://github.com/susu2020oxo/bolishijie/raw/refs/heads/downloads/bolishijie-fabric-1.21.10-v1.0.0.jar)
- [公开下载目录](https://github.com/susu2020oxo/bolishijie/tree/downloads)
- [最近一次发布的构建记录](https://github.com/susu2020oxo/bolishijie/blob/downloads/build-status.txt)

服务端自动测试通过不等同于完成了客户端画面和全维度长时间刷怪对比；这些手动验收项目仍见 TESTING.md。

## 当前云环境的本地构建限制

1. 官方 Gradle Wrapper 已下载，Gradle 发行包 SHA-256 为 `bd71102213493060956ec229d946beee57158dbd89d0e62b91bca0fa2c5f3531`，`./gradlew --version` 在配置代理后通过。
2. `python3 tools/check_resources.py`：13 项测试通过。
3. `./gradlew build --no-daemon`：失败，停在插件解析阶段，尚未编译 Java。
4. 使用 `help --info` 进一步诊断，实际错误为：

```text
HTTP/1.1 403 Forbidden:
https://maven.fabricmc.net/fabric-loom/fabric-loom.gradle.plugin/1.13.6/fabric-loom.gradle.plugin-1.13.6.pom
```

以上为本地执行结果；之后已通过 GitHub Actions 完成编译与游戏测试，不再以本地失败判断公开成品的构建状态。

## 需要的额外网络域名

保留已有允许列表与包管理器预设，追加以下域名：

```text
maven.fabricmc.net
meta.fabricmc.net
piston-meta.mojang.com
piston-data.mojang.com
libraries.minecraft.net
resources.download.minecraft.net
```

前五个用于 Fabric 开发依赖、版本信息、Minecraft 客户端/服务端及运行库；最后一个用于客户端资源。Gradle 的 `services.gradle.org`、`downloads.gradle.org`、GitHub、Maven Central 已由现有预设覆盖。

尝试保存这些新增域名时，平台返回 `draft_not_editable`；因此**网络修改没有保存或生效**。如需继续在云环境构建，应在环境设置中修改网络配置；若已发布配置不允许直接编辑，可从环境设置发起新的设置聊天。当前源码可完整打包带走，无须重新编写项目。

Java 默认不读取 `HTTPS_PROXY`。本环境曾以这些非秘密参数成功下载并验证 Gradle：

```sh
JAVA_OPTS="${JAVA_OPTS:-} -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080" \
  ./gradlew -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 \
  -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 build --no-daemon
```

上述命令仅适用于代理确实为 `proxy:8080` 的当前云环境。自己的电脑通常直接使用 `./gradlew build`。不要关闭 TLS、校验和或绕过环境网络策略。
