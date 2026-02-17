#!/bin/bash
# L2ExplorerDat Launcher Script
cd "$(dirname "$0")"

find_java() {
    if command -v java &>/dev/null; then
        JAVA_CMD=$(command -v java)
    else
        echo "Java not found."
        exit 1
    fi
}

# Garante que a pasta de logs existe antes de iniciar
prepare_environment() {
    if [ ! -d "log" ]; then
        mkdir "log"
        echo "Pasta log criada com sucesso."
    fi
}

run_clientdat() {
    exec "$JAVA_CMD" \
    --enable-native-access=ALL-UNNAMED \
    -splash:images/splash.png \
    -Dfile.encoding=UTF-8 \
    -Djava.util.logging.manager=org.l2explorer.log.AppLogManager \
    -Xms1g -Xmx2g \
    -jar libs/L2ExplorerDat.jar \
    -debug
}

find_java
prepare_environment
run_clientdat