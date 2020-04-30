/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib.configurator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadware.komodo.baselib.PathNames;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages configuration among various domains.
 * Implemented as a singleton such that it can be easily accessed from unrelated other modules.
 * Currently we read/write configuration as JSON objects in domain-specific files which are
 * stored in the config directory (see PathNames class).
 */
public final class Configurator {

    public enum Domain {
        KOMODO_OPERATING_SYSTEM(null, "kexec.json"),    //  TODO
        KOMODO_HARDWARE(HardwareConfiguration.class, "hardware.json"),
        KOMODO_SYSTEM_CONTROL_INTERFACE(SoftwareConfiguration.class, "software.json");

        public final Class<?> _class;
        public final String _jsonFileName;

        Domain(final Class<?> clazz,
               final String jsonFileName) {
            _class = clazz;
            _jsonFileName = jsonFileName;
        }
    }

    private static Configurator _instance = null;
    private static Logger LOGGER = LogManager.getLogger(Configurator.class.getSimpleName());
    private Map<Domain, Configuration> _cachedConfigurations = new HashMap<>();


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static Configurator getInstance() {
        synchronized (Configurator.class) {
            if (_instance == null) {
                _instance = new Configurator();
            }
        }
        return _instance;
    }


    public Configuration getConfiguration(
        final Domain domain
    ) throws IOException {
        synchronized (Configurator.class) {
            if (!_cachedConfigurations.containsKey(domain)) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String fullFileName = PathNames.CONFIG_ROOT_DIRECTORY + domain._jsonFileName;
                    _cachedConfigurations.put(domain, (Configuration) mapper.readValue(new File(fullFileName), domain._class));
                } catch (IOException ex) {
                    LOGGER.catching(ex);
                    throw ex;
                }
            }

            return _cachedConfigurations.get(domain);
        }
    }

    public void saveConfiguration(
        final Domain domain
    ) throws IOException {
        synchronized (Configurator.class) {
            if (_cachedConfigurations.containsKey(domain)) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String fullFileName = PathNames.CONFIG_ROOT_DIRECTORY + domain._jsonFileName;
                    mapper.writeValue(new File(fullFileName), _cachedConfigurations.get(domain));
                } catch (IOException ex) {
                    LOGGER.catching(ex);
                    throw ex;
                }
            }
        }
    }
}
