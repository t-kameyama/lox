cmake_minimum_required(VERSION 3.26)
project(clox C)

set(CMAKE_C_STANDARD 11)

add_executable(clox main.c
        chunk.c
        mem.c
        debug.c
        value.c)
