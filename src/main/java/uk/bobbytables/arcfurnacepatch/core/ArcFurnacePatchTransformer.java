package uk.bobbytables.arcfurnacepatch.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;

import java.util.Iterator;

import static uk.bobbytables.arcfurnacepatch.core.ArcFurnacePatchLoadingPlugin.AFP_LOG;
import static uk.bobbytables.arcfurnacepatch.core.ArcFurnacePatchLoadingPlugin.UPDATE;

public class ArcFurnacePatchTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!transformedName.equals("blusunrize.immersiveengineering.common.blocks.metal.TileEntityArcFurnace"))
            return basicClass;
    
        ClassReader classReader = new ClassReader(basicClass);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals(UPDATE)) {
                Iterator<AbstractInsnNode> insnNodeIterator = methodNode.instructions.iterator();
                while (insnNodeIterator.hasNext()) {
                    AbstractInsnNode abstractInsnNode = insnNodeIterator.next();
                    if (abstractInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode)abstractInsnNode;
                        if (methodInsnNode.name.equals("setInputAmounts")) {
                            int invokeIndex = methodNode.instructions.indexOf(abstractInsnNode);
                            int targetIndex = invokeIndex - 26;
                            AbstractInsnNode targetInsnNode = methodNode.instructions.get(targetIndex);
                            if (targetInsnNode.getOpcode() != Opcodes.ICONST_1) {
                                AFP_LOG.warn("Not applying arc furnace patch, opcode mismatch! Has this been resolved in Immersive Engineering?");
                                AFP_LOG.warn("Expected {}, but found {}", Printer.OPCODES[Opcodes.ICONST_1], Printer.OPCODES[targetInsnNode.getOpcode()]);
                                break;
                            }
                            InsnList insnList = new InsnList();
                            insnList.add(new VarInsnNode(Opcodes.ALOAD, 5));
                            insnList.add(new FieldInsnNode(Opcodes.GETFIELD, "blusunrize/immersiveengineering/api/crafting/ArcFurnaceRecipe", "input", "Lblusunrize/immersiveengineering/api/crafting/IngredientStack;"));
                            insnList.add(new FieldInsnNode(Opcodes.GETFIELD, "blusunrize/immersiveengineering/api/crafting/IngredientStack", "inputSize", "I"));
                            methodNode.instructions.insertBefore(targetInsnNode, insnList);
                            methodNode.instructions.remove(targetInsnNode);
                            AFP_LOG.info("Successful transformation of {}", transformedName);
                            break;
                        }
                    }
                }
            }
        }
        
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
