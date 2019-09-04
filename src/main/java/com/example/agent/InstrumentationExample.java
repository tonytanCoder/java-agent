package com.example.agent;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class InstrumentationExample {


    // Java agent指定的premain方法，会在main方法之前被调用
    public static void premain(String args, Instrumentation inst) {
        // Instrumentation提供的addTransformer方法，在类加载时会回调ClassFileTransformer接口
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer)
                                    throws IllegalClassFormatException {
                if (!"com/lianshuo/settlement/customer/comm/SchedulerMasterCheck".equals(className)) {
                    // 只修改指定的Class
                    return classfileBuffer;
                }
        
                byte[] transformed = null;
                CtClass cl = null;
                try {
                    // CtClass、ClassPool、CtMethod、ExprEditor都是javassist提供的字节码操作的类
                    ClassPool pool = ClassPool.getDefault();
                    cl = pool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    CtMethod[] methods = cl.getDeclaredMethods();
                    for (int i = 0; i < methods.length; i++) {
                        methods[i].instrument(new ExprEditor() {
        
                            @Override
                            public void edit(MethodCall m) throws CannotCompileException {
                                // 把方法体直接替换掉，其中 $proceed($$);是javassist的语法，用来表示原方法体的调用
                                m.replace("{ long stime = System.currentTimeMillis();" + " $_ = $proceed($$);"
                                          + "System.out.println(\"" + m.getClassName() + "." + m.getMethodName()
                                          + " cost:\" + (System.currentTimeMillis() - stime) + \" ms\"); }");
                            }
                        });
                    }
                    // javassist会把输入的Java代码再编译成字节码byte[]
                    transformed = cl.toBytecode();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (cl != null) {
                        cl.detach();// ClassPool默认不会回收，需要手动清理
                    }                           
                }
                return transformed;
            }
        });
    }
}
