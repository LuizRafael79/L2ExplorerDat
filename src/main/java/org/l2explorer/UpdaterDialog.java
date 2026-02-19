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

import org.l2explorer.util.I18nManager;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class UpdaterDialog extends JDialog
{
	private static final String CURRENT_VERSION = I18nManager.getInstance().getString("app.version");
	private static final String REPO_OWNER = "LuizRafael79";
	private static final String REPO_NAME = "L2ExplorerDat";

	private static final Color PRIMARY_BG = new Color(41, 49, 52);
	private static final Color SECONDARY_BG = new Color(30, 36, 40);
	private static final Color ACCENT_COLOR = new Color(0, 173, 181);
	@SuppressWarnings("unused")
	private static final Color ACCENT_HOVER = new Color(0, 200, 209);
	private static final Color CLOSE_COLOR = new Color(71, 85, 105);
	private static final Color CLOSE_HOVER = new Color(100, 116, 139);
	private static final Color SUCCESS_COLOR = new Color(34, 197, 94);
	private static final Color SUCCESS_HOVER = new Color(22, 163, 74);
	private static final Color TEXT_PRIMARY = new Color(237, 242, 247);
	private static final Color TEXT_SECONDARY = new Color(160, 174, 192);

	private JLabel statusLabel;
	private JLabel versionLabel;
	private JTextArea releaseNotesArea;
	private JButton downloadButton;
	@SuppressWarnings("FieldCanBeLocal")
	private JButton closeButton;

	private String latestVersion;
	private String downloadUrl;

	public UpdaterDialog(JFrame parent)
	{
		super(parent, "Check for Updates", true);
		initComponents();
		checkForUpdates();
	}

	private void initComponents()
	{
		setLayout(new BorderLayout());
		setUndecorated(true);
		setShape(new RoundRectangle2D.Double(0, 0, 500, 550, 20, 20));

		JPanel mainPanel = getJPanel();

		// Top panel com botão fechar
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setOpaque(false);
		topPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

		JButton closeTopBtn = createCloseButton();
		topPanel.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
		topPanel.add(closeTopBtn, BorderLayout.EAST);

		// Header
		JPanel headerPanel = createHeaderPanel();

		// Status area
		JPanel statusPanel = createStatusPanel();

		// Release notes
		JPanel notesPanel = createNotesPanel();

		// Buttons
		JPanel buttonsPanel = createButtonsPanel();

		mainPanel.add(topPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		mainPanel.add(headerPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
		mainPanel.add(statusPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
		mainPanel.add(notesPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
		mainPanel.add(buttonsPanel);

		add(mainPanel, BorderLayout.CENTER);
		pack();
		setSize(500, 550);
		setLocationRelativeTo(getOwner());
	}

	private JPanel getJPanel() {
		JPanel mainPanel = new JPanel()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				GradientPaint gp = new GradientPaint(0, 0, PRIMARY_BG, 0, getHeight(), SECONDARY_BG);
				g2d.setPaint(gp);
				g2d.fillRect(0, 0, getWidth(), getHeight());
			}
		};

		// Arrastar janela
		final Point offset = new Point();
		mainPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				offset.x = e.getX();
				offset.y = e.getY();
			}
		});
		mainPanel.addMouseMotionListener(new MouseAdapter()
		{
			@Override
			public void mouseDragged(MouseEvent e)
			{
				setLocation(e.getXOnScreen() - offset.x, e.getYOnScreen() - offset.y);
			}
		});

		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 30, 30, 30));
		return mainPanel;
	}

	private JButton createCloseButton()
	{
		JButton closeBtn = new JButton("X")
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				if (getModel().isPressed())
				{
					g2d.setColor(new Color(239, 68, 68));
				}
				else if (getModel().isRollover())
				{
					g2d.setColor(new Color(220, 38, 38));
				}
				else
				{
					g2d.setColor(new Color(100, 116, 139, 100));
				}
				g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

				g2d.setColor(Color.WHITE);
				g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
				int stringWidth = g2d.getFontMetrics().stringWidth("✕");
				int stringHeight = g2d.getFontMetrics().getAscent();
				g2d.drawString("✕", (getWidth() - stringWidth) / 2, ((getHeight() + stringHeight) / 2) - 2);
			}
		};
		closeBtn.setPreferredSize(new Dimension(36, 36));
		closeBtn.setContentAreaFilled(false);
		closeBtn.setBorderPainted(false);
		closeBtn.setFocusPainted(false);
		closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		closeBtn.addActionListener(_ -> dispose());
		return closeBtn;
	}

	private JPanel createHeaderPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);

		JLabel iconLabel = new JLabel("🔄");
		iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
		iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel titleLabel = new JLabel("Software Update");
		titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
		titleLabel.setForeground(TEXT_PRIMARY);
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		panel.add(iconLabel);
		panel.add(Box.createRigidArea(new Dimension(0, 10)));
		panel.add(titleLabel);

		return panel;
	}

	private JPanel createStatusPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);

		statusLabel = new JLabel("Checking for updates...");
		statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		statusLabel.setForeground(TEXT_SECONDARY);
		statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		versionLabel = new JLabel("Current: " + CURRENT_VERSION);
		versionLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
		versionLabel.setForeground(ACCENT_COLOR);
		versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		panel.add(statusLabel);
		panel.add(Box.createRigidArea(new Dimension(0, 8)));
		panel.add(versionLabel);

		return panel;
	}

	private JPanel createNotesPanel()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

		JLabel notesTitle = new JLabel("Release Notes");
		notesTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
		notesTitle.setForeground(TEXT_PRIMARY);
		notesTitle.setBorder(BorderFactory.createEmptyBorder(0, 5, 8, 0));

		releaseNotesArea = new JTextArea();
		releaseNotesArea.setEditable(false);
		releaseNotesArea.setBackground(new Color(30, 36, 40));
		releaseNotesArea.setForeground(TEXT_SECONDARY);
		releaseNotesArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		releaseNotesArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(71, 85, 105)), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		releaseNotesArea.setLineWrap(true);
		releaseNotesArea.setWrapStyleWord(true);
		releaseNotesArea.setText("Loading...");

		panel.add(notesTitle, BorderLayout.NORTH);
		panel.add(releaseNotesArea, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createButtonsPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setOpaque(false);

		downloadButton = createModernButton("Download Update", SUCCESS_COLOR, SUCCESS_HOVER);
		downloadButton.setEnabled(false);
		downloadButton.addActionListener(_ -> openDownloadPage());

		closeButton = createModernButton("Close", CLOSE_COLOR, CLOSE_HOVER);
		closeButton.setToolTipText("Close the updater");
		closeButton.addActionListener(_ -> dispose());

		panel.add(Box.createHorizontalGlue());
		panel.add(closeButton);
		panel.add(Box.createRigidArea(new Dimension(15, 0)));
		panel.add(downloadButton);
		panel.add(Box.createHorizontalGlue());

		return panel;
	}

	private JButton createModernButton(String text, Color bgColor, Color hoverColor)
	{
		JButton button = new JButton(text)
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				ButtonModel model = getModel();
				Color bg;
				if (!model.isEnabled())
				{
					bg = new Color(71, 85, 105);
				}
				else if (model.isPressed())
				{
					bg = hoverColor.darker();
				}
				else if (model.isRollover())
				{
					bg = hoverColor;
				}
				else
				{
					bg = bgColor;
				}

				g2d.setColor(bg);
				g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

				g2d.setColor(model.isEnabled() ? Color.WHITE : new Color(160, 174, 192));
				g2d.setFont(getFont());
				int stringWidth = g2d.getFontMetrics().stringWidth(getText());
				int stringHeight = g2d.getFontMetrics().getAscent();
				g2d.drawString(getText(), (getWidth() - stringWidth) / 2, ((getHeight() + stringHeight) / 2) - 2);
			}
		};

		button.setFont(new Font("Segoe UI", Font.BOLD, 13));
		button.setForeground(Color.WHITE);
		button.setPreferredSize(new Dimension(160, 40));
		button.setMaximumSize(new Dimension(160, 40));
		button.setContentAreaFilled(false);
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));

		return button;
	}

	private void checkForUpdates()
	{
		new SwingWorker<UpdateInfo, Void>()
		{
			@Override
			protected UpdateInfo doInBackground() throws Exception {
				String apiUrl = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/releases/latest";
				URL url = URI.create(apiUrl).toURL();
				String jsonResponse = getString(url);

				UpdateInfo info = new UpdateInfo();
				String tagName = extractJsonValue(jsonResponse, "tag_name");

				if (tagName.matches(".*\\d+\\.\\d+.*")) {
					info.latestVersion = tagName.replaceAll("[^0-9.]", "");
				} else {
					String publishedAt = extractJsonValue(jsonResponse, "published_at");
					if (!publishedAt.isEmpty()) {
						info.latestVersion = publishedAt.substring(0, 10).replace("-", ".");
					} else {
						info.latestVersion = "0.0.0";
					}
				}

				info.releaseNotes = extractJsonValue(jsonResponse, "body");
				if (info.releaseNotes.isEmpty()) {
					info.releaseNotes = "No release notes available.";
				}

				String browserDownloadUrl = extractJsonValue(jsonResponse, "browser_download_url");
				if (!browserDownloadUrl.isEmpty()) {
					info.downloadUrl = browserDownloadUrl;
				} else {
					info.downloadUrl = extractJsonValue(jsonResponse, "html_url");
				}

				return info;
			}

			@Override
			protected void done()
			{
				try
				{
					UpdateInfo info = get();

					if ((info == null) || (info.latestVersion == null) || info.latestVersion.isEmpty())
					{
						throw new Exception("Invalid response from GitHub API");
					}

					latestVersion = info.latestVersion;
					downloadUrl = info.downloadUrl;

					if (compareVersions(latestVersion, CURRENT_VERSION) > 0)
					{
						statusLabel.setText("✨ New version available!");
						statusLabel.setForeground(SUCCESS_COLOR);
						versionLabel.setText("Current: " + CURRENT_VERSION + " → Latest: " + latestVersion);
						releaseNotesArea.setText(info.releaseNotes);
						downloadButton.setEnabled(true);
						L2ExplorerDat.addLogConsole("Update available: " + latestVersion, true);
						Window parent = getOwner();
						if (parent instanceof L2ExplorerDat)
						{
							((L2ExplorerDat) parent).notifyUpdateAvailable();
						}
					}
					else
					{
						statusLabel.setText("✓ You're up to date!");
						statusLabel.setForeground(SUCCESS_COLOR);
						versionLabel.setText("Current version: " + CURRENT_VERSION);
						releaseNotesArea.setText("You are running the latest version of L2ExplorerDat.");
						L2ExplorerDat.addLogConsole("Software is up to date.", true);
					}
				}
				catch (Exception e)
				{
					statusLabel.setText("❌ Failed to check for updates");
					statusLabel.setForeground(new Color(239, 68, 68));
					releaseNotesArea.setText("Error: " + e.getMessage() + "\n\nPlease check your internet connection or try again later.");
					L2ExplorerDat.addLogConsole("Update check failed: " + e.getMessage(), false);
				}
			}
		}.execute();
	}

	private String getString(URL url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(5000);

		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Failed to fetch updates: HTTP " + conn.getResponseCode());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder response = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			response.append(line);
		}
		br.close();

        return response.toString();
	}

	@SuppressWarnings("unused")
	public static void checkInBackground(L2ExplorerDat mainApp) {
		// Cria uma instância temporária apenas para disparar o worker
		// mas não chama o setVisible(true)
		UpdaterDialog backgroundChecker = new UpdaterDialog(mainApp);
	}

	private int compareVersions(String v1, String v2)
	{
		try
		{
			// Remove caracteres não numéricos exceto pontos
			v1 = v1.replaceAll("[^0-9.]", "");
			v2 = v2.replaceAll("[^0-9.]", "");

			String[] parts1 = v1.split("\\.");
			String[] parts2 = v2.split("\\.");
			int length = Math.max(parts1.length, parts2.length);

			for (int i = 0; i < length; i++)
			{
				int num1 = 0;
				int num2 = 0;

				if ((i < parts1.length) && !parts1[i].isEmpty())
				{
					num1 = Integer.parseInt(parts1[i]);
				}
				if ((i < parts2.length) && !parts2[i].isEmpty())
				{
					num2 = Integer.parseInt(parts2[i]);
				}

				if (num1 != num2)
				{
					return Integer.compare(num1, num2);
				}
			}
		}
		catch (Exception e)
		{
			System.err.println("Error comparing versions: " + e.getMessage());
			return 0;
		}
		return 0;
	}

	private void openDownloadPage()
	{
		try
		{
			Desktop.getDesktop().browse(new URI(downloadUrl));
			L2ExplorerDat.addLogConsole("Opening download page: " + downloadUrl, true);
		}
		catch (Exception e)
		{
			L2ExplorerDat.addLogConsole("Failed to open browser: " + e.getMessage(), false);
		}
	}

	private String extractJsonValue(String json, String key)
	{
		try
		{
			Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
			Matcher matcher = pattern.matcher(json);
			if (matcher.find())
			{
				String value = matcher.group(1);
				value = value.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
				return value;
			}
		}
		catch (Exception e)
		{
			System.err.println("Error extracting JSON value for key '" + key + "': " + e.getMessage());
		}
		return "";
	}

	private static class UpdateInfo
	{
		String latestVersion;
		String releaseNotes;
		String downloadUrl;
	}

	public static void showUpdateDialog(JFrame parent)
	{
		SwingUtilities.invokeLater(() ->
		{
			UpdaterDialog dialog = new UpdaterDialog(parent);
			dialog.setVisible(true);
		});
	}
}