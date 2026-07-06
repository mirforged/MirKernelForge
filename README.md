# MirKernelForge

> [!WARNING]\
> 当前处于早期开发阶段 BUG很多 请慎重使用！\
> 使用前必须备份数据 开发者不对产生的数据损失负责

### 目的
此项目意图接续[KPatch-Next](https://github.com/KernelSU-Next/KPatch-Next)的任务\
实现对Magisk/KernelSU/..的KPM支持\

### 能力
> 1.往kernel嵌入kpm\
> 2.即时加载kpm能力(可选)

借助 kforged_user_api.kpm 实现即时(加/卸载、控制kpm)\
~~(代价是暴露少量接口 我不认为这会降低过多隐藏性能)~~


待解决：如果未启用kforged_user_api, 那么点击Modules将会闪退