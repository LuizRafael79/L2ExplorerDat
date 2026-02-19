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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.l2explorer.util.I18nManager;

public class AboutDialog extends JDialog
{
	// private static final Logger LOGGER = Logger.getLogger(AboutDialog.class.getName());

	private static final Color PRIMARY_BG = new Color(41, 49, 52);
	private static final Color SECONDARY_BG = new Color(30, 36, 40);
	private static final Color ACCENT_COLOR = new Color(0, 173, 181);
	private static final Color ACCENT_HOVER = new Color(0, 200, 209);
	private static final Color DONATION_COLOR = new Color(236, 72, 153);
	private static final Color DONATION_HOVER = new Color(249, 115, 181);
	private static final Color TEXT_PRIMARY = new Color(237, 242, 247);
	private static final Color TEXT_SECONDARY = new Color(160, 174, 192);

	// Componentes que precisam ser atualizados quando mudar idioma
	private JLabel titleLabel;
	private JLabel versionLabel;
	private JLabel descLabel;
	private JLabel donationTitle;
	private JLabel donationDesc;
	private JLabel socialLabel;
	private JLabel copyrightLabel;
	private JButton closeTopBtn;
	private JButton copyEmailBtn;
	private JButton githubBtn;
	private JButton sponsorBtn;
	private JButton updateBtn;
	private JButton closeButton;

	// Helper para i18n
	private String i18n(String key)
	{
		return I18nManager.getInstance().getString(key);
	}

	public AboutDialog(JFrame parent)
	{
		super(parent, I18nManager.getInstance().getString("about.title"), true);
		initComponents();
	}

