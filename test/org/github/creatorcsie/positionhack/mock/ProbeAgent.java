package org.github.creatorcsie.positionhack.mock;

import java.io.FileOutputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;

/**
 * 极简探针 agent：只写日志文件 + 打印 System.out，用于确认
 * "attach -&gt; loadAgent -&gt; agentmain 执行"链路在目标 JVM 上是否真正生效。
 */
public final class ProbeAgent {

    private ProbeAgent() {
    }

    public static void agentmain(String args, Instrumentation inst) {
        String msg = "PROBE agentmain ran! pid=" + ManagementFactory.getRuntimeMXBean().getName()
                + " loader=" + ProbeAgent.class.getClassLoader()
                + " inst=" + inst + "\n";
        System.out.println(msg);
        String userHome = System.getProperty("user.home");
        String tmpDir = System.getProperty("java.io.tmpdir");
        try {
            FileOutputStream fos = new FileOutputStream("C:/Users/GAME/probe-agent.log", true);
            fos.write(msg.getBytes("UTF-8"));
            fos.close();
        } catch (Throwable t) {
            System.err.println("probe write userHome failed: " + t);
        }
        try {
            FileOutputStream fos = new FileOutputStream(tmpDir + "/probe-agent.log", true);
            fos.write(msg.getBytes("UTF-8"));
            fos.close();
        } catch (Throwable t) {
            System.err.println("probe write tmp failed: " + t);
        }
    }
}