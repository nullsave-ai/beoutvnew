cmake_minimum_required(VERSION 3.18.1)
project(beout_optical C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(beout_optical SHARED beout_optical.cpp)

# خفة قصوى: بلا استثناءات ولا RTTI ولا STL، رموز مخفية، حذف الأكواد غير المستخدمة
target_compile_options(beout_optical PRIVATE
    -O2 -fvisibility=hidden -fvisibility-inlines-hidden
    -ffunction-sections -fdata-sections -fno-exceptions -fno-rtti)
target_link_options(beout_optical PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL -flto)
target_link_libraries(beout_optical jnigraphics)
