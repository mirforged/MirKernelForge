cmake_minimum_required(VERSION 3.22.1)

project("moduleloader")

add_library(
        moduleloader
        SHARED
        native-lib.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        moduleloader
        ${log-lib})
