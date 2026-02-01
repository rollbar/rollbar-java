package com.rollbar.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import static org.objectweb.asm.Opcodes.*;

public class NetworkTransformer implements ClassFileTransformer {

  @Override
  public byte[] transform(
    ClassLoader loader,
    String className,
    Class<?> classBeingRedefined,
    ProtectionDomain domain,
    byte[] classfileBuffer
  ) {

    if (!"sun/net/www/protocol/http/HttpURLConnection".equals(className)) {
      return null;
    }

    System.out.println("Instrumenting " + className);

    ClassReader cr = new ClassReader(classfileBuffer);
    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);

    ClassVisitor cv = new ClassVisitor(ASM9, cw) {

      @Override
      public MethodVisitor visitMethod(
        int access,
        String name,
        String descriptor,
        String signature,
        String[] exceptions
      ) {

        MethodVisitor mv =
          super.visitMethod(access, name, descriptor, signature, exceptions);

        if ("connect".equals(name) && "()V".equals(descriptor)) {

          return new AdviceAdapter(ASM9, mv, access, name, descriptor) {

            @Override
            protected void onMethodEnter() {

              // this.getURL()
              loadThis();
              invokeVirtual(
                Type.getObjectType("java/net/URLConnection"),
                new Method("getURL", "()Ljava/net/URL;")
              );

              // this.getRequestMethod()
              loadThis();
              invokeVirtual(
                Type.getObjectType("java/net/HttpURLConnection"),
                new Method("getRequestMethod", "()Ljava/lang/String;")
              );

              // System.nanoTime()
              invokeStatic(
                Type.getType(System.class),
                new Method("nanoTime", "()J")
              );

              // NetworkHook.onConnect(url, method, startTime)
              invokeStatic(
                Type.getObjectType("com/rollbar/agent/NetworkHook"),
                new Method(
                  "onConnect",
                  "(Ljava/net/URL;Ljava/lang/String;J)V"
                )
              );
            }
          };
        }

        if ("getResponseCode".equals(name) && "()I".equals(descriptor)) {

          return new AdviceAdapter(ASM9, mv, access, name, descriptor) {

            @Override
            protected void onMethodExit(int opcode) {
              if (opcode != IRETURN) return;

              dup();
              int statusVar = newLocal(Type.INT_TYPE);
              storeLocal(statusVar);

              // NetworkHook.onResponse(status)
              loadLocal(statusVar);
              invokeStatic(
                Type.getObjectType("com/rollbar/agent/NetworkHook"),
                new Method("onResponse", "(I)V")
              );

              loadLocal(statusVar);
            }
          };
        }

        return mv;
      }
    };

    cr.accept(cv, ClassReader.EXPAND_FRAMES);
    return cw.toByteArray();
  }
}
