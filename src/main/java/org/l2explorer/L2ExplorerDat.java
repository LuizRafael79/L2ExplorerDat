/*
 * This file is part of the L2ExplorerDat project.
 * * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Maintened by Galagard
 */

package org.l2explorer;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import org.l2explorer.actions.*;
import org.l2explorer.clientcryptor.crypt.DatCrypter;
import org.l2explorer.config.ConfigDebug;
import org.l2explorer.config.ConfigWindow;
import org.l2explorer.forms.JPopupTextArea;
import org.l2explorer.util.I18nManager;
import org.l2explorer.util.Util;
import org.l2explorer.xml.CryptVersionParser;
import org.l2explorer.xml.DescriptorParser;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.desktop.AboutEvent;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class L2ExplorerDat extends JFrame
{
    private static final Logger LOGGER = Logger.getLogger(L2ExplorerDat.class.getName());

    private static final Color ACCENT_COLOR = new Color(99, 102, 241);
    private static final Color ACCENT_HOVER = new Color(129, 140, 248);
    private static final Color SUCCESS_COLOR = new Color(16, 185, 129);
    private static final Color DANGER_COLOR = new Color(239, 68, 68);

    public static boolean DEV_MODE = false;

    private static JTextPane _textPaneLog;
    private final ExecutorService _executorService = Executors.newCachedThreadPool();
    private final JPopupTextArea _textPaneMain;
    private final LineNumberingTextArea _lineNumberingTextArea;
    private final JComboBox<String> _jComboBoxChronicle;
    private final JComboBox<String> _jComboBoxEncrypt;
    private final JComboBox<String> _jComboBoxFormatter;
    private final JComboBox<String> _jComboBoxLanguage;
    private final ArrayList<JPanel> _actionPanels = new ArrayList<>();
    private final JButton _openDatButton;
    private final JButton _saveTxtButton;
    private final JButton _saveDatButton;
    private final JButton _abortTaskButton;
    private final JButton _massUnpackButton;
    private final JButton _massPackButton;
    private final JButton _massRecryptButton;
    private final JButton _updateNotifyButton;
    private JLabel _lblStructure;
    private JLabel _lblEncrypt;
    private JLabel _lblFormatter;
    private JLabel _lblLanguage;
    private JLabel _subtitleLabel;
    private final JButton _themeToggleButton;
    private final JButton _aboutButton;
    private final ModernProgressBar _progressBar;

    private File _currentFileWindow = null;
    private ActionTask _progressTask = null;

    private enum NotifyMode { APP_UPDATE, PARSERS_AVAILABLE }
    private volatile NotifyMode _currentNotifyMode = null;
    private int _pendingParserCount = 0;

    private boolean isDarkTheme;

    // Helper method para i18n
    private String i18n(String key)
    {
        return I18nManager.getInstance().getString(key);
    }

    // Helper method para i18n com parâmetros
    private String i18n(String key, Object... params)
    {
        return I18nManager.getInstance().getString(key, params);
    }

    public static void main(String[] args)
    {
        ConfigWindow.load();
        ConfigDebug.load();

        String savedLanguage = ConfigWindow.CURRENT_LANGUAGE;

        I18nManager i18n = I18nManager.getInstance();

        if (savedLanguage == null || savedLanguage.isEmpty()) {
            i18n.initialize(Locale.getDefault());
        } else {
            Locale initialLocale = getLocaleFromLanguageName(savedLanguage);
            i18n.initialize(initialLocale);
        }

        EventQueue.invokeLater(() ->
        {
            boolean startDark = "dark".equalsIgnoreCase(ConfigWindow.THEME_PREFERENCE);

            try
            {
                UIManager.setLookAndFeel(startDark ? new FlatMacDarkLaf() : new FlatMacLightLaf());
            }
            catch (Exception ex)
            {
                LOGGER.log(Level.SEVERE, "Failed to initialize FlatLaf. Falling back to default.", ex);
                startDark = false;
                try
                {
                    UIManager.setLookAndFeel(new FlatMacLightLaf());
                }
                catch (Exception ignored)
                {
                }
            }

            final ModernSplashWindow splash = new ModernSplashWindow();

            final AtomicBoolean loadingFinished = new AtomicBoolean(false);
            final AtomicBoolean animationFinished = new AtomicBoolean(false);
            final AtomicReference<L2ExplorerDat> mainAppRef = new AtomicReference<>(null);
            final boolean finalStartDark = startDark;

            final Runnable tryFinishSplash = () ->
            {
                if (animationFinished.get() && loadingFinished.get())
                {
                    L2ExplorerDat mainApp = mainAppRef.get();
                    if (mainApp != null)
                    {
                        splash.dispose();
                        mainApp.setVisible(true);
                        mainApp.toFront();
                    }
                }
            };

            class LoadingTask extends SwingWorker<L2ExplorerDat, Void>
            {
                @SuppressWarnings("ResultOfMethodCallIgnored")
                @Override
                protected L2ExplorerDat doInBackground()
                {
                    final File logFolder = new File(".", ".idea/log");
                    logFolder.mkdir();
                    try (InputStream is = new FileInputStream("config/log.cfg"))
                    {
                        LogManager.getLogManager().readConfiguration(is);
                    }
                    catch (Exception e)
                    {
                        LOGGER.log(Level.SEVERE, null, e);
                    }
                    DEV_MODE = Util.contains(args, (Object) "-dev");

                    CryptVersionParser.getInstance().parse();
                    DescriptorParser.getInstance().parse();

                    return new L2ExplorerDat(finalStartDark);
                }

                @Override
                protected void done()
                {
                    try
                    {
                        mainAppRef.set(get());
                    }
                    catch (Exception e)
                    {
                        LOGGER.log(Level.SEVERE, "Critical failure at creating main window", e);
                        System.exit(1);
                    }

                    loadingFinished.set(true);
                    tryFinishSplash.run();
                }
            }

            final int ANIMATION_MS = 1000;
            final int MIN_WAIT_MS = 1500;
            final int REFRESH_MS = 15;
            final AtomicInteger elapsed = new AtomicInteger(0);

            splash.setVisible(true);

            final Timer animationTimer = new Timer(REFRESH_MS, null);
            animationTimer.addActionListener(e ->
            {
                int time = elapsed.addAndGet(REFRESH_MS);
                double animProgress = Math.min(1.0, (double) time / ANIMATION_MS);
                double easedProgress = 1 - Math.pow(1 - animProgress, 3);
                splash.setAnimationProgress(easedProgress);

                if ((time >= MIN_WAIT_MS) && loadingFinished.get())
                {
                    animationTimer.stop();
                    animationFinished.set(true);
                    tryFinishSplash.run();
                }
            });

            animationTimer.start();
            new LoadingTask().execute();
        });
    }


    /**
     * Converte o nome salvo do idioma para Locale.
     * Usa o I18nManager para encontrar automaticamente.
     */
    private static Locale getLocaleFromLanguageName(String languageName)
    {
        if (languageName == null)
        {
            return Locale.ENGLISH;
        }

        // Tenta encontrar o locale pelo nome de display
        Locale locale = I18nManager.getInstance().getLocaleByDisplayName(languageName);

        if (locale != null)
        {
            return locale;
        }

        // Fallback: tenta algumas conversões antigas para retrocompatibilidade
        return switch (languageName.toLowerCase()) {
            case "portuguese", "português" -> Locale.forLanguageTag("pt-BR");
            case "spanish", "español" -> Locale.forLanguageTag("es-ES");
            case "russian", "русский" -> Locale.forLanguageTag("ru-RU");
            case "greek", "ελληνικά" -> Locale.forLanguageTag("el-GR");
            case "chinese (hant)", "中文 (繁體)" -> Locale.forLanguageTag("zh-Hant");
            case "chinese (hans)", "中文 (简体)" -> Locale.forLanguageTag("zh-Hans");
            case "japanese", "日本語" -> Locale.forLanguageTag("ja-JP");
            default -> Locale.forLanguageTag("en-US");
        };
    }

    public L2ExplorerDat(boolean startDark)
    {
        this.isDarkTheme = startDark;

        _jComboBoxChronicle = new JComboBox<>();
        _jComboBoxEncrypt = new JComboBox<>();
        _jComboBoxFormatter = new JComboBox<>();
        _jComboBoxLanguage = new JComboBox<>();
        _openDatButton = createModernButton(i18n("open.btn"), ACCENT_COLOR);
        _saveTxtButton = createModernButton(i18n("save.txt.btn"), SUCCESS_COLOR);
        _saveDatButton = createModernButton(i18n("save.dat.btn"), SUCCESS_COLOR);
        _massUnpackButton = createModernButton(i18n("decrypt.all.btn"), ACCENT_COLOR);
        _massPackButton = createModernButton(i18n("encrypt.all.btn"), ACCENT_COLOR);
        _massRecryptButton = createModernButton(i18n("patch.all.btn"), ACCENT_COLOR);
        _updateNotifyButton = createModernButton(i18n("update.notify.btn"), SUCCESS_COLOR);
        _progressBar = new ModernProgressBar();
        _abortTaskButton = createModernButton(i18n("abort.btn"), DANGER_COLOR);
        _themeToggleButton = createModernButton(i18n("toggle.theme"), new Color(100, 116, 139));
        _aboutButton = createModernButton(i18n("about.btn"), new Color(100, 116, 139));
        _textPaneMain = new JPopupTextArea();
        _lineNumberingTextArea = new LineNumberingTextArea(_textPaneMain);
        _textPaneLog = new JTextPane();

        initComponents();
        setupFrame();
        UpdaterDialog.checkInBackground(this);
        XmlUpdaterDialog.checkInBackground(this);

    }

    public L2ExplorerDat()
    {
        this(false);
    }

    private void initComponents()
    {
        getContentPane().setLayout(new BorderLayout(0, 0));
        getContentPane().add(createSidebarPanel(), BorderLayout.WEST);
        getContentPane().add(createMainPanel(), BorderLayout.CENTER);
    }

    private JPanel createSidebarPanel() {
        JPanel sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        sidebarPanel.setPreferredSize(new Dimension(220, 0));

        JLabel titleLabel = new JLabel("L2ExplorerDat");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        _subtitleLabel = new JLabel(i18n("subtitle"));
        _subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        _subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebarPanel.add(titleLabel);
        sidebarPanel.add(_subtitleLabel);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        sidebarPanel.add(new JSeparator());
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel actionsButtonPanel = new JPanel();
        actionsButtonPanel.setLayout(new BoxLayout(actionsButtonPanel, BoxLayout.Y_AXIS));
        actionsButtonPanel.setOpaque(false);

        _openDatButton.setToolTipText(i18n("open.btn.tooltip"));
        _openDatButton.addActionListener(this::openSelectFileWindow);

        _saveTxtButton.addActionListener(this::saveTxtActionPerformed);
        _saveTxtButton.setToolTipText(i18n("save.txt.btn.tooltip.disabled"));
        _saveTxtButton.setEnabled(false);

        _saveDatButton.addActionListener(this::saveDatActionPerformed);
        _saveDatButton.setToolTipText(i18n("save.dat.btn.tooltip.disabled"));
        _saveDatButton.setEnabled(false);

        _massUnpackButton.setToolTipText(i18n("decrypt.all.btn.tooltip"));
        _massUnpackButton.addActionListener(this::massTxtUnpackActionPerformed);

        _massPackButton.setToolTipText(i18n("pack.all.btn.tooltip"));
        _massPackButton.addActionListener(this::massTxtPackActionPerformed);

        _massRecryptButton.setToolTipText(i18n("patch.all.btn.tooltip"));
        _massRecryptButton.addActionListener(this::massRecryptActionPerformed);

        _updateNotifyButton.setToolTipText(i18n("notify.btn.tooltip"));
        _updateNotifyButton.setVisible(false);
        _updateNotifyButton.addActionListener(e -> UpdaterDialog.showUpdateDialog(this));

        actionsButtonPanel.add(_massRecryptButton);
        int spacing = 0;
        actionsButtonPanel.add(Box.createRigidArea(new Dimension(0, spacing)));
        actionsButtonPanel.add(_updateNotifyButton); // Ele entra na fila aqui

        spacing = 8;

        actionsButtonPanel.add(_openDatButton);
        actionsButtonPanel.add(Box.createRigidArea(new Dimension(0, spacing)));
        actionsButtonPanel.add(_saveTxtButton);
        actionsButtonPanel.add(Box.createRigidArea(new Dimension(0, spacing)));
        actionsButtonPanel.add(_saveDatButton);
        actionsButtonPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        actionsButtonPanel.add(new JSeparator());
        actionsButtonPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        actionsButtonPanel.add(_massUnpackButton);
        actionsButtonPanel.add(Box.createRigidArea(new Dimension(0, spacing)));
        actionsButtonPanel.add(_massPackButton);
        actionsButtonPanel.add(Box.createRigidArea(new Dimension(0, spacing)));
        actionsButtonPanel.add(_massRecryptButton);
        actionsButtonPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        actionsButtonPanel.add(new JSeparator());
        actionsButtonPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        actionsButtonPanel.add(_updateNotifyButton);

        sidebarPanel.add(actionsButtonPanel);
        sidebarPanel.add(Box.createVerticalGlue());
        sidebarPanel.add(new JSeparator());
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        _themeToggleButton.setToolTipText(i18n("toggle.theme.tooltip"));
        _themeToggleButton.addActionListener(e -> toggleTheme());
        sidebarPanel.add(_themeToggleButton);

        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        _aboutButton.setToolTipText(i18n("about.btn.tooltip"));
        _aboutButton.addActionListener(this::aboutL2ExplorerDat);
        sidebarPanel.add(_aboutButton);

        _actionPanels.add(actionsButtonPanel);

        return sidebarPanel;
    }

    /**
     * Método chamado pelo UpdaterDialog quando uma nova versão é detectada.
     */
    public void notifyUpdateAvailable()
    {
        SwingUtilities.invokeLater(() ->
        {
            _currentNotifyMode = NotifyMode.APP_UPDATE; // sempre sobrescreve
            refreshNotifyButton();
            addLogConsole("✨ " + i18n("log.update.available.sidebar"), false);
        });
    }

    public void notifyParsersAvailable(int count)
    {
        SwingUtilities.invokeLater(() ->
        {
            // Se já tem app update pendente, ignora completamente —
            // o app novo já vai vir com os parsers atualizados
            if (_currentNotifyMode == NotifyMode.APP_UPDATE) return;

            _currentNotifyMode = NotifyMode.PARSERS_AVAILABLE;
            _pendingParserCount = count;
            refreshNotifyButton();
            addLogConsole("📦 " + count + " parser(s) available for update.", false);
        });
    }

    private void refreshNotifyButton()
    {
        // Remove listeners anteriores para evitar duplicação
        for (ActionListener al : _updateNotifyButton.getActionListeners())
            _updateNotifyButton.removeActionListener(al);

        if (_currentNotifyMode == NotifyMode.APP_UPDATE)
        {
            _updateNotifyButton.setText(i18n("update.notify.btn"));
            _updateNotifyButton.addActionListener(e -> UpdaterDialog.showUpdateDialog(this));
        }
        else if (_currentNotifyMode == NotifyMode.PARSERS_AVAILABLE)
        {
            _updateNotifyButton.setText("Parsers Available (" + _pendingParserCount + ")");
            _updateNotifyButton.addActionListener(e -> XmlUpdaterDialog.showDialog(this));
        }

        _updateNotifyButton.setVisible(_currentNotifyMode != null);
        _updateNotifyButton.getParent().revalidate();
        _updateNotifyButton.getParent().repaint();
    }

    private JButton createModernButton(String text, Color color)
    {
        JButton button = new JButton(text)
        {
            private boolean isHovered = false;

            @Override
            protected void paintComponent(Graphics g)
            {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bgColor = isEnabled() ? (isHovered ? brighten(color) : color) : new Color(71, 85, 105);

                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                g2d.setColor(isEnabled() ? Color.WHITE : new Color(160, 174, 192));
                g2d.setFont(getFont());

                FontMetrics fm = g2d.getFontMetrics();
                int stringWidth = fm.stringWidth(getText());
                int stringHeight = fm.getAscent();

                g2d.drawString(getText(), (getWidth() - stringWidth) / 2, ((getHeight() + stringHeight) / 2) - 2);
            }

            {
                addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseEntered(MouseEvent e)
                    {
                        if (isEnabled())
                        {
                            isHovered = true;
                            repaint();
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent e)
                    {
                        isHovered = false;
                        repaint();
                    }
                });
            }
        };

        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setPreferredSize(new Dimension(190, 38));
        button.setMaximumSize(new Dimension(190, 38));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    private Color brighten(Color color)
    {
        return new Color(
                Math.min(255, Math.round(color.getRed() * 1.2f)),
                Math.min(255, Math.round(color.getGreen() * 1.2f)),
                Math.min(255, Math.round(color.getBlue() * 1.2f))
        );
    }

    private JPanel createMainPanel()
    {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel settingsPanel = createSettingsPanel();
        JSplitPane editorSplitPane = createEditorSplitPane();
        JPanel progressPanel = createProgressPanel();

        mainPanel.add(settingsPanel, BorderLayout.NORTH);
        mainPanel.add(editorSplitPane, BorderLayout.CENTER);
        mainPanel.add(progressPanel, BorderLayout.SOUTH);

        _actionPanels.add(settingsPanel);

        return mainPanel;
    }

    private JPanel createSettingsPanel()
    {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 12));
        settingsPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1, true), BorderFactory.createEmptyBorder(7, 10, 7, 10))));

        _lblStructure = new JLabel(i18n("dat.structure"));
        _lblStructure.setFont(new Font("Segoe UI", Font.BOLD, 12));
        settingsPanel.add(_lblStructure);

        final String[] chronicles = DescriptorParser.getInstance().getChronicleNames().toArray(new String[0]);
        _jComboBoxChronicle.setToolTipText(i18n("chronicle.tooltip"));
        _jComboBoxChronicle.setModel(new DefaultComboBoxModel<>(chronicles));
        _jComboBoxChronicle.setSelectedItem(Util.contains(chronicles, (Object) ConfigWindow.CURRENT_CHRONICLE) ? ConfigWindow.CURRENT_CHRONICLE : chronicles[chronicles.length - 1]);
        _jComboBoxChronicle.addActionListener(e -> saveComboBox(_jComboBoxChronicle, "CURRENT_CHRONICLE"));
        settingsPanel.add(_jComboBoxChronicle);

        _lblEncrypt = new JLabel(i18n("encrypt.label"));
        _lblEncrypt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        settingsPanel.add(_lblEncrypt);

        DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>(CryptVersionParser.getInstance().getEncryptKey().keySet().toArray(new String[0]));
        // IMPORTANTE: "Source" é um valor de SISTEMA, não traduzir!
        comboBoxModel.insertElementAt("Source", 0);
        _jComboBoxEncrypt.setToolTipText(i18n("encrypt.tooltip"));
        _jComboBoxEncrypt.setModel(comboBoxModel);
        _jComboBoxEncrypt.setSelectedItem(ConfigWindow.CURRENT_ENCRYPT);
        _jComboBoxEncrypt.addActionListener(e -> saveComboBox(_jComboBoxEncrypt, "CURRENT_ENCRYPT"));
        settingsPanel.add(_jComboBoxEncrypt);

        _lblFormatter = new JLabel(i18n("formatter.label"));
        _lblFormatter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        settingsPanel.add(_lblFormatter);

        comboBoxModel = new DefaultComboBoxModel<>(new String[]
                {
                        // IMPORTANTE: "Enabled"/"Disabled" são valores de SISTEMA salvos no config, não traduzir!
                        "Enabled",
                        "Disabled"
                });
        _jComboBoxFormatter.setToolTipText(i18n("formatter.tooltip"));
        _jComboBoxFormatter.setModel(comboBoxModel);
        _jComboBoxFormatter.setSelectedItem(ConfigWindow.CURRENT_FORMATTER);
        _jComboBoxFormatter.addActionListener(e -> saveComboBox(_jComboBoxFormatter, "CURRENT_FORMATTER"));
        settingsPanel.add(_jComboBoxFormatter);

        _lblLanguage = new JLabel(i18n("language.label"));
        _lblLanguage.setFont(new Font("Segoe UI", Font.BOLD, 12));
        settingsPanel.add(_lblLanguage);

        // Popula automaticamente com os idiomas disponíveis detectados
        String[] availableLanguages = I18nManager.getInstance().getAvailableLanguageNames();
        comboBoxModel = new DefaultComboBoxModel<>(availableLanguages);
        _jComboBoxLanguage.setToolTipText(i18n("language.tooltip"));
        _jComboBoxLanguage.setModel(comboBoxModel);

        // Define o idioma atual baseado no locale
        String currentDisplayName = I18nManager.getInstance().getDisplayNameByLocale(
                I18nManager.getInstance().getCurrentLocale()
        );
        _jComboBoxLanguage.setSelectedItem(currentDisplayName);
        _jComboBoxLanguage.addActionListener(e -> changeLanguage());

        settingsPanel.add(_jComboBoxLanguage);

        return settingsPanel;
    }


    /**
     * Chamado quando o usuário muda o idioma no combo box.
     * Agora totalmente automático!
     */
    private void changeLanguage()
    {
        String selectedDisplayName = (String) _jComboBoxLanguage.getSelectedItem();
        if (selectedDisplayName == null)
        {
            return;
        }

        // Obtém o Locale automaticamente pelo nome de display
        Locale newLocale = I18nManager.getInstance().getLocaleByDisplayName(selectedDisplayName);
        if (newLocale == null)
        {
            newLocale = Locale.ENGLISH;
            selectedDisplayName = "English";
        }

        I18nManager.getInstance().setLocale(newLocale);
        ConfigWindow.save("CURRENT_LANGUAGE", selectedDisplayName);

        updateAllUIStrings();
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void updateAllUIStrings()
    {
        // Atualiza título da janela
        setTitle(i18n("window.title"));

        // Atualiza botões
        _openDatButton.setText(i18n("open.btn"));
        _saveTxtButton.setText(i18n("save.txt.btn"));
        _saveDatButton.setText(i18n("save.dat.btn"));
        _massUnpackButton.setText(i18n("decrypt.all.btn"));
        _massPackButton.setText(i18n("encrypt.all.btn"));
        _massRecryptButton.setText(i18n("patch.all.btn"));
        _abortTaskButton.setText(i18n("abort.btn"));
        _updateNotifyButton.setText(i18n("update.notify.btn"));

        // Atualiza tooltips dos botões
        _openDatButton.setToolTipText(i18n("open.btn.tooltip"));
        _massUnpackButton.setToolTipText(i18n("decrypt.all.btn.tooltip"));
        _massPackButton.setToolTipText(i18n("pack.all.btn.tooltip"));
        _massRecryptButton.setToolTipText(i18n("patch.all.btn.tooltip"));
        _abortTaskButton.setToolTipText(i18n("abort.btn.tooltip"));
        _themeToggleButton.setText(i18n("toggle.theme"));
        _aboutButton.setText(i18n("about.btn"));
        _themeToggleButton.setToolTipText(i18n("toggle.theme.tooltip"));
        _aboutButton.setToolTipText(i18n("about.btn.tooltip"));

        // Atualiza tooltips condicionais
        if (_currentFileWindow != null)
        {
            _saveTxtButton.setToolTipText(i18n("save.txt.btn.tooltip"));
            _saveDatButton.setToolTipText(i18n("save.dat.btn.tooltip"));
        }
        else
        {
            _saveTxtButton.setToolTipText(i18n("save.txt.btn.tooltip.disabled"));
            _saveDatButton.setToolTipText(i18n("save.dat.btn.tooltip.disabled"));
        }

        // Atualiza labels
        if (_lblStructure != null)
        {
            _lblStructure.setText(i18n("dat.structure"));
        }
        if (_lblEncrypt != null)
        {
            _lblEncrypt.setText(i18n("encrypt.label"));
        }
        if (_lblFormatter != null)
        {
            _lblFormatter.setText(i18n("formatter.label"));
        }
        if (_lblLanguage != null)
        {
            _lblLanguage.setText(i18n("language.label"));
        }

        _subtitleLabel.setText(i18n("subtitle"));

        // Atualiza combo boxes
        updateComboBoxItems();

        // Força redesenho
        revalidate();
        repaint();

        addLogConsole(i18n("log.language.loaded", I18nManager.getInstance().getCurrentLocale().getDisplayLanguage()), false);
    }

    private void updateComboBoxItems()
    {
        // Atualiza Formatter combo
        String currentFormatter = (String) _jComboBoxFormatter.getSelectedItem();
        boolean wasEnabled = "Enabled".equals(currentFormatter);

        // IMPORTANTE: "Enabled"/"Disabled" são valores de SISTEMA, não traduzir!
        DefaultComboBoxModel<String> formatterModel = new DefaultComboBoxModel<>(new String[]
                {
                        "Enabled",
                        "Disabled"
                });
        _jComboBoxFormatter.setModel(formatterModel);
        _jComboBoxFormatter.setSelectedItem(wasEnabled ? "Enabled" : "Disabled");
        _jComboBoxFormatter.setToolTipText(i18n("formatter.tooltip"));

        // Atualiza Language combo - AGORA AUTOMÁTICO!
        String[] availableLanguages = I18nManager.getInstance().getAvailableLanguageNames();
        DefaultComboBoxModel<String> languageModel = new DefaultComboBoxModel<>(availableLanguages);
        String currentDisplayName = I18nManager.getInstance().getDisplayNameByLocale(
                I18nManager.getInstance().getCurrentLocale()
        );
        _jComboBoxLanguage.setModel(languageModel);
        _jComboBoxLanguage.setSelectedItem(currentDisplayName);
        _jComboBoxLanguage.setToolTipText(i18n("language.tooltip"));

        // Atualiza Encrypt combo
        DefaultComboBoxModel<String> encryptModel = new DefaultComboBoxModel<>(CryptVersionParser.getInstance().getEncryptKey().keySet().toArray(new String[0]));
        // IMPORTANTE: "Source" é um valor de SISTEMA, não traduzir!
        encryptModel.insertElementAt("Source", 0);
        String currentEncrypt = (String) _jComboBoxEncrypt.getSelectedItem();
        _jComboBoxEncrypt.setModel(encryptModel);
        _jComboBoxEncrypt.setSelectedItem(currentEncrypt);
        _jComboBoxEncrypt.setToolTipText(i18n("encrypt.tooltip"));

        // Atualiza Chronicle combo tooltip
        _jComboBoxChronicle.setToolTipText(i18n("chronicle.tooltip"));
    }

    private JPanel createProgressPanel()
    {
        JPanel progressPanel = new JPanel(new BorderLayout(10, 0));
        progressPanel.setOpaque(false);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        _progressBar.setPreferredSize(new Dimension(100, 28));
        progressPanel.add(_progressBar, BorderLayout.CENTER);

        _abortTaskButton.setToolTipText(i18n("abort.btn.tooltip"));
        _abortTaskButton.addActionListener(this::abortActionPerformed);
        _abortTaskButton.setEnabled(false);
        _abortTaskButton.setPreferredSize(new Dimension(100, 28));
        _abortTaskButton.setMaximumSize(new Dimension(100, 28));
        progressPanel.add(_abortTaskButton, BorderLayout.EAST);

        return progressPanel;
    }

    private JSplitPane createEditorSplitPane()
    {
        final Font font = new Font("JetBrains Mono", Font.PLAIN, 13);

        _textPaneMain.setFont(font);
        _textPaneMain.setCaretColor(ACCENT_COLOR);
        ((AbstractDocument) _textPaneMain.getDocument()).setDocumentFilter(new DocumentFilter()
        {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
            {
                final String replacedText = text.replace("\r\n", "\n");
                super.replace(fb, offset, length, replacedText, attrs);
            }
        });

        _lineNumberingTextArea.setFont(font.deriveFont(12.0f));
        _lineNumberingTextArea.setEditable(false);
        _textPaneMain.getDocument().addDocumentListener(_lineNumberingTextArea);

        final JScrollPane scrollPaneEditor = new JScrollPane(_textPaneMain);
        scrollPaneEditor.setRowHeaderView(_lineNumberingTextArea);
        scrollPaneEditor.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        _textPaneLog.setBackground(Color.BLACK);
        _textPaneLog.setForeground(new Color(0, 255, 0));
        _textPaneLog.setEditable(false);
        _textPaneLog.setFont(new Font("Consolas", Font.PLAIN, 13));

        final JScrollPane scrollPaneLog = new JScrollPane(_textPaneLog);
        scrollPaneLog.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        final JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPaneEditor, scrollPaneLog);
        jsp.setResizeWeight(0.7);
        jsp.setOneTouchExpandable(true);
        jsp.setDividerSize(8);

        return jsp;
    }

    private void setupFrame()
    {
        setTitle(i18n("window.title"));
        setMinimumSize(new Dimension(1100, 650));
        setSize(new Dimension(Math.max(1200, ConfigWindow.WINDOW_WIDTH), Math.max(750, ConfigWindow.WINDOW_HEIGHT)));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent evt)
            {
                ConfigWindow.save("WINDOW_HEIGHT", String.valueOf(getHeight()));
                ConfigWindow.save("WINDOW_WIDTH", String.valueOf(getWidth()));
                ConfigWindow.save("THEME_PREFERENCE", isDarkTheme ? "dark" : "light");
                System.exit(0);
            }
        });

        final List<Image> icons = new ArrayList<>();
        try
        {
            icons.add(new ImageIcon("." + File.separator + "images" + File.separator + "l2jmobius_16x16.png").getImage());
            icons.add(new ImageIcon("." + File.separator + "images" + File.separator + "l2jmobius_32x32.png").getImage());
            icons.add(new ImageIcon("." + File.separator + "images" + File.separator + "l2jmobius_64x64.png").getImage());
            icons.add(new ImageIcon("." + File.separator + "images" + File.separator + "l2jmobius_128x128.png").getImage());
            setIconImages(icons);
        }
        catch (Exception e)
        {
            LOGGER.warning("Unable to load application icons.");
        }

        pack();
    }

    private void aboutL2ExplorerDat(ActionEvent about)
    {
        AboutDialog dialog = new AboutDialog(this);
        dialog.setVisible(true);
    }

    private void toggleTheme()
    {
        isDarkTheme = !isDarkTheme;
        try
        {
            UIManager.setLookAndFeel(isDarkTheme ? new FlatMacDarkLaf() : new FlatMacLightLaf());

            for (Window window : Window.getWindows())
            {
                SwingUtilities.updateComponentTreeUI(window);
            }

            String currentLogText = "";
            try
            {
                currentLogText = _textPaneLog.getDocument().getText(0, _textPaneLog.getDocument().getLength());
            }
            catch (Exception e)
            {
                LOGGER.log(Level.SEVERE, "Critical error on: ", e);
            }

            if (isDarkTheme)
            {
                _textPaneLog.setBackground(Color.BLACK);
                _textPaneLog.setForeground(new Color(0, 255, 0));
            }
            else
            {
                _textPaneLog.setBackground(Color.WHITE);
                _textPaneLog.setForeground(Color.BLACK);
            }

            _textPaneLog.setText("");
            if (!currentLogText.isEmpty())
            {
                String[] logLines = currentLogText.split("\n");
                for (String line : logLines)
                {
                    if (!line.trim().isEmpty())
                    {
                        Color lineColor = detectLogColor(line);
                        appendColoredLog(line + "\n", lineColor);
                    }
                }
            }

            ConfigWindow.save("THEME_PREFERENCE", isDarkTheme ? "dark" : "light");

        }
        catch (Exception ex)
        {
            LOGGER.log(Level.SEVERE, "Error while changing theme.", ex);
            isDarkTheme = !isDarkTheme;
        }
    }

    public JPopupTextArea getTextPaneMain()
    {
        return _textPaneMain;
    }

    public static void addLogConsole(String log, boolean isLog)
    {
        if (isLog)
        {
            LOGGER.info(log);
        }

        Color logColor = detectLogColor(log);

        final String logLine = log + "\n";
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> appendColoredLog(logLine, logColor));
        }
        else
        {
            appendColoredLog(logLine, logColor);
        }
    }

    private static Color detectLogColor(String log)
    {
        if (_textPaneLog.getBackground().equals(Color.WHITE))
        {
            return Color.BLACK;
        }

        String logLower = log.toLowerCase();

        if (logLower.contains("error") || logLower.contains("failed") || logLower.contains("exception") || logLower.contains("critical") || logLower.contains("severe") || logLower.contains("abort"))
        {
            return new Color(255, 85, 85);
        }

        if (logLower.contains("warning") || logLower.contains("warn") || logLower.contains("caution") || logLower.contains("not found") || logLower.contains("unable"))
        {
            return new Color(255, 200, 50);
        }

        if (logLower.contains("success") || logLower.contains("completed") || logLower.contains("saved") || logLower.contains("done") || logLower.contains("opening file") || logLower.contains("selected"))
        {
            return new Color(0, 255, 200);
        }

        return new Color(0, 255, 0);
    }

    private static void appendColoredLog(String logLine, Color color)
    {
        try
        {
            StyledDocument doc = (StyledDocument) _textPaneLog.getDocument();
            Style style = doc.addStyle("LogStyle", null);
            StyleConstants.setForeground(style, color);

            doc.insertString(doc.getLength(), logLine, style);
            _textPaneLog.setCaretPosition(doc.getLength());
        }
        catch (Exception e)
        {
            try
            {
                Document doc = _textPaneLog.getDocument();
                doc.insertString(doc.getLength(), logLine, null);
                _textPaneLog.setCaretPosition(doc.getLength());
            }
            catch (Exception ex)
            {
                LOGGER.log(Level.WARNING, "Failed to append log", ex);
            }
        }
    }

    public void setEditorText(String text)
    {
        _lineNumberingTextArea.cleanUp();
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() ->
            {
                _textPaneMain.setText(text);
                _textPaneMain.setCaretPosition(0);
            });
        }
        else
        {
            _textPaneMain.setText(text);
            _textPaneMain.setCaretPosition(0);
        }
    }

    private void massTxtPackActionPerformed(ActionEvent evt)
    {
        if (_progressTask != null)
        {
            return;
        }

        final JFileChooser fileopen = new JFileChooser();
        fileopen.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileopen.setAcceptAllFileFilterUsed(false);
        fileopen.setCurrentDirectory(new File(ConfigWindow.FILE_OPEN_CURRENT_DIRECTORY_PACK));
        fileopen.setPreferredSize(new Dimension(700, 500));

        final int ret = fileopen.showDialog(this, i18n("select.btn"));
        if (ret == JFileChooser.APPROVE_OPTION)
        {
            _currentFileWindow = fileopen.getSelectedFile();
            ConfigWindow.save("FILE_OPEN_CURRENT_DIRECTORY_PACK", _currentFileWindow.getPath());
            addLogConsole(i18n("log.divider"), true);
            addLogConsole(i18n("log.folder.selected.pack", _currentFileWindow.getPath()), true);
            _progressTask = new MassTxtPacker(this, String.valueOf(_jComboBoxChronicle.getSelectedItem()), _currentFileWindow.getPath());
            _executorService.execute(_progressTask);
        }
    }

    private void massTxtUnpackActionPerformed(ActionEvent evt)
    {
        if (_progressTask != null)
        {
            return;
        }

        final JFileChooser fileopen = new JFileChooser();
        fileopen.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileopen.setAcceptAllFileFilterUsed(false);
        fileopen.setCurrentDirectory(new File(ConfigWindow.FILE_OPEN_CURRENT_DIRECTORY_UNPACK));
        fileopen.setPreferredSize(new Dimension(700, 500));

        final int ret = fileopen.showDialog(this, i18n("select.btn"));
        if (ret == JFileChooser.APPROVE_OPTION)
        {
            _currentFileWindow = fileopen.getSelectedFile();
            ConfigWindow.save("FILE_OPEN_CURRENT_DIRECTORY_UNPACK", _currentFileWindow.getPath());
            addLogConsole(i18n("log.divider"), true);
            addLogConsole(i18n("log.folder.selected.unpack", _currentFileWindow.getPath()), true);
            _progressTask = new MassTxtUnpacker(this, String.valueOf(_jComboBoxChronicle.getSelectedItem()), _currentFileWindow.getPath());
            _executorService.execute(_progressTask);
        }
    }

    private void massRecryptActionPerformed(ActionEvent evt)
    {
        if (_progressTask != null)
        {
            return;
        }

        final JFileChooser fileopen = new JFileChooser();
        fileopen.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileopen.setAcceptAllFileFilterUsed(false);
        fileopen.setCurrentDirectory(new File(ConfigWindow.FILE_OPEN_CURRENT_DIRECTORY));
        fileopen.setPreferredSize(new Dimension(700, 500));

        final int ret = fileopen.showDialog(this, i18n("select.btn"));
        if (ret == JFileChooser.APPROVE_OPTION)
        {
            _currentFileWindow = fileopen.getSelectedFile();
            ConfigWindow.save("FILE_OPEN_CURRENT_DIRECTORY", _currentFileWindow.getPath());
            addLogConsole(i18n("log.divider"), true);
            addLogConsole(i18n("log.folder.selected.patch", _currentFileWindow.getPath()), true);
            _progressTask = new MassRecryptor(this, _currentFileWindow.getPath());
            _executorService.execute(_progressTask);
        }
    }

    private void openSelectFileWindow(ActionEvent evt)
    {
        if (_progressTask != null)
        {
            return;
        }

        final JFileChooser fileopen = new JFileChooser();
        fileopen.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileopen.setMultiSelectionEnabled(false);
        fileopen.setAcceptAllFileFilterUsed(false);
        if (DEV_MODE || ConfigDebug.SAVE_DECODE)
        {
            fileopen.addChoosableFileFilter(new FileNameExtensionFilter(i18n("filter.client.files"), "dat", "ini", "txt", "htm", "unr", "u"));
        }
        else
        {
            fileopen.addChoosableFileFilter(new FileNameExtensionFilter(i18n("filter.client.files.basic"), "dat", "ini", "txt", "htm"));
            addLogConsole(i18n("log.save.decode.inactive"), false);
            addLogConsole(i18n("log.unr.files.info"), false);
        }
        fileopen.addChoosableFileFilter(new FileNameExtensionFilter(i18n("filter.dat.files"), "dat"));
        fileopen.addChoosableFileFilter(new FileNameExtensionFilter(i18n("filter.ini.files"), "ini"));
        fileopen.addChoosableFileFilter(new FileNameExtensionFilter(i18n("filter.txt.files"), "txt"));
        fileopen.addChoosableFileFilter(new FileNameExtensionFilter(i18n("filter.htm.files"), "htm"));

        if (DEV_MODE || ConfigDebug.SAVE_DECODE)
        {
            fileopen.addChoosableFileFilter(new FileNameExtensionFilter(i18n("filter.unr.files"), "unr"));
            fileopen.addChoosableFileFilter(new FileNameExtensionFilter(i18n("filter.u.files"), "u"));

            addLogConsole(i18n("log.save.decode.active"), false);
        }

        fileopen.setFileFilter(fileopen.getChoosableFileFilters()[0]);

        try
        {
            File initialFile = new File(ConfigWindow.LAST_FILE_SELECTED);
            if (initialFile.exists())
            {
                fileopen.setSelectedFile(initialFile);
            }
            else if ((initialFile.getParentFile() != null) && initialFile.getParentFile().exists())
            {
                fileopen.setCurrentDirectory(initialFile.getParentFile());
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Critical fail on: ", e);
        }

        fileopen.setPreferredSize(new Dimension(700, 500));

        final int ret = fileopen.showDialog(this, i18n("file.select.btn"));
        if (ret == JFileChooser.APPROVE_OPTION)
        {
            _currentFileWindow = fileopen.getSelectedFile();
            ConfigWindow.save("LAST_FILE_SELECTED", _currentFileWindow.getAbsolutePath());
            addLogConsole(i18n("log.divider"), true);
            addLogConsole(i18n("log.file.opening", _currentFileWindow.getName()), true);
            _progressTask = new OpenDat(this, String.valueOf(_jComboBoxChronicle.getSelectedItem()), _currentFileWindow);
            _executorService.execute(_progressTask);
        }
    }

    private void saveTxtActionPerformed(ActionEvent evt)
    {
        if (_progressTask != null)
        {
            return;
        }

        if (_currentFileWindow == null)
        {
            addLogConsole(i18n("log.no.file.open"), true);
            return;
        }

        final JFileChooser fileSave = new JFileChooser();
        File currentDir = _currentFileWindow.getParentFile();
        if ((currentDir == null) || !currentDir.exists())
        {
            currentDir = new File(ConfigWindow.FILE_SAVE_CURRENT_DIRECTORY);
        }
        fileSave.setCurrentDirectory(currentDir);

        String currentName = _currentFileWindow.getName();
        int dotIndex = currentName.lastIndexOf('.');
        String suggestedName = (dotIndex > 0) ? currentName.substring(0, dotIndex) + ".txt" : currentName + ".txt";
        fileSave.setSelectedFile(new File(suggestedName));

        fileSave.setFileFilter(new FileNameExtensionFilter(i18n("filter.text.files"), "txt"));
        fileSave.setAcceptAllFileFilterUsed(false);
        fileSave.setPreferredSize(new Dimension(700, 500));

        final int ret = fileSave.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = fileSave.getSelectedFile();
            if (!selectedFile.getName().toLowerCase().endsWith(".txt"))
            {
                selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".txt");
            }

            ConfigWindow.save("FILE_SAVE_CURRENT_DIRECTORY", selectedFile.getParent());
            _progressTask = new SaveTxt(this, selectedFile);
            _executorService.execute(_progressTask);
        }
    }

    private void saveDatActionPerformed(ActionEvent evt)
    {
        if (_progressTask != null)
        {
            return;
        }

        if (_currentFileWindow == null)
        {
            addLogConsole(i18n("log.no.file.reference"), true);
            return;
        }

        addLogConsole(i18n("log.saving.dat", _currentFileWindow.getName()), true);
        _progressTask = new SaveDat(this, _currentFileWindow, String.valueOf(_jComboBoxChronicle.getSelectedItem()));
        _executorService.execute(_progressTask);
    }

    private void abortActionPerformed(ActionEvent evt)
    {
        if (_progressTask == null)
        {
            return;
        }

        _progressTask.abort();
        addLogConsole(i18n("log.divider"), true);
        addLogConsole(i18n("log.abort.sent"), true);
    }

    public DatCrypter getEncryptor(File file)
    {
        DatCrypter crypter = null;
        String encryptorName = ConfigWindow.CURRENT_ENCRYPT;

        // IMPORTANTE: "Source" é um valor de SISTEMA, sempre em inglês
        if ((encryptorName == null) || encryptorName.equalsIgnoreCase(".") || encryptorName.equalsIgnoreCase("Source") || encryptorName.trim().isEmpty())
        {
            final DatCrypter lastDatDecryptor = OpenDat.getLastDatCrypter(file);
            if (lastDatDecryptor != null)
            {
                crypter = CryptVersionParser.getInstance().getEncryptKey(lastDatDecryptor.getName());
                if (crypter != null)
                {
                    addLogConsole(i18n("log.using.source.encryptor", crypter.getName()), false);
                }
                else
                {
                    addLogConsole(i18n("log.warning.encryptor.mismatch", lastDatDecryptor.getName()), true);
                }
            }
            else
            {
                addLogConsole(i18n("log.source.not.detected", file.getName()), true);
                encryptorName = String.valueOf(_jComboBoxEncrypt.getSelectedItem());
                if (encryptorName.equalsIgnoreCase("Source"))
                {
                    addLogConsole(i18n("log.cannot.use.source"), true);
                    return null;
                }
            }
        }

        if ((crypter == null) && !((encryptorName == null) || encryptorName.equalsIgnoreCase(".") || encryptorName.equalsIgnoreCase("Source") || encryptorName.trim().isEmpty()))
        {
            crypter = CryptVersionParser.getInstance().getEncryptKey(encryptorName);
            if (crypter != null)
            {
                addLogConsole(i18n("log.using.selected.encryptor", crypter.getName()), false);
            }
            else
            {
                addLogConsole(i18n("log.encryptor.not.found", encryptorName), true);
                return null;
            }
        }

        if (crypter == null)
        {
            addLogConsole(i18n("log.could.not.determine", file.getName()), true);
        }

        return crypter;
    }

    private void saveComboBox(JComboBox<String> jComboBox, String param)
    {
        ConfigWindow.save(param, String.valueOf(jComboBox.getSelectedItem()));
    }

    public void onStartTask()
    {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        _progressBar.setValue(0);
        _progressBar.setIndeterminate(true);
        checkButtons();
    }

    public void onProgressTask(int val)
    {
        if (_progressBar.isIndeterminate())
        {
            _progressBar.setIndeterminate(false);
            _progressBar.setStringPainted(true);
        }
        _progressBar.setValue(val);
    }

    public void onStopTask()
    {
        _progressTask = null;
        _progressBar.setValue(100);
        _progressBar.setIndeterminate(false);
        checkButtons();
        Toolkit.getDefaultToolkit().beep();
        setCursor(Cursor.getDefaultCursor());
    }

    public void onAbortTask()
    {
        if (_progressTask == null)
        {
            return;
        }

        _progressTask = null;
        _progressBar.setValue(0);
        _progressBar.setIndeterminate(false);
        addLogConsole(i18n("log.task.aborted"), true);
        checkButtons();
        setCursor(Cursor.getDefaultCursor());
    }

    private void checkButtons()
    {
        final boolean taskRunning = (_progressTask != null);

        for (JPanel panel : _actionPanels)
        {
            for (Component c : panel.getComponents())
            {
                c.setEnabled(!taskRunning);
            }
        }

        _abortTaskButton.setEnabled(taskRunning);

        if (!taskRunning)
        {
            boolean fileIsOpen = (_currentFileWindow != null);
            _saveTxtButton.setEnabled(fileIsOpen);
            _saveTxtButton.setToolTipText(fileIsOpen ? i18n("save.txt.btn.tooltip") : i18n("save.txt.btn.tooltip.disabled"));
            _saveDatButton.setEnabled(fileIsOpen);
            _saveDatButton.setToolTipText(fileIsOpen ? i18n("save.dat.btn.tooltip") : i18n("save.dat.btn.tooltip.disabled"));
        }
        else
        {
            _saveTxtButton.setEnabled(false);
            _saveDatButton.setEnabled(false);
        }
    }

    private static class ModernProgressBar extends JProgressBar
    {
        public ModernProgressBar()
        {
            super(0, 100);
            setStringPainted(true);
            setForeground(ACCENT_COLOR);
            setBorderPainted(false);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(UIManager.getColor("ProgressBar.background"));
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

            if ((getValue() > 0) || isIndeterminate())
            {
                int progressWidth = (int) ((getWidth() - 4) * (getValue() / 100.0));
                if (isIndeterminate())
                {
                    progressWidth = getWidth() - 4;
                }

                GradientPaint gp = new GradientPaint(0, 0, ACCENT_COLOR, progressWidth, 0, ACCENT_HOVER);
                g2d.setPaint(gp);
                g2d.fillRoundRect(2, 2, progressWidth, getHeight() - 4, 6, 6);
            }

            if (isStringPainted() && !isIndeterminate())
            {
                g2d.setColor(UIManager.getColor("ProgressBar.foreground"));
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String text = getString();
                int stringWidth = g2d.getFontMetrics().stringWidth(text);
                int stringHeight = g2d.getFontMetrics().getAscent();
                g2d.drawString(text, (getWidth() - stringWidth) / 2, ((getHeight() + stringHeight) / 2) - 2);
            }
        }
    }

    private static class ModernSplashWindow extends JWindow
    {
        private final Point finalPos;
        private final int windowHeight;
        private final Image image;

        public ModernSplashWindow()
        {
            ImageIcon splashIcon = null;
            try
            {
                splashIcon = new ImageIcon("." + File.separator + "images" + File.separator + "splash.png");
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "Splash image not found.", e);
            }

            if ((splashIcon != null) && (splashIcon.getIconWidth() > 0))
            {
                this.image = splashIcon.getImage();
                setPreferredSize(new Dimension(splashIcon.getIconWidth(), splashIcon.getIconHeight()));
            }
            else
            {
                this.image = null;
                setPreferredSize(new Dimension(300, 200));
                LOGGER.warning("Using default splash size.");
            }

            setBackground(new Color(0, 0, 0, 0));

            pack();

            setLocationRelativeTo(null);
            this.finalPos = getLocation();
            this.windowHeight = getHeight();

            Point startPos = new Point(this.finalPos.x, this.finalPos.y + this.windowHeight);

            setLocation(startPos);
        }

        @Override
        public void paint(Graphics g)
        {
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, getWidth(), getHeight());

            if (this.image != null)
            {
                int x = (getWidth() - this.image.getWidth(null)) / 2;
                int y = (getHeight() - this.image.getHeight(null)) / 2;
                g.drawImage(this.image, x, y, this);
            }

        }

        public void setAnimationProgress(double progress)
        {
            progress = Math.max(0.0, Math.min(progress, 1.0));

            int newY = (int) (this.finalPos.y + (this.windowHeight * (1.0 - progress)));

            setLocation(this.finalPos.x, newY);
        }
    }

    private static class LineNumberingTextArea extends JTextArea implements DocumentListener
    {
        private final JTextArea textArea;
        private int lastLines;

        public LineNumberingTextArea(JTextArea area)
        {
            lastLines = 0;
            textArea = area;
        }

        public void cleanUp()
        {
            setText("");
            removeAll();
            lastLines = 0;
        }

        private void updateText()
        {
            final int length = textArea.getLineCount();
            if (length == lastLines)
            {
                return;
            }

            lastLines = length;
            final StringBuilder lineNumbersTextBuilder = new StringBuilder();
            lineNumbersTextBuilder.append("1").append(System.lineSeparator());
            for (int line = 2; line <= length; ++line)
            {
                lineNumbersTextBuilder.append(line).append(System.lineSeparator());
            }
            setText(lineNumbersTextBuilder.toString());
        }

        @Override
        public void insertUpdate(DocumentEvent documentEvent)
        {
            SwingUtilities.invokeLater(this::updateText);
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent)
        {
            SwingUtilities.invokeLater(this::updateText);
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent)
        {

        }
    }
}