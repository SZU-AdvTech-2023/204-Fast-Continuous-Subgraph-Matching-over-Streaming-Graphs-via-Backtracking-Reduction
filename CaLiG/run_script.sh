#!/bin/bash

# 设置循环次数
N=50  # 你想要的循环次数

for ((i=1; i<=N; i++))
do
    # 构建命令
    compile_command="g++ -O2 ./calig1.cpp -o ./calig -std=c++11"
    run_command="./calig -d ./deezer5/initial -q ./deezer5/Q/6/q$i -s ./deezer5/s"

    # 打印命令
    echo "Running command: $compile_command"
    echo "Running command: $run_command"

    # 编译
    $compile_command

    # 检查编译是否成功
    if [ $? -eq 0 ]; then
        # 执行命令
        $run_command
    else
        echo "Compilation failed. Check your code and try again."
        exit 1
    fi
done