	private void initComponents()
	{
		setLayout(new BorderLayout());
		setUndecorated(true);
		setShape(new RoundRectangle2D.Double(0, 0, 480, 680, 20, 20));

		JPanel mainPanel = getJPanel();

		JPanel topPanel = createTopPanel();
		JPanel headerPanel = createHeaderPanel();
		JPanel donationPanel = createDonationPanel();
		JPanel socialPanel = createSocialPanel();
		JPanel footerPanel = createFooterPanel();

		mainPanel.add(topPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		mainPanel.add(headerPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 25)));
		mainPanel.add(donationPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 25)));
		mainPanel.add(socialPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 25)));
		mainPanel.add(footerPanel);

		add(mainPanel, BorderLayout.CENTER);
		pack();
		setSize(480, 680);
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
		mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 40, 30, 40));
		return mainPanel;
	}

	private JPanel createTopPanel()
	{
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setOpaque(false);
		topPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

		closeTopBtn = new JButton("X")
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
		closeTopBtn.setPreferredSize(new Dimension(36, 36));
		closeTopBtn.setContentAreaFilled(false);
		closeTopBtn.setBorderPainted(false);
		closeTopBtn.setFocusPainted(false);
		closeTopBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		closeTopBtn.setToolTipText(i18n("about.close"));
		closeTopBtn.addActionListener(e -> dispose());

		topPanel.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
		topPanel.add(closeTopBtn, BorderLayout.EAST);

		return topPanel;
	}

	private JPanel createHeaderPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);

		titleLabel = new JLabel(i18n("about.app.title"));
		titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
		titleLabel.setForeground(TEXT_PRIMARY);
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		versionLabel = new JLabel(i18n("about.version"));
		versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		versionLabel.setForeground(ACCENT_COLOR);
		versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		descLabel = new JLabel(formatHTML(i18n("about.description")));
		descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		descLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

		panel.add(titleLabel);
		panel.add(Box.createRigidArea(new Dimension(0, 5)));
		panel.add(versionLabel);
		panel.add(descLabel);

		return panel;
	}

	private JPanel createDonationPanel()
	{
		JPanel panel = getPanel();

		JLabel heartLabel = new JLabel("❤️");
		heartLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
		heartLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		donationTitle = new JLabel(i18n("about.donation.title"));
		donationTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
		donationTitle.setForeground(TEXT_PRIMARY);
		donationTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

		donationDesc = new JLabel(formatHTML(i18n("about.donation.description")));
		donationDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel emailLabel = new JLabel("luizramicco@gmail.com");
		emailLabel.setFont(new Font("Consolas", Font.BOLD, 14));
		emailLabel.setForeground(DONATION_COLOR);
		emailLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		emailLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

		copyEmailBtn = createModernButton(i18n("about.donation.copy.email"), DONATION_COLOR, DONATION_HOVER);
		copyEmailBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		Dimension bigBtn = new Dimension(280, 25); // Largura 250, Altura 50
		copyEmailBtn.setPreferredSize(bigBtn);
		copyEmailBtn.setMinimumSize(bigBtn);
		copyEmailBtn.setMaximumSize(bigBtn);
		copyEmailBtn.setToolTipText(i18n("about.donation.copy.tooltip"));
		copyEmailBtn.addActionListener(e ->
		{
			StringSelection selection = new StringSelection(emailLabel.getText());
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
			L2ExplorerDat.addLogConsole(i18n("about.donation.copy.success"), true);
			copyEmailBtn.setText(i18n("about.donation.copy.copied"));
			new Thread(() ->
			{
				try
				{
					Thread.sleep(2000);
					copyEmailBtn.setText(i18n("about.donation.copy.email"));
				}
				catch (InterruptedException ignored)
				{
				}
			}).start();
		});

		panel.add(heartLabel);
		panel.add(Box.createRigidArea(new Dimension(0, 10)));
		panel.add(donationTitle);
		panel.add(Box.createRigidArea(new Dimension(0, 10)));
		panel.add(donationDesc);
		panel.add(Box.createRigidArea(new Dimension(0, 15)));
		panel.add(emailLabel);
		panel.add(copyEmailBtn);

		return panel;
	}

	private static JPanel getPanel() {
		JPanel panel = new JPanel()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				GradientPaint gp = new GradientPaint(0, 0, new Color(236, 72, 153, 30), getWidth(), getHeight(), new Color(168, 85, 247, 30));
				g2d.setPaint(gp);
				g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
				g2d.setColor(new Color(236, 72, 153, 80));
				g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
			}
		};
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
		panel.setOpaque(false);
		panel.setMaximumSize(new Dimension(400, 280));
		return panel;
	}

	private JPanel createSocialPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);

		socialLabel = new JLabel(i18n("about.social.title"));
		socialLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
		socialLabel.setForeground(TEXT_PRIMARY);
		socialLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
		buttonsPanel.setOpaque(false);
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

		githubBtn = createModernButton(i18n("about.social.github"), ACCENT_COLOR, ACCENT_HOVER);
		githubBtn.addActionListener(e ->
		{
			L2ExplorerDat.addLogConsole(i18n("about.social.github.opening"), true);
			openURL("https://github.com/LuizRafael79/L2ExplorerDat");
		});

		sponsorBtn = createModernButton(i18n("about.social.sponsor"), new Color(219, 97, 162), new Color(236, 72, 153));
		sponsorBtn.addActionListener(e ->
		{
			L2ExplorerDat.addLogConsole(i18n("about.social.sponsor.opening"), true);
			openURL("https://github.com/sponsors/LuizRafael79");
		});

		updateBtn = createModernButton(i18n("about.social.updates"), new Color(59, 130, 246), new Color(37, 99, 235));
		updateBtn.addActionListener(e ->
		{
			L2ExplorerDat.addLogConsole(i18n("about.social.updates.checking"), true);
			UpdaterDialog.showUpdateDialog((JFrame) getOwner());
			XmlUpdaterDialog.checkInBackground((L2ExplorerDat) getOwner());
		});

		buttonsPanel.add(Box.createHorizontalGlue());
		buttonsPanel.add(githubBtn);
		buttonsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonsPanel.add(updateBtn);
		buttonsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonsPanel.add(sponsorBtn);
		buttonsPanel.add(Box.createHorizontalGlue());

		panel.add(socialLabel);
		panel.add(buttonsPanel);

		return panel;
	}

	private JPanel createFooterPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);

		copyrightLabel = new JLabel(i18n("about.copyright"));
		copyrightLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		copyrightLabel.setForeground(TEXT_SECONDARY);
		copyrightLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		closeButton = createModernButton(i18n("about.close.btn"), new Color(71, 85, 105), new Color(100, 116, 139));
		closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		closeButton.addActionListener(e -> dispose());

		panel.add(copyrightLabel);
		panel.add(Box.createRigidArea(new Dimension(0, 15)));
		panel.add(closeButton);

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
					bg = bgColor.darker();
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

				g2d.setColor(getForeground());
				g2d.setFont(getFont());

				int stringWidth = g2d.getFontMetrics().stringWidth(getText());
				int stringHeight = g2d.getFontMetrics().getAscent();
				g2d.drawString(getText(), (getWidth() - stringWidth) / 2, ((getHeight() + stringHeight) / 2) - 2);
			}
		};

		button.setFont(new Font("Segoe UI", Font.BOLD, 13));
		button.setForeground(Color.WHITE);
		button.setPreferredSize(new Dimension(180, 40));
		button.setMaximumSize(new Dimension(180, 40));
		button.setContentAreaFilled(false);
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));

		return button;
	}

	/**
	 * Formata string HTML para JLabel com texto centralizado
	 */
	private String formatHTML(String text)
	{
		return "<html><body style='text-align: center; width: 100%; color: #CBD5E0;'>" + text + "</body></html>";
	}

	private void openURL(String url)
	{
		try
		{
			Desktop.getDesktop().browse(new URI(url));
		}
		catch (Exception ignored)
		{
		}
	}

	/**
	 * Atualiza todos os textos quando o idioma muda.
	 * Chame este método quando trocar o idioma.
	 */
	public void updateLanguage()
	{
		setTitle(i18n("about.title"));
		titleLabel.setText(i18n("about.app.title"));
		versionLabel.setText(i18n("about.version"));
		descLabel.setText(formatHTML(i18n("about.description")));
		donationTitle.setText(i18n("about.donation.title"));
		donationDesc.setText(formatHTML(i18n("about.donation.description")));
		socialLabel.setText(i18n("about.social.title"));
		copyrightLabel.setText(i18n("about.copyright"));

		closeTopBtn.setToolTipText(i18n("about.close"));
		copyEmailBtn.setText(i18n("about.donation.copy.email"));
		copyEmailBtn.setToolTipText(i18n("about.donation.copy.tooltip"));
		githubBtn.setText(i18n("about.social.github"));
		sponsorBtn.setText(i18n("about.social.sponsor"));
		updateBtn.setText(i18n("about.social.updates"));
		closeButton.setText(i18n("about.close.btn"));

		revalidate();
		repaint();
	}
}