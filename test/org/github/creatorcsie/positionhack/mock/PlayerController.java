package org.github.creatorcsie.positionhack.mock;

/**
 * 模拟 PlayerController：类名含 "Player" 但继承链上没有 Entity 基类，不是实体。
 * 用于验证实体识别不会被名字误导。
 */
public class PlayerController {

    protected final Object a;
    public boolean b = false;

    public PlayerController(Object mc) {
        this.a = mc;
    }
}