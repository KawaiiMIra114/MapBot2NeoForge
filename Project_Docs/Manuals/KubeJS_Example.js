// MapBot 签到联动脚本
// 将此文件复制到: kubejs/server_scripts/mapbot_sign.js

// 监听 MapBot 签到事件
// 注意: 事件类名必须与 MapBot 源代码完全一致
ForgeEvents.on('com.mapbot.event.MapBotSignInEvent', event => {
    // 获取玩家对象和 QQ 号
    let player = event.player
    let qq = event.qq
    
    // 关键: 取消事件，阻止 MapBot 发放默认的金苹果保底
    // 如果您想保留金苹果并额外给东西，请删除这行
    event.setCanceled(true)
    
    // 控制台日志
    console.info(`[KubeJS] 处理 MapBot 签到: ${player.name.string} (QQ: ${qq})`)
    
    // === 自定义奖励逻辑 ===
    
    // 1. 给予经验
    player.giveExperiencePoints(50)
    
    // 2. 给予基础物品
    player.give('3x minecraft:cooked_beef')
    player.give('1x minecraft:iron_ingot')
    
    // 3. 随机奖励 (20% 概率)
    if (Math.random() < 0.2) {
        player.give('1x minecraft:diamond')
        player.tell(Component.gold('✨ [欧皇] 签到暴击！获得 1x 钻石'))
    }
    
    // 4. 反馈消息
    player.tell(Component.green(`[MapBot] QQ ${qq} 签到成功！`))
})
