package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Config;
import org.springframework.stereotype.Service;

@Service
public class ConfigService {
    private final ConfigRepository configRepository;

    public ConfigService(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public String get(Config.Key key) {
        return configRepository.findById(key.toString()).map(Config::getValue).orElse("");
    }

    public int getInt(Config.Key key) {
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public long getLong(Config.Key key) {
        try {
            return Long.parseLong(get(key));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public boolean getBoolean(Config.Key key) {
        return get(key).equals("true");
    }

    public void save(Config.Key key, Object value) {
        Config config = configRepository.findById(key.toString()).orElseGet(() -> {
            Config c = new Config();
            c.setId(key.toString());
            return c;
        });
        config.setValue(String.valueOf(value));
        configRepository.save(config);
    }
}