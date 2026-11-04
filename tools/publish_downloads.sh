#!/usr/bin/env bash
# Executed by the trusted main-branch GitHub workflow after build/tests.
set -euo pipefail

: "${GITHUB_SHA:?}"
: "${GITHUB_REPOSITORY:?}"
: "${RUNNER_TEMP:?}"
version=$(sed -n 's/^mod_version=//p' gradle.properties)
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || exit 1
jar_name="bolishijie-fabric-1.21.10-v$version.jar"
source_name="bolishijie-source-v$version.zip"
package_name="bolishijie-fabric-1.21.10-v$version.zip"
export RELEASE_VERSION="$version" RELEASE_JAR="$jar_name" RELEASE_PACKAGE="$package_name"
result="${BUILD_RESULT:-not-run}"

git archive --format=zip --output="$RUNNER_TEMP/$source_name" "$GITHUB_SHA"
if [[ "$result" == success ]]; then
    # Verify this is a built mod, not the wrapper/sources JAR or an empty archive.
    python3 - <<'PY'
import json, os
from zipfile import ZipFile
with ZipFile('build/libs/bolishijie-' + os.environ['RELEASE_VERSION'] + '.jar') as jar:
    assert jar.testzip() is None
    mod = json.loads(jar.read('fabric.mod.json'))
    assert mod['id'] == 'bolishijie' and mod['version'] == os.environ['RELEASE_VERSION']
    assert 'cn/susu2020oxo/bolishijie/GlassWorld.class' in jar.namelist()
    assert 'assets/bolishijie/equipment/glass.json' in jar.namelist()
    assert 'bolishijie.mixins.json' in jar.namelist()
PY
    cp "build/libs/bolishijie-$version.jar" "$RUNNER_TEMP/$jar_name"
fi

# Exit code 2 means an absent branch; authentication/network errors must fail.
if git ls-remote --exit-code --heads origin refs/heads/downloads > /dev/null; then
    git fetch --no-tags origin downloads
    git switch -C downloads FETCH_HEAD
else
    code=$?
    [[ "$code" == 2 ]] || exit "$code"
    git switch --orphan downloads
fi

cp "$RUNNER_TEMP/$source_name" "$source_name"
if [[ -f "$RUNNER_TEMP/bolishijie-build.log" ]]; then
    cp "$RUNNER_TEMP/bolishijie-build.log" build.log
else
    printf 'Build did not run; inspect the GitHub Actions run.\n' > build.log
fi
printf 'commit=%s\nresult=%s\nrun=https://github.com/%s/actions/runs/%s\n' \
    "$GITHUB_SHA" "$result" "$GITHUB_REPOSITORY" "$GITHUB_RUN_ID" > build-status.txt
git add -- "$source_name" build.log build-status.txt

if [[ "$result" == success ]]; then
    cp "$RUNNER_TEMP/$jar_name" "$jar_name"
    python3 - <<'PY'
from zipfile import ZipFile, ZIP_DEFLATED
import os
jar = os.environ['RELEASE_JAR']
with ZipFile(os.environ['RELEASE_PACKAGE'], 'w', ZIP_DEFLATED) as bundle:
    bundle.write(jar, 'mods/' + jar)
    bundle.writestr('安装说明.txt',
        '玻璃世界 Fabric 1.21.10\n'
        '需要 Minecraft Java 1.21.10、Java 21、Fabric Loader 0.17.3+。\n'
        '请另外安装适用于 1.21.10 的 Fabric API（本 ZIP 未包含 Fabric API）。\n'
        '将 mods 文件夹中的 JAR 放进游戏的 mods 目录。客户端和服务端都需要安装。\n'
        '新世界或尚未生成的区块才会生成玻璃地形，已有区块不会转换。\n')
PY
    sha256sum "$jar_name" "$package_name" "$source_name" > SHA256SUMS.txt
    cat > README.md <<EOF
# 玻璃世界 · Fabric 1.21.10 下载

需要 Minecraft Java 1.21.10、Java 21、Fabric Loader 0.17.3+，以及对应 1.21.10 的 Fabric API。
将成品 JAR 与 Fabric API 放入客户端和服务端的 mods 文件夹。创建新世界以生成玻璃地形。

- [下载模组 JAR](https://github.com/susu2020oxo/bolishijie/raw/refs/heads/downloads/$jar_name)
- [下载 ZIP 安装包](https://github.com/susu2020oxo/bolishijie/raw/refs/heads/downloads/$package_name)
- [下载源码 ZIP](https://github.com/susu2020oxo/bolishijie/raw/refs/heads/downloads/$source_name)
- [SHA-256](SHA256SUMS.txt)

源码 ZIP 不是成品模组。此目录仅在构建与服务端自动测试成功后更新 JAR。

1.0.0 存在普通世界生成越界问题，请替换为 1.0.1 或更新版本，不要同时安装两版。
EOF
    git add -- "$jar_name" "$package_name" SHA256SUMS.txt README.md
fi

if ! git diff --cached --quiet; then
    git -c user.name='github-actions[bot]' \
        -c user.email='41898282+github-actions[bot]@users.noreply.github.com' \
        commit -m "Publish $result build for $GITHUB_SHA"
    git push origin HEAD:refs/heads/downloads
fi
