#!/bin/bash

# Chess Opening Trainer - 実行スクリプト

JAR_FILE="target/chess-1.0-SNAPSHOT-jar-with-dependencies.jar"

# JARファイルが存在しない場合はビルド
if [ ! -f "$JAR_FILE" ]; then
    echo "JARファイルが見つかりません。ビルドを実行します..."
    mvn clean package
    if [ $? -ne 0 ]; then
        echo "ビルドに失敗しました。"
        exit 1
    fi
fi

# 引数をそのまま渡して実行
java -jar "$JAR_FILE" "$@"
