package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.CommandRegistry;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.command.QqRoleResolver;
import com.mapbot.alpha.config.AlphaConfig;
import com.mapbot.alpha.security.ContractRole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 帮助命令
 * #help / #菜单
 */
public class HelpCommand implements ICommand {

    private enum Category {
        FUNCTION("功能类"),
        FUN("娱乐类"),
        TOOL("工具类"),
        ADMIN("管理类"),
        OTHER("其他");

        final String title;

        Category(String title) {
            this.title = title;
        }
    }

    private static final Map<String, String> SUBCOMMAND_TAGS = Map.of(
        "accept", "sign",
        "cancelstop", "stopserver",
        "agreeunbind", "adminunbind"
    );

    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        boolean showAll = "all".equalsIgnoreCase(args == null ? "" : args.trim());
        boolean isPlayerGroup = sourceGroupId == AlphaConfig.getPlayerGroupId();
        boolean isAdminGroup = sourceGroupId == AlphaConfig.getAdminGroupId();
        boolean isPrivateContext = !isPlayerGroup && !isAdminGroup;

        ContractRole callerRole = QqRoleResolver.resolveRole(senderQQ);

        StringBuilder sb = new StringBuilder();
        sb.append("=== MapBot 命令帮助 ===\n");
        sb.append("当前角色: ").append(QqRoleResolver.roleLabel(callerRole)).append("\n");

        if (showAll) {
            Set<Category> categoryFilter = categoriesForContext(isPlayerGroup, isAdminGroup, true);
            renderAll(sb, callerRole, isAdminGroup, isPrivateContext, categoryFilter);
            return sb.toString().trim();
        }

        if (isPlayerGroup) {
            renderAvailable(sb, callerRole, false, false, EnumSet.of(Category.FUNCTION, Category.FUN));
            sb.append("\n[提示] 输入 #help all 查看全部命令");
            return sb.toString().trim();
        }

        if (isAdminGroup) {
            renderAvailable(sb, callerRole, true, false, EnumSet.of(Category.ADMIN, Category.TOOL));
            sb.append("\n[提示] 输入 #help all 查看全部命令");
            return sb.toString().trim();
        }

