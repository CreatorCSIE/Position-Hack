package org.github.creatorcsie.positionhack.mock;

import com.sun.tools.attach.VirtualMachine;

/**
 * 探针注入器：把指定 jar 作为 agent 注入到指定 PID（用于 attach 链路排查）。
 * 用法: ProbeLoader &lt;pid&gt; &lt;agent.jar&gt;
 */
public final class ProbeLoader {

    private ProbeLoader() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: ProbeLoader <pid> <agent.jar>");
            return;
        }
        String pid = args[0];
        String jar = args[1];
        VirtualMachine vm = VirtualMachine.attach(pid);
        try {
            vm.loadAgent(jar);
            System.out.println("loadAgent returned OK for " + jar);
        } finally {
            vm.detach();
        }
    }
}