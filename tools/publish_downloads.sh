#!/usr/bin/env bash
# Executed by the trusted main-branch GitHub workflow after build/tests.
set -euo pipefail

: "${GITHUB_SHA:?}"
: "${GITHUB_REPOSITORY:?}"
: "${RUNNER_TEMP:?}"
jar_name=bolishijie-fabric-1.21.10-v1.0.0.jar
source_name=bolishijie-source-v1.0.0.zip
result="${BUILD_RESULT:-not-run}"

git archive --format=zip --output="$RUNNER_TEMP/$source_name" "$GITHUB_SHA"
if [[ "$result" == success ]]; then
    # Verify this is a built mod, not the wrapper/sources JAR or an empty archive.
    python3 - <<'PY'
import json
from zipfile import ZipFile
with ZipFile('build/libs/bolishijie-1.0.0.jar') as jar:
    assert jar.testzip() is None
    mod = json.loads(jar.read('fabric.mod.json'))
    assert mod['id'] == 'bolishijie' and mod['version'] == '1.0.0'
    assert 'cn/susu2020oxo/bolishijie/GlassWorld.class' in jar.namelist()
    assert 'assets/bolishijie/equipment/glass.json' in jar.namelist()
    assert 'bolishijie.mixins.json' in jar.namelist()
PY
    cp build/libs/bolishijie-1.0.0.jar "$RUNNER_TEMP/$jar_name"
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
    sha256sum "$jar_name" "$source_name" > SHA256SUMS.txt
    cat > README.md <<'EOF'
# 玻璃世界 · Fabric 1.21.10 下载

需要 Minecraft Java 1.21.10、Java 21、Fabric Loader 0.17.3+，以及对应 1.21.10 的 Fabric API。
将成品 JAR 与 Fabric API 放入客户端和服务端的 mods 文件夹。创建新世界以生成玻璃地形。

- [下载模组 JAR](https://github.com/susu2020oxo/bolishijie/raw/refs/heads/downloads/bolishijie-fabric-1.21.10-v1.0.0.jar)
- [下载源码 ZIP](https://github.com/susu2020oxo/bolishijie/raw/refs/heads/downloads/bolishijie-source-v1.0.0.zip)
- [SHA-256](SHA256SUMS.txt)

源码 ZIP 不是成品模组。此目录仅在构建与服务端自动测试成功后更新 JAR。
EOF
    git add -- "$jar_name" SHA256SUMS.txt README.md
fi

if ! git diff --cached --quiet; then
    git -c user.name='github-actions[bot]' \
        -c user.email='41898282+github-actions[bot]@users.noreply.github.com' \
        commit -m "Publish $result build for $GITHUB_SHA"
    git push origin HEAD:refs/heads/downloads
fi
