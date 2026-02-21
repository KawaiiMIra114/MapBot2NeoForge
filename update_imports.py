import os

files = [
    r"d:\axm\mcs\MapBot2NeoForge\MapBot_Reforged\src\main\java\com\mapbot\network\BridgeClient.java",
    r"d:\axm\mcs\MapBot2NeoForge\MapBot_Reforged\src\main\java\com\mapbot\network\BridgeHandlers.java",
    r"d:\axm\mcs\MapBot2NeoForge\Mapbot-Alpha-V1\src\main\java\com\mapbot\alpha\bridge\BridgeServer.java",
    r"d:\axm\mcs\MapBot2NeoForge\Mapbot-Alpha-V1\src\main\java\com\mapbot\alpha\bridge\BridgeProxy.java",
    r"d:\axm\mcs\MapBot2NeoForge\Mapbot-Alpha-V1\src\main\java\com\mapbot\alpha\bridge\BridgeFileProxy.java"
]

for f in files:
    try:
        with open(f, "r", encoding="utf-8") as file:
            content = file.read()
        
        if "com.mapbot.common.protocol.BridgeErrorMapper" not in content:
            if "package com.mapbot.network;" in content:
                content = content.replace("package com.mapbot.network;", "package com.mapbot.network;\n\nimport com.mapbot.common.protocol.BridgeErrorMapper;")
            elif "package com.mapbot.alpha.bridge;" in content:
                content = content.replace("package com.mapbot.alpha.bridge;", "package com.mapbot.alpha.bridge;\n\nimport com.mapbot.common.protocol.BridgeErrorMapper;")
                
            with open(f, "w", encoding="utf-8") as file:
                file.write(content)
            print(f"Updated {os.path.basename(f)}")
    except Exception as e:
        print(f"Error on {f}: {e}")

# Reforged build.gradle
try:
    reforged_bg = r"d:\axm\mcs\MapBot2NeoForge\MapBot_Reforged\build.gradle"
    with open(reforged_bg, "r", encoding="utf-8") as f:
        rbg = f.read()
    if "project(':MapBot_Common')" not in rbg:
        with open(reforged_bg, "a", encoding="utf-8") as f:
            f.write("\ndependencies {\n    implementation project(':MapBot_Common')\n}\n")
        print("Updated Reforged build.gradle")
except Exception as e:
    print(e)
    
# Alpha build.gradle
try:
    alpha_bg = r"d:\axm\mcs\MapBot2NeoForge\Mapbot-Alpha-V1\build.gradle"
    with open(alpha_bg, "r", encoding="utf-8") as f:
        bg = f.read()
    if "project(':MapBot_Common')" not in bg:
        bg = bg.replace("implementation 'redis.clients:jedis:5.1.0'", "implementation 'redis.clients:jedis:5.1.0'\n    implementation project(':MapBot_Common')")
        with open(alpha_bg, "w", encoding="utf-8") as f:
            f.write(bg)
        print("Updated Alpha build.gradle")
except Exception as e:
    print(e)
