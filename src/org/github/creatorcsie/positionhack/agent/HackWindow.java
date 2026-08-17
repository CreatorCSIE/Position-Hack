package org.github.creatorcsie.positionhack.agent;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * 坐标修改器窗口（复刻 RetroWrapper 的 HackRunnable 界面）。
 * x/y/z 每行：当前坐标显示 + 输入框 + “读取坐标”按钮；底部一个“传送”按钮。
 * 这个窗口运行在 Minecraft 的 JVM 内，由注入时弹出的新线程驱动。
 */
public final class HackWindow {

    private static final NumberFormat FORMAT = NumberFormat.getNumberInstance(Locale.US);

    static {
        FORMAT.setMaximumFractionDigits(2);
    }

    private final RetroPlayer player;

    private volatile boolean uiReady = false;
    private JFrame frame;
    private JLabel xLabel;
    private JLabel yLabel;
    private JLabel zLabel;
    private JTextField xField;
    private JTextField yField;
    private JTextField zField;
    private JButton teleportButton;
    private JButton xCopyButton;
    private JButton yCopyButton;
    private JButton zCopyButton;

    public HackWindow(RetroPlayer player) {
        this.player = player;
        buildUi();
        startTicker();
    }

    private void buildUi() {
        // 使用 invokeLater 而非 invokeAndWait：即使目标 JVM 的 EDT 正忙（例如某些启动器场景），
        // 也不会阻塞 agentmain 线程导致窗口永不出现。
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    frame = new JFrame("Position Hack - 坐标修改器");
                    Dimension dim = new Dimension(560, 280);
                    frame.setPreferredSize(dim);
                    frame.setMinimumSize(dim);
                    frame.setLayout(new GridLayout(0, 3));
                    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    frame.setLocationRelativeTo(null);

                    xLabel = new JLabel("x: null");
                    xLabel.setHorizontalAlignment(JLabel.CENTER);
                    frame.add(xLabel);
                    xField = new JTextField();
                    xField.setHorizontalAlignment(JTextField.CENTER);
                    frame.add(xField);
                    xCopyButton = new JButton("读取 x");
                    frame.add(xCopyButton);

                    yLabel = new JLabel("y: null");
                    yLabel.setHorizontalAlignment(JLabel.CENTER);
                    frame.add(yLabel);
                    yField = new JTextField();
                    yField.setHorizontalAlignment(JTextField.CENTER);
                    frame.add(yField);
                    yCopyButton = new JButton("读取 y");
                    frame.add(yCopyButton);

                    zLabel = new JLabel("z: null");
                    zLabel.setHorizontalAlignment(JLabel.CENTER);
                    frame.add(zLabel);
                    zField = new JTextField();
                    zField.setHorizontalAlignment(JTextField.CENTER);
                    frame.add(zField);
                    zCopyButton = new JButton("读取 z");
                    frame.add(zCopyButton);

                    frame.add(new JLabel(""));
                    teleportButton = new JButton("未找到玩家，等待中...");
                    teleportButton.setEnabled(false);
                    frame.add(teleportButton);
                    frame.add(new JLabel(""));

                    xCopyButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            readIntoField(xField, "x");
                        }
                    });
                    yCopyButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            readIntoField(yField, "y");
                        }
                    });
                    zCopyButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            readIntoField(zField, "z");
                        }
                    });
                    teleportButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            doTeleport();
                        }
                    });

                    frame.pack();
                    frame.setVisible(true);
                } catch (Exception e) {
                    AgentMain.log("hack window build failed: " + e);
                } finally {
                    uiReady = true;
                }
            }
        });
    }

    private void readIntoField(JTextField field, String axis) {
        try {
            double value = "x".equals(axis) ? player.getX() : "y".equals(axis) ? player.getY() : player.getZ();
            field.setText(FORMAT.format(value));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "读取坐标失败：" + e, "Position Hack", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doTeleport() {
        try {
            double dx = Double.parseDouble(xField.getText().trim());
            double dy = Double.parseDouble(yField.getText().trim());
            double dz = Double.parseDouble(zField.getText().trim());
            player.teleport(dx, dy, dz);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(frame, "坐标格式不正确，请填写数字（如 100.5）", "Position Hack", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "传送失败：" + e, "Position Hack", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startTicker() {
        Thread ticker = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        if (!uiReady) {
                            Thread.sleep(100L);
                            continue;
                        }
                        Object aabb = player.getAABBOrNull();
                        final boolean hasPlayer = aabb != null;
                        if (hasPlayer) {
                            if (player.getAabb() != aabb) {
                                player.setAabb(aabb);
                            }
                            final double x = player.getX();
                            final double y = player.getY();
                            final double z = player.getZ();
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    xLabel.setText("x: " + FORMAT.format(x));
                                    yLabel.setText("y: " + FORMAT.format(y));
                                    zLabel.setText("z: " + FORMAT.format(z));
                                }
                            });
                        } else {
                            player.setAabb(null);
                        }
                        // 每帧都同步按钮状态，避免首次触发条件漏掉导致按钮永远禁用
                        setActive(hasPlayer);
                        Thread.sleep(100L);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException ignored) {
                            return;
                        }
                    }
                }
            }
        }, "position-hack-ticker");
        ticker.setDaemon(true);
        ticker.start();
    }

    private void setActive(boolean active) {
        final boolean activeFlag = active;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (teleportButton == null || xCopyButton == null || yCopyButton == null || zCopyButton == null) {
                    return;
                }
                teleportButton.setEnabled(activeFlag);
                teleportButton.setText(activeFlag ? "传送" : "未找到玩家，等待中...");
                xCopyButton.setEnabled(activeFlag);
                yCopyButton.setEnabled(activeFlag);
                zCopyButton.setEnabled(activeFlag);
            }
        });
    }
}