        renderAvailable(sb, callerRole, false, true, EnumSet.allOf(Category.class));
        sb.append("\n[提示] 输入 #help all 查看全部命令");
        return sb.toString().trim();
    }

    private Set<Category> categoriesForContext(boolean isPlayerGroup, boolean isAdminGroup, boolean showAll) {
        if (isPlayerGroup) {
            return EnumSet.of(Category.FUNCTION, Category.FUN);
        }
        if (isAdminGroup && !showAll) {
            return EnumSet.of(Category.ADMIN, Category.TOOL);
        }
        return EnumSet.allOf(Category.class);
    }

    private void renderAll(
        StringBuilder sb,
        ContractRole callerRole,
        boolean inAdminGroup,
        boolean privateContext,
        Set<Category> categoryFilter
    ) {
        sb.append("\n[可用命令]\n");
        renderByCategory(sb, callerRole, inAdminGroup, privateContext, true, categoryFilter);

        sb.append("\n[暂不可用]\n");
        renderByCategory(sb, callerRole, inAdminGroup, privateContext, false, categoryFilter);
    }

    private void renderAvailable(
        StringBuilder sb,
        ContractRole callerRole,
        boolean inAdminGroup,
        boolean privateContext,
        Set<Category> categoryFilter
    ) {
        sb.append("\n[可用命令]\n");
        renderByCategory(sb, callerRole, inAdminGroup, privateContext, true, categoryFilter);
    }

    private void renderByCategory(
        StringBuilder sb,
        ContractRole callerRole,
        boolean inAdminGroup,
        boolean privateContext,
        boolean available,
        Set<Category> categoryFilter
    ) {
        Map<Category, List<String>> grouped = groupCommandsByCategory();
        for (Category category : Category.values()) {
            if (!categoryFilter.contains(category)) {
                continue;
            }
            List<String> names = grouped.get(category);
            if (names == null || names.isEmpty()) {
                continue;
            }

            List<String> lines = new ArrayList<>();
            for (String name : names) {
                ICommand cmd = CommandRegistry.getCommands().get(name);
                if (cmd == null) {
                    continue;
                }
                boolean canUse = canExecute(cmd, callerRole, inAdminGroup, privateContext);
                if (canUse != available) {
                    continue;
                }

                String line = formatLine(name, cmd);
                if (!canUse) {
                    line += " " + permissionTag(cmd);
                }
                lines.add(line);
            }

            if (lines.isEmpty()) {
                continue;
            }
            sb.append("\n[").append(category.title).append("]\n");
            for (String line : lines) {
                sb.append(line).append("\n");
            }
        }
    }

    private Map<Category, List<String>> groupCommandsByCategory() {
        Map<Category, List<String>> grouped = new EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            grouped.put(c, new ArrayList<>());
        }

        List<String> names = new ArrayList<>(CommandRegistry.getCommands().keySet());
        names.sort(Comparator.naturalOrder());
        for (String name : names) {
            grouped.get(resolveCategory(name)).add(name);
        }
        return grouped;
    }

    private Category resolveCategory(String commandName) {
        String cmd = commandName.toLowerCase(Locale.ROOT);
        return switch (cmd) {
            case "help", "list", "status", "myperm", "id", "unbind", "playtime", "time" -> Category.FUNCTION;
            case "sign", "accept", "cdk" -> Category.FUN;
            case "reload" -> Category.TOOL;
            case "mute", "unmute", "setperm", "addadmin", "removeadmin",
                 "adminunbind", "agreeunbind", "stopserver", "cancelstop",
                 "inv", "location" -> Category.ADMIN;
            default -> Category.OTHER;
        };
    }

    private String formatLine(String commandName, ICommand cmd) {
        StringBuilder line = new StringBuilder();
        line.append("#").append(commandName);

        String help = cmd.getHelp();
        if (help != null && !help.isEmpty()) {
            line.append(" - ").append(help);
        }

        List<String> aliases = CommandRegistry.getAliasesFor(commandName);
        if (!aliases.isEmpty()) {
            line.append(" (别名: ");
            for (int i = 0; i < aliases.size(); i++) {
                if (i > 0) {
                    line.append(", ");
                }
                line.append("#").append(aliases.get(i));
            }
            line.append(")");
        }

        String parent = SUBCOMMAND_TAGS.get(commandName);
        if (parent != null && !parent.isBlank()) {
            line.append(" [附属 #").append(parent).append("]");
        }

        return line.toString();
    }

    private String permissionTag(ICommand cmd) {
        List<String> tags = new ArrayList<>();
        if (cmd.requiredRole() != null && cmd.requiredRole() != ContractRole.USER) {
            tags.add("需 " + QqRoleResolver.roleKey(cmd.requiredRole()));
        }
        if (cmd.requiresAdmin() && (cmd.requiredRole() == null || cmd.requiredRole() == ContractRole.USER)) {
            tags.add("需 admin (legacy)");
        }
        if (cmd.requiredPermLevel() > 0 && (cmd.requiredRole() == null || cmd.requiredRole() == ContractRole.USER)) {
            tags.add("需 admin (legacy-level)");
        }
        if (cmd.adminGroupOnly()) {
            tags.add("限管理群");
        }
        if (tags.isEmpty()) {
            return "";
        }
        return "[" + String.join(" / ", tags) + "]";
    }

    private boolean canExecute(ICommand cmd, ContractRole callerRole, boolean inAdminGroup, boolean privateContext) {
        if (cmd.adminGroupOnly() && !inAdminGroup) {
            if (!(privateContext && callerRole.hasAtLeast(ContractRole.ADMIN))) {
                return false;
            }
        }
        if (!callerRole.hasAtLeast(cmd.requiredRole())) {
            return false;
        }
        if (cmd.requiresAdmin() && !callerRole.hasAtLeast(ContractRole.ADMIN)) {
            return false;
        }
        return cmd.requiredPermLevel() <= 0 || callerRole.hasAtLeast(ContractRole.ADMIN);
    }

    @Override
    public String getHelp() {
        return "显示命令帮助 (#help all 查看全部)";
    }
}
