/*
 * This file is part of the L2ExplorerDat project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2explorer.util;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gerenciador de internacionalização (i18n) para a aplicação.
 * Centraliza o acesso às strings traduzidas e detecta automaticamente idiomas disponíveis.
 */
public class I18nManager {
    private static final Logger LOGGER = Logger.getLogger(I18nManager.class.getName());
    private static final String BUNDLE_NAME = "messages";

    private static I18nManager instance;
    private ResourceBundle bundle;
    private Locale currentLocale;
    private final Map<String, Locale> availableLanguages = new LinkedHashMap<>();

    private I18nManager() {
        detectAvailableLanguages();
    }

    public static I18nManager getInstance() {
        if (instance == null) {
            instance = new I18nManager();
        }
        return instance;
    }

    /**
     * Detecta automaticamente todos os idiomas disponíveis procurando por arquivos messages_*.properties
     */
    private void detectAvailableLanguages() {
        // Sempre adiciona inglês como padrão
        availableLanguages.put("English", Locale.ENGLISH);

        try {
            // Tenta detectar de onde estamos rodando (JAR ou IDE)
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resourceUrl = classLoader.getResource(BUNDLE_NAME + ".properties");

            if (resourceUrl == null) {
                LOGGER.warning("Could not find base messages.properties file");
                return;
            }

            String protocol = resourceUrl.getProtocol();

            if ("file".equals(protocol)) {
                // Rodando de IDE ou pasta descompactada
                detectFromFileSystem(resourceUrl);
            } else if ("jar".equals(protocol)) {
                // Rodando de JAR
                detectFromJar(resourceUrl);
            }

            LOGGER.info("Detected " + availableLanguages.size() + " available languages: " + availableLanguages.keySet());

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error detecting available languages", e);
        }
    }

    /**
     * Detecta idiomas disponíveis quando rodando de filesystem (IDE)
     */
    private void detectFromFileSystem(URL resourceUrl) {
        try {
            File resourceFile = new File(resourceUrl.toURI());
            File resourceDir = resourceFile.getParentFile();

            if (resourceDir == null || !resourceDir.exists()) {
                return;
            }

            File[] files = resourceDir.listFiles((dir, name) ->
                    name.startsWith(BUNDLE_NAME + "_") && name.endsWith(".properties")
            );

            if (files != null) {
                for (File file : files) {
                    processLanguageFile(file.getName());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error detecting languages from filesystem", e);
        }
    }

    /**
     * Detecta idiomas disponíveis quando rodando de JAR
     */
    private void detectFromJar(URL resourceUrl) {
        try {
            String jarPath = resourceUrl.getPath().substring(5, resourceUrl.getPath().indexOf("!"));

            try (JarFile jarFile = new JarFile(jarPath)) {
                Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();

                    // Procura por messages_*.properties
                    if (name.contains(BUNDLE_NAME + "_") && name.endsWith(".properties")) {
                        String fileName = name.substring(name.lastIndexOf('/') + 1);
                        processLanguageFile(fileName);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error detecting languages from JAR", e);
        }
    }

    /**
     * Processa um arquivo de idioma e adiciona à lista de disponíveis
     */
    private void processLanguageFile(String fileName) {
        try {
            // Extrai o código do locale do nome do arquivo (ex: messages_pt_BR.properties -> pt_BR)
            String localeCode = fileName.substring(BUNDLE_NAME.length() + 1, fileName.lastIndexOf(".properties"));

            Locale locale = Locale.forLanguageTag(localeCode.replace('_', '-'));

            // Tenta carregar o bundle pra verificar se tem a chave de display name
            ResourceBundle testBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);

            String displayName;
            if (testBundle.containsKey("language.display.name")) {
                displayName = testBundle.getString("language.display.name");
            } else {
                // Fallback para o nome do locale
                displayName = locale.getDisplayLanguage(locale);
                if (displayName.isEmpty()) {
                    displayName = locale.getDisplayLanguage(Locale.ENGLISH);
                }
                // Capitalize primeira letra
                displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
            }

            availableLanguages.put(displayName, locale);
            LOGGER.fine("Detected language: " + displayName + " (" + localeCode + ")");

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing language file: " + fileName, e);
        }
    }

    /**
     * Retorna um array com os nomes de display dos idiomas disponíveis
     */
    public String[] getAvailableLanguageNames() {
        return availableLanguages.keySet().toArray(new String[0]);
    }

    /**
     * Retorna o mapa completo de idiomas disponíveis
     */
    public Map<String, Locale> getAvailableLanguages() {
        return new LinkedHashMap<>(availableLanguages);
    }

    /**
     * Obtém o Locale a partir do nome de display
     */
    public Locale getLocaleByDisplayName(String displayName) {
        return availableLanguages.get(displayName);
    }

    /**
     * Obtém o nome de display a partir do Locale
     */
    public String getDisplayNameByLocale(Locale locale) {
        for (Map.Entry<String, Locale> entry : availableLanguages.entrySet()) {
            if (entry.getValue().equals(locale)) {
                return entry.getKey();
            }
        }
        return "English"; // fallback
    }

    /**
     * Inicializa o gerenciador com um locale específico
     */
    public void initialize(Locale locale) {
        setLocale(locale);
    }

    /**
     * Define o locale atual e recarrega o bundle
     */
    public void setLocale(Locale locale) {
        try {
            this.currentLocale = locale;
            this.bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
            LOGGER.log(Level.INFO, "Locale set to: " + locale.getDisplayLanguage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load resource bundle for locale: " + locale, e);
            // Fallback para inglês
            this.currentLocale = Locale.ENGLISH;
            this.bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);
        }
    }

    /**
     * Obtém uma string traduzida pela chave
     */
    public String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Missing translation key: " + key, e);
            return "!" + key + "!"; // Retorna a chave com ! para facilitar debug
        }
    }

    /**
     * Obtém uma string traduzida com parâmetros formatados
     */
    public String getString(String key, Object... params) {
        try {
            String pattern = bundle.getString(key);
            return String.format(pattern, params);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Missing translation key: " + key, e);
            return "!" + key + "!";
        }
    }

    /**
     * Retorna o locale atual
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Verifica se uma chave existe no bundle
     */
    public boolean hasKey(String key) {
        try {
            bundle.getString(key);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}