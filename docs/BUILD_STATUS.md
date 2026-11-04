# 构建状态与云环境依赖

当前环境 `/workspace/bolishijie`：Java 21.0.12.1、Gradle 8.14.3。仓库最初为空，本次新增完整模组工程。

## 实际执行

1. 官方 Gradle Wrapper 已下载，Gradle 发行包 SHA-256 为 `bd71102213493060956ec229d946beee57158dbd89d0e62b91bca0fa2c5f3531`，`./gradlew --version` 在配置代理后通过。
2. `python3 tools/check_resources.py`：13 项测试通过。
3. `./gradlew build --no-daemon`：失败，停在插件解析阶段，尚未编译 Java。
4. 使用 `help --info` 进一步诊断，实际错误为：

```text
HTTP/1.1 403 Forbidden:
https://maven.fabricmc.net/fabric-loom/fabric-loom.gradle.plugin/1.13.6/fabric-loom.gradle.plugin-1.13.6.pom
```

这不是成功构建，也不能证明 Java 接口、Mixin 或游戏运行已通过。9 项 GameTests 都尚未运行。

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
