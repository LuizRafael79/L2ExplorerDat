/*
 * This file is part of the L2ExplorerDat project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maintened by Galagard
 */
package org.l2explorer;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class XmlUpdaterDialog extends JDialog
{
    private static final String REPO_OWNER = "LuizRafael79";
    private static final String REPO_NAME  = "L2ExplorerDat";
    private static final String RAW_BASE   = "https://raw.githubusercontent.com/" + REPO_OWNER + "/" + REPO_NAME + "/main/";
    private static final String API_BASE   = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/commits?per_page=1&path=";

    private static final String[] LOCAL_DIRS = {
            "data/structure",
            "data/structure/dats"
    };

    private static final Color PRIMARY_BG    = new Color(41, 49, 52);
    private static final Color SECONDARY_BG  = new Color(30, 36, 40);
    private static final Color ACCENT_COLOR  = new Color(0, 173, 181);
    private static final Color CLOSE_COLOR   = new Color(71, 85, 105);
    private static final Color CLOSE_HOVER   = new Color(100, 116, 139);
    private static final Color SUCCESS_COLOR = new Color(34, 197, 94);
    private static final Color SUCCESS_HOVER = new Color(22, 163, 74);
    private static final Color WARN_COLOR    = new Color(234, 179, 8);
    private static final Color TEXT_PRIMARY  = new Color(237, 242, 247);
    private static final Color TEXT_SECONDARY= new Color(160, 174, 192);

    private JLabel statusLabel;
    private JProgressBar progressBar;
    private DefaultTableModel tableModel;
    private JButton downloadSelectedBtn;
    private JButton downloadAllBtn;

    private final List<XmlFileInfo> outdatedFiles = new ArrayList<>();

    // -------------------------------------------------------------------------
    public XmlUpdaterDialog(JFrame parent)
    {
        super(parent, "XML Data Updater", true);
        initComponents();
        scanAndCheck();
    }

    // -------------------------------------------------------------------------
    // UI
    // -------------------------------------------------------------------------
    private void initComponents()
    {
        setUndecorated(true);
        setShape(new RoundRectangle2D.Double(0, 0, 680, 520, 20, 20));
        setSize(680, 520);
        setLayout(new BorderLayout());
        setLocationRelativeTo(getOwner());

        JPanel main = buildMainPanel();

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        topBar.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
        topBar.add(createCloseButton(), BorderLayout.EAST);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel icon = new JLabel("📦");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("XML Data Updater");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Checks structure & dats folders against the repository");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(TEXT_SECONDARY);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(icon);
        header.add(Box.createRigidArea(new Dimension(0, 6)));
        header.add(title);
        header.add(Box.createRigidArea(new Dimension(0, 4)));
        header.add(sub);

        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setOpaque(false);

        statusLabel = new JLabel("Scanning local files...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(TEXT_SECONDARY);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        progressBar = new JProgressBar();
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        progressBar.setForeground(ACCENT_COLOR);
        progressBar.setBackground(SECONDARY_BG);
        progressBar.setBorderPainted(false);
        progressBar.setStringPainted(false);

        statusPanel.add(statusLabel);
        statusPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        statusPanel.add(progressBar);

        String[] cols = { "File", "Folder", "Local Modified", "Remote Commit", "Status" };
        tableModel = new DefaultTableModel(cols, 0)
        {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        table.setBackground(SECONDARY_BG);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(26);
        table.setGridColor(new Color(55, 65, 81));
        table.getTableHeader().setBackground(new Color(30, 36, 40));
        table.getTableHeader().setForeground(TEXT_SECONDARY);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionBackground(new Color(0, 173, 181, 60));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setShowVerticalLines(false);

        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer()
        {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col)
            {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String v = val == null ? "" : val.toString();
                if (v.equals("Outdated"))        setForeground(WARN_COLOR);
                else if (v.equals("Up to date")) setForeground(SUCCESS_COLOR);
                else                             setForeground(TEXT_SECONDARY);
                setBackground(sel ? new Color(0, 173, 181, 60) : SECONDARY_BG);
                setHorizontalAlignment(CENTER);
                return this;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(SECONDARY_BG);
        scroll.getViewport().setBackground(SECONDARY_BG);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(71, 85, 105)));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setOpaque(false);

        downloadSelectedBtn = createModernButton("Download Selected", ACCENT_COLOR, ACCENT_COLOR.darker());
        downloadSelectedBtn.setEnabled(false);
        downloadSelectedBtn.addActionListener(_ -> downloadFiles(getSelectedFiles(table)));

        downloadAllBtn = createModernButton("Download All Outdated", SUCCESS_COLOR, SUCCESS_HOVER);
        downloadAllBtn.setEnabled(false);
        downloadAllBtn.addActionListener(_ -> downloadFiles(outdatedFiles));

        JButton closeBtn = createModernButton("Close", CLOSE_COLOR, CLOSE_HOVER);
        closeBtn.addActionListener(_ -> dispose());

        buttons.add(Box.createHorizontalGlue());
        buttons.add(closeBtn);
        buttons.add(Box.createRigidArea(new Dimension(10, 0)));
        buttons.add(downloadSelectedBtn);
        buttons.add(Box.createRigidArea(new Dimension(10, 0)));
        buttons.add(downloadAllBtn);
        buttons.add(Box.createHorizontalGlue());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                downloadSelectedBtn.setEnabled(table.getSelectedRowCount() > 0);
        });

        main.add(topBar);
        main.add(Box.createRigidArea(new Dimension(0, 8)));
        main.add(header);
        main.add(Box.createRigidArea(new Dimension(0, 16)));
        main.add(statusPanel);
        main.add(Box.createRigidArea(new Dimension(0, 12)));
        main.add(scroll);
        main.add(Box.createRigidArea(new Dimension(0, 16)));
        main.add(buttons);

        add(main, BorderLayout.CENTER);
    }

    private JPanel buildMainPanel()
    {
        JPanel p = new JPanel()
        {
            @Override protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setPaint(new GradientPaint(0, 0, PRIMARY_BG, 0, getHeight(), SECONDARY_BG));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(15, 28, 28, 28));

        final Point offset = new Point();
        p.addMouseListener(new MouseAdapter()
        {
            @Override public void mousePressed(MouseEvent e) { offset.x = e.getX(); offset.y = e.getY(); }
        });
        p.addMouseMotionListener(new MouseAdapter()
        {
            @Override public void mouseDragged(MouseEvent e) { setLocation(e.getXOnScreen() - offset.x, e.getYOnScreen() - offset.y); }
        });
        return p;
    }

    private JButton createCloseButton()
    {
        JButton btn = new JButton()
        {
            @Override protected void paintComponent(Graphics g)
            {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(220, 38, 38) : new Color(100, 116, 139, 100));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                int sw = g2.getFontMetrics().stringWidth("✕");
                int sh = g2.getFontMetrics().getAscent();
                g2.drawString("✕", (getWidth() - sw) / 2, (getHeight() + sh) / 2 - 2);
            }
        };
        btn.setPreferredSize(new Dimension(36, 36));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(_ -> dispose());
        return btn;
    }

    private JButton createModernButton(String text, Color bg, Color hover)
    {
        JButton btn = new JButton(text)
        {
            @Override protected void paintComponent(Graphics g)
            {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = !getModel().isEnabled() ? new Color(71, 85, 105)
                        : getModel().isPressed() ? hover.darker()
                        : getModel().isRollover() ? hover : bg;
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(getModel().isEnabled() ? Color.WHITE : new Color(160, 174, 192));
                g2.setFont(getFont());
                int sw = g2.getFontMetrics().stringWidth(getText());
                int sh = g2.getFontMetrics().getAscent();
                g2.drawString(getText(), (getWidth() - sw) / 2, (getHeight() + sh) / 2 - 2);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(175, 38));
        btn.setMaximumSize(new Dimension(175, 38));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------
    private void scanAndCheck()
    {
        new SwingWorker<Void, Object[]>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                List<Path> xmlFiles = new ArrayList<>();

                for (String dir : LOCAL_DIRS)
                {
                    Path folder = Paths.get(dir);
                    if (!Files.exists(folder)) continue;

                    try (var stream = Files.walk(folder, 1))
                    {
                        stream.filter(p -> p.toString().endsWith(".xml"))
                                .forEach(xmlFiles::add);
                    }
                }

                int total = xmlFiles.size();
                if (total == 0)
                {
                    publish(new Object[]{ "status", "No XML files found in local folders.", false });
                    return null;
                }

                publish(new Object[]{ "status", "Checking " + total + " files against repository...", false });
                progressBar.setMaximum(total);

                AtomicInteger done = new AtomicInteger(0);

                for (Path localFile : xmlFiles)
                {
                    String repoPath = localFile.toString().replace("\\", "/");
                    String folder   = localFile.getParent().getFileName().toString();
                    String fileName = localFile.getFileName().toString();

                    try
                    {
                        Instant localModified  = Files.getLastModifiedTime(localFile).toInstant();
                        Instant remoteCommitAt = fetchLastCommitDateStatic(repoPath);

                        String localStr  = localModified.toString().substring(0, 10);
                        String remoteStr = remoteCommitAt != null ? remoteCommitAt.toString().substring(0, 10) : "unknown";

                        boolean outdated = remoteCommitAt != null && remoteCommitAt.isAfter(localModified);
                        String status    = outdated ? "Outdated" : "Up to date";

                        XmlFileInfo info = new XmlFileInfo(fileName, folder, repoPath, localStr, remoteStr);
                        publish(new Object[]{ outdated ? "row" : "row_ok", info, status });
                    }
                    catch (Exception e)
                    {
                        publish(new Object[]{ "row_err", new XmlFileInfo(fileName, folder, repoPath, "?", "error"), "Error" });
                    }

                    int d = done.incrementAndGet();
                    publish(new Object[]{ "progress", d, null });
                }

                return null;
            }

            @Override
            protected void process(List<Object[]> chunks)
            {
                for (Object[] chunk : chunks)
                {
                    String type = (String) chunk[0];
                    switch (type)
                    {
                        case "status" -> statusLabel.setText((String) chunk[1]);
                        case "progress" -> progressBar.setValue((int) chunk[1]);
                        case "row", "row_ok", "row_err" ->
                        {
                            XmlFileInfo info = (XmlFileInfo) chunk[1];
                            String status    = (String) chunk[2];
                            tableModel.addRow(new Object[]{ info.fileName, info.folder, info.localDate, info.remoteDate, status });
                            if (type.equals("row"))
                                outdatedFiles.add(info);
                        }
                    }
                }
            }

            @Override
            protected void done()
            {
                progressBar.setValue(progressBar.getMaximum());

                if (outdatedFiles.isEmpty())
                {
                    statusLabel.setText("✓ All XML files are up to date!");
                    statusLabel.setForeground(SUCCESS_COLOR);
                }
                else
                {
                    statusLabel.setText("⚠ " + outdatedFiles.size() + " file(s) outdated — select rows or download all");
                    statusLabel.setForeground(WARN_COLOR);
                    downloadAllBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void downloadFiles(List<XmlFileInfo> files)
    {
        if (files == null || files.isEmpty()) return;

        List<XmlFileInfo> toDownload = new ArrayList<>(files);
        downloadAllBtn.setEnabled(false);
        downloadSelectedBtn.setEnabled(false);
        statusLabel.setForeground(ACCENT_COLOR);

        new SwingWorker<Void, String>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                for (XmlFileInfo info : toDownload)
                {
                    publish("Downloading " + info.fileName + "...");
                    try
                    {
                        String rawUrl = RAW_BASE + info.repoPath;
                        URL url = URI.create(rawUrl).toURL();
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestProperty("User-Agent", REPO_NAME);
                        conn.setConnectTimeout(8000);
                        conn.setReadTimeout(8000);

                        Path dest = Paths.get(info.repoPath);
                        Files.createDirectories(dest.getParent());

                        try (InputStream in = conn.getInputStream())
                        {
                            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                        L2ExplorerDat.addLogConsole("Downloaded: " + info.repoPath, true);
                    }
                    catch (Exception e)
                    {
                        L2ExplorerDat.addLogConsole("Failed to download " + info.fileName + ": " + e.getMessage(), false);
                    }
                }
                return null;
            }

            @Override
            protected void process(List<String> msgs)
            {
                if (!msgs.isEmpty()) statusLabel.setText(msgs.get(msgs.size() - 1));
            }

            @Override
            protected void done()
            {
                statusLabel.setText("✓ Download complete! Restart may be needed for changes to take effect.");
                statusLabel.setForeground(SUCCESS_COLOR);
                outdatedFiles.removeAll(toDownload);
                downloadAllBtn.setEnabled(!outdatedFiles.isEmpty());
            }
        }.execute();
    }

    private List<XmlFileInfo> getSelectedFiles(JTable table)
    {
        List<XmlFileInfo> selected = new ArrayList<>();
        for (int row : table.getSelectedRows())
        {
            String fileName = (String) tableModel.getValueAt(row, 0);
            outdatedFiles.stream()
                    .filter(f -> f.fileName.equals(fileName))
                    .findFirst()
                    .ifPresent(selected::add);
        }
        return selected;
    }

    // -------------------------------------------------------------------------
    // Shared fetch (static — used by both dialog and background checker)
    // -------------------------------------------------------------------------
    private static Instant fetchLastCommitDateStatic(String repoPath) throws Exception
    {
        URL url = URI.create(API_BASE + repoPath).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", REPO_NAME);
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(6000);

        if (conn.getResponseCode() != 200)
            throw new RuntimeException("HTTP " + conn.getResponseCode());

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();

        Pattern p = Pattern.compile("\"commit\"\\s*:\\s*\\{[^}]*\"committer\"\\s*:\\s*\\{[^}]*\"date\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(sb.toString());
        if (m.find())
            return Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(m.group(1)));

        return null;
    }

    // -------------------------------------------------------------------------
    // Model
    // -------------------------------------------------------------------------
    private static class XmlFileInfo
    {
        final String fileName;
        final String folder;
        final String repoPath;
        final String localDate;
        final String remoteDate;

        XmlFileInfo(String fileName, String folder, String repoPath, String localDate, String remoteDate)
        {
            this.fileName   = fileName;
            this.folder     = folder;
            this.repoPath   = repoPath;
            this.localDate  = localDate;
            this.remoteDate = remoteDate;
        }
    }

    // -------------------------------------------------------------------------
    // Entry points
    // -------------------------------------------------------------------------

    /**
     * Abre o dialog normalmente (chamado pelo botão na sidebar quando parsers disponíveis).
     */
    public static void showDialog(JFrame parent)
    {
        SwingUtilities.invokeLater(() -> new XmlUpdaterDialog(parent).setVisible(true));
    }

    /**
     * Roda em background no startup, sem abrir o dialog.
     * Se encontrar XMLs desatualizados, chama mainApp.notifyParsersAvailable(count).
     */
    public static void checkInBackground(L2ExplorerDat mainApp)
    {
        new SwingWorker<Integer, Void>()
        {
            @Override
            protected Integer doInBackground()
            {
                int outdatedCount = 0;
                for (String dir : LOCAL_DIRS)
                {
                    Path folder = Paths.get(dir);
                    if (!Files.exists(folder)) continue;

                    try (var stream = Files.walk(folder, 1))
                    {
                        for (Path localFile : stream.filter(p -> p.toString().endsWith(".xml")).toList())
                        {
                            try
                            {
                                Instant localModified  = Files.getLastModifiedTime(localFile).toInstant();
                                Instant remoteCommitAt = fetchLastCommitDateStatic(localFile.toString().replace("\\", "/"));
                                if (remoteCommitAt != null && remoteCommitAt.isAfter(localModified))
                                    outdatedCount++;
                            }
                            catch (Exception ignored) { /* silencioso no background */ }
                        }
                    }
                    catch (Exception ignored) { }
                }
                return outdatedCount;
            }

            @Override
            protected void done()
            {
                try
                {
                    int count = get();
                    if (count > 0)
                        mainApp.notifyParsersAvailable(count);
                }
                catch (Exception ignored) { }
            }
        }.execute();
    }
}