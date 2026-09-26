package local.lpcpro;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

public final class FoliaJarPatcher {
    private static final String HELPER = "com/wikmor/lpcpro/folia/FoliaScheduler";

    private FoliaJarPatcher() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: FoliaJarPatcher <input.jar> <output.jar>");
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Files.deleteIfExists(output);
        try (JarInputStream in = new JarInputStream(Files.newInputStream(input));
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            JarEntry entry;
            while ((entry = in.getNextJarEntry()) != null) {
                byte[] bytes = in.readAllBytes();
                String name = entry.getName();
                if ("plugin.yml".equals(name)) {
                    bytes = patchPluginYml(bytes);
                } else if (name.endsWith(".class") && name.startsWith("com/wikmor/lpcpro/")) {
                    bytes = patchClass(bytes);
                }
                JarEntry newEntry = new JarEntry(name);
                out.putNextEntry(newEntry);
                out.write(bytes);
                out.closeEntry();
            }
            addHelper(out, "com/wikmor/lpcpro/folia/FoliaScheduler.class");
            addHelper(out, "com/wikmor/lpcpro/folia/FoliaAudienceCompat.class");
            addHelper(out, "com/wikmor/lpcpro/folia/FoliaEntityCompat.class");
            addHelper(out, "com/wikmor/lpcpro/folia/FoliaDisplayCompat.class");
            addHelper(out, "com/wikmor/lpcpro/folia/FoliaManagerCompat.class");
            addHelper(out, "com/wikmor/lpcpro/folia/FoliaServerCompat.class");
            addHelper(out, "com/wikmor/lpcpro/bubble/FoliaChatBubbleCompat.class");
            addHelper(out, "com/wikmor/lpcpro/listener/FoliaChatCompat.class");
        }
    }

    private static byte[] patchPluginYml(byte[] bytes) {
        String yml = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        if (!yml.contains("folia-supported:")) {
            yml = yml.replaceFirst("(?m)^api-version:.*$", "$0\nfolia-supported: true");
        }
        yml = yml.replaceAll("(?m)^author:.*\\R?", "");
        yml = yml.replaceAll("(?m)^authors:.*\\R?", "");
        yml = yml.replaceAll("(?m)^website:.*\\R?", "");
        yml = yml.replaceFirst("(?m)^version:.*$", "$0\nauthor: Kayji_Tizi\nwebsite: Kayji_Tizi");
        return yml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void addHelper(JarOutputStream out, String resource) throws IOException {
        try (var stream = FoliaJarPatcher.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Missing helper class: " + resource);
            }
            out.putNextEntry(new JarEntry(resource));
            out.write(stream.readAllBytes());
            out.closeEntry();
        }
    }

    private static byte[] patchClass(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            private String className;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.className = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (className.equals("com/wikmor/lpcpro/update/UpdateChecker") && name.equals("checkForUpdates")
                        && descriptor.equals("(Z)V")) {
                    return emptyVoidMethod(mv, access, descriptor);
                }
                if (className.equals("com/wikmor/lpcpro/update/UpdateChecker") && name.startsWith("lambda$checkForUpdates$")) {
                    return emptyVoidMethod(mv, access, descriptor);
                }
                if (className.equals("com/wikmor/lpcpro/BukkitPlugin") && name.equals("loadConfig0")
                        && descriptor.equals("()V")) {
                    return emptyVoidMethod(mv, access, descriptor);
                }
                if (className.equals("com/wikmor/lpcpro/util/Metrics$MetricsBase")
                        && (name.equals("startSubmitting") || name.equals("submitData") || name.equals("sendData"))) {
                    return emptyVoidMethod(mv, access, descriptor);
                }
                if (className.equals("com/wikmor/lpcpro/bubble/ChatBubbleManager$1") && name.equals("run")
                        && descriptor.equals("()V")) {
                    return chatBubbleFollowRun(mv);
                }
                if (className.equals("com/wikmor/lpcpro/bubble/ChatBubbleManager") && name.equals("removeBubbles")
                        && descriptor.equals("(Lorg/bukkit/entity/Player;)V")) {
                    return chatBubbleRemoveBubbles(mv);
                }
                String currentMethod = name;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC && owner.equals("org/bukkit/Bukkit") && name.equals("getScheduler")
                                && descriptor.equals("()Lorg/bukkit/scheduler/BukkitScheduler;")) {
                            return;
                        }
                        if (opcode == Opcodes.INVOKESTATIC && owner.equals("org/bukkit/Bukkit") && name.equals("dispatchCommand")
                                && descriptor.equals("(Lorg/bukkit/command/CommandSender;Ljava/lang/String;)Z")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaServerCompat", "dispatchCommand",
                                    descriptor, false);
                            return;
                        }
                        if ((opcode == Opcodes.INVOKEINTERFACE || opcode == Opcodes.INVOKEVIRTUAL)
                                && (owner.equals("org/bukkit/Server") || owner.equals("org/bukkit/plugin/java/JavaPlugin"))
                                && name.equals("getScheduler")
                                && descriptor.equals("()Lorg/bukkit/scheduler/BukkitScheduler;")) {
                            super.visitInsn(Opcodes.POP);
                            return;
                        }
                        if (owner.equals("org/bukkit/scheduler/BukkitScheduler")) {
                            String mapped = switch (name) {
                                case "runTask" -> "runTask";
                                case "runTaskLater" -> "runTaskLater";
                                case "runTaskTimer" -> "runTaskTimer";
                                case "runTaskAsynchronously" -> "runTaskAsynchronously";
                                case "runTaskLaterAsynchronously" -> "runTaskLaterAsynchronously";
                                case "runTaskTimerAsynchronously" -> "runTaskTimerAsynchronously";
                                case "cancelTasks" -> "cancelTasks";
                                default -> null;
                            };
                            if (mapped != null) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, mapped, descriptor, false);
                                return;
                            }
                        }
                        if (owner.equals("org/bukkit/scheduler/BukkitRunnable") && name.equals("runTaskTimer")
                                && descriptor.equals("(Lorg/bukkit/plugin/Plugin;JJ)Lorg/bukkit/scheduler/BukkitTask;")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "runBukkitRunnableTimer",
                                    "(Lorg/bukkit/scheduler/BukkitRunnable;Lorg/bukkit/plugin/Plugin;JJ)Lorg/bukkit/scheduler/BukkitTask;", false);
                            return;
                        }
                        if (owner.equals("org/bukkit/scheduler/BukkitRunnable") && name.equals("runTask")
                                && descriptor.equals("(Lorg/bukkit/plugin/Plugin;)Lorg/bukkit/scheduler/BukkitTask;")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "runBukkitRunnable",
                                    "(Lorg/bukkit/scheduler/BukkitRunnable;Lorg/bukkit/plugin/Plugin;)Lorg/bukkit/scheduler/BukkitTask;", false);
                            return;
                        }
                        if (owner.equals("org/bukkit/scheduler/BukkitRunnable") && name.equals("runTaskLater")
                                && descriptor.equals("(Lorg/bukkit/plugin/Plugin;J)Lorg/bukkit/scheduler/BukkitTask;")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "runBukkitRunnableLater",
                                    "(Lorg/bukkit/scheduler/BukkitRunnable;Lorg/bukkit/plugin/Plugin;J)Lorg/bukkit/scheduler/BukkitTask;", false);
                            return;
                        }
                        if (owner.equals("org/bukkit/scheduler/BukkitRunnable") && name.equals("runTaskAsynchronously")
                                && descriptor.equals("(Lorg/bukkit/plugin/Plugin;)Lorg/bukkit/scheduler/BukkitTask;")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "runBukkitRunnableAsynchronously",
                                    "(Lorg/bukkit/scheduler/BukkitRunnable;Lorg/bukkit/plugin/Plugin;)Lorg/bukkit/scheduler/BukkitTask;", false);
                            return;
                        }
                        if (owner.equals("org/bukkit/scheduler/BukkitRunnable") && name.equals("runTaskLaterAsynchronously")
                                && descriptor.equals("(Lorg/bukkit/plugin/Plugin;J)Lorg/bukkit/scheduler/BukkitTask;")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "runBukkitRunnableLaterAsynchronously",
                                    "(Lorg/bukkit/scheduler/BukkitRunnable;Lorg/bukkit/plugin/Plugin;J)Lorg/bukkit/scheduler/BukkitTask;", false);
                            return;
                        }
                        if (owner.equals("org/bukkit/scheduler/BukkitRunnable") && name.equals("runTaskTimerAsynchronously")
                                && descriptor.equals("(Lorg/bukkit/plugin/Plugin;JJ)Lorg/bukkit/scheduler/BukkitTask;")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "runBukkitRunnableTimerAsynchronously",
                                    "(Lorg/bukkit/scheduler/BukkitRunnable;Lorg/bukkit/plugin/Plugin;JJ)Lorg/bukkit/scheduler/BukkitTask;", false);
                            return;
                        }
                        if (owner.equals("com/wikmor/lpcpro/listener/ChatHandler") && name.equals("handle")
                                && descriptor.equals("(Lcom/wikmor/lpcpro/channel/ChatMessage;)V")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/listener/FoliaChatCompat", "handle", descriptor, false);
                            return;
                        }
                        if (owner.equals("com/wikmor/lpcpro/tablist/TabListManager") && name.equals("updatePlayer")
                                && descriptor.equals("(Lorg/bukkit/entity/Player;)V")
                                && !(owner.equals(className) && currentMethod.equals("updatePlayer"))) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaManagerCompat", "updateTabPlayer",
                                    "(Lcom/wikmor/lpcpro/tablist/TabListManager;Lorg/bukkit/entity/Player;)V", false);
                            return;
                        }
                        if (owner.equals("com/wikmor/lpcpro/platform/BukkitPlayer")) {
                            String mapped = switch (name) {
                                case "sendMessage" -> descriptor.equals("(Lnet/kyori/adventure/text/Component;)V") ? "sendMessage" : null;
                                case "sendPlayerListHeaderAndFooter" -> descriptor.equals("(Lnet/kyori/adventure/text/Component;Lnet/kyori/adventure/text/Component;)V") ? "sendPlayerListHeaderAndFooter" : null;
                                case "showTitle" -> descriptor.equals("(Lnet/kyori/adventure/title/Title;)V") ? "showTitle" : null;
                                case "playSound" -> descriptor.equals("(Lnet/kyori/adventure/sound/Sound;)V") ? "playSound" : null;
                                case "sendActionBar" -> descriptor.equals("(Lnet/kyori/adventure/text/Component;)V") ? "sendActionBar" : null;
                                default -> null;
                            };
                            if (mapped != null) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaAudienceCompat", mapped,
                                        "(Lcom/wikmor/lpcpro/platform/BukkitPlayer;" + descriptor.substring(1), false);
                                return;
                            }
                        }
                        if (owner.equals("org/bukkit/entity/Player")) {
                            if (name.equals("kickPlayer") && descriptor.equals("(Ljava/lang/String;)V")) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaAudienceCompat", "kickPlayer",
                                        "(Lorg/bukkit/entity/Player;Ljava/lang/String;)V", false);
                                return;
                            }
                            if (name.equals("banPlayer") && descriptor.equals("(Ljava/lang/String;Ljava/util/Date;Ljava/lang/String;)Lorg/bukkit/BanEntry;")) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaAudienceCompat", "banPlayer",
                                        "(Lorg/bukkit/entity/Player;Ljava/lang/String;Ljava/util/Date;Ljava/lang/String;)Lorg/bukkit/BanEntry;", false);
                                return;
                            }
                            if (name.equals("banPlayer") && descriptor.equals("(Ljava/lang/String;Ljava/util/Date;Ljava/lang/String;Z)Lorg/bukkit/BanEntry;")) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaAudienceCompat", "banPlayer",
                                        "(Lorg/bukkit/entity/Player;Ljava/lang/String;Ljava/util/Date;Ljava/lang/String;Z)Lorg/bukkit/BanEntry;", false);
                                return;
                            }
                            if (name.equals("ban") && descriptor.equals("(Ljava/lang/String;Ljava/util/Date;Ljava/lang/String;Z)Lorg/bukkit/BanEntry;")) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaAudienceCompat", "ban",
                                        "(Lorg/bukkit/entity/Player;Ljava/lang/String;Ljava/util/Date;Ljava/lang/String;Z)Lorg/bukkit/BanEntry;", false);
                                return;
                            }
                            if (name.equals("teleport") && descriptor.equals("(Lorg/bukkit/Location;)Z")) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaAudienceCompat", "teleport",
                                        "(Lorg/bukkit/entity/Player;Lorg/bukkit/Location;)Z", false);
                                return;
                            }
                            if (name.equals("openInventory") && descriptor.equals("(Lorg/bukkit/inventory/Inventory;)Lorg/bukkit/inventory/InventoryView;")) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaAudienceCompat", "openInventory",
                                        "(Lorg/bukkit/entity/Player;Lorg/bukkit/inventory/Inventory;)Lorg/bukkit/inventory/InventoryView;", false);
                                return;
                            }
                            if (name.equals("closeInventory") && descriptor.equals("()V")) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaAudienceCompat", "closeInventory",
                                        "(Lorg/bukkit/entity/Player;)V", false);
                                return;
                            }
                            if (name.equals("sendPluginMessage") && descriptor.equals("(Lorg/bukkit/plugin/Plugin;Ljava/lang/String;[B)V")) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaAudienceCompat", "sendPluginMessage",
                                        "(Lorg/bukkit/entity/Player;Lorg/bukkit/plugin/Plugin;Ljava/lang/String;[B)V", false);
                                return;
                            }
                            if (name.equals("performCommand") && descriptor.equals("(Ljava/lang/String;)Z")) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaAudienceCompat", "performCommand",
                                        "(Lorg/bukkit/entity/Player;Ljava/lang/String;)Z", false);
                                return;
                            }
                            if (name.equals("chat") && descriptor.equals("(Ljava/lang/String;)V")) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaAudienceCompat", "chat",
                                        "(Lorg/bukkit/entity/Player;Ljava/lang/String;)V", false);
                                return;
                            }
                            if (name.equals("playSound") && descriptor.equals("(Lorg/bukkit/Location;Lorg/bukkit/Sound;FF)V")) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaAudienceCompat", "playSound",
                                        "(Lorg/bukkit/entity/Player;Lorg/bukkit/Location;Lorg/bukkit/Sound;FF)V", false);
                                return;
                            }
                        }
                        if (owner.startsWith("org/bukkit/entity/") && name.equals("teleport")
                                && descriptor.equals("(Lorg/bukkit/Location;)Z")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaEntityCompat", "teleport",
                                    "(Lorg/bukkit/entity/Entity;Lorg/bukkit/Location;)Z", false);
                            return;
                        }
                        if (owner.startsWith("org/bukkit/entity/") && name.equals("remove")
                                && descriptor.equals("()V")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaEntityCompat", "remove",
                                    "(Lorg/bukkit/entity/Entity;)V", false);
                            return;
                        }
                        if (owner.startsWith("org/bukkit/entity/") && name.equals("isDead")
                                && descriptor.equals("()Z")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaEntityCompat", "isDead",
                                    "(Lorg/bukkit/entity/Entity;)Z", false);
                            return;
                        }
                        if (owner.startsWith("org/bukkit/entity/") && name.equals("setInterpolationDuration")
                                && descriptor.equals("(I)V")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaDisplayCompat", "setInterpolationDuration",
                                    "(Lorg/bukkit/entity/Display;I)V", false);
                            return;
                        }
                        if (owner.startsWith("org/bukkit/entity/") && name.equals("setInterpolationDelay")
                                && descriptor.equals("(I)V")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaDisplayCompat", "setInterpolationDelay",
                                    "(Lorg/bukkit/entity/Display;I)V", false);
                            return;
                        }
                        if (owner.startsWith("org/bukkit/entity/") && name.equals("setTransformation")
                                && descriptor.equals("(Lorg/bukkit/util/Transformation;)V")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaDisplayCompat", "setTransformation",
                                    "(Lorg/bukkit/entity/Display;Lorg/bukkit/util/Transformation;)V", false);
                            return;
                        }
                        if (owner.startsWith("org/bukkit/entity/") && name.equals("setBillboard")
                                && descriptor.equals("(Lorg/bukkit/entity/Display$Billboard;)V")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaDisplayCompat", "setBillboard",
                                    "(Lorg/bukkit/entity/Display;Lorg/bukkit/entity/Display$Billboard;)V", false);
                            return;
                        }
                        if (owner.startsWith("org/bukkit/entity/") && name.equals("setText")
                                && descriptor.equals("(Ljava/lang/String;)V")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaDisplayCompat", "setText",
                                    "(Lorg/bukkit/entity/TextDisplay;Ljava/lang/String;)V", false);
                            return;
                        }
                        if (owner.startsWith("org/bukkit/entity/") && name.equals("setShadowed")
                                && descriptor.equals("(Z)V")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaDisplayCompat", "setShadowed",
                                    "(Lorg/bukkit/entity/TextDisplay;Z)V", false);
                            return;
                        }
                        if (owner.startsWith("org/bukkit/entity/") && name.equals("setAlignment")
                                && descriptor.equals("(Lorg/bukkit/entity/TextDisplay$TextAlignment;)V")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaDisplayCompat", "setAlignment",
                                    "(Lorg/bukkit/entity/TextDisplay;Lorg/bukkit/entity/TextDisplay$TextAlignment;)V", false);
                            return;
                        }
                        if (owner.startsWith("org/bukkit/entity/") && name.equals("setLineWidth")
                                && descriptor.equals("(I)V")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaDisplayCompat", "setLineWidth",
                                    "(Lorg/bukkit/entity/TextDisplay;I)V", false);
                            return;
                        }
                        if (owner.startsWith("org/bukkit/entity/") && name.equals("setDefaultBackground")
                                && descriptor.equals("(Z)V")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaDisplayCompat", "setDefaultBackground",
                                    "(Lorg/bukkit/entity/TextDisplay;Z)V", false);
                            return;
                        }
                        if (owner.startsWith("org/bukkit/entity/") && name.equals("setBackgroundColor")
                                && descriptor.equals("(Lorg/bukkit/Color;)V")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaDisplayCompat", "setBackgroundColor",
                                    "(Lorg/bukkit/entity/TextDisplay;Lorg/bukkit/Color;)V", false);
                            return;
                        }
                        if (owner.startsWith("org/bukkit/entity/") && name.equals("setPersistent")
                                && descriptor.equals("(Z)V")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/folia/FoliaDisplayCompat", "setPersistent",
                                    "(Lorg/bukkit/entity/Entity;Z)V", false);
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }
                };
            }
        }, 0);
        return writer.toByteArray();
    }

    private static MethodVisitor emptyVoidMethod(MethodVisitor mv, int access, String descriptor) {
        return new MethodVisitor(Opcodes.ASM9, mv) {
            private boolean emitted;

            @Override
            public void visitCode() {
                if (emitted) {
                    return;
                }
                emitted = true;
                super.visitCode();
                super.visitInsn(Opcodes.RETURN);
                super.visitMaxs(0, maxLocals(access, descriptor));
                super.visitEnd();
            }

            @Override public void visitInsn(int opcode) {}
            @Override public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {}
            @Override public void visitIntInsn(int opcode, int operand) {}
            @Override public void visitVarInsn(int opcode, int varIndex) {}
            @Override public void visitTypeInsn(int opcode, String type) {}
            @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {}
            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {}
            @Override public void visitInvokeDynamicInsn(String name, String descriptor, org.objectweb.asm.Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {}
            @Override public void visitJumpInsn(int opcode, org.objectweb.asm.Label label) {}
            @Override public void visitLabel(org.objectweb.asm.Label label) {}
            @Override public void visitLdcInsn(Object value) {}
            @Override public void visitIincInsn(int varIndex, int increment) {}
            @Override public void visitTableSwitchInsn(int min, int max, org.objectweb.asm.Label dflt, org.objectweb.asm.Label... labels) {}
            @Override public void visitLookupSwitchInsn(org.objectweb.asm.Label dflt, int[] keys, org.objectweb.asm.Label[] labels) {}
            @Override public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {}
            @Override public void visitTryCatchBlock(org.objectweb.asm.Label start, org.objectweb.asm.Label end, org.objectweb.asm.Label handler, String type) {}
            @Override public void visitLocalVariable(String name, String descriptor, String signature, org.objectweb.asm.Label start, org.objectweb.asm.Label end, int index) {}
            @Override public void visitLineNumber(int line, org.objectweb.asm.Label start) {}
            @Override public void visitMaxs(int maxStack, int maxLocals) {}
            @Override public void visitEnd() {}
        };
    }

    private static MethodVisitor chatBubbleFollowRun(MethodVisitor mv) {
        return new MethodVisitor(Opcodes.ASM9, mv) {
            private boolean emitted;

            @Override
            public void visitCode() {
                if (emitted) {
                    return;
                }
                emitted = true;
                super.visitCode();
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitFieldInsn(Opcodes.GETFIELD, "com/wikmor/lpcpro/bubble/ChatBubbleManager$1", "this$0",
                        "Lcom/wikmor/lpcpro/bubble/ChatBubbleManager;");
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitFieldInsn(Opcodes.GETFIELD, "com/wikmor/lpcpro/bubble/ChatBubbleManager$1", "val$player",
                        "Lorg/bukkit/entity/Player;");
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitFieldInsn(Opcodes.GETFIELD, "com/wikmor/lpcpro/bubble/ChatBubbleManager$1", "val$bubble",
                        "Lcom/wikmor/lpcpro/bubble/ChatBubbleManager$ActiveBubble;");
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitFieldInsn(Opcodes.GETFIELD, "com/wikmor/lpcpro/bubble/ChatBubbleManager$1", "val$heightOffset", "F");
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitFieldInsn(Opcodes.GETFIELD, "com/wikmor/lpcpro/bubble/ChatBubbleManager$1", "val$scale", "F");
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/bubble/FoliaChatBubbleCompat", "followTick",
                        "(Lorg/bukkit/scheduler/BukkitRunnable;Lcom/wikmor/lpcpro/bubble/ChatBubbleManager;Lorg/bukkit/entity/Player;Ljava/lang/Object;FF)V", false);
                super.visitInsn(Opcodes.RETURN);
                super.visitMaxs(6, 1);
                super.visitEnd();
            }

            @Override public void visitInsn(int opcode) {}
            @Override public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {}
            @Override public void visitIntInsn(int opcode, int operand) {}
            @Override public void visitVarInsn(int opcode, int varIndex) {}
            @Override public void visitTypeInsn(int opcode, String type) {}
            @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {}
            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {}
            @Override public void visitInvokeDynamicInsn(String name, String descriptor, org.objectweb.asm.Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {}
            @Override public void visitJumpInsn(int opcode, org.objectweb.asm.Label label) {}
            @Override public void visitLabel(org.objectweb.asm.Label label) {}
            @Override public void visitLdcInsn(Object value) {}
            @Override public void visitIincInsn(int varIndex, int increment) {}
            @Override public void visitTableSwitchInsn(int min, int max, org.objectweb.asm.Label dflt, org.objectweb.asm.Label... labels) {}
            @Override public void visitLookupSwitchInsn(org.objectweb.asm.Label dflt, int[] keys, org.objectweb.asm.Label[] labels) {}
            @Override public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {}
            @Override public void visitTryCatchBlock(org.objectweb.asm.Label start, org.objectweb.asm.Label end, org.objectweb.asm.Label handler, String type) {}
            @Override public void visitLocalVariable(String name, String descriptor, String signature, org.objectweb.asm.Label start, org.objectweb.asm.Label end, int index) {}
            @Override public void visitLineNumber(int line, org.objectweb.asm.Label start) {}
            @Override public void visitMaxs(int maxStack, int maxLocals) {}
            @Override public void visitEnd() {}
        };
    }

    private static MethodVisitor chatBubbleRemoveBubbles(MethodVisitor mv) {
        return new MethodVisitor(Opcodes.ASM9, mv) {
            private boolean emitted;

            @Override
            public void visitCode() {
                if (emitted) {
                    return;
                }
                emitted = true;
                super.visitCode();
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitVarInsn(Opcodes.ALOAD, 1);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wikmor/lpcpro/bubble/FoliaChatBubbleCompat", "removeBubbles",
                        "(Lcom/wikmor/lpcpro/bubble/ChatBubbleManager;Lorg/bukkit/entity/Player;)V", false);
                super.visitInsn(Opcodes.RETURN);
                super.visitMaxs(2, 2);
                super.visitEnd();
            }

            @Override public void visitInsn(int opcode) {}
            @Override public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {}
            @Override public void visitIntInsn(int opcode, int operand) {}
            @Override public void visitVarInsn(int opcode, int varIndex) {}
            @Override public void visitTypeInsn(int opcode, String type) {}
            @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {}
            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {}
            @Override public void visitInvokeDynamicInsn(String name, String descriptor, org.objectweb.asm.Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {}
            @Override public void visitJumpInsn(int opcode, org.objectweb.asm.Label label) {}
            @Override public void visitLabel(org.objectweb.asm.Label label) {}
            @Override public void visitLdcInsn(Object value) {}
            @Override public void visitIincInsn(int varIndex, int increment) {}
            @Override public void visitTableSwitchInsn(int min, int max, org.objectweb.asm.Label dflt, org.objectweb.asm.Label... labels) {}
            @Override public void visitLookupSwitchInsn(org.objectweb.asm.Label dflt, int[] keys, org.objectweb.asm.Label[] labels) {}
            @Override public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {}
            @Override public void visitTryCatchBlock(org.objectweb.asm.Label start, org.objectweb.asm.Label end, org.objectweb.asm.Label handler, String type) {}
            @Override public void visitLocalVariable(String name, String descriptor, String signature, org.objectweb.asm.Label start, org.objectweb.asm.Label end, int index) {}
            @Override public void visitLineNumber(int line, org.objectweb.asm.Label start) {}
            @Override public void visitMaxs(int maxStack, int maxLocals) {}
            @Override public void visitEnd() {}
        };
    }

    private static int maxLocals(int access, String descriptor) {
        int locals = (access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
        for (Type type : Type.getArgumentTypes(descriptor)) {
            locals += type.getSize();
        }
        return locals;
    }
